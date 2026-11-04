package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Cuerpo de error de PharmaSoft (ErrorResponseDTO). En un 400 de validacion,
 * validationErrors trae el mensaje por campo: {"nombre": "...", "precio": "..."}.
 */
@Serializable
data class ErrorResponseDto(
    val status: Int,
    val error: String,
    val message: String,
    val validationErrors: Map<String, String>? = null
)
