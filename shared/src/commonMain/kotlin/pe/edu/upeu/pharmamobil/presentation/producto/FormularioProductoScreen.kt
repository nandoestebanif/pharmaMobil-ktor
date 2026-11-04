package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upeu.pharmamobil.presentation.components.EstadoError
import pe.edu.upeu.pharmamobil.presentation.components.IndicadorCarga
import pe.edu.upeu.pharmamobil.presentation.components.MensajeError
import pe.edu.upeu.pharmamobil.presentation.components.Tarjeta
import pe.edu.upeu.pharmamobil.presentation.components.ValidatedTextField
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoUiState.Operacion

/**
 * Formulario para registrar un producto o editar uno existente. Usa el mismo
 * ProductoViewModel que el inventario: al guardar, el listado ya queda al dia.
 *
 * @param productoId id del producto a editar; null para registrar uno nuevo.
 * @param activa false mientras la pantalla se esta retirando (animacion de salida).
 * @param onGuardado se invoca cuando el servidor confirmo el guardado.
 * @param onCerrar se invoca al pulsar Cancelar.
 */
@Composable
fun FormularioProductoScreen(
    viewModel: ProductoViewModel,
    productoId: Long?,
    activa: Boolean,
    onGuardado: () -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val formulario = uiState.formulario
    val operacion = uiState.operacion

    // En modo edicion el formulario se muestra cuando ya llego el producto pedido.
    val esperandoProducto = productoId != null && formulario.editandoId != productoId

    // El formulario se abre con el mensaje de exito en blanco, asi que cualquier
    // mensaje nuevo significa que el servidor confirmo el guardado.
    LaunchedEffect(uiState.mensajeExito) {
        if (uiState.mensajeExito != null) onGuardado()
    }

    // Si la app se restauro y el ViewModel ya no tiene el producto, se vuelve a pedir.
    val faltaPedirlo = activa && esperandoProducto &&
        operacion is Operacion.Inactiva && uiState.mensajeExito == null

    LaunchedEffect(productoId, faltaPedirlo) {
        if (productoId != null && faltaPedirlo) viewModel.editar(productoId)
    }

    Box(modifier = modifier.fillMaxSize()) {

        when {

            esperandoProducto && operacion is Operacion.Fallida ->
                EstadoError(
                    titulo = "No pudimos abrir el producto",
                    mensaje = operacion.mensaje,
                    onReintentar = { viewModel.editar(productoId) },
                    modifier = Modifier.align(Alignment.Center)
                )

            esperandoProducto ->
                IndicadorCarga(
                    texto = "Cargando producto…",
                    modifier = Modifier.align(Alignment.Center)
                )

            else ->
                CamposDelProducto(
                    formulario = formulario,
                    operacion = operacion,
                    onNombreChange = viewModel::onNombreChange,
                    onPrecioChange = viewModel::onPrecioChange,
                    onStockChange = viewModel::onStockChange,
                    onGuardar = viewModel::guardar,
                    onDescartarError = viewModel::descartarError,
                    onCerrar = onCerrar
                )
        }
    }
}


@Composable
private fun CamposDelProducto(
    formulario: FormularioProducto,
    operacion: Operacion,
    onNombreChange: (String) -> Unit,
    onPrecioChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onDescartarError: () -> Unit,
    onCerrar: () -> Unit
) {

    val guardando = operacion is Operacion.EnCurso

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = if (formulario.enEdicion) {
                "Los cambios se guardan en el inventario de PharmaSoft."
            } else {
                "El producto se registra en PharmaSoft. Sin conexión se guarda en el teléfono y se envía al sincronizar."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        if (operacion is Operacion.Fallida) {
            MensajeError(
                texto = operacion.mensaje,
                onCerrar = onDescartarError
            )
        }

        Tarjeta(modifier = Modifier.fillMaxWidth()) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                ValidatedTextField(
                    value = formulario.nombre,
                    onValueChange = onNombreChange,
                    label = "Nombre",
                    error = formulario.nombreError,
                    ayuda = "Entre 3 y 150 caracteres",
                    habilitado = !guardando,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    ValidatedTextField(
                        value = formulario.precio,
                        onValueChange = onPrecioChange,
                        label = "Precio",
                        error = formulario.precioError,
                        ayuda = "En soles",
                        prefijo = "S/ ",
                        keyboardType = KeyboardType.Decimal,
                        habilitado = !guardando,
                        modifier = Modifier.weight(1f)
                    )

                    ValidatedTextField(
                        value = formulario.stock,
                        onValueChange = onStockChange,
                        label = "Stock",
                        error = formulario.stockError,
                        ayuda = "Unidades",
                        keyboardType = KeyboardType.Number,
                        habilitado = !guardando,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (guardando) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = onGuardar,
            enabled = !guardando,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                when {
                    formulario.enEdicion && guardando -> "Guardando…"
                    formulario.enEdicion -> "Guardar cambios"
                    guardando -> "Registrando…"
                    else -> "Registrar producto"
                }
            )
        }

        OutlinedButton(
            onClick = onCerrar,
            enabled = !guardando,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Cancelar")
        }
    }
}
