package pe.edu.upeu.pharmamobil.domain.error

/**
 * Errores que puede devolver la API, expresados en terminos del negocio.
 * La interfaz trabaja con este tipo y nunca con las excepciones de Ktor.
 */
sealed interface ErrorApi {

    /** 400: datos invalidos. Las claves son los nombres de los campos del formulario. */
    data class Validacion(val porCampo: Map<String, String>) : ErrorApi

    /** 404: el recurso ya no existe en el servidor. */
    data object NoEncontrado : ErrorApi

    /** 409: una regla de negocio impide la operacion (p. ej. nombre duplicado). */
    data class Conflicto(val mensaje: String) : ErrorApi

    /** 401: no hay sesion o el token ya no vale (vencido o invalido). */
    data object NoAutenticado : ErrorApi

    /** 403: hay sesion, pero el rol no tiene permiso para esta operacion. */
    data object SinPermiso : ErrorApi

    /** 5xx: fallo del servidor, no del usuario. */
    data object Servidor : ErrorApi

    /** Sin red o servidor inalcanzable. */
    data object SinConexion : ErrorApi

    /** El servidor no respondio dentro del tiempo configurado. */
    data object TiempoAgotado : ErrorApi

    /** La respuesta llego, pero no tiene la forma que espera el DTO. */
    data object RespuestaInesperada : ErrorApi
}

/** Transporta un [ErrorApi] por el canal de fallo de Result. */
class ErrorApiException(val error: ErrorApi) : Exception(error.toString())
