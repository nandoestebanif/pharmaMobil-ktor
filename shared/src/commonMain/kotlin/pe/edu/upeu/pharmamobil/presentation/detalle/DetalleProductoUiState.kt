package pe.edu.upeu.pharmamobil.presentation.detalle

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.platform.formatearSoles

/** Estados excluyentes del detalle: solo uno puede estar activo. */
sealed interface DetalleProductoUiState {

    data object Cargando : DetalleProductoUiState

    data class Contenido(val producto: DetalleProductoUi) : DetalleProductoUiState

    data class Error(val mensaje: String) : DetalleProductoUiState
}

/** Lo que el detalle pinta: textos ya listos, sin logica en el composable. */
data class DetalleProductoUi(
    val id: Long,
    val nombre: String,
    val precio: String,
    val stock: String,
    val estado: String,
    val activo: Boolean,
    val requiereReposicion: Boolean
)

fun Producto.aDetalleUi(): DetalleProductoUi = DetalleProductoUi(
    id = id,
    nombre = nombre,
    precio = formatearSoles(precio),
    stock = "$stock unidades",
    estado = if (activo) "Activo" else "Dado de baja",
    activo = activo,
    requiereReposicion = requiereReposicion
)
