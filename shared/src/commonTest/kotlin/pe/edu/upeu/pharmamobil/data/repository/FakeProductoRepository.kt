package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.sync.ResumenSync
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Doble del inventario para las pruebas: sin delay y capaz de fallar a
 * voluntad. Sin el, probar el camino de error del caso de uso o del ViewModel
 * era imposible, porque el repositorio en memoria nunca falla.
 *
 * Sesion 11: hace de base local (un StateFlow que observar() expone) y de servidor
 * a la vez: [enServidor] es lo que sincronizar() «baja».
 */
class FakeProductoRepository(
    productos: MutableList<Producto> = mutableListOf()
) : ProductoRepository {

    /** Lo que la base local ya tiene al empezar la prueba (por defecto, nada). */
    private val local = MutableStateFlow<List<Producto>>(emptyList())

    /** Lo que devolveria el servidor. sincronizar() lo copia a la base local. */
    val enServidor: MutableList<Producto> = productos

    var fallaAlRegistrar: Throwable? = null
    var fallaAlSincronizar: Throwable? = null
    var fallaAlObtener: Throwable? = null
    var fallaAlActualizar: Throwable? = null
    var fallaAlEliminar: Throwable? = null

    /** Veces que se llamo a sincronizar(): el ViewModel no debe pedir la lista a mano. */
    var sincronizaciones = 0
        private set

    /**
     * Si se asigna, registrar/actualizar/eliminar esperan a que se complete:
     * permite observar el estado "operacion en curso" antes de que termine.
     */
    var compuerta: CompletableDeferred<Unit>? = null

    /** Igual que [compuerta], pero para sincronizar: permite observar la fase Cargando. */
    var compuertaAlSincronizar: CompletableDeferred<Unit>? = null

    private var siguienteId = (productos.maxOfOrNull { it.id } ?: 0L) + 1

    /** Simula una base local con datos de una sesion anterior (abrir la app sin red). */
    fun conBaseLocal(productos: List<Producto>) = apply { local.value = productos }

    override fun observar(): Flow<List<Producto>> = local.asStateFlow()

    /** Autonoma 11: instante de la ultima sincronizacion correcta (null = nunca). */
    val ultima = MutableStateFlow<Long?>(null)

    /** Instante que sincronizar() registra cuando termina bien. */
    var reloj: Long = 1_000L

    override fun ultimaSincronizacion(): Flow<Long?> = ultima.asStateFlow()

    /** Sesion 13: lo que devuelve sincronizar() cuando termina bien. */
    var resumen = ResumenSync(0, 0, 0, 0)
    var reintentos = 0

    override suspend fun sincronizar(): ResumenSync {
        sincronizaciones++
        compuertaAlSincronizar?.await()
        fallaAlSincronizar?.let { throw it }
        local.value = enServidor.toList()
        ultima.value = reloj
        return resumen
    }

    override suspend fun reintentarFallidas(): ResumenSync {
        reintentos++
        return sincronizar()
    }

    override suspend fun registrar(producto: Producto): Producto {

        compuerta?.await()
        fallaAlRegistrar?.let { throw it }

        val guardado = producto.copy(id = siguienteId++)
        enServidor.add(guardado)
        local.update { it + guardado }
        return guardado
    }

    override suspend fun obtener(id: Long): Producto {

        fallaAlObtener?.let { throw it }

        return enServidor.firstOrNull { it.id == id }
            ?: throw ErrorApiException(ErrorApi.NoEncontrado)
    }

    override suspend fun actualizar(producto: Producto): Producto {

        compuerta?.await()
        fallaAlActualizar?.let { throw it }

        val indice = enServidor.indexOfFirst { it.id == producto.id }
        if (indice < 0) throw ErrorApiException(ErrorApi.NoEncontrado)
        enServidor[indice] = producto
        local.update { lista -> lista.map { if (it.id == producto.id) producto else it } }
        return producto
    }

    override suspend fun eliminar(id: Long) {

        compuerta?.await()
        fallaAlEliminar?.let { throw it }

        if (!enServidor.removeAll { it.id == id }) throw ErrorApiException(ErrorApi.NoEncontrado)
        local.update { lista -> lista.filterNot { it.id == id } }
    }
}
