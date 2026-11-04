package pe.edu.upeu.pharmamobil.data.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesion

/**
 * Canal por el que el cliente HTTP avisa que el servidor rechazo el token.
 * Lo comparten el plugin SesionVencida (emite) y AuthRepositorioRest (expone).
 */
class AvisosDeSesion {

    private val avisos = MutableSharedFlow<ErrorSesion>(extraBufferCapacity = 1)

    val sesionTerminada: Flow<ErrorSesion> = avisos.asSharedFlow()

    fun avisar(error: ErrorSesion) {
        avisos.tryEmit(error)
    }
}
