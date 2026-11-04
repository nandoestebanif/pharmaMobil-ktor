package pe.edu.upeu.pharmamobil.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.comoTextoParaCompartir
import pe.edu.upeu.pharmamobil.presentation.error.mensajeDe

/**
 * Detalle de un producto. Recibe el [Compartidor] por constructor: no sabe si
 * detras hay un Intent de Android o una hoja de UIKit, y en las pruebas se le
 * entrega un doble.
 */
class DetalleProductoViewModel(
    private val obtenerProducto: ObtenerProductoUseCase,
    private val compartidor: Compartidor
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleProductoUiState>(DetalleProductoUiState.Cargando)
    val uiState: StateFlow<DetalleProductoUiState> = _uiState.asStateFlow()

    /** Version del servidor del producto que se esta mostrando. */
    private var producto: Producto? = null

    private var carga: Job? = null

    /** Trae del servidor el producto [id]. Si habia otra carga en curso, la reemplaza. */
    fun cargar(id: Long) {

        carga?.cancel()
        producto = null
        _uiState.value = DetalleProductoUiState.Cargando

        carga = viewModelScope.launch {
            obtenerProducto(id).fold(
                onSuccess = { obtenido ->
                    producto = obtenido
                    _uiState.value = DetalleProductoUiState.Contenido(obtenido.aDetalleUi())
                },
                onFailure = { fallo ->
                    _uiState.value = DetalleProductoUiState.Error(mensajeDe(fallo))
                }
            )
        }
    }

    /** El texto se arma en codigo comun; la plataforma solo decide como mostrarlo. */
    fun compartir() {
        producto?.let { compartidor.compartir(it.comoTextoParaCompartir()) }
    }
}
