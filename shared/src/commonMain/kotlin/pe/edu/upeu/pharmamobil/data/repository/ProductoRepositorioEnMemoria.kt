package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import pe.edu.upeu.pharmamobil.domain.sync.ResumenSync
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Almacenamiento en memoria de productos. El id lo asigna el repositorio,
 * no la pantalla, para evitar identificadores duplicados.
 *
 * Desde la sesion 8 Koin inyecta otra implementacion (REST y, desde la 11,
 * ProductoRepositorioOffline); esta se conserva como alternativa sin red.
 * Sesion 11: la lista vive en un StateFlow, asi observar() emite con cada cambio.
 *
 * El [Mutex] protege el contador de ids de dos registros que se crucen.
 */
class ProductoRepositorioEnMemoria : ProductoRepository {

    private val candado = Mutex()
    private val productos = MutableStateFlow<List<Producto>>(emptyList())
    private var siguienteId = 1L

    override fun observar(): Flow<List<Producto>> = productos.asStateFlow()

    /** Sin servidor no hay nada que sincronizar: nunca. */
    override fun ultimaSincronizacion(): Flow<Long?> = flowOf(null)

    /** No hay servidor: la memoria ya es la fuente de verdad. */
    override suspend fun sincronizar() = ResumenSync(0, 0, 0, 0)

    override suspend fun reintentarFallidas() = ResumenSync(0, 0, 0, 0)

    override suspend fun registrar(producto: Producto): Producto {
        delay(RETARDO_REGISTRO_MS)
        return candado.withLock {
            val guardado = producto.copy(id = siguienteId++)
            productos.update { it + guardado }
            guardado
        }
    }

    override suspend fun obtener(id: Long): Producto =
        productos.value.firstOrNull { it.id == id } ?: throw ErrorApiException(ErrorApi.NoEncontrado)

    override suspend fun actualizar(producto: Producto): Producto {
        delay(RETARDO_REGISTRO_MS)
        return candado.withLock {
            if (productos.value.none { it.id == producto.id }) throw ErrorApiException(ErrorApi.NoEncontrado)
            productos.update { lista -> lista.map { if (it.id == producto.id) producto else it } }
            producto
        }
    }

    override suspend fun eliminar(id: Long) {
        delay(RETARDO_REGISTRO_MS)
        candado.withLock {
            if (productos.value.none { it.id == id }) throw ErrorApiException(ErrorApi.NoEncontrado)
            productos.update { lista -> lista.filterNot { it.id == id } }
        }
    }

    private companion object {
        const val RETARDO_REGISTRO_MS = 400L
    }
}
