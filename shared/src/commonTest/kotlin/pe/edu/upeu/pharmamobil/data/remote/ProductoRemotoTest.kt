package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import pe.edu.upeu.pharmamobil.data.local.AlmacenEnMemoria
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * El repositorio REST probado sin depender de que PharmaSoft este encendido:
 * MockEngine reemplaza al motor real y responde con los cuerpos del backend.
 */
class ProductoRemotoTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val config = ConfiguracionApi(urlBase = "http://10.0.2.2:8080/api/v1/")
    private val CATEGORIA = 1L

    private fun repositorioCon(engine: MockEngine) =
        ProductoRemoto(ProductoApi(crearHttpClient(engine, config, AlmacenEnMemoria())), CATEGORIA)

    /** Sesion 11: el listado ya no es un caso de uso de la pantalla; lo usa la sincronizacion. */
    private suspend fun listarCon(engine: MockEngine) = runCatching { repositorioCon(engine).listar() }

    private val productoJson = """
        {"id":11,"nombre":"Omeprazol 20mg","precio":4.5,"stock":40,"estado":true,
         "categoriaId":2,"categoriaNombre":"Antibioticos",
         "fechaCreacion":"2026-10-01T22:44:56.2","fechaModificacion":null}
    """.trimIndent()

    // Forma real de PaginaResponseDTO<ProductoResponseDTO> de PharmaSoft.
    private val paginaValida = """
        {"contenido":[
            {"id":1,"nombre":"Paracetamol 500mg","precio":3.50,"stock":120,"estado":true,
             "categoriaId":1,"categoriaNombre":"Analgesicos",
             "fechaCreacion":"2026-09-30T10:00:00","fechaModificacion":null},
            {"id":2,"nombre":"Ibuprofeno 400mg","precio":5.20,"stock":4,"estado":false,
             "categoriaId":1,"categoriaNombre":"Analgesicos"}
         ],
         "pagina":0,"tamanio":20,"totalElementos":2,"totalPaginas":1,"ultima":true}
    """.trimIndent()

    @Test
    fun caso1_exito200_deserializaLaPaginaYMapeaAlDominio() = runTest {
        var urlPedida = ""
        val engine = MockEngine { request ->
            urlPedida = request.url.toString()
            respond(paginaValida, HttpStatusCode.OK, jsonHeaders)
        }

        val productos = listarCon(engine).getOrThrow()

        assertEquals(2, productos.size)
        with(productos.first()) {
            assertEquals(1L, id)
            assertEquals("Paracetamol 500mg", nombre)
            assertEquals(3.50, precio)
            assertEquals(120, stock)
            assertTrue(activo)
        }
        // estado=false llega como producto dado de baja: la pantalla lo separa para reactivarlo.
        assertFalse(productos.last().activo)
        assertEquals("http://10.0.2.2:8080/api/v1/productos?pagina=0&tamanio=100", urlPedida)
    }

    @Test
    fun caso2_error404_seTraduceANoEncontrado() = runTest {
        val engine = MockEngine { respond("""{"mensaje":"No encontrado"}""", HttpStatusCode.NotFound, jsonHeaders) }

        val fallo = listarCon(engine).exceptionOrNull()

        assertIs<ErrorApiException>(fallo)
        assertEquals(ErrorApi.NoEncontrado, fallo.error)
    }

    @Test
    fun caso3_sinConexion_seTraduceASinConexion() = runTest {
        val engine = MockEngine { throw IOException("Failed to connect to /10.0.2.2:8080") }

        val fallo = listarCon(engine).exceptionOrNull()

        assertIs<ErrorApiException>(fallo)
        assertEquals(ErrorApi.SinConexion, fallo.error)
    }

    @Test
    fun caso4_timeout_seTraduceATiempoAgotado() = runTest {
        val engine = MockEngine { throw HttpRequestTimeoutException("productos", 15_000) }

        val fallo = listarCon(engine).exceptionOrNull()

        assertIs<ErrorApiException>(fallo)
        assertEquals(ErrorApi.TiempoAgotado, fallo.error)
    }

    @Test
    fun caso5_jsonConCampoNuevo_seIgnoraGraciasAIgnoreUnknownKeys() = runTest {
        val conCampoNuevo = """
            {"contenido":[{"id":7,"nombre":"Loratadina","precio":2.0,"stock":30,
              "estado":true,"codigoBarras":"775000123","laboratorio":{"id":3}}],
             "pagina":0,"tamanio":20,"totalElementos":1,"totalPaginas":1,"ultima":true,
             "enlaces":{"siguiente":null}}
        """.trimIndent()
        val engine = MockEngine { respond(conCampoNuevo, HttpStatusCode.OK, jsonHeaders) }

        val productos = listarCon(engine).getOrThrow()

        assertEquals(listOf("Loratadina"), productos.map { it.nombre })
    }

    @Test
    fun caso5b_sinIgnoreUnknownKeys_seTraduceARespuestaInesperada() = runTest {
        val estricto = config.copy(escenario = EscenarioPrueba.JSON_ESTRICTO)
        val engine = MockEngine { respond(paginaValida, HttpStatusCode.OK, jsonHeaders) }
        val remoto = ProductoRemoto(ProductoApi(crearHttpClient(engine, estricto, AlmacenEnMemoria())), CATEGORIA)

        val fallo = runCatching { remoto.listar() }.exceptionOrNull()

        assertIs<ErrorApiException>(fallo)
        assertEquals(ErrorApi.RespuestaInesperada, fallo.error)
    }

    @Test
    fun registroQueRompeReglasDelDominio_seDescartaSinTumbarLaLista() = runTest {
        val json = """{"contenido":[
            {"id":1,"nombre":"Muestra gratis","precio":0,"stock":5,"estado":true},
            {"id":2,"nombre":"Valido","precio":5,"stock":5,"estado":true}],
            "pagina":0,"tamanio":20,"totalElementos":2,"totalPaginas":1,"ultima":true}"""
        val engine = MockEngine { respond(json, HttpStatusCode.OK, jsonHeaders) }

        val productos = listarCon(engine).getOrThrow()

        assertEquals(listOf("Valido"), productos.map { it.nombre })
    }

    // ---------- Sesion 8: los otros cuatro verbos ----------

    @Test
    fun obtener_pideElRecursoPorIdYConservaLaCategoria() = runTest {
        var metodo = ""; var url = ""
        val engine = MockEngine { request ->
            metodo = request.method.value; url = request.url.toString()
            respond(productoJson, HttpStatusCode.OK, jsonHeaders)
        }

        val producto = repositorioCon(engine).obtener(11)

        assertEquals("GET", metodo)
        assertEquals("http://10.0.2.2:8080/api/v1/productos/11", url)
        assertEquals(2L, producto.categoriaId)
    }

    @Test
    fun registrar_enviaPostConLosCincoCamposQueExigeElBackend() = runTest {
        var metodo = ""; var cuerpo = ""
        val engine = MockEngine { request ->
            metodo = request.method.value
            cuerpo = (request.body as TextContent).text
            respond(productoJson, HttpStatusCode.Created, jsonHeaders)
        }

        val creado = repositorioCon(engine).registrar(
            Producto(id = 0, nombre = "Omeprazol 20mg", precio = 4.5, stock = 40)
        )

        assertEquals("POST", metodo)
        assertEquals(
            """{"nombre":"Omeprazol 20mg","precio":4.5,"stock":40,"estado":true,"categoriaId":1}""",
            cuerpo
        )
        assertEquals(11L, creado.id)
    }

    @Test
    fun actualizar_enviaPutAlIdYNoPisaLaCategoriaDelProducto() = runTest {
        var metodo = ""; var url = ""; var cuerpo = ""
        val engine = MockEngine { request ->
            metodo = request.method.value; url = request.url.toString()
            cuerpo = (request.body as TextContent).text
            respond(productoJson, HttpStatusCode.OK, jsonHeaders)
        }

        repositorioCon(engine).actualizar(
            Producto(id = 11, nombre = "Omeprazol 20mg", precio = 5.0, stock = 35, categoriaId = 2)
        )

        assertEquals("PUT", metodo)
        assertEquals("http://10.0.2.2:8080/api/v1/productos/11", url)
        assertTrue(cuerpo.contains(""""categoriaId":2"""), cuerpo)
    }

    @Test
    fun actualizar_conActivoTrue_enviaEstadoTrueParaReactivar() = runTest {
        var cuerpo = ""
        val engine = MockEngine { request ->
            cuerpo = (request.body as TextContent).text
            respond(productoJson, HttpStatusCode.OK, jsonHeaders)
        }
        val dadoDeBaja = Producto(id = 11, nombre = "Omeprazol 20mg", precio = 4.5, stock = 40, categoriaId = 2, activo = false)

        repositorioCon(engine).actualizar(dadoDeBaja.copy(activo = true))

        assertTrue(cuerpo.contains(""""estado":true"""), cuerpo)
    }

    @Test
    fun eliminar_aceptaEl204SinCuerpo() = runTest {
        var metodo = ""; var url = ""
        val engine = MockEngine { request ->
            metodo = request.method.value; url = request.url.toString()
            respond("", HttpStatusCode.NoContent)
        }

        repositorioCon(engine).eliminar(11)

        assertEquals("DELETE", metodo)
        assertEquals("http://10.0.2.2:8080/api/v1/productos/11", url)
    }

    @Test
    fun registrar_conNombreDuplicado_lanzaConflictoConElMensajeDelServidor() = runTest {
        val engine = MockEngine {
            respond(
                """{"status":409,"error":"Conflict","message":"Ya existe un producto con el nombre Omeprazol 20mg",
                    "path":"/api/v1/productos","validationErrors":null}""",
                HttpStatusCode.Conflict, jsonHeaders
            )
        }

        val fallo = assertFailsWith<ErrorApiException> {
            repositorioCon(engine).registrar(Producto(id = 0, nombre = "Omeprazol 20mg", precio = 4.5, stock = 40))
        }

        assertEquals(ErrorApi.Conflicto("Ya existe un producto con el nombre Omeprazol 20mg"), fallo.error)
    }
}
