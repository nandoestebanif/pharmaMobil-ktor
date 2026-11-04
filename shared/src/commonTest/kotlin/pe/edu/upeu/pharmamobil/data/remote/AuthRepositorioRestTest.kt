package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import pe.edu.upeu.pharmamobil.data.local.AlmacenEnMemoria
import pe.edu.upeu.pharmamobil.data.local.PerfilLocal
import pe.edu.upeu.pharmamobil.data.local.baseEnMemoria
import pe.edu.upeu.pharmamobil.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobil.data.remote.ProductoRemoto
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sesion 10: el login, el plugin Auth y el plugin SesionVencida probados con
 * MockEngine. El motor falso responde como PharmaSoft y guarda cada peticion
 * para revisar la cabecera Authorization que realmente salio.
 */
class AuthRepositorioRestTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val config = ConfiguracionApi(urlBase = "http://localhost:8080/api/v1/")

    private val loginAdmin = """
        {"token":"aaa.bbb.ccc","tipo":"Bearer","expiraEnSegundos":3600,"username":"admin",
         "nombreCompleto":"Carlos Ramirez Torres","roles":["ADMIN"],
         "permisos":["CATALOGO_LEER","CATALOGO_ESCRIBIR","CLIENTES_LEER"]}
    """.trimIndent()

    private val paginaVacia =
        """{"contenido":[],"pagina":0,"tamanio":20,"totalElementos":0,"totalPaginas":0,"ultima":true}"""

    private fun error(status: Int, mensaje: String) =
        """{"timestamp":"2026-10-14T10:00:00","status":$status,"error":"x","message":"$mensaje","path":"/api/v1/x"}"""

    /** Conjunto listo para probar: motor falso, almacen en memoria y el repositorio. */
    private class Escenario(
        val almacen: AlmacenEnMemoria,
        val avisos: AvisosDeSesion,
        val peticiones: MutableList<HttpRequestData>,
        motor: MockEngine,
        config: ConfiguracionApi
    ) {
        val cliente = crearHttpClient(motor, config, almacen, avisos)
        // Sesion 11: con perfil local (base SQLite en memoria, nueva por prueba).
        val perfiles = PerfilLocal(baseEnMemoria())
        val auth = AuthRepositorioRest(cliente, almacen, avisos, perfiles)
        val productos = ProductoRemoto(ProductoApi(cliente), 1L)

        fun authorizationDe(ruta: String): List<String?> =
            peticiones.filter { it.url.encodedPath.endsWith(ruta) }.map { it.headers[HttpHeaders.Authorization] }
    }

    private fun escenario(
        token: String? = null,
        responder: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): Escenario {
        val peticiones = mutableListOf<HttpRequestData>()
        val motor = MockEngine { peticion ->
            peticiones += peticion
            responder(peticion)
        }
        return Escenario(AlmacenEnMemoria(token), AvisosDeSesion(), peticiones, motor, config)
    }

    @Test
    fun elLoginGuardaElTokenYNoEnviaAuthorization() = runTest {

        val e = escenario { respond(loginAdmin, HttpStatusCode.OK, json) }

        val sesion = e.auth.iniciarSesion(Credenciales("admin", "Admin123"))

        assertEquals("aaa.bbb.ccc", e.almacen.token)
        assertEquals("Carlos Ramirez Torres", sesion.nombreCompleto)
        assertTrue(sesion.puede("CATALOGO_ESCRIBIR"))
        assertEquals(listOf<String?>(null), e.authorizationDe("auth/login"))
    }

    @Test
    fun trasElLoginLaPrimeraPeticionYaLlevaElToken() = runTest {

        val e = escenario { peticion ->
            if (peticion.url.encodedPath.endsWith("auth/login")) respond(loginAdmin, HttpStatusCode.OK, json)
            else respond(paginaVacia, HttpStatusCode.OK, json)
        }

        e.auth.iniciarSesion(Credenciales("admin", "Admin123"))
        e.productos.listar()

        // Una sola peticion al listado, y ya con el token: sendWithoutRequest evito
        // el ciclo de enviar sin token, recibir 401 y repetir.
        assertEquals(listOf<String?>("Bearer aaa.bbb.ccc"), e.authorizationDe("productos"))
    }

    @Test
    fun unSegundoLoginUsaElTokenNuevoYNoElDeLaCache() = runTest {

        var vuelta = 0
        val e = escenario { peticion ->
            if (peticion.url.encodedPath.endsWith("auth/login")) {
                vuelta++
                respond(loginAdmin.replace("aaa.bbb.ccc", "token.numero.$vuelta"), HttpStatusCode.OK, json)
            } else {
                respond(paginaVacia, HttpStatusCode.OK, json)
            }
        }

        e.auth.iniciarSesion(Credenciales("admin", "Admin123"))
        e.productos.listar()
        e.auth.cerrarSesion()
        e.auth.iniciarSesion(Credenciales("almacen", "Almacen123"))
        e.productos.listar()

        assertEquals(listOf<String?>("Bearer token.numero.1", "Bearer token.numero.2"), e.authorizationDe("productos"))
    }

    @Test
    fun unLoginFallidoNoSeRepiteConElTokenAnterior() = runTest {

        val e = escenario(token = "anterior.x.y") {
            respond(error(401, "Usuario o contraseña incorrectos"), HttpStatusCode.Unauthorized, json)
        }

        assertFailsWith<ErrorSesionException> { e.auth.iniciarSesion(Credenciales("admin", "Incorrecta1")) }

        assertEquals(listOf<String?>(null), e.authorizationDe("auth/login"))
    }

    @Test
    fun credencialesIncorrectasNoGuardanToken() = runTest {

        val e = escenario { respond(error(401, "Usuario o contraseña incorrectos"), HttpStatusCode.Unauthorized, json) }

        val fallo = assertFailsWith<ErrorSesionException> {
            e.auth.iniciarSesion(Credenciales("admin", "Incorrecta1"))
        }

        assertEquals(ErrorSesion.CredencialesInvalidas, fallo.error)
        assertNull(e.almacen.token)
    }

    @Test
    fun un401EnCualquierPeticionBorraElTokenYAvisaQueExpiro() = runTest {

        val e = escenario(token = "vencido.x.y") {
            respond(error(401, "El token ha expirado. Inicie sesión nuevamente"), HttpStatusCode.Unauthorized, json)
        }
        val aviso = async(UnconfinedTestDispatcher(testScheduler)) { e.avisos.sesionTerminada.first() }

        val fallo = assertFailsWith<ErrorApiException> { e.productos.listar() }

        assertEquals(ErrorApi.NoAutenticado, fallo.error)
        assertNull(e.almacen.token)
        assertEquals(ErrorSesion.SesionExpirada, aviso.await())
    }

    @Test
    fun un401DeUsuarioDeshabilitadoBorraElTokenYAvisaConSuPropioMotivo() = runTest {

        val e = escenario(token = "valido.x.y") {
            respond(error(401, "El usuario está deshabilitado"), HttpStatusCode.Unauthorized, json)
        }
        val aviso = async(UnconfinedTestDispatcher(testScheduler)) { e.avisos.sesionTerminada.first() }

        assertFailsWith<ErrorApiException> { e.productos.listar() }

        assertNull(e.almacen.token)
        assertEquals(ErrorSesion.UsuarioDeshabilitado, aviso.await())
    }

    @Test
    fun siempreSeEnviaElPrefijoBearerConMayuscula() = runTest {

        val e = escenario(token = "valido.x.y") { respond(paginaVacia, HttpStatusCode.OK, json) }

        e.productos.listar()

        // El servidor solo reconoce «Bearer » (con mayuscula): la app nunca manda «bearer».
        assertEquals(listOf<String?>("Bearer valido.x.y"), e.authorizationDe("productos"))
    }

    @Test
    fun un403MantieneLaSesionYNoAvisa() = runTest {

        val e = escenario(token = "valido.x.y") {
            respond(error(403, "No tiene permisos para realizar esta operación"), HttpStatusCode.Forbidden, json)
        }

        val fallo = assertFailsWith<ErrorApiException> { e.productos.listar() }
        val aviso = withTimeoutOrNull(100) { e.avisos.sesionTerminada.first() }

        assertEquals(ErrorApi.SinPermiso, fallo.error)
        assertEquals("valido.x.y", e.almacen.token)
        assertNull(aviso)
    }

    @Test
    fun sinServidorSeEntraConElPerfilDeLaUltimaSesion() = runTest {

        var servidorEncendido = true
        val e = escenario { peticion ->
            if (!servidorEncendido) throw kotlinx.io.IOException("Connection refused")
            respond(loginAdmin, HttpStatusCode.OK, json)
        }
        e.auth.iniciarSesion(Credenciales("admin", "Admin123"))

        // Se cierra la app y PharmaSoft se apaga: GET auth/me no llega al servidor.
        servidorEncendido = false
        val sesion = e.auth.sesionGuardada()

        assertEquals("Carlos Ramirez Torres", sesion?.nombreCompleto)
        assertEquals("aaa.bbb.ccc", sesion?.token)
        assertTrue(sesion?.puede("CATALOGO_ESCRIBIR") == true)
    }

    @Test
    fun cerrarSesionBorraTambienElPerfilLocal() = runTest {

        var servidorEncendido = true
        val e = escenario { peticion ->
            if (!servidorEncendido) throw kotlinx.io.IOException("Connection refused")
            respond(loginAdmin, HttpStatusCode.OK, json)
        }
        e.auth.iniciarSesion(Credenciales("admin", "Admin123"))
        e.auth.cerrarSesion()
        servidorEncendido = false

        assertNull(e.auth.sesionGuardada())
        assertNull(e.perfiles.leer("x"))
    }

    @Test
    fun sinTokenGuardadoNoHaySesionNiPeticiones() = runTest {

        val e = escenario { respond("{}", HttpStatusCode.OK, json) }

        assertNull(e.auth.sesionGuardada())
        assertTrue(e.peticiones.isEmpty())
    }

    @Test
    fun conTokenGuardadoRecuperaLaSesionDesdeAuthMe() = runTest {

        val e = escenario(token = "guardado.x.y") {
            respond(
                """{"id":3,"username":"almacen","empleadoNombre":"Jorge Mendoza Huaman",
                   "roles":["ALMACENERO"],"permisos":["CATALOGO_LEER","CATALOGO_ESCRIBIR"]}""",
                HttpStatusCode.OK, json
            )
        }

        val sesion = e.auth.sesionGuardada()

        assertEquals("almacen", sesion?.usuario)
        assertEquals(false, sesion?.puede("CLIENTES_LEER"))
        assertEquals(listOf<String?>("Bearer guardado.x.y"), e.authorizationDe("auth/me"))
    }

    @Test
    fun unTokenGuardadoVencidoTerminaEnSesionExpirada() = runTest {

        val e = escenario(token = "vencido.x.y") {
            respond(error(401, "El token ha expirado. Inicie sesión nuevamente"), HttpStatusCode.Unauthorized, json)
        }

        val fallo = assertFailsWith<ErrorSesionException> { e.auth.sesionGuardada() }

        assertEquals(ErrorSesion.SesionExpirada, fallo.error)
        assertNull(e.almacen.token)
    }

    @Test
    fun cerrarSesionBorraElTokenYLaSiguientePeticionVaSinEl() = runTest {

        val e = escenario(token = "valido.x.y") { respond(paginaVacia, HttpStatusCode.OK, json) }

        e.productos.listar()
        e.auth.cerrarSesion()
        e.productos.listar()

        assertNull(e.almacen.token)
        assertEquals(listOf("Bearer valido.x.y", null), e.authorizationDe("productos"))
    }

    @Test
    fun sinServidorElLoginFallaPorConexion() = runTest {

        val e = escenario { throw kotlinx.io.IOException("Connection refused") }

        val fallo = assertFailsWith<ErrorSesionException> {
            e.auth.iniciarSesion(Credenciales("admin", "Admin123"))
        }

        assertIs<ErrorSesion.SinConexion>(fallo.error)
    }
}
