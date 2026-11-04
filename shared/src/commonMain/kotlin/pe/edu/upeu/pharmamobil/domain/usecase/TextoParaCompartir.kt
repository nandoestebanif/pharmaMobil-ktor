package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.platform.formatearSoles

/**
 * Texto que se comparte de un producto. Se arma en codigo comun: es el mismo
 * en Android y en iOS, salvo el precio, que sale con el formato de cada una.
 */
fun Producto.comoTextoParaCompartir(): String =
    "$nombre — ${formatearSoles(precio)} · Stock: $stock"
