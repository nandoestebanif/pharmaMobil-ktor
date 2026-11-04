package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import pe.edu.upeu.pharmamobil.data.local.AlmacenEnMemoria
import pe.edu.upeu.pharmamobil.data.local.MarcaDeSincronizacion
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.local.baseEnMemoria
import pe.edu.upeu.pharmamobil.data.remote.ConfiguracionApi
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.ProductoRemoto
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Actividad autonoma 11, producto 1: las seis pruebas de la ficha, en su orden.
 *
 * Cada prueba arma su propia base SQLite en memoria (baseEnMemoria(): una nueva por
 * llamada) y un PharmaSoft falso con MockEngine. Ninguna usa emulador, red ni esperas.
 */
class SuiteOfflineTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val RELOJ = 1_792_000_000_000L

    private fun pagina(vararg productos: String) =
        """{"contenido":[${productos.joinToString(",")}],"pagina":0,"tamanio":100,
            "totalElementos":${productos.size},"totalPaginas":1,"ultima":true}"""

    private fun productoJson(id: Long, nombre: String, precio: Double = 3.5, stock: Int = 20) =
        """{"id":$id,"nombre":"$nombre","precio":$precio,"stock":$stock,"estado":true,"categoriaId":1}"""

    private val paracetamol = productoJson(1, "Paracetamol 500mg", 3.5, 120)
    private val ibuprofeno = productoJson(2, "Ibuprofeno 400mg", 5.2, 80)

    /** Repositorio sobre una base nueva en memoria y un servidor falso.
     *  `peticiones` guarda solo las que llegaron al servidor (en modo avión no llega ninguna). */
    private class Montaje(
        val repo: ProductoRepositorioOffline,
        val local: ProductoLocal,
        val peticiones: List<HttpRequestData>
    ) {
        val posts get() = peticiones.count { it.method == HttpMethod.Post }
    }

    private fun montaje(responder: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): Montaje {
        val peticiones = mutableListOf<HttpRequestData>()
        val motor = MockEngine { peticion -> responder(peticion).also { peticiones += peticion } }
        val cliente = crearHttpClient(
            motor, ConfiguracionApi(urlBase = "http://localhost:8080/api/v1/", registroHttp = false), AlmacenEnMemoria("t.o.k")
        )
        val base = baseEnMemoria()
        val local = ProductoLocal(base)
        val repo = ProductoRepositorioOffline(local, ProductoRemoto(ProductoApi(cliente), 1L), MarcaDeSincronizacion(base), reloj = { RELOJ })
        return Montaje(repo, local, peticiones)
    }

    private val sinRed: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData =
        { throw IOException("modo avion") }

    // 1 -----------------------------------------------------------------------------------
    @Test
    fun `1 sincronizacion con red - la base queda con los productos del servidor y el Flow los emite`() = runTest {
        val m = montaje { respond(pagina(paracetamol, ibuprofeno), HttpStatusCode.OK, json) }
        assertNull(m.repo.ultimaSincronizacion().first(), "antes de sincronizar: nunca")

        m.repo.sincronizar()

        val emitidos = m.repo.observar().first()
        assertEquals(listOf("Ibuprofeno 400mg", "Paracetamol 500mg"), emitidos.map { it.nombre })
        assertEquals(listOf(2L, 1L), emitidos.map { it.id })
        assertTrue(emitidos.none { it.pendiente })
        assertEquals(RELOJ, m.repo.ultimaSincronizacion().first(), "queda la hora de la sincronizacion")
    }

    // 2 -----------------------------------------------------------------------------------
    @Test
    fun `2 registro sin red - el producto queda pendiente y el Flow lo emite igual`() = runTest {
        val m = montaje(sinRed)

        val guardado = m.repo.registrar(Producto(0, "Omeprazol 20mg", 4.5, 40))

        val emitidos = m.repo.observar().first()
        assertEquals(1, emitidos.size)
        assertTrue(emitidos.single().pendiente)
        assertTrue(guardado.pendiente && guardado.id < 0)
        assertEquals(1, m.local.pendientes().size)
        assertNull(m.repo.ultimaSincronizacion().first(), "un envio fallido no cuenta como sincronizacion")
    }

    // 3 -----------------------------------------------------------------------------------
    @Test
    fun `3 envio al reconectar - sincronizar sube lo pendiente y la fila deja de estar marcada`() = runTest {
        var hayRed = false
        val m = montaje { peticion ->
            if (!hayRed) throw IOException("modo avion")
            if (peticion.method == HttpMethod.Post) respond(productoJson(11, "Omeprazol 20mg", 4.5, 40), HttpStatusCode.Created, json)
            else respond(pagina(paracetamol, productoJson(11, "Omeprazol 20mg", 4.5, 40)), HttpStatusCode.OK, json)
        }
        m.repo.registrar(Producto(0, "Omeprazol 20mg", 4.5, 40))

        hayRed = true
        m.repo.sincronizar()

        assertEquals(1, m.posts, "un solo POST: el del pendiente")
        val omeprazol = m.repo.observar().first().single { it.nombre == "Omeprazol 20mg" }
        assertEquals(11L, omeprazol.id)
        assertFalse(omeprazol.pendiente)
        assertTrue(m.local.pendientes().isEmpty())
    }

    // 4 -----------------------------------------------------------------------------------
    @Test
    fun `4 la bajada no borra lo pendiente - la lista del servidor conserva lo creado sin conexion`() = runTest {
        val m = montaje { respond(pagina(paracetamol), HttpStatusCode.OK, json) }
        m.repo.sincronizar()
        val pendiente = m.local.guardarPendiente(Producto(0, "Omeprazol 20mg", 4.5, 40))

        // Llega otra lista del servidor, que todavia no conoce el pendiente.
        m.local.reemplazarSincronizados(listOf(Producto(1, "Paracetamol 500mg", 3.5, 118)))

        val filas = m.repo.observar().first()
        assertEquals(2, filas.size)
        assertTrue(filas.any { it.id == pendiente.id && it.pendiente }, "el pendiente sigue ahi")
        assertEquals(118, filas.single { it.id == 1L }.stock, "y lo sincronizado se actualizo")
    }

    // 5 -----------------------------------------------------------------------------------
    @Test
    fun `5 identificadores sin colision - un producto local y uno del servidor conviven`() = runTest {
        val m = montaje { respond(pagina(paracetamol, ibuprofeno), HttpStatusCode.OK, json) }
        m.repo.sincronizar()

        val local1 = m.local.guardarPendiente(Producto(0, "Omeprazol 20mg", 4.5, 40))
        val local2 = m.local.guardarPendiente(Producto(0, "Ranitidina 150mg", 3.2, 30))

        // Los del servidor son 1 y 2; los locales, negativos: ninguno pisa a otro.
        assertEquals(-1L, local1.id)
        assertEquals(-2L, local2.id)
        val filas = m.repo.observar().first()
        assertEquals(4, filas.size)
        assertEquals(setOf(-2L, -1L, 1L, 2L), filas.map { it.id }.toSet())
        assertEquals("Paracetamol 500mg", filas.single { it.id == 1L }.nombre)
    }

    // 6 -----------------------------------------------------------------------------------
    @Test
    fun `6 sincronizar dos veces no duplica`() = runTest {
        var hayRed = false
        val m = montaje { peticion ->
            if (!hayRed) throw IOException("modo avion")
            if (peticion.method == HttpMethod.Post) respond(productoJson(11, "Omeprazol 20mg", 4.5, 40), HttpStatusCode.Created, json)
            else respond(pagina(paracetamol, productoJson(11, "Omeprazol 20mg", 4.5, 40)), HttpStatusCode.OK, json)
        }
        m.repo.registrar(Producto(0, "Omeprazol 20mg", 4.5, 40))
        hayRed = true

        m.repo.sincronizar()
        val tras1 = m.repo.observar().first().size
        m.repo.sincronizar()
        val tras2 = m.repo.observar().first().size

        assertEquals(2, tras1)
        assertEquals(tras1, tras2, "misma cantidad de filas")
        assertEquals(1, m.posts, "el pendiente se envio una sola vez")
    }
}
