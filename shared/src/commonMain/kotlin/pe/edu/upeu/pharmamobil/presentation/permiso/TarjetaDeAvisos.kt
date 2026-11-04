package pe.edu.upeu.pharmamobil.presentation.permiso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso

/**
 * Sesion 13: los tres estados del permiso de notificaciones, cada uno con su texto y su
 * accion. Se vuelve a consultar al regresar a la app (por ejemplo, desde Ajustes).
 */
@Composable
fun TarjetaDeAvisos(viewModel: AvisosViewModel, modifier: Modifier = Modifier) {

    val estado by viewModel.estado.collectAsState()
    val avisoEnviado by viewModel.avisoEnviado.collectAsState()

    LifecycleResumeEffect(Unit) {
        viewModel.refrescar()
        onPauseOrDispose { }
    }

    val esquema = MaterialTheme.colorScheme
    val (icono, titulo, texto) = when (estado) {
        EstadoPermiso.Concedido -> Triple(
            Icons.Default.NotificationsActive, "Avisos activados",
            "Te avisaremos si un cambio hecho sin conexión no se puede enviar a PharmaSoft."
        )
        EstadoPermiso.DenegadoParaSiempre -> Triple(
            Icons.Default.NotificationsOff, "Avisos bloqueados",
            "Android ya no muestra la pregunta. Para recibir avisos, actívalos en Ajustes › Notificaciones."
        )
        else -> Triple(
            Icons.Default.NotificationsNone, "Avisos de sincronización",
            "Si registras productos sin conexión y alguno no se puede enviar, te avisaremos con una notificación. " +
                "Para eso, Android te pedirá permiso."
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (estado == EstadoPermiso.Concedido) esquema.secondaryContainer else esquema.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icono, contentDescription = null)
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(texto, style = MaterialTheme.typography.bodyMedium)
            when (estado) {
                EstadoPermiso.Denegado -> Button(onClick = viewModel::activar) { Text("Activar avisos") }
                EstadoPermiso.DenegadoParaSiempre -> OutlinedButton(onClick = viewModel::abrirAjustes) { Text("Abrir Ajustes") }
                EstadoPermiso.Concedido -> OutlinedButton(onClick = viewModel::probar) {
                    Text(if (avisoEnviado) "Aviso de prueba enviado" else "Enviar aviso de prueba")
                }
                null -> Unit
            }
        }
    }
}
