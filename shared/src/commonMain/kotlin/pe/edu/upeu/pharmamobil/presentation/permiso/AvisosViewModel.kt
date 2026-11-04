package pe.edu.upeu.pharmamobil.presentation.permiso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso
import pe.edu.upeu.pharmamobil.domain.port.Notificador

/**
 * Sesion 13: tarjeta «Avisos» de la portada. Explica para que sirve el permiso ANTES de
 * pedirlo, lo pide con el dialogo del sistema y, si quedo bloqueado, lleva a Ajustes.
 * No conoce Android: habla con el [GestorPermisos] del dominio.
 */
class AvisosViewModel(
    private val gestor: GestorPermisos,
    private val notificador: Notificador
) : ViewModel() {

    /** null mientras se consulta por primera vez. */
    private val _estado = MutableStateFlow<EstadoPermiso?>(null)
    val estado: StateFlow<EstadoPermiso?> = _estado.asStateFlow()

    private val _avisoEnviado = MutableStateFlow(false)
    val avisoEnviado: StateFlow<Boolean> = _avisoEnviado.asStateFlow()

    /** Al aparecer la pantalla y al volver de Ajustes (el usuario pudo cambiarlo alli). */
    fun refrescar() {
        viewModelScope.launch { _estado.value = gestor.estado(Permiso.NOTIFICACIONES) }
    }

    fun activar() {
        viewModelScope.launch { _estado.value = gestor.solicitar(Permiso.NOTIFICACIONES) }
    }

    fun abrirAjustes() = gestor.abrirAjustes()

    fun probar() {
        notificador.avisoDePrueba()
        _avisoEnviado.value = true
    }
}
