package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
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
import pe.edu.upeu.pharmamobil.domain.sync.PoliticaDeReintento
import pe.edu.upeu.pharmamobil.domain.sync.PoliticaDeReintento.MAX_INTENTOS
import pe.edu.upeu.pharmamobil.permiso.TelemetriaFalsa
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sesion 13, paso 7: la sincronizacion refinada sin red y sin dispositivo. Base SQLite en
 * memoria (una nueva por prueba) y PharmaSoft falso con MockEngine. Las esperas no se
 * duermen de verdad: se anotan para comprobar que crecen.
 */
class SincronizacionTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun pagina(vararg productos: String) =
        """{"contenido":[${productos.joinToString(",")}],"pagina":0,"tamanio":100,
            "totalElementos":${productos.size},"totalPaginas":1,"ultima":true}"""

    private fun productoJson(id: Long, nombre: String, precio: Double = 4.5, stock: Int = 40) =
        """{"id":$id,"nombre":"$nombre","precio":$precio,"stock":$stock,"estado":true,"categoriaId":1}"""

    private val paracetamol = productoJson(1, "Paracetamol 500mg", 3.5, 120)
    private val omeprazol = Producto(0, "Omeprazol 20mg", 4.5, 40)

    private class Montaje(
        val repo: ProductoRepositorioOffline,
        val local: ProductoLocal,
        val posts: () -> Int,
        val esperas: List<Long>,
        val telemetria: TelemetriaFalsa
    ) {
        suspend fun fila(id: Long) = local.obtener(id)!!
    }

    /** POST y GET responden lo que diga cada prueba; las peticiones que llegan se cuentan. */
    private fun montaje(alPost: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
                        alGet: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { respond(pagina(paracetamol), HttpStatusCode.OK, json) }): Montaje {
        var posts = 0
        val motor = MockEngine { p -> if (p.method == HttpMethod.Post) { posts++; alPost(p) } else alGet(p) }
        val cliente = crearHttpClient(motor, ConfiguracionApi(urlBase = "http://localhost:8080/api/v1/", registroHttp = false), AlmacenEnMemoria("t.o.k"))
        val local = ProductoLocal(baseEnMemoria())
        val esperas = mutableListOf<Long>()
        val telemetria = TelemetriaFalsa()
        val repo = ProductoRepositorioOffline(local, ProductoRemoto(ProductoApi(cliente), 1L),
            esperar = { esperas += it }, telemetria = telemetria)
        return Montaje(repo, local, { posts }, esperas, telemetria)
    }

    private fun MockRequestHandleScope.error(status: HttpStatusCode, mensaje: String) = respond(
        """{"timestamp":"x","status":${status.value},"error":"${status.description}","message":"$mensaje","path":"/api/v1/productos"}""",
        status, json
    )

    @Test
    fun unFalloTransitorioSumaIntentoYNoMarcaFallida() = runTest {
        val m = montaje(alPost = { error(HttpStatusCode.ServiceUnavailable, "Servicio no disponible") })
        val pendiente = m.local.guardarPendiente(omeprazol)

        val resumen = m.repo.sincronizar()

        val fila = m.fila(pendiente.id)
        assertEquals(1, fila.intentos)
        assertTrue(fila.pendiente)
        assertFalse(fila.envioFallido)
        assertEquals("El servidor tuvo un problema", fila.ultimoError)
        assertEquals(listOf(1_000L), m.esperas, "espera antes de seguir")
        assertEquals(1L, resumen.pendientes)
        assertEquals(0L, resumen.fallidas)
    }

    @Test
    fun unFalloPermanenteMarcaLaFilaYNoLaReintenta() = runTest {
        val m = montaje(alPost = { error(HttpStatusCode.Conflict, "Ya existe un producto con el nombre Paracetamol 500mg") })
        val pendiente = m.local.guardarPendiente(Producto(0, "Paracetamol 500mg", 3.5, 10))

        val primera = m.repo.sincronizar()
        val segunda = m.repo.sincronizar()

        val fila = m.fila(pendiente.id)
        assertTrue(fila.envioFallido, "queda fallida y visible")
        assertEquals("Ya existe un producto con el nombre Paracetamol 500mg", fila.ultimoError)
        assertEquals(1, m.posts(), "la segunda sincronizacion ya no la envia")
        assertEquals(1, primera.nuevasFallidas)
        assertEquals(0, segunda.nuevasFallidas)
        assertEquals(1L, segunda.fallidas)
        assertTrue(m.esperas.isEmpty(), "un fallo permanente no espera")
        assertEquals("envio_rechazado", m.telemetria.eventos.first().first)
    }

    @Test
    fun laEsperaCreceEntreIntentosYTieneTope() {
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 16_000L),
            (0..5).map { PoliticaDeReintento.espera(it) })
    }

    @Test
    fun alAgotarLosIntentosLaFilaQuedaFallidaYNoSeReintentaSola() = runTest {
        val m = montaje(alPost = { throw HttpRequestTimeoutException("productos", 15_000) })
        val pendiente = m.local.guardarPendiente(omeprazol)

        val resumenes = (1..MAX_INTENTOS).map { m.repo.sincronizar() }
        m.repo.sincronizar()

        assertEquals(MAX_INTENTOS, m.fila(pendiente.id).intentos)
        assertTrue(m.fila(pendiente.id).envioFallido)
        assertEquals(listOf(0, 0, 0, 0, 1), resumenes.map { it.nuevasFallidas }, "se cuenta como fallida en el ultimo intento")
        assertEquals(MAX_INTENTOS, m.posts(), "la sexta sincronizacion ya no la envia")
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L), m.esperas)
    }

    @Test
    fun sinRedSeDetieneYNoGastaIntentos() = runTest {
        val m = montaje(alPost = { throw IOException("modo avion") }, alGet = { throw IOException("modo avion") })
        val pendiente = m.local.guardarPendiente(omeprazol)

        val fallo = assertFailsWith<ErrorApiException> { m.repo.sincronizar() }

        assertEquals(ErrorApi.SinConexion, fallo.error)
        assertEquals(0, m.fila(pendiente.id).intentos, "la peticion nunca llego: no cuenta")
        assertNull(m.fila(pendiente.id).ultimoError)
    }

    @Test
    fun laSesionVencidaNoMarcaFallidoUnProductoValido() = runTest {
        val m = montaje(alPost = { error(HttpStatusCode.Unauthorized, "El token ha expirado. Inicie sesión nuevamente") })
        val pendiente = m.local.guardarPendiente(omeprazol)

        val fallo = assertFailsWith<ErrorApiException> { m.repo.sincronizar() }

        assertEquals(ErrorApi.NoAutenticado, fallo.error)
        assertEquals(0, m.fila(pendiente.id).intentos)
        assertFalse(m.fila(pendiente.id).envioFallido)
    }

    @Test
    fun reintentarDevuelveLasFallidasALaColaYLasEnvia() = runTest {
        var aceptar = false
        val m = montaje(alPost = {
            if (aceptar) respond(productoJson(11, "Omeprazol 20mg"), HttpStatusCode.Created, json)
            else error(HttpStatusCode.BadRequest, "Existen errores de validación")
        }, alGet = { respond(pagina(paracetamol, productoJson(11, "Omeprazol 20mg")), HttpStatusCode.OK, json) })
        val pendiente = m.local.guardarPendiente(omeprazol)
        m.repo.sincronizar()
        assertTrue(m.fila(pendiente.id).envioFallido)

        aceptar = true
        val resumen = m.repo.reintentarFallidas()

        assertEquals(1, resumen.enviadas)
        assertEquals(0L, resumen.fallidas)
        assertEquals(11L, m.repo.observar().first().single { it.nombre == "Omeprazol 20mg" }.id)
    }

    @Test
    fun corregirUnaFilaFallidaLaDevuelveALaCola() = runTest {
        val m = montaje(alPost = { error(HttpStatusCode.Conflict, "Ya existe un producto con el nombre Paracetamol 500mg") })
        val pendiente = m.local.guardarPendiente(Producto(0, "Paracetamol 500mg", 3.5, 10))
        m.repo.sincronizar()

        m.repo.actualizar(m.fila(pendiente.id).copy(nombre = "Paracetamol 1g"))

        assertEquals(0, m.fila(pendiente.id).intentos)
        assertNull(m.fila(pendiente.id).ultimoError)
    }

    @Test
    fun unReenvioDespuesDeUnTiempoAgotadoNoDuplicaElProducto() = runTest {
        // El primer POST llega al servidor pero la respuesta se pierde (tiempo agotado).
        // Al repetirlo, PharmaSoft dice 409 «ya existe»: si el que existe es identico, era el mismo envio.
        var primero = true
        val m = montaje(alPost = {
            if (primero) { primero = false; throw HttpRequestTimeoutException("productos", 15_000) }
            error(HttpStatusCode.Conflict, "Ya existe un producto con el nombre Omeprazol 20mg")
        }, alGet = { respond(pagina(paracetamol, productoJson(11, "Omeprazol 20mg")), HttpStatusCode.OK, json) })
        val pendiente = m.local.guardarPendiente(omeprazol)

        m.repo.sincronizar()
        val resumen = m.repo.sincronizar()

        assertEquals(1, resumen.enviadas)
        assertEquals(0L, resumen.fallidas)
        assertNull(m.local.obtener(pendiente.id), "la fila local paso a tener el id real")
        assertEquals(1, m.repo.observar().first().count { it.nombre == "Omeprazol 20mg" }, "sin duplicados")
    }

    @Test
    fun subirAntesDeBajarNoPierdeLoPendienteAunqueFalle() = runTest {
        val m = montaje(alPost = { error(HttpStatusCode.ServiceUnavailable, "Servicio no disponible") })
        val pendiente = m.local.guardarPendiente(omeprazol)

        m.repo.sincronizar()

        val lista = m.repo.observar().first()
        assertEquals(2, lista.size, "la bajada trajo Paracetamol y no borro el pendiente")
        assertTrue(lista.any { it.id == pendiente.id && it.pendiente })
    }

    @Test
    fun registrarConElServidorFallandoNoPierdeElProducto() = runTest {
        val m = montaje(alPost = { error(HttpStatusCode.ServiceUnavailable, "Servicio no disponible") })

        val guardado = m.repo.registrar(omeprazol)

        val fila = m.fila(guardado.id)
        assertTrue(fila.pendiente, "un 503 es pasajero: el producto se queda en el dispositivo")
        assertEquals(1, fila.intentos)
    }
}
