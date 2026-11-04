package pe.edu.upeu.pharmamobil.presentation.producto

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.platform.formatearSoles


data class ProductoUi(
    val id: Long,
    val nombre: String,
    val precio: String,
    val stock: String,
    val requiereReposicion: Boolean,
    /** Sesion 11: creado sin red; todavia no esta en PharmaSoft. */
    val pendiente: Boolean = false,
    /** Sesion 13: pendiente que agoto sus intentos; espera que el usuario lo revise. */
    val fallido: Boolean = false,
    /** Sesion 13: por que no se envio (lo que respondio el servidor). */
    val motivoFallo: String? = null
)

/**
 * El formato de moneda se aplica aqui, en el mapeo a la capa de presentacion:
 * el dominio conserva el precio como numero y el composable solo pinta texto.
 * formatearSoles es un expect: cada plataforma lo resuelve con su API nativa.
 */
fun Producto.aUi(): ProductoUi = ProductoUi(
    id = id,
    nombre = nombre,
    precio = formatearSoles(precio),
    stock = "$stock u.",
    requiereReposicion = requiereReposicion,
    pendiente = pendiente,
    fallido = envioFallido,
    motivoFallo = if (envioFallido) ultimoError else null
)
