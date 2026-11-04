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
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.local.baseEnMemoria
import pe.edu.upeu.pharmamobil.data.remote.ConfiguracionApi
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.ProductoRemoto
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Sesion 11: los dos caminos del repositorio offline-first sin emulador y sin red.
 * Base SQLite en memoria (una NUEVA por prueba) y un PharmaSoft falso con MockEngine
 * que guarda cada peticion para revisar el orden en que salieron.
 */
class ProductoRepositorioOfflineTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun pagina(vararg productos: String) =
        """{"contenido":[${productos.joinToString(",")}],"pagina":0,"tamanio":100,
            "totalElementos":${productos.size},"totalPaginas":1,"ultima":true}"""

    private fun productoJson(id: Long, nombre: String, precio: Double = 2.8, stock: Int = 25) =
        """{"id":$id,"nombre":"$nombre","precio":$precio,"stock":$stock,"estado":true,"categoriaId":1}"""

    private val paracetamol = productoJson(1, "Paracetamol 500mg", 3.5, 120)

    /** Repositorio armado con la base en memoria y el servidor falso. */
    private class Escenario(val repo: ProductoRepositorioOffline, val local: ProductoLocal, val peticiones: List<HttpRequestData>) {
        /** «POST productos», «GET productos»... en el orden en que salieron. */
        val orden get() = peticiones.map { "${it.method.value} ${it.url.encodedPath.substringAfterLast("/v1/")}" }
    }

    private fun repositorioCon(responder: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): Escenario {
        val peticiones = mutableListOf<HttpRequestData>()
        val motor = MockEngine { peticion -> peticiones += peticion; responder(peticion) }
        val cliente = crearHttpClient(motor, ConfiguracionApi(urlBase = "http://localhost:8080/api/v1/", registroHttp = false), AlmacenEnMemoria("t.o.k"))
        val local = ProductoLocal(baseEnMemoria())
        return Escenario(ProductoRepositorioOffline(local, ProductoRemoto(ProductoApi(cliente), 1L)), local, peticiones)
    }

    /** PharmaSoft apagado o modo avion: toda peticion falla por conexion. */
    private fun apiQueFalla(): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData =
        { throw IOException("Connection refused") }

    @Test
    fun `sin red el producto queda pendiente y la pantalla lo ve`() = runTest {
        val e = repositorioCon(apiQueFalla())

        val guardado = e.repo.registrar(Producto(0, "Ibuprofeno", 4.5, 20))

        val enPantalla = e.repo.observar().first()
        assertEquals(1, enPantalla.size)
        assertTrue(enPantalla.single().pendiente)
        assertTrue(guardado.id < 0, "sin servidor el id es local y negativo")
    }

    @Test
    fun `con red el producto se sube y queda con el id del servidor`() = runTest {
        val e = repositorioCon { peticion ->
            if (peticion.method == HttpMethod.Post) respond(productoJson(11, "Clorfenamina 4mg"), HttpStatusCode.Created, json)
            else respond(pagina(paracetamol, productoJson(11, "Clorfenamina 4mg")), HttpStatusCode.OK, json)
        }

        val creado = e.repo.registrar(Producto(0, "Clorfenamina 4mg", 2.8, 25))

        assertEquals(11L, creado.id)
        assertFalse(creado.pendiente)
        assertEquals(listOf("Clorfenamina 4mg", "Paracetamol 500mg"), e.repo.observar().first().map { it.nombre })
        assertTrue(e.local.pendientes().isEmpty())
    }

    @Test
    fun `sincronizar sube lo pendiente antes de bajar y no se pierde nada`() = runTest {
        var hayRed = false
        val e = repositorioCon { peticion ->
            if (!hayRed) throw IOException("modo avion")
            if (peticion.method == HttpMethod.Post) respond(productoJson(11, "Clorfenamina 4mg"), HttpStatusCode.Created, json)
            else respond(pagina(paracetamol, productoJson(11, "Clorfenamina 4mg")), HttpStatusCode.OK, json)
        }
        e.repo.registrar(Producto(0, "Clorfenamina 4mg", 2.8, 25))   // sin red: queda pendiente

        hayRed = true
        e.repo.sincronizar()

        // Primero el POST del pendiente, despues el GET de la lista. Al reves, la lista
        // del servidor (que aun no lo tenia) habria borrado lo creado sin red.
        val ultimas = e.orden.takeLast(2)
        assertEquals(listOf("POST productos", "GET productos"), ultimas)
        val lista = e.repo.observar().first()
        assertEquals(listOf(11L, 1L), lista.map { it.id })
        assertTrue(lista.none { it.pendiente })
    }

    @Test
    fun `si la red se corta a la mitad lo ya subido no se vuelve a enviar`() = runTest {
        var posts = 0
        var hayRed = false
        val e = repositorioCon { peticion ->
            if (!hayRed) throw IOException("modo avion")
            if (peticion.method == HttpMethod.Post) {
                posts++
                // El segundo pendiente encuentra la red caida.
                if (posts == 2) throw IOException("se corto la red")
                respond(productoJson(11, "Clorfenamina 4mg"), HttpStatusCode.Created, json)
            } else respond(pagina(productoJson(11, "Clorfenamina 4mg")), HttpStatusCode.OK, json)
        }
        e.repo.registrar(Producto(0, "Clorfenamina 4mg", 2.8, 25))
        e.repo.registrar(Producto(0, "Loratadina 10mg", 2.4, 60))

        hayRed = true
        val fallo = assertFailsWith<ErrorApiException> { e.repo.sincronizar() }

        assertEquals(ErrorApi.SinConexion, fallo.error)
        // El primero ya tiene id real; solo el segundo sigue pendiente.
        assertEquals(listOf("Loratadina 10mg"), e.local.pendientes().map { it.nombre })
        assertEquals(11L, e.repo.observar().first().first { it.nombre.startsWith("Clorfenamina") }.id)
    }

    @Test
    fun `un pendiente que el servidor rechaza no bloquea la bajada de la lista`() = runTest {
        var hayRed = false
        val e = repositorioCon { peticion ->
            if (!hayRed) throw IOException("modo avion")
            if (peticion.method == HttpMethod.Post) respond(
                """{"timestamp":"x","status":409,"error":"Conflict","message":"Ya existe un producto con el nombre Paracetamol 500mg","path":"/api/v1/productos"}""",
                HttpStatusCode.Conflict, json
            ) else respond(pagina(paracetamol), HttpStatusCode.OK, json)
        }
        e.repo.registrar(Producto(0, "Paracetamol 500mg", 3.5, 10))

        hayRed = true
        // Sesion 13: el rechazo ya no se lanza como excepcion; la fila queda fallida y visible.
        val resumen = e.repo.sincronizar()

        assertEquals(1L, resumen.fallidas)
        // La lista del servidor llego igual y el rechazado sigue en el dispositivo (editable).
        val lista = e.repo.observar().first()
        assertEquals(2, lista.size)
        assertEquals(1, lista.count { it.pendiente && it.envioFallido })
    }

    @Test
    fun `con la sesion vencida se corta la sincronizacion y lo pendiente se conserva`() = runTest {
        var hayRed = false
        val e = repositorioCon { peticion ->
            if (!hayRed) throw IOException("modo avion")
            respond(
                """{"timestamp":"x","status":401,"error":"Unauthorized","message":"El token ha expirado. Inicie sesión nuevamente","path":"/api/v1/productos"}""",
                HttpStatusCode.Unauthorized, json
            )
        }
        e.repo.registrar(Producto(0, "Omeprazol 20mg", 4.5, 40))
        e.repo.registrar(Producto(0, "Loratadina 10mg", 1.2, 30))
        val antes = e.peticiones.size

        hayRed = true
        val fallo = assertFailsWith<ErrorApiException> { e.repo.sincronizar() }

        assertEquals(ErrorApi.NoAutenticado, fallo.error)
        // Al primer 401 se detiene: un solo POST y ningun GET con un token que ya no vale.
        assertEquals(listOf("POST productos"), e.orden.drop(antes))
        val lista = e.repo.observar().first()
        assertEquals(2, lista.count { it.pendiente }, "nada se pierde: se envia al volver a entrar")
    }

    @Test
    fun `sin red el detalle se muestra desde la copia local`() = runTest {
        var hayRed = true
        val e = repositorioCon { peticion ->
            if (!hayRed) throw IOException("modo avion")
            respond(pagina(paracetamol), HttpStatusCode.OK, json)
        }
        e.repo.sincronizar()

        hayRed = false
        val producto = e.repo.obtener(1)

        assertEquals("Paracetamol 500mg", producto.nombre)
    }
}
