package pe.edu.upeu.pharmamobil.data.remote

/**
 * URL base del backend PharmaSoft. Cambia segun desde donde se ejecuta la app,
 * por eso la aporta el platformModule de cada plataforma:
 *  - Emulador Android: 10.0.2.2 es el "localhost" de la PC.
 *  - Simulador iOS: comparte la red de la Mac, usa localhost.
 *  - Celular fisico: IP de la PC en la red local (ej. http://192.168.1.50:8080/api/v1/).
 */
data class ConfiguracionApi(
    val urlBase: String,
    val requestTimeoutMs: Long = 15_000,
    val connectTimeoutMs: Long = 10_000,
    val escenario: EscenarioPrueba = EscenarioPrueba.NINGUNO,
    /** Sesion 10: registro de cabeceras HTTP. Solo en depuracion; lo decide cada plataforma. */
    val registroHttp: Boolean = true
)

/**
 * Escenarios de las bitacoras de pruebas (actividades autonomas S7 y S8) que no
 * se pueden provocar desde la interfaz. Se activan cambiando UNA linea en
 * PlatformModule.android.kt; en uso normal: NINGUNO.
 */
enum class EscenarioPrueba {
    NINGUNO,
    /** requestTimeoutMillis = 1 ms: HttpRequestTimeoutException. */
    TIEMPO_AGOTADO,
    /** ignoreUnknownKeys = false: los campos extra de PharmaSoft provocan SerializationException. */
    JSON_ESTRICTO,
    /** Crear, actualizar y eliminar tardan 8 s: da tiempo a salir de la pantalla (escenario de cancelacion). */
    RESPUESTA_LENTA
}
