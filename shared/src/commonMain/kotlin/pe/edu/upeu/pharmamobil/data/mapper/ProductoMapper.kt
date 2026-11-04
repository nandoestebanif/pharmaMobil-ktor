package pe.edu.upeu.pharmamobil.data.mapper

import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoResponseDto
import pe.edu.upeu.pharmamobil.domain.model.Producto

/** Traduce el contrato del backend (DTO) al concepto del negocio (dominio). */
fun ProductoResponseDto.toDomain(): Producto = Producto(
    id = id,
    nombre = nombre.trim(),
    precio = precio,
    stock = stock,
    categoriaId = categoriaId,
    activo = estado
)

/**
 * Traduce el dominio al cuerpo que exige el backend. Si el producto ya tiene
 * categoria (viene del servidor) se conserva; si es nuevo, usa la categoria
 * por defecto. Sin esto, editar un producto le cambiaria la categoria.
 */
fun Producto.toRequest(categoriaPorDefecto: Long): ProductoRequestDto = ProductoRequestDto(
    nombre = nombre,
    precio = precio,
    stock = stock,
    estado = activo,
    categoriaId = categoriaId ?: categoriaPorDefecto
)
