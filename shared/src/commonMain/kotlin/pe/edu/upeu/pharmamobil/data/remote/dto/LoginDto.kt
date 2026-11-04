package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable

/** POST auth/login: los nombres de los campos son los que espera PharmaSoft. */
@Serializable
data class LoginRequestDto(val username: String, val password: String)

/** Respuesta del login. El token viaja sin el prefijo "Bearer". */
@Serializable
data class LoginResponseDto(
    val token: String,
    val tipo: String,
    val expiraEnSegundos: Long,
    val username: String,
    val nombreCompleto: String,
    val roles: List<String> = emptyList(),
    val permisos: List<String> = emptyList()
)

/** GET auth/me: el usuario dueno del token. */
@Serializable
data class UsuarioActualDto(
    val username: String,
    val empleadoNombre: String? = null,
    val roles: List<String> = emptyList(),
    val permisos: List<String> = emptyList()
)
