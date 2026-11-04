package pe.edu.upeu.pharmamobil.presentation.error

import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException

/**
 * Convierte un fallo en el texto que lee el usuario. Trabaja con ErrorApi
 * (dominio): la capa de presentacion no importa nada de io.ktor.
 */
fun mensajeDe(fallo: Throwable): String =
    (fallo as? ErrorApiException)?.let { mensajeDe(it.error) }
        ?: fallo.message
        ?: "Ocurrió un error inesperado."

fun mensajeDe(error: ErrorApi): String = when (error) {
    is ErrorApi.Validacion -> "Revisa los datos ingresados."
    ErrorApi.NoEncontrado -> "El producto ya no existe en el servidor."
    is ErrorApi.Conflicto -> error.mensaje
    ErrorApi.NoAutenticado -> "Tu sesión terminó. Vuelve a iniciar sesión."
    ErrorApi.SinPermiso -> "No tienes permiso para realizar esta operación."
    ErrorApi.Servidor -> "El servidor tuvo un problema. Intenta de nuevo en un momento."
    ErrorApi.SinConexion -> "No se pudo conectar con el servidor. Revisa tu conexión."
    ErrorApi.TiempoAgotado -> "El servidor tardó demasiado en responder."
    ErrorApi.RespuestaInesperada -> "La respuesta del servidor no tiene el formato esperado."
}
