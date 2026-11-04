package pe.edu.upeu.pharmamobil.platform

import androidx.compose.runtime.Composable

/**
 * Ejecuta [alVolver] cuando el usuario pulsa el boton Atras del sistema.
 *
 * Es un expect porque ese boton solo existe en Android. En iOS no hay un
 * equivalente: se vuelve con la flecha de la barra superior.
 */
@Composable
expect fun AlPulsarAtras(habilitado: Boolean, alVolver: () -> Unit)
