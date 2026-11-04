package pe.edu.upeu.pharmamobil.presentation.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.pharmamobil.presentation.components.EstadoError
import pe.edu.upeu.pharmamobil.presentation.components.Etiqueta
import pe.edu.upeu.pharmamobil.presentation.components.IconoEnRecuadro
import pe.edu.upeu.pharmamobil.presentation.components.IndicadorCarga
import pe.edu.upeu.pharmamobil.presentation.components.Tarjeta

/**
 * Detalle de un producto. Se abre al tocar una fila del inventario y trae del
 * servidor la version vigente del producto.
 *
 * El composable no conoce la implementacion de compartir: solo invoca la
 * accion del ViewModel. Editar y eliminar los resuelve quien navega.
 */
@Composable
fun DetalleProductoScreen(
    productoId: Long,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetalleProductoViewModel = koinViewModel(),
    // Sesion 10: sin CATALOGO_ESCRIBIR solo se ofrece compartir.
    puedeEscribir: Boolean = true
) {

    val estadoActual by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(productoId) {
        viewModel.cargar(productoId)
    }

    // El ViewModel se reutiliza entre aperturas: hasta que llegue el producto
    // pedido no se muestra el que quedo de la vez anterior.
    val estado = estadoActual.let { actual ->
        if (actual is DetalleProductoUiState.Contenido && actual.producto.id != productoId) {
            DetalleProductoUiState.Cargando
        } else {
            actual
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        // when exhaustivo sobre la sealed interface, igual que en el inventario.
        when (estado) {

            DetalleProductoUiState.Cargando ->
                IndicadorCarga(
                    texto = "Cargando producto…",
                    modifier = Modifier.align(Alignment.Center)
                )

            is DetalleProductoUiState.Error ->
                EstadoError(
                    titulo = "No pudimos abrir el producto",
                    mensaje = estado.mensaje,
                    onReintentar = { viewModel.cargar(productoId) },
                    modifier = Modifier.align(Alignment.Center)
                )

            is DetalleProductoUiState.Contenido ->
                DatosDelProducto(
                    producto = estado.producto,
                    onCompartir = viewModel::compartir,
                    onEditar = onEditar,
                    onEliminar = onEliminar,
                    puedeEscribir = puedeEscribir
                )
        }
    }
}


@Composable
private fun DatosDelProducto(
    producto: DetalleProductoUi,
    onCompartir: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    puedeEscribir: Boolean
) {

    val esquema = MaterialTheme.colorScheme

    // Confirmacion antes de eliminar (estado solo de la interfaz).
    var confirmandoEliminar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Tarjeta(modifier = Modifier.fillMaxWidth()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                IconoEnRecuadro(
                    icono = Icons.Default.Medication,
                    lado = 56.dp
                )

                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    Etiqueta(
                        texto = producto.estado,
                        fondo = if (producto.activo) esquema.primaryContainer else esquema.surfaceVariant,
                        color = if (producto.activo) esquema.onPrimaryContainer else esquema.onSurfaceVariant
                    )

                    if (producto.requiereReposicion) {
                        Etiqueta(
                            texto = "Stock bajo",
                            fondo = esquema.errorContainer,
                            color = esquema.onErrorContainer
                        )
                    }
                }

                HorizontalDivider(color = esquema.outlineVariant.copy(alpha = 0.6f))

                Text(
                    text = "Precio",
                    style = MaterialTheme.typography.labelLarge,
                    color = esquema.onSurfaceVariant
                )

                Text(
                    text = producto.precio,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = esquema.primary
                )
            }
        }

        Tarjeta(modifier = Modifier.fillMaxWidth()) {

            Column(modifier = Modifier.padding(vertical = 4.dp)) {

                Dato(icono = Icons.Default.Inventory2, etiqueta = "Stock", valor = producto.stock)

                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = esquema.outlineVariant.copy(alpha = 0.6f)
                )

                Dato(icono = Icons.Default.CheckCircle, etiqueta = "Estado", valor = producto.estado)

                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = esquema.outlineVariant.copy(alpha = 0.6f)
                )

                Dato(icono = Icons.Default.Tag, etiqueta = "Código", valor = "#${producto.id}")
            }
        }

        Button(
            onClick = { onCompartir() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Compartir")
        }

        if (puedeEscribir) Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedButton(
                onClick = onEditar,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Editar")
            }

            OutlinedButton(
                onClick = { confirmandoEliminar = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = esquema.error),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Eliminar")
            }
        }
    }

    if (confirmandoEliminar) {
        AlertDialog(
            onDismissRequest = { confirmandoEliminar = false },
            title = { Text("Eliminar producto") },
            text = { Text("¿Dar de baja \"${producto.nombre}\" del inventario?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmandoEliminar = false
                        onEliminar()
                    }
                ) {
                    Text("Sí, eliminar", color = esquema.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoEliminar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}


@Composable
private fun Dato(
    icono: ImageVector,
    etiqueta: String,
    valor: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = valor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
