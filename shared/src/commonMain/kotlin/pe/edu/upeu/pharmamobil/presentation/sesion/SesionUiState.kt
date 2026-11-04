package pe.edu.upeu.pharmamobil.presentation.sesion

import pe.edu.upeu.pharmamobil.domain.model.Sesion

/**
 * Estado de la sesion. El composable no conoce el token: solo sabe si hay
 * sesion (y que permite) o si hay que mostrar el login.
 *
 * @param verificando true mientras se comprueba el token guardado al arrancar.
 * @param aviso mensaje informativo en el login (sesion expirada, sesion cerrada).
 */
data class SesionUiState(
    val usuario: String = "",
    val clave: String = "",
    val usuarioError: String? = null,
    val claveError: String? = null,
    val cargando: Boolean = false,
    val error: String? = null,
    val aviso: String? = null,
    val verificando: Boolean = true,
    val sesion: Sesion? = null
)
