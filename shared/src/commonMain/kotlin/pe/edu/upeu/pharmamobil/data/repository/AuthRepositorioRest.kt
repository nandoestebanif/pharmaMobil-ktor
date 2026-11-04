package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.io.IOException
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.PerfilLocal
import pe.edu.upeu.pharmamobil.data.remote.AvisosDeSesion
import pe.edu.upeu.pharmamobil.data.remote.RUTA_LOGIN
import pe.edu.upeu.pharmamobil.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.pharmamobil.data.remote.dto.LoginRequestDto
import pe.edu.upeu.pharmamobil.data.remote.dto.LoginResponseDto
import pe.edu.upeu.pharmamobil.data.remote.dto.UsuarioActualDto
import pe.edu.upeu.pharmamobil.data.remote.mapearErrorSesion
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesionException
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository

/**
 * Sesion contra PharmaSoft. Traduce el login al modelo de dominio y guarda el
 * token en el almacen cifrado de la plataforma. No escribe la cabecera
 * Authorization: de eso se encarga el plugin Auth del cliente.
 */
class AuthRepositorioRest(
    private val cliente: HttpClient,
    private val almacen: AlmacenSeguro,
    private val avisos: AvisosDeSesion,
    /** Sesion 11: perfil de la ultima sesion para entrar sin servidor (null = sin cache). */
    private val perfiles: PerfilLocal? = null
) : AuthRepository {

    override val sesionTerminada: Flow<ErrorSesion> = avisos.sesionTerminada

    override suspend fun iniciarSesion(credenciales: Credenciales): Sesion = traducirErrores(enLogin = true) {
        val dto: LoginResponseDto = cliente.post(RUTA_LOGIN) {
            // El plugin Auth no interviene en el login: sin esto, ante un 401
            // repetiria la peticion con el token anterior.
            attributes.put(AuthCircuitBreaker, Unit)
            setBody(LoginRequestDto(credenciales.usuario, credenciales.clave))
        }.body()
        // El almacen cifrado hace E/S y criptografia (Keystore, Keychain): fuera del hilo principal.
        withContext(Dispatchers.IO) { almacen.guardarToken(dto.token) }
        // loadTokens quedo en cache con el valor anterior (o con null): se vacia
        // para que la proxima peticion lea el token recien guardado.
        cliente.authProvider<BearerAuthProvider>()?.clearToken()
        dto.aSesion().also { perfiles?.guardar(it) }
    }

    /**
     * Si hay token, pregunta al servidor de quien es (GET auth/me): asi se
     * comprueba que sigue vigente y se recuperan nombre y permisos actuales.
     * Un 401 aqui significa token vencido o invalido: el plugin SesionVencida
     * ya lo borro.
     */
    override suspend fun sesionGuardada(): Sesion? {
        val token = withContext(Dispatchers.IO) { almacen.leerToken() } ?: return null
        return try {
            traducirErrores(enLogin = false) {
                cliente.get("auth/me").body<UsuarioActualDto>().aSesion(token)
            }.also { perfiles?.guardar(it) }
        } catch (sinRed: ErrorSesionException) {
            // Sesion 11 (offline-first): sin servidor se entra con el perfil guardado y la app
            // muestra el inventario local. Si el token ya no vale, el primer 401 al volver la
            // red cierra la sesion (plugin SesionVencida). Sin perfil, se informa el error.
            if (sinRed.error == ErrorSesion.SinConexion) perfiles?.leer(token) ?: throw sinRed
            else throw sinRed
        }
    }

    override suspend fun cerrarSesion() {
        withContext(Dispatchers.IO) { almacen.borrarToken() }
        perfiles?.borrar()
        cliente.authProvider<BearerAuthProvider>()?.clearToken()
    }

    private suspend fun <T> traducirErrores(enLogin: Boolean, bloque: suspend () -> T): T =
        try {
            bloque()
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (e: ClientRequestException) {
            val mensaje = runCatching { e.response.body<ErrorResponseDto>().message }.getOrNull()
            throw ErrorSesionException(mapearErrorSesion(e.response.status, mensaje, enLogin))
        } catch (e: ResponseException) {
            throw ErrorSesionException(mapearErrorSesion(e.response.status, null, enLogin))
        } catch (e: HttpRequestTimeoutException) {
            throw ErrorSesionException(ErrorSesion.SinConexion)
        } catch (e: IOException) {                 // sin red, servidor apagado, timeout de socket
            throw ErrorSesionException(ErrorSesion.SinConexion)
        }
}

private fun LoginResponseDto.aSesion() = Sesion(
    token = token,
    usuario = username,
    nombreCompleto = nombreCompleto,
    permisos = permisos.toSet(),
    roles = roles
)

private fun UsuarioActualDto.aSesion(token: String) = Sesion(
    token = token,
    usuario = username,
    nombreCompleto = empleadoNombre ?: username,
    permisos = permisos.toSet(),
    roles = roles
)
