package pe.edu.upeu.pharmamobil.platform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/** Android: intercepta el boton (o gesto) Atras con BackHandler de androidx.activity. */
@Composable
actual fun AlPulsarAtras(habilitado: Boolean, alVolver: () -> Unit) {
    BackHandler(enabled = habilitado, onBack = alVolver)
}
