package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Envoltorio paginado de PharmaSoft (PaginaResponseDTO). El GET de listado
 * NO devuelve un arreglo: los productos llegan dentro de "contenido".
 */
@Serializable
data class PaginaResponseDto<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean
)
