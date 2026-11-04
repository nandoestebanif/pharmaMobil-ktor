package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Vuelve a activar un producto dado de baja. PharmaSoft no tiene un endpoint
 * propio para esto: se trae la version vigente y se actualiza con activo = true,
 * asi se conservan su nombre, precio, stock y categoria.
 */
class ReactivarProductoUseCase(
    private val productoRepository: ProductoRepository
) {

    suspend operator fun invoke(id: Long): Result<Producto> = resultadoDe {
        val producto = productoRepository.obtener(id)
        productoRepository.actualizar(producto.copy(activo = true))
    }
}
