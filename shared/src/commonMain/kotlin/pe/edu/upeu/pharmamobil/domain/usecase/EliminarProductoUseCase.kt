package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/** Da de baja un producto del inventario. */
class EliminarProductoUseCase(
    private val productoRepository: ProductoRepository
) {

    suspend operator fun invoke(id: Long): Result<Unit> = resultadoDe {
        productoRepository.eliminar(id)
    }
}
