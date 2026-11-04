package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductoRepositorioEnMemoriaTest {

    private fun nuevoProducto(nombre: String) = Producto(
        id = 0L,
        nombre = nombre,
        precio = 12.50,
        stock = 5
    )

    @Test
    fun asignaIdCorrelativoAlRegistrar() = runTest {

        val repositorio = ProductoRepositorioEnMemoria()

        val primero = repositorio.registrar(nuevoProducto("Paracetamol"))
        val segundo = repositorio.registrar(nuevoProducto("Ibuprofeno"))

        assertEquals(1L, primero.id)
        assertEquals(2L, segundo.id)
    }

    @Test
    fun observarEmiteLosProductosRegistrados() = runTest {

        val repositorio = ProductoRepositorioEnMemoria()
        repositorio.registrar(nuevoProducto("Paracetamol"))
        repositorio.registrar(nuevoProducto("Ibuprofeno"))

        val resultado = repositorio.observar().first()

        assertEquals(2, resultado.size)
        assertEquals("Paracetamol", resultado[0].nombre)
    }
}
