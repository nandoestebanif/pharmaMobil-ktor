package pe.edu.upeu.pharmamobil.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Sesion 11: la capa local con una base SQLite en memoria (una nueva por prueba). */
class ProductoLocalTest {

    private fun producto(nombre: String, id: Long = 0) = Producto(id, nombre, 2.5, 10)

    @Test
    fun losPendientesRecibenIdsLocalesNegativosYDistintos() = runTest {
        val local = ProductoLocal(baseEnMemoria())

        val primero = local.guardarPendiente(producto("Ibuprofeno"))
        val segundo = local.guardarPendiente(producto("Naproxeno"))

        assertEquals(-1L, primero.id)
        assertEquals(-2L, segundo.id)
        assertTrue(primero.pendiente && segundo.pendiente)
        // Se suben en el orden en que se crearon: -1 antes que -2.
        assertEquals(listOf(-1L, -2L), local.pendientes().map { it.id })
    }

    @Test
    fun laListaDelServidorNoBorraLoCreadoSinRed() = runTest {
        val local = ProductoLocal(baseEnMemoria())
        local.reemplazarSincronizados(listOf(producto("Paracetamol", 1), producto("Aspirina", 2)))
        local.guardarPendiente(producto("Clorfenamina"))

        // Llega otra lista del servidor (sin el pendiente, que aun no se subio).
        local.reemplazarSincronizados(listOf(producto("Paracetamol", 1)))

        val nombres = local.observar().first().map { it.nombre }
        assertEquals(listOf("Clorfenamina", "Paracetamol"), nombres)
    }

    @Test
    fun confirmarLaSubidaCambiaElIdLocalPorElReal() = runTest {
        val local = ProductoLocal(baseEnMemoria())
        val pendiente = local.guardarPendiente(producto("Clorfenamina"))

        local.confirmarSubida(pendiente.id, pendiente.copy(id = 11, pendiente = false))

        val fila = local.observar().first().single()
        assertEquals(11L, fila.id)
        assertEquals(false, fila.pendiente)
        assertTrue(local.pendientes().isEmpty())
    }
}
