package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Cuerpo que PharmaSoft exige al crear (POST) o actualizar (PUT) un producto:
 * ProductoRequestDTO. Los cinco campos son obligatorios en el backend; si falta
 * alguno responde 400 con el detalle por campo.
 */
@Serializable
data class ProductoRequestDto(
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val estado: Boolean = true,
    val categoriaId: Long
)

/**
 * Respuesta de PharmaSoft: ProductoResponseDTO. Las fechas de creacion y
 * modificacion no se declaran: ignoreUnknownKeys las descarta.
 */
@Serializable
data class ProductoResponseDto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val estado: Boolean = true,
    val categoriaId: Long? = null,
    val categoriaNombre: String? = null
)
