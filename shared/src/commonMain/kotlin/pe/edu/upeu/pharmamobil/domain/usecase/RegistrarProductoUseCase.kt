package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository


data class ErroresDeProducto(
    val nombre: String? = null,
    val precio: String? = null,
    val stock: String? = null
) {

    val hayErrores: Boolean
        get() = nombre != null || precio != null || stock != null
}

class ProductoInvalidoException(
    val errores: ErroresDeProducto
) : IllegalArgumentException("Los datos del producto no cumplen las reglas del negocio")


class RegistrarProductoUseCase(
    private val productoRepository: ProductoRepository
) {

    suspend operator fun invoke(
        nombre: String,
        precio: String,
        stock: String
    ): Result<Producto> {

        val errores = validarProducto(nombre, precio, stock)

        if (errores.hayErrores) {
            return Result.failure(ProductoInvalidoException(errores))
        }

        return resultadoDe {
            productoRepository.registrar(
                Producto(
                    id = 0L,
                    nombre = nombre.trim(),
                    precio = precio.toDouble(),
                    stock = stock.toInt()
                )
            )
        }
    }
}
