package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/** Trae la version vigente de un producto antes de editarlo. */
class ObtenerProductoUseCase(
    private val productoRepository: ProductoRepository
) {

    suspend operator fun invoke(id: Long): Result<Producto> = resultadoDe {
        productoRepository.obtener(id)
    }
}
