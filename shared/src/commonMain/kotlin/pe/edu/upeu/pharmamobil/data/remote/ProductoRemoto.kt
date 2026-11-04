package pe.edu.upeu.pharmamobil.data.remote

import pe.edu.upeu.pharmamobil.data.mapper.toDomain
import pe.edu.upeu.pharmamobil.data.mapper.toRequest
import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Fuente remota del inventario (PharmaSoft). Hasta la sesion 10 era el repositorio
 * (ProductoRepositorioRest); desde la sesion 11 la interfaz ya no lee de aqui: lee de
 * la base local, y ProductoRepositorioOffline usa esta clase solo para sincronizar.
 *
 * Toda llamada pasa por ejecutarLlamada: si algo falla, lo que sale de aqui
 * es siempre un ErrorApiException (dominio), nunca una excepcion de Ktor.
 *
 * @param categoriaPorDefecto categoria que se envia al crear un producto: el
 * formulario de la app aun no la pide y el backend la exige.
 */
class ProductoRemoto(
    private val api: ProductoApi,
    private val categoriaPorDefecto: Long
) {

    /** Inventario completo del servidor, incluidos los dados de baja (activo = false). */
    suspend fun listar(): List<Producto> = ejecutarLlamada {
        api.listar(tamanio = TAMANIO_SINCRONIZACION).contenido
            // Un registro que rompe las reglas del dominio (p. ej. precio 0)
            // se descarta en vez de tumbar toda la lista.
            .mapNotNull { dto -> runCatching { dto.toDomain() }.getOrNull() }
    }.getOrThrow()

    suspend fun obtener(id: Long): Producto = ejecutarLlamada {
        api.obtener(id).toDomain()
    }.getOrThrow()

    /** POST: el servidor asigna el id real (el local negativo no viaja: el DTO no lleva id). */
    suspend fun registrar(producto: Producto): Producto = ejecutarLlamada {
        api.crear(producto.toRequest(categoriaPorDefecto)).toDomain()
    }.getOrThrow()

    suspend fun actualizar(producto: Producto): Producto = ejecutarLlamada {
        api.actualizar(producto.id, producto.toRequest(categoriaPorDefecto)).toDomain()
    }.getOrThrow()

    suspend fun eliminar(id: Long) = ejecutarLlamada {
        api.eliminar(id)
    }.getOrThrow()

    private companion object {
        /** Una sola pagina con todo el inventario de la botica (PharmaSoft pagina de 20 en 20). */
        const val TAMANIO_SINCRONIZACION = 100
    }
}
