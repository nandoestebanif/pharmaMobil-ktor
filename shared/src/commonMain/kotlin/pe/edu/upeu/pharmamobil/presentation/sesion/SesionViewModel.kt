package pe.edu.upeu.pharmamobil.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesionException
import pe.edu.upeu.pharmamobil.domain.usecase.CerrarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.IniciarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObservarFinDeSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObtenerSesionGuardadaUseCase

/**
 * Decide si se muestra el login o la aplicacion.
 *
 * - Al crearse busca la sesion guardada (el token sobrevive al cierre de la app).
 * - Si el servidor rechaza el token en cualquier pantalla (401), vuelve al login
 *   con un aviso. Un 403 no pasa por aqui: la sesion sigue y la pantalla avisa.
 */
class SesionViewModel(
    private val iniciarSesionUseCase: IniciarSesionUseCase,
    private val obtenerSesionGuardada: ObtenerSesionGuardadaUseCase,
    private val cerrarSesionUseCase: CerrarSesionUseCase,
    observarFinDeSesion: ObservarFinDeSesionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SesionUiState())
    val uiState: StateFlow<SesionUiState> = _uiState.asStateFlow()

    init {
        comprobarSesionGuardada()

        viewModelScope.launch {
            observarFinDeSesion().collect { motivo -> terminarSesion(avisoPara(motivo)) }
        }
    }

    fun comprobarSesionGuardada() {
        _uiState.update { it.copy(verificando = true, error = null) }
        viewModelScope.launch {
            obtenerSesionGuardada().fold(
                onSuccess = { sesion ->
                    _uiState.update { it.copy(verificando = false, sesion = sesion) }
                },
                onFailure = { fallo ->
                    val error = (fallo as? ErrorSesionException)?.error
                    _uiState.update {
                        when (error) {
                            // Sin red no se descarta el token: se puede reintentar luego.
                            ErrorSesion.SinConexion, null ->
                                it.copy(verificando = false, error = mensajePara(error))
                            else ->
                                it.copy(verificando = false, aviso = avisoPara(error))
                        }
                    }
                }
            )
        }
    }

    fun onUsuarioChange(valor: String) {
        _uiState.update { it.copy(usuario = valor, usuarioError = null, error = null) }
    }

    fun onClaveChange(valor: String) {
        _uiState.update { it.copy(clave = valor, claveError = null, error = null) }
    }

    fun descartarMensajes() {
        _uiState.update { it.copy(error = null, aviso = null) }
    }

    fun iniciarSesion() {
        val estado = _uiState.value
        if (estado.cargando) return

        val usuarioError = if (estado.usuario.isBlank()) "Ingresa tu usuario" else null
        val claveError = if (estado.clave.isEmpty()) "Ingresa tu contraseña" else null
        if (usuarioError != null || claveError != null) {
            _uiState.update { it.copy(usuarioError = usuarioError, claveError = claveError) }
            return
        }

        _uiState.update { it.copy(cargando = true, error = null, aviso = null) }
        viewModelScope.launch {
            iniciarSesionUseCase(Credenciales(estado.usuario, estado.clave)).fold(
                onSuccess = { sesion ->
                    // La contrasena no se queda en memoria una vez usada.
                    _uiState.update { it.copy(cargando = false, clave = "", sesion = sesion) }
                },
                onFailure = { fallo ->
                    val error = (fallo as? ErrorSesionException)?.error
                    _uiState.update { it.copy(cargando = false, clave = "", error = mensajePara(error)) }
                }
            )
        }
    }

    /** Accion «Cerrar sesion»: borra el token y vuelve al login. */
    fun cerrarSesion() {
        viewModelScope.launch {
            cerrarSesionUseCase()
            terminarSesion("Cerraste sesión.", conservarUsuario = false)
        }
    }

    /**
     * Vuelve al login con el estado limpio. Si el servidor rechazo el token se
     * conserva el usuario para que solo falte la contrasena; al cerrar sesion a
     * proposito no queda nada de quien estuvo (otra persona puede usar el telefono).
     */
    private fun terminarSesion(aviso: String, conservarUsuario: Boolean = true) {
        _uiState.update {
            SesionUiState(
                usuario = if (conservarUsuario) it.sesion?.usuario ?: it.usuario else "",
                verificando = false,
                aviso = aviso
            )
        }
    }

    private fun mensajePara(error: ErrorSesion?): String = when (error) {
        ErrorSesion.CredencialesInvalidas -> "Usuario o contraseña incorrectos."
        ErrorSesion.SinConexion -> "No se pudo conectar con el servidor. Revisa tu conexión."
        ErrorSesion.SinPermiso -> "Tu usuario no tiene permiso para ingresar."
        ErrorSesion.SesionExpirada, ErrorSesion.SesionInvalida, ErrorSesion.UsuarioDeshabilitado -> avisoPara(error)
        null -> "Ocurrió un error inesperado. Intenta de nuevo."
    }

    private fun avisoPara(error: ErrorSesion): String = when (error) {
        ErrorSesion.SesionExpirada -> "Tu sesión expiró. Vuelve a iniciar sesión."
        ErrorSesion.SesionInvalida -> "Tu sesión ya no es válida. Vuelve a iniciar sesión."
        ErrorSesion.UsuarioDeshabilitado -> "Tu usuario fue deshabilitado. Consulta con el administrador."
        else -> mensajePara(error)
    }
}
