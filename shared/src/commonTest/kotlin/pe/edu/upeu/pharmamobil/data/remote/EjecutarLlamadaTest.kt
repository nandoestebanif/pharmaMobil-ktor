package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.local.AlmacenEnMemoria
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * El traductor de excepciones de Ktor a ErrorApi, probado con los cuerpos
 * de error reales de PharmaSoft (ErrorResponseDTO).
 */
class EjecutarLlamadaTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val config = ConfiguracionApi(urlBase = "http://10.0.2.2:8080/api/v1/")

    private suspend fun errorAl(status: HttpStatusCode, cuerpo: String): ErrorApi {
        val cliente = crearHttpClient(MockEngine { respond(cuerpo, status, jsonHeaders) }, config, AlmacenEnMemoria())
        val fallo = ejecutarLlamada { cliente.get("productos").body<String>() }.exceptionOrNull()
        return assertIs<ErrorApiException>(fallo).error
    }

    @Test
    fun el400TraeLosErroresPorCampoDelBackend() = runTest {
        val cuerpo = """
            {"timestamp":"2026-10-01T22:44:56.25","status":400,"error":"Bad Request",
             "message":"Existen errores de validación","path":"/api/v1/productos",
             "validationErrors":{"nombre":"El nombre debe tener entre 3 y 150 caracteres",
                                 "precio":"El precio debe ser mayor que cero"}}
        """.trimIndent()

        val error = assertIs<ErrorApi.Validacion>(errorAl(HttpStatusCode.BadRequest, cuerpo))

        assertEquals("El nombre debe tener entre 3 y 150 caracteres", error.porCampo["nombre"])
        assertEquals("El precio debe ser mayor que cero", error.porCampo["precio"])
    }

    @Test
    fun el400SinCuerpoLegibleDevuelveValidacionVacia() = runTest {
        val error = assertIs<ErrorApi.Validacion>(errorAl(HttpStatusCode.BadRequest, "no es json"))

        assertEquals(emptyMap(), error.porCampo)
    }

    @Test
    fun el404EsNoEncontrado() = runTest {
        val cuerpo = """{"status":404,"error":"Not Found","message":"Producto no encontrado con id: 999999",
            "path":"/api/v1/productos/999999","validationErrors":null}"""

        assertEquals(ErrorApi.NoEncontrado, errorAl(HttpStatusCode.NotFound, cuerpo))
    }

    @Test
    fun el409ConservaElMensajeDelServidor() = runTest {
        val cuerpo = """{"status":409,"error":"Conflict","message":"Ya existe un producto con el nombre Omeprazol 20mg",
            "path":"/api/v1/productos","validationErrors":null}"""

        assertEquals(
            ErrorApi.Conflicto("Ya existe un producto con el nombre Omeprazol 20mg"),
            errorAl(HttpStatusCode.Conflict, cuerpo)
        )
    }

    @Test
    fun el401EsNoAutenticadoYEl403SinPermiso() = runTest {
        assertEquals(ErrorApi.NoAutenticado, errorAl(HttpStatusCode.Unauthorized, "{}"))
        assertEquals(ErrorApi.SinPermiso, errorAl(HttpStatusCode.Forbidden, "{}"))
    }

    @Test
    fun el500EsErrorDeServidor() = runTest {
        assertEquals(ErrorApi.Servidor, errorAl(HttpStatusCode.InternalServerError, "{}"))
    }

    @Test
    fun laCancelacionSeRelanzaYNoSeConvierteEnFailure() = runTest {
        assertFailsWith<CancellationException> {
            ejecutarLlamada<Unit> { throw CancellationException("pantalla cerrada") }
        }
    }
}
