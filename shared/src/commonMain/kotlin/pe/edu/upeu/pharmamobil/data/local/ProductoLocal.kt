package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.db.Producto as FilaProducto

/**
 * Sesion 11: capa local del inventario. Concentra las consultas de Producto.sq y
 * traduce entre las filas generadas por SQLDelight y el modelo de dominio.
 *
 * No conoce Ktor ni los DTO del backend (punto de control 3): lo que viene del
 * servidor llega aqui ya convertido en Producto.
 *
 * Las escrituras van fuera del hilo principal (SQLite hace E/S de disco).
 */
class ProductoLocal(base: PharmaDb) {

    private val consultas = base.productoQueries

    /** La pantalla se suscribe aqui: emite de nuevo cada vez que la tabla cambia. */
    fun observar(): Flow<List<Producto>> =
        consultas.listarTodos()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { filas -> filas.map { it.aDominio() } }

    suspend fun pendientes(): List<Producto> = enDisco {
        consultas.listarPendientes().executeAsList().map { it.aDominio() }
    }

    /** Sesion 13: la cola de envio; deja fuera las filas que agotaron sus intentos. */
    suspend fun porEnviar(maxIntentos: Int): List<Producto> = enDisco {
        consultas.pendientesPorEnviar(maxIntentos.toLong()).executeAsList().map { it.aDominio() }
    }

    suspend fun contarPendientes(maxIntentos: Int): Long = enDisco {
        consultas.contarPendientes(maxIntentos.toLong()).executeAsOne()
    }

    suspend fun contarFallidas(maxIntentos: Int): Long = enDisco {
        consultas.contarFallidas(maxIntentos.toLong()).executeAsOne()
    }

    /** Fallo pasajero: un intento mas y el motivo, para mostrarlo si se agota. */
    suspend fun sumarIntento(id: Long, motivo: String) { enDisco { consultas.sumarIntento(motivo, id) } }

    /** Fallo permanente: la fila queda fallida de una vez, con el motivo del servidor. */
    suspend fun marcarFallida(id: Long, motivo: String, maxIntentos: Int) {
        enDisco { consultas.marcarFallida(maxIntentos.toLong(), motivo, id) }
    }

    /** «Reintentar»: las fallidas vuelven a la cola con los intentos en cero. */
    suspend fun reintentarFallidas(maxIntentos: Int) { enDisco { consultas.reintentarFallidas(maxIntentos.toLong()) } }

    suspend fun obtener(id: Long): Producto? = enDisco {
        consultas.obtenerPorId(id).executeAsOneOrNull()?.aDominio()
    }

    /**
     * Guarda lo que el usuario creo sin conexion. El id es local y NEGATIVO: el backend
     * nunca usa esos numeros, asi que no puede chocar con un producto real.
     */
    suspend fun guardarPendiente(producto: Producto): Producto = enDisco {
        consultas.transactionWithResult {
            val menor = consultas.siguienteIdLocal().executeAsOne()
            val local = producto.copy(id = minOf(menor, 0L) - 1L, pendiente = true, intentos = 0, ultimoError = null)
            insertar(local)
            local
        }
    }

    /**
     * Cambia un producto que todavia no llego al servidor: sigue pendiente y con su id local.
     * Sesion 13: si estaba fallido, corregirlo lo devuelve a la cola con los intentos en cero.
     */
    suspend fun actualizarPendiente(producto: Producto): Producto = enDisco {
        val local = producto.copy(pendiente = true, intentos = 0, ultimoError = null)
        insertar(local)
        local
    }

    /**
     * Recibe la lista del servidor. Borra SOLO lo sincronizado (pendiente = 0): lo que el
     * usuario creo sin red se conserva. Todo va en una transaccion: el Flow emite una vez
     * y la lista no parpadea mientras se reconstruye.
     */
    suspend fun reemplazarSincronizados(productos: List<Producto>) = enDisco {
        consultas.transaction {
            consultas.borrarSincronizados()
            productos.forEach { insertar(it.copy(pendiente = false, intentos = 0, ultimoError = null)) }
        }
    }

    /** El servidor acepto un pendiente: la fila local (id negativo) se cambia por la real. */
    suspend fun confirmarSubida(idLocal: Long, delServidor: Producto) = enDisco {
        consultas.transaction {
            consultas.borrarPorId(idLocal)
            insertar(delServidor.copy(pendiente = false, intentos = 0, ultimoError = null))
        }
    }

    /** Guarda la version que devolvio el servidor tras editar o reactivar. */
    suspend fun guardarSincronizado(producto: Producto) = enDisco {
        insertar(producto.copy(pendiente = false, intentos = 0, ultimoError = null))
    }

    /** PharmaSoft no borra: desactiva. Se refleja igual en la copia local. */
    suspend fun marcarDeBaja(id: Long) { enDisco { consultas.marcarDeBaja(id) } }

    /** Un pendiente que el usuario elimina antes de sincronizar nunca llega al servidor. */
    suspend fun borrar(id: Long) { enDisco { consultas.borrarPorId(id) } }

    private fun insertar(p: Producto) {
        consultas.insertar(
            p.id, p.nombre, p.precio, p.stock.toLong(), p.categoriaId,
            if (p.activo) 1L else 0L, if (p.pendiente) 1L else 0L,
            p.intentos.toLong(), p.ultimoError
        )
    }

    private suspend fun <T> enDisco(bloque: () -> T): T = withContext(Dispatchers.IO) { bloque() }
}

private fun FilaProducto.aDominio() = Producto(
    id = id,
    nombre = nombre,
    precio = precio,
    stock = stock.toInt(),
    categoriaId = categoria_id,
    activo = activo == 1L,
    pendiente = pendiente == 1L,
    intentos = intentos.toInt(),
    ultimoError = ultimo_error
)
