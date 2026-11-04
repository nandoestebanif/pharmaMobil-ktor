package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Actualiza un producto existente con los datos del formulario.
 * Recibe el producto original para conservar lo que el formulario no edita
 * (el id y la categoria).
 */
class ActualizarProductoUseCase(
    private val productoRepository: ProductoRepository
) {

    suspend operator fun invoke(
        original: Producto,
        nombre: String,
        precio: String,
        stock: String
    ): Result<Producto> {

        val errores = validarProducto(nombre, precio, stock)

        if (errores.hayErrores) {
            return Result.failure(ProductoInvalidoException(errores))
        }

        return resultadoDe {
            productoRepository.actualizar(
                original.copy(
                    nombre = nombre.trim(),
                    precio = precio.toDouble(),
                    stock = stock.toInt()
                )
            )
        }
    }
}
