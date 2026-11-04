package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.pharmamobil.platform.escribirRegistro

private const val RETARDO_RESPUESTA_LENTA_MS = 8_000L

/** Ruta del login (relativa a la URL base): la unica que no lleva token. */
internal const val RUTA_LOGIN = "auth/login"

/**
 * Unico punto donde se configura el cliente HTTP. Es comun a Android e iOS:
 * lo que cambia por plataforma (motor y URL base) llega inyectado por Koin.
 *
 * Crear un HttpClient es costoso (pool de conexiones): Koin lo registra como
 * single y se reutiliza en toda la app.
 *
 * Sesion 10: el plugin Auth adjunta el JWT a cada peticion (ninguna llamada
 * escribe la cabecera Authorization a mano) y SesionVencida avisa cuando el
 * servidor lo rechaza con un 401.
 */
fun crearHttpClient(
    engine: HttpClientEngine,
    config: ConfiguracionApi,
    almacen: AlmacenSeguro,
    avisos: AvisosDeSesion = AvisosDeSesion()
): HttpClient =
    HttpClient(engine) {
        // 4xx -> ClientRequestException, 5xx -> ServerResponseException
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                // PharmaSoft manda fechas, categoria, etc. que no usamos (escenario 5 lo desactiva)
                ignoreUnknownKeys = config.escenario != EscenarioPrueba.JSON_ESTRICTO
                isLenient = true
                encodeDefaults = true
            })
        }

        install(Logging) {
            // HEADERS en depuracion (sin volcar cuerpos); NONE en una version publicada.
            level = if (config.registroHttp) LogLevel.HEADERS else LogLevel.NONE
            // Prefijo fijo para filtrar en Logcat / consola de Xcode: "KtorHttp"
            logger = object : Logger {
                // Ktor manda bloques de varias lineas: se antepone el prefijo a CADA linea
                // para que el filtro de Logcat las muestre todas.
                override fun log(message: String) =
                    message.lineSequence().forEach { linea -> escribirRegistro("KtorHttp => $linea") }
            }
            // El token nunca se imprime: el registro muestra "Authorization: ***".
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }

        install(Auth) {
            bearer {
                // Se consulta una vez y queda en cache: tras el login y el cierre de
                // sesion se llama a clearToken() para que vuelva a leer el almacen.
                loadTokens {
                    // El almacen cifrado hace E/S y criptografia: fuera del hilo principal.
                    withContext(Dispatchers.IO) { almacen.leerToken() }
                        ?.let { token -> BearerTokens(accessToken = token, refreshToken = null) }
                }
                // El token viaja desde la primera peticion (salvo en el login): sin esto
                // Ktor envia sin token, recibe 401 y repite, duplicando cada llamada.
                sendWithoutRequest { peticion -> !peticion.url.encodedPath.endsWith(RUTA_LOGIN) }
                // PharmaSoft no renueva tokens: sin refreshTokens, el 401 llega tal cual.
            }
        }

        // Cualquier 401 fuera del login significa que el token ya no sirve.
        install(createClientPlugin("SesionVencida") {
            val clienteHttp = client
            onResponse { respuesta ->
                val ruta = respuesta.call.request.url.encodedPath
                if (respuesta.status == HttpStatusCode.Unauthorized && !ruta.endsWith(RUTA_LOGIN)) {
                    val mensaje = runCatching { respuesta.body<ErrorResponseDto>().message }.getOrNull()
                    withContext(Dispatchers.IO) { almacen.borrarToken() }
                    clienteHttp.authProvider<BearerAuthProvider>()?.clearToken()
                    avisos.avisar(mapearErrorSesion(respuesta.status, mensaje))
                }
            }
        })

        install(HttpTimeout) {
            requestTimeoutMillis =
                if (config.escenario == EscenarioPrueba.TIEMPO_AGOTADO) 1 else config.requestTimeoutMs
            connectTimeoutMillis = config.connectTimeoutMs
        }

        // Bitacora S8, escenario de cancelacion: retrasa las operaciones de escritura
        // para poder salir de la pantalla mientras estan en curso.
        if (config.escenario == EscenarioPrueba.RESPUESTA_LENTA) {
            install(createClientPlugin("RespuestaLenta") {
                onRequest { request, _ ->
                    if (request.method != HttpMethod.Get) {
                        println(
                            "KtorHttp => RESPUESTA_LENTA: ${request.method.value} retenido " +
                                "${RETARDO_RESPUESTA_LENTA_MS / 1000} s antes de enviarse"
                        )
                        delay(RETARDO_RESPUESTA_LENTA_MS)
                    }
                }
            })
        }

        defaultRequest {
            url(config.urlBase)
            contentType(ContentType.Application.Json)
        }
    }
