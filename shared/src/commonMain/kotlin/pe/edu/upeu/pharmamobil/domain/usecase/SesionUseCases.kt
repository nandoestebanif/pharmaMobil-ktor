package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesionException
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository

class IniciarSesionUseCase(private val repository: AuthRepository) {

    suspend operator fun invoke(credenciales: Credenciales): Result<Sesion> {
        val usuario = credenciales.usuario.trim()
        if (usuario.isEmpty() || credenciales.clave.isEmpty()) {
            return Result.failure(ErrorSesionException(ErrorSesion.CredencialesInvalidas))
        }
        return resultadoDe { repository.iniciarSesion(credenciales.copy(usuario = usuario)) }
    }
}

class ObtenerSesionGuardadaUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<Sesion?> = resultadoDe { repository.sesionGuardada() }
}

class CerrarSesionUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.cerrarSesion()
}

/** Emite cuando el servidor rechaza el token en cualquier pantalla (401). */
class ObservarFinDeSesionUseCase(private val repository: AuthRepository) {
    operator fun invoke(): Flow<ErrorSesion> = repository.sesionTerminada
}
