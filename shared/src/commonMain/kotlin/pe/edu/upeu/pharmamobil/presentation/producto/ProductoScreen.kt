package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobil.domain.platform.RelojDelSistema
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upeu.pharmamobil.presentation.components.EstadoError
import pe.edu.upeu.pharmamobil.presentation.components.EstadoVacio
import pe.edu.upeu.pharmamobil.presentation.components.Etiqueta
import pe.edu.upeu.pharmamobil.presentation.components.IconoEnRecuadro
import pe.edu.upeu.pharmamobil.presentation.components.IndicadorCarga
import pe.edu.upeu.pharmamobil.presentation.components.MensajeError
import pe.edu.upeu.pharmamobil.presentation.components.MensajeExito
import pe.edu.upeu.pharmamobil.presentation.components.Tarjeta
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoUiState.Fase
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoUiState.Operacion

/**
 * Inventario de productos. La pantalla solo lista: registrar y editar se hacen
 * en el formulario, y compartir, editar o eliminar, desde el detalle.
 *
 * @param onNuevo abre el formulario para registrar un producto.
 * @param onVerDetalle abre el detalle del producto tocado.
 */
@Composable
fun ProductoScreen(
    viewModel: ProductoViewModel,
    onNuevo: () -> Unit,
    onVerDetalle: (Long) -> Unit,
    modifier: Modifier = Modifier,
    // Sesion 10: sin CATALOGO_ESCRIBIR no se ofrece registrar ni reactivar.
    puedeEscribir: Boolean = true
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // El filtro aparece cuando ya hay un inventario que filtrar.
            if (uiState.fase is Fase.ConProductos || uiState.fase is Fase.SinProductos) {
                FiltroInventario(
                    fase = uiState.fase,
                    cantidadDeBaja = uiState.dadosDeBaja.size,
                    viendoBajas = uiState.viendoBajas,
                    onVerBajas = viewModel::verDadosDeBaja
                )
            }

            // Sesion 11: estado de la sincronizacion y boton para sincronizar a mano.
            if (uiState.fase !is Fase.Error) {
                BarraSincronizacion(
                    sincronizando = uiState.sincronizando,
                    avisoRed = uiState.avisoRed,
                    pendientes = uiState.pendientes,
                    fallidas = uiState.fallidas,
                    ultimaSincronizacion = uiState.ultimaSincronizacion,
                    onSincronizar = viewModel::sincronizar,
                    onReintentarFallidas = viewModel::reintentarFallidas
                )
            }

            // La operacion en curso se anuncia sin tapar el listado.
            when (val operacion = uiState.operacion) {

                Operacion.Inactiva -> Unit

                is Operacion.EnCurso ->
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                is Operacion.Fallida ->
                    MensajeError(
                        texto = operacion.mensaje,
                        onCerrar = viewModel::descartarError
                    )
            }

            uiState.mensajeExito?.let {
                MensajeExito(it)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                // when exhaustivo sobre la sealed interface: si se agrega una fase
                // y no se trata aqui, el proyecto no compila.
                when (val fase = uiState.fase) {

                    Fase.Cargando ->
                        IndicadorCarga(
                            texto = "Cargando inventario…",
                            modifier = Modifier.align(Alignment.Center)
                        )

                    Fase.SinProductos ->
                        if (uiState.viendoBajas) {
                            ListaDadosDeBaja(
                                productos = uiState.dadosDeBaja,
                                operacion = uiState.operacion,
                                onReactivar = viewModel::reactivar,
                                puedeReactivar = puedeEscribir,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            EstadoVacio(
                                icono = Icons.Default.Inventory2,
                                titulo = "Todavía no hay productos",
                                descripcion = if (puedeEscribir) {
                                    "Registra el primero con el botón Nuevo producto."
                                } else {
                                    "Cuando se registren productos aparecerán aquí."
                                },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                    is Fase.ConProductos ->
                        if (uiState.viendoBajas) {
                            ListaDadosDeBaja(
                                productos = uiState.dadosDeBaja,
                                operacion = uiState.operacion,
                                onReactivar = viewModel::reactivar,
                                puedeReactivar = puedeEscribir,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            ListaProductos(
                                productos = fase.productos,
                                operacion = uiState.operacion,
                                onVerDetalle = onVerDetalle
                            )
                        }

                    Fase.SinDatosSinConexion ->
                        EstadoVacio(
                            icono = Icons.Default.CloudOff,
                            titulo = "Todavía no hay datos en este teléfono",
                            descripcion = "El inventario se descarga la primera vez que hay conexión. " +
                                "Conéctate y pulsa Sincronizar." +
                                if (puedeEscribir) " Puedes registrar productos: se enviarán al reconectar." else "",
                            modifier = Modifier.align(Alignment.Center)
                        )

                    is Fase.Error ->
                        EstadoError(
                            titulo = "No pudimos cargar el inventario",
                            mensaje = fase.mensaje,
                            onReintentar = viewModel::cargarProductos,
                            modifier = Modifier.align(Alignment.Center)
                        )
                }
            }
        }

        // Con una operacion en curso no se abre el formulario: se espera a que termine.
        if (puedeEscribir && !uiState.viendoBajas && uiState.fase !is Fase.Cargando && uiState.fase !is Fase.Error) {

            ExtendedFloatingActionButton(
                onClick = { if (!uiState.operando) onNuevo() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo producto") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    // Nombre accesible del boton para lectores de pantalla y pruebas.
                    .semantics { contentDescription = "Nuevo producto" }
            )
        }
    }
}


/**
 * Sesion 11: la lista sale de la base local; esta barra dice si esta al dia.
 * Sin conexion muestra el aviso y cuantos productos esperan para subir.
 */
@Composable
private fun BarraSincronizacion(
    sincronizando: Boolean,
    avisoRed: String?,
    pendientes: Int,
    fallidas: Int,
    ultimaSincronizacion: Long?,
    onSincronizar: () -> Unit,
    onReintentarFallidas: () -> Unit
) {

    val esquema = MaterialTheme.colorScheme

    // Autonoma 11: «Actualizado hace...» se recalcula cada 30 s y con cada sincronizacion.
    var ahora by remember { mutableLongStateOf(RelojDelSistema.ahoraMs()) }
    LaunchedEffect(ultimaSincronizacion) {
        while (true) {
            ahora = RelojDelSistema.ahoraMs()
            delay(30_000)
        }
    }
    val sinRed = avisoRed != null && !sincronizando

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (sinRed) esquema.tertiaryContainer else esquema.surfaceVariant,
        contentColor = if (sinRed) esquema.onTertiaryContainer else esquema.onSurfaceVariant
    ) {

        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (sincronizando) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = if (sinRed) Icons.Default.CloudOff else Icons.Default.CloudDone,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        sincronizando -> "Sincronizando con PharmaSoft…"
                        avisoRed != null -> avisoRed
                        else -> "Inventario al día"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = textoDeAntiguedad(ultimaSincronizacion, ahora),
                    style = MaterialTheme.typography.labelMedium
                )
                if (pendientes > 0) {
                    Text(
                        text = if (pendientes == 1) "1 producto pendiente de enviar"
                               else "$pendientes productos pendientes de enviar",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                // Sesion 13: lo que el servidor rechazo no se reintenta solo; se dice y se ofrece una accion.
                if (fallidas > 0) {
                    Text(
                        text = if (fallidas == 1) "1 cambio no se pudo enviar" else "$fallidas cambios no se pudieron enviar",
                        style = MaterialTheme.typography.labelMedium,
                        color = esquema.error
                    )
                    TextButton(
                        onClick = onReintentarFallidas,
                        enabled = !sincronizando,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                    ) { Text("Reintentar envío", style = MaterialTheme.typography.labelLarge) }
                }
            }

            TextButton(
                onClick = onSincronizar,
                enabled = !sincronizando,
                modifier = Modifier.semantics { contentDescription = "Sincronizar" }
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Sincronizar", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}


/** Alterna entre el inventario activo y los productos dados de baja. */
@Composable
private fun FiltroInventario(
    fase: Fase,
    cantidadDeBaja: Int,
    viendoBajas: Boolean,
    onVerBajas: (Boolean) -> Unit
) {

    val cantidadActivos = (fase as? Fase.ConProductos)?.productos?.size ?: 0

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {

        SegmentedButton(
            selected = !viendoBajas,
            onClick = { onVerBajas(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) {
            Text("Activos · $cantidadActivos")
        }

        SegmentedButton(
            selected = viendoBajas,
            onClick = { onVerBajas(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) {
            Text("De baja · $cantidadDeBaja")
        }
    }
}


@Composable
private fun ListaProductos(
    productos: List<ProductoUi>,
    operacion: Operacion,
    onVerDetalle: (Long) -> Unit
) {

    val enCurso = operacion as? Operacion.EnCurso

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        // Espacio al final para que el boton flotante no tape la ultima fila.
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        items(
            items = productos,
            key = { it.id }
        ) { producto ->
            ProductoItem(
                producto = producto,
                // Solo la fila afectada muestra progreso; el resto queda deshabilitado.
                enProceso = enCurso?.productoId == producto.id,
                habilitado = enCurso == null,
                onVerDetalle = { onVerDetalle(producto.id) }
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductoItem(
    producto: ProductoUi,
    enProceso: Boolean,
    habilitado: Boolean,
    onVerDetalle: () -> Unit
) {

    val esquema = MaterialTheme.colorScheme

    // Tocar la fila abre el detalle del producto.
    Tarjeta(
        onClick = onVerDetalle,
        habilitada = habilitado,
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            IconoEnRecuadro(icono = Icons.Default.Medication)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                FlowRow(
                    // Sesion 11: si no caben «Reponer» y «Pendiente», bajan de linea en vez de cortarse.
                    itemVerticalAlignment = Alignment.CenterVertically,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = producto.stock,
                        style = MaterialTheme.typography.bodyMedium,
                        color = esquema.onSurfaceVariant
                    )

                    if (producto.requiereReposicion) {
                        Etiqueta(
                            texto = "Reponer",
                            fondo = esquema.errorContainer,
                            color = esquema.onErrorContainer
                        )
                    }

                    // Sesion 11: creado sin red, todavia no esta en PharmaSoft.
                    if (producto.fallido) {
                        // Sesion 13: el servidor lo rechazo o se agotaron los intentos.
                        Etiqueta(
                            texto = "No se envió",
                            fondo = esquema.errorContainer,
                            color = esquema.onErrorContainer
                        )
                    } else if (producto.pendiente) {
                        Etiqueta(
                            texto = "Pendiente",
                            fondo = esquema.tertiaryContainer,
                            color = esquema.onTertiaryContainer
                        )
                    }
                }

                producto.motivoFallo?.let {
                    Text(
                        text = "$it. Edítalo o elimínalo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = esquema.error
                    )
                }
            }

            if (enProceso) {

                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )

            } else {

                Text(
                    text = producto.precio,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = esquema.primary
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = esquema.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


@Composable
private fun ListaDadosDeBaja(
    productos: List<ProductoUi>,
    operacion: Operacion,
    onReactivar: (Long) -> Unit,
    puedeReactivar: Boolean,
    modifier: Modifier = Modifier
) {

    if (productos.isEmpty()) {
        EstadoVacio(
            icono = Icons.Default.DeleteOutline,
            titulo = "No hay productos dados de baja",
            descripcion = "Los productos que elimines aparecerán aquí y podrás reactivarlos.",
            modifier = modifier
        )
        return
    }

    val esquema = MaterialTheme.colorScheme
    val enCurso = operacion as? Operacion.EnCurso

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(
            items = productos,
            key = { it.id }
        ) { producto ->

            Tarjeta(
                modifier = Modifier.fillMaxWidth()
            ) {

                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    IconoEnRecuadro(
                        icono = Icons.Default.Medication,
                        fondo = esquema.surfaceVariant,
                        color = esquema.onSurfaceVariant
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        Text(
                            text = producto.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            color = esquema.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Dado de baja  ·  ${producto.precio}  ·  ${producto.stock}",
                            style = MaterialTheme.typography.bodySmall,
                            color = esquema.onSurfaceVariant
                        )
                    }

                    if (enCurso?.productoId == producto.id) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )

                    } else if (puedeReactivar) {

                        FilledTonalButton(
                            onClick = { onReactivar(producto.id) },
                            enabled = enCurso == null
                        ) {
                            Text("Reactivar")
                        }
                    }
                }
            }
        }
    }
}
