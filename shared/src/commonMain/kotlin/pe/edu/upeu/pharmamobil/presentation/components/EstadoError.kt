package pe.edu.upeu.pharmamobil.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Fase Error: la pantalla no puede mostrarse. Mensaje mas boton Reintentar. */
@Composable
fun EstadoError(
    titulo: String,
    mensaje: String,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {

    EstadoVacio(
        icono = Icons.Default.CloudOff,
        titulo = titulo,
        descripcion = mensaje,
        colorIcono = MaterialTheme.colorScheme.error,
        modifier = modifier,
        accion = {
            FilledTonalButton(onClick = onReintentar) {
                Text("Reintentar")
            }
        }
    )
}
