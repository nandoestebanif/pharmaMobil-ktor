package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import pe.edu.upeu.pharmamobil.data.local.MarcaDeSincronizacion
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.ProductoRemoto
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.platform.Reloj
import pe.edu.upeu.pharmamobil.domain.platform.RelojDelSistema
import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.sync.PoliticaDeReintento
import pe.edu.upeu.pharmamobil.domain.sync.PoliticaDeReintento.MAX_INTENTOS
import pe.edu.upeu.pharmamobil.domain.sync.ResultadoEnvio
import pe.edu.upeu.pharmamobil.domain.sync.ResumenSync

/**
 * Sesion 11: repositorio offline-first. La fuente de verdad es la base local
 * ([ProductoLocal]); [ProductoRemoto] (PharmaSoft) solo sirve para sincronizar.
 *
 * - Leer: siempre de la base local, con o sin red.
 * - Crear: primero en local (pendiente) y despues se intenta subir.
 * - Editar, dar de baja y reactivar necesitan al servidor (es quien valida
 *   nombres duplicados y permisos); el resultado se guarda en local y el Flow emite.
 */
class ProductoRepositorioOffline(
    private val local: ProductoLocal,
    private val remoto: ProductoRemoto,
    /** Actividad autonoma 11: donde queda la hora de la ultima sincronizacion correcta. */
    private val marca: MarcaDeSincronizacion? = null,
    private val reloj: Reloj = RelojDelSistema,
    /** Sesion 13: espera entre reintentos (en las pruebas no espera de verdad). */
    private val esperar: suspend (Long) -> Unit = { delay(it) },
    /** Sesion 13: errores y eventos hacia Firebase Crashlytics (o a la consola en iOS). */
    private val telemetria: Telemetria? = null
) : ProductoRepository {

    override fun observar(): Flow<List<Producto>> = local.observar()

    override fun ultimaSincronizacion(): Flow<Long?> = marca?.observar() ?: flowOf(null)

    override suspend fun registrar(producto: Producto): Producto {
        // 1. Primero en local: desde aqui el producto ya aparece en la lista (pendiente).
        val guardado = local.guardarPendiente(producto)
        return try {
            // 2. Con red se sube de inmediato y cambia su id local por el real.
            val creado = remoto.registrar(guardado)
            local.confirmarSubida(guardado.id, creado)
            // 3. Se aprovecha la conexion para subir otros pendientes y bajar la lista.
            intentar { sincronizar() }
            creado
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (e: ErrorApiException) {
            if (e.error.esFaltaDeRed()) {
                // Sin red no pasa nada: queda pendiente y se sube al sincronizar.
                guardado
            } else {
                // El servidor lo rechazo (nombre duplicado, datos invalidos...): no se guarda,
                // igual que antes de la sesion 11, y el formulario muestra el error.
                local.borrar(guardado.id)
                throw e
            }
        }
    }

    /**
     * Primero subir, despues bajar. Al reves, la bajada borraria lo creado sin red.
     *
     * Sesion 13: cada fila tiene un limite de intentos. Un fallo transitorio suma un intento
     * y espera cada vez mas antes de seguir; uno permanente marca la fila como fallida (ya no
     * se reintenta sola); sin red o con la sesion vencida se detiene sin tocar las filas.
     */
    override suspend fun sincronizar(): ResumenSync {
        var enviadas = 0
        var nuevasFallidas = 0
        for (fila in local.porEnviar(MAX_INTENTOS)) {
            when (val r = enviar(fila)) {
                is ResultadoEnvio.Exito -> { local.confirmarSubida(fila.id, r.delServidor); enviadas++ }
                is ResultadoEnvio.Permanente -> {
                    local.marcarFallida(fila.id, r.motivo, MAX_INTENTOS); nuevasFallidas++
                    telemetria?.registrarEvento("envio_rechazado", mapOf("motivo" to r.motivo))
                }
                is ResultadoEnvio.Transitorio -> {
                    local.sumarIntento(fila.id, r.motivo)
                    if (fila.intentos + 1 >= MAX_INTENTOS) nuevasFallidas++
                    esperar(PoliticaDeReintento.espera(fila.intentos))
                }
                // La peticion no llego a evaluarse: nada cambia en la fila. El ViewModel
                // muestra «Sin conexion» (o el plugin SesionVencida lleva al login).
                is ResultadoEnvio.Detener -> throw r.causa
            }
        }
        local.reemplazarSincronizados(remoto.listar())
        // La lista del servidor ya esta en la base: los datos son de este instante.
        marca?.registrar(reloj.ahoraMs())
        return ResumenSync(enviadas, nuevasFallidas, local.contarPendientes(MAX_INTENTOS), local.contarFallidas(MAX_INTENTOS))
            .also { if (it.enviadas + it.nuevasFallidas > 0) telemetria?.registrarEvento("sincronizacion", mapOf(
                "enviadas" to it.enviadas.toString(), "fallidas" to it.fallidas.toString())) }
    }

    override suspend fun reintentarFallidas(): ResumenSync {
        local.reintentarFallidas(MAX_INTENTOS)
        return sincronizar()
    }

    /** Envia una fila y traduce el resultado: aqui, y solo aqui, se miran los ErrorApi. */
    private suspend fun enviar(fila: Producto): ResultadoEnvio = try {
        ResultadoEnvio.Exito(remoto.registrar(fila))
    } catch (cancelacion: CancellationException) {
        throw cancelacion
    } catch (e: ErrorApiException) {
        val resultado = e.aResultado()
        // Creacion no idempotente: si un intento anterior se quedo sin respuesta (tiempo
        // agotado), el servidor pudo haberlo guardado igual. Al repetirlo responde 409 «ya
        // existe»: si en el servidor hay uno identico, era el mismo envio y no un duplicado.
        if (e.error is ErrorApi.Conflicto && fila.intentos > 0) mismoEnvio(fila)?.let { ResultadoEnvio.Exito(it) } ?: resultado
        else resultado
    }

    private suspend fun mismoEnvio(fila: Producto): Producto? =
        try {
            remoto.listar().firstOrNull {
                it.nombre.equals(fila.nombre, ignoreCase = true) && it.precio == fila.precio && it.stock == fila.stock
            }
        } catch (e: ErrorApiException) {
            null
        }

    override suspend fun obtener(id: Long): Producto {
        // Un pendiente solo existe en el dispositivo.
        if (id < 0) return local.obtener(id) ?: throw ErrorApiException(ErrorApi.NoEncontrado)
        return try {
            remoto.obtener(id).also { local.guardarSincronizado(it) }
        } catch (e: ErrorApiException) {
            // Sin red se muestra la copia local; cualquier otro error se informa.
            if (e.error.esFaltaDeRed()) local.obtener(id) ?: throw e else throw e
        }
    }

    override suspend fun actualizar(producto: Producto): Producto {
        if (producto.id < 0) return local.actualizarPendiente(producto)
        val guardado = conservandoLaListaAlDia { remoto.actualizar(producto) }
        local.guardarSincronizado(guardado)
        return guardado
    }

    override suspend fun eliminar(id: Long) {
        if (id < 0) return local.borrar(id)
        conservandoLaListaAlDia { remoto.eliminar(id) }
        local.marcarDeBaja(id)
    }

    /**
     * Un 404 o un 409 al editar significa que la copia local quedo vieja (otro usuario
     * cambio el producto): se sincroniza en segundo plano y se informa el error igual.
     */
    private suspend fun <T> conservandoLaListaAlDia(bloque: suspend () -> T): T =
        try {
            bloque()
        } catch (e: ErrorApiException) {
            if (e.error == ErrorApi.NoEncontrado || e.error is ErrorApi.Conflicto) intentar { sincronizar() }
            throw e
        }

    /** Un fallo de la sincronizacion no debe tumbar la operacion local que ya se hizo. */
    private suspend fun intentar(bloque: suspend () -> Unit) {
        try {
            bloque()
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (sinRed: ErrorApiException) {
            // Queda pendiente; se reintentara al sincronizar.
        }
    }
}

/** Sin red, servidor apagado o sin respuesta: el producto se queda en el dispositivo. */
internal fun ErrorApi.esFaltaDeRed(): Boolean =
    this == ErrorApi.SinConexion || this == ErrorApi.TiempoAgotado

/**
 * Sesion 13: de un error de la API a una de las ramas de [ResultadoEnvio]. Es la misma capa
 * donde la sesion 8 traduce las excepciones de Ktor; el dominio nunca ve un codigo HTTP.
 */
internal fun ErrorApiException.aResultado(): ResultadoEnvio = when (val e = error) {
    ErrorApi.TiempoAgotado -> ResultadoEnvio.Transitorio("El servidor no respondió a tiempo")
    ErrorApi.Servidor -> ResultadoEnvio.Transitorio("El servidor tuvo un problema")
    ErrorApi.SinConexion, ErrorApi.NoAutenticado -> ResultadoEnvio.Detener(this)
    is ErrorApi.Validacion -> ResultadoEnvio.Permanente(
        e.porCampo.values.firstOrNull() ?: "El servidor rechazó los datos")
    is ErrorApi.Conflicto -> ResultadoEnvio.Permanente(e.mensaje)
    ErrorApi.NoEncontrado -> ResultadoEnvio.Permanente("El recurso ya no existe en el servidor")
    ErrorApi.SinPermiso -> ResultadoEnvio.Permanente("Tu usuario no tiene permiso para registrar productos")
    ErrorApi.RespuestaInesperada -> ResultadoEnvio.Permanente("El servidor respondió algo inesperado")
}
