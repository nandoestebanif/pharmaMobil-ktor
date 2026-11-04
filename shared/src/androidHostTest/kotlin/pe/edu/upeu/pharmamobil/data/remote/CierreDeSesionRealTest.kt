package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.runBlocking
import pe.edu.upeu.pharmamobil.data.local.AlmacenEnMemoria
import pe.edu.upeu.pharmamobil.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.pharmamobil.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Sesion 10 (actividad autonoma, producto 3, efecto 2): el cierre de sesion contra
 * PharmaSoft DE VERDAD, con el cliente HTTP de la app (OkHttp + plugin Auth).
 *
 * Solo corre si existe la variable de entorno PHARMASOFT_URL (por ejemplo
 * http://localhost:8080/api/v1/); sin ella la prueba se omite y no afecta a las demas.
 * El registro «KtorHttp» muestra cada peticion con Authorization saneado: tras el
 * cierre de sesion la peticion sale SIN esa cabecera y el servidor responde 401.
 */
class CierreDeSesionRealTest {

    @Test
    fun trasCerrarSesionLaSiguientePeticionSaleSinTokenYDa401() = runBlocking {

        val url = System.getenv("PHARMASOFT_URL")
        if (url.isNullOrBlank()) {
            println("PHARMASOFT_URL no esta definida: prueba omitida")
            return@runBlocking
        }

        val almacen = AlmacenEnMemoria()
        val avisos = AvisosDeSesion()
        val cliente = crearHttpClient(OkHttp.create(), ConfiguracionApi(urlBase = url), almacen, avisos)
        val auth = AuthRepositorioRest(cliente, almacen, avisos)

        println("== 1. Login de vendedor")
        val sesion = auth.iniciarSesion(Credenciales("vendedor", "Vendedor123"))
        assertNotNull(almacen.token)
        println("   sesion de ${sesion.usuario}; token en el almacen: SI")

        println("== 2. GET productos con la sesion abierta")
        val conSesion: HttpResponse = cliente.get("productos")
        println("   HTTP ${conSesion.status.value}")
        assertEquals(200, conSesion.status.value)

        println("== 3. cerrarSesion()")
        auth.cerrarSesion()
        assertNull(almacen.token)
        println("   token en el almacen: NO (borrado " + almacen.vecesBorrado + " vez)")

        println("== 4. GET productos tras cerrar sesion (el cliente no debe recordar el token)")
        val fallo = runCatching { cliente.get("productos") }.exceptionOrNull()
        val respuesta = (fallo as? ClientRequestException)?.response
        assertNotNull(respuesta, "la peticion sin token debia fallar con 4xx, fallo: $fallo")
        val mensaje = runCatching { respuesta.body<ErrorResponseDto>().message }.getOrNull()
        println("   HTTP ${respuesta.status.value}  message: \"$mensaje\"")
        assertEquals(401, respuesta.status.value)

        cliente.close()
    }
}
