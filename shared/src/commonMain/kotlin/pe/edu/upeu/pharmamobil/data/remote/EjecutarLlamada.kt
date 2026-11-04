package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import pe.edu.upeu.pharmamobil.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException

/**
 * UNICO punto donde las excepciones de Ktor se traducen a [ErrorApi].
 * Si manana cambia el manejo de errores, se cambia solo este archivo.
 *
 * El orden de los catch importa: los timeouts heredan de IOException,
 * asi que deben ir antes.
 */
suspend fun <T> ejecutarLlamada(bloque: suspend () -> T): Result<T> =
    try {
        Result.success(bloque())
    } catch (cancelacion: CancellationException) {
        // Nunca se convierte en Result.failure: se deja constancia en el log y se
        // relanza, para que la corrutina termine y no actualice un estado que ya no existe.
        println("KtorHttp => llamada cancelada: se relanza CancellationException")
        throw cancelacion
    } catch (e: ClientRequestException) {                   // 4xx
        Result.failure(ErrorApiException(traducirCliente(e)))
    } catch (e: ServerResponseException) {                  // 5xx
        Result.failure(ErrorApiException(ErrorApi.Servidor))
    } catch (e: HttpRequestTimeoutException) {
        Result.failure(ErrorApiException(ErrorApi.TiempoAgotado))
    } catch (e: ContentConvertException) {                  // JSON que no encaja con el DTO
        Result.failure(ErrorApiException(ErrorApi.RespuestaInesperada))
    } catch (e: SerializationException) {
        Result.failure(ErrorApiException(ErrorApi.RespuestaInesperada))
    } catch (e: IOException) {                              // sin red, host inalcanzable, timeout de socket
        Result.failure(ErrorApiException(ErrorApi.SinConexion))
    }

/** Aprovecha el ErrorResponseDTO que ya devuelve PharmaSoft en los 4xx. */
private suspend fun traducirCliente(e: ClientRequestException): ErrorApi {
    val cuerpo = runCatching {
        e.response.body<ErrorResponseDto>()
    }.getOrNull()
    return when (e.response.status.value) {
        400 -> ErrorApi.Validacion(cuerpo?.validationErrors.orEmpty())
        401 -> ErrorApi.NoAutenticado
        403 -> ErrorApi.SinPermiso
        404 -> ErrorApi.NoEncontrado
        409 -> ErrorApi.Conflicto(cuerpo?.message ?: "Operación no permitida")
        else -> ErrorApi.Servidor
    }
}
