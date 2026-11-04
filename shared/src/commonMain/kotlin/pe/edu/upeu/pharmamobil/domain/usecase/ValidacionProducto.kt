package pe.edu.upeu.pharmamobil.domain.usecase

/**
 * Reglas locales del formulario de producto, compartidas por registrar y
 * actualizar. Evitan un viaje al servidor por errores evidentes; el backend
 * vuelve a validar (longitud del nombre, duplicados) y su respuesta 400 se
 * muestra bajo el mismo campo.
 */
internal fun validarProducto(nombre: String, precio: String, stock: String) = ErroresDeProducto(
    nombre = validarNombre(nombre),
    precio = validarPrecio(precio),
    stock = validarStock(stock)
)

private fun validarNombre(nombre: String): String? {
    return if (nombre.isBlank()) "El nombre es obligatorio" else null
}

private fun validarPrecio(precio: String): String? {
    val precioValor = precio.toDoubleOrNull()
    return when {
        precio.isBlank() -> "El precio es obligatorio"
        precioValor == null || !precioValor.isFinite() -> "El precio debe ser un número válido"
        precioValor <= 0 -> "El precio debe ser mayor a 0"
        else -> null
    }
}

private fun validarStock(stock: String): String? {
    val stockValor = stock.toIntOrNull()
    return when {
        stock.isBlank() -> "El stock es obligatorio"
        stockValor == null -> "El stock debe ser un número entero"
        stockValor < 0 -> "El stock no puede ser negativo"
        else -> null
    }
}
