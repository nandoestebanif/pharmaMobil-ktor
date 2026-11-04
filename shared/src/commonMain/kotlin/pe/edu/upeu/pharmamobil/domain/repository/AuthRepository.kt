package pe.edu.upeu.pharmamobil.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobil.domain.model.Sesion

interface AuthRepository {

    /** Pide el token al servidor y lo guarda. Falla con ErrorSesionException. */
    suspend fun iniciarSesion(credenciales: Credenciales): Sesion

    /** Sesion del token guardado, o null si no hay token. */
    suspend fun sesionGuardada(): Sesion?

    /** Borra el token: la siguiente peticion ya no lo envia. */
    suspend fun cerrarSesion()

    /**
     * Avisa cuando el servidor rechaza el token en cualquier peticion (401).
     * Para entonces el token ya se borro.
     */
    val sesionTerminada: Flow<ErrorSesion>
}
