package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import pe.edu.upeu.pharmamobil.data.remote.dto.PaginaResponseDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoResponseDto

/**
 * Servicio remoto del recurso Producto de PharmaSoft (/api/v1/productos).
 * Solo habla HTTP y devuelve DTO: no conoce el dominio ni la interfaz.
 */
class ProductoApi(private val client: HttpClient) {

    /** GET productos?pagina=0&tamanio=20 -> 200 con el envoltorio de paginacion. */
    suspend fun listar(pagina: Int = 0, tamanio: Int = 20): PaginaResponseDto<ProductoResponseDto> =
        client.get("productos") {
            parameter("pagina", pagina)
            parameter("tamanio", tamanio)
        }.body()

    /** GET productos/{id} -> 200, o 404 si no existe. */
    suspend fun obtener(id: Long): ProductoResponseDto =
        client.get("productos/$id").body()

    /** POST productos -> 201 con el producto creado. */
    suspend fun crear(request: ProductoRequestDto): ProductoResponseDto =
        client.post("productos") { setBody(request) }.body()

    /** PUT productos/{id} -> 200 con el producto actualizado. */
    suspend fun actualizar(id: Long, request: ProductoRequestDto): ProductoResponseDto =
        client.put("productos/$id") { setBody(request) }.body()

    /** DELETE productos/{id} -> 204 sin cuerpo: NO se llama a body(). */
    suspend fun eliminar(id: Long) {
        client.delete("productos/$id")
    }
}
