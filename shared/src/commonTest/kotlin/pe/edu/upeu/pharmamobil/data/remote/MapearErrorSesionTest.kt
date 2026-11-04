package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.http.HttpStatusCode
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesion
import kotlin.test.Test
import kotlin.test.assertEquals

/** Los mensajes son los que devuelve PharmaSoft (JwtAuthenticationFilter y AuthServiceImpl). */
class MapearErrorSesionTest {

    @Test
    fun un401EnElLoginSonCredencialesInvalidas() {
        assertEquals(
            ErrorSesion.CredencialesInvalidas,
            mapearErrorSesion(HttpStatusCode.Unauthorized, "Usuario o contraseña incorrectos", enLogin = true)
        )
    }

    @Test
    fun un401ConElMensajeDeExpiracionEsSesionExpirada() {
        assertEquals(
            ErrorSesion.SesionExpirada,
            mapearErrorSesion(HttpStatusCode.Unauthorized, "El token ha expirado. Inicie sesión nuevamente")
        )
    }

    @Test
    fun un401ConUnTokenAlteradoEsSesionInvalida() {
        assertEquals(ErrorSesion.SesionInvalida, mapearErrorSesion(HttpStatusCode.Unauthorized, "El token no es válido"))
        assertEquals(ErrorSesion.SesionInvalida, mapearErrorSesion(HttpStatusCode.Unauthorized, null))
    }

    @Test
    fun un401DeUnUsuarioDeshabilitadoEsUsuarioDeshabilitado() {
        assertEquals(
            ErrorSesion.UsuarioDeshabilitado,
            mapearErrorSesion(HttpStatusCode.Unauthorized, "El usuario está deshabilitado")
        )
    }

    @Test
    fun sinCabeceraOConPrefijoMalEscritoElServidorDiceQueDebeIniciarSesion() {
        // «Debe iniciar sesión...» es el 401 de Spring Security (sin token util): sesion invalida.
        assertEquals(
            ErrorSesion.SesionInvalida,
            mapearErrorSesion(HttpStatusCode.Unauthorized, "Debe iniciar sesión para acceder a este recurso")
        )
    }

    @Test
    fun un403EsFaltaDePermisos() {
        assertEquals(
            ErrorSesion.SinPermiso,
            mapearErrorSesion(HttpStatusCode.Forbidden, "No tiene permisos para realizar esta operación")
        )
    }
}
