package pe.edu.upeu.pharmamobil.presentation.detalle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.data.repository.FakeProductoRepository
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.platform.CompartidorFalso
import pe.edu.upeu.pharmamobil.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.pharmamobil.platform.formatearSoles
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleProductoViewModelTest {

    private val compartidor = CompartidorFalso()

    @BeforeTest
    fun instalarMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restaurarMain() {
        Dispatchers.resetMain()
    }

    private fun repositorioConDos() = FakeProductoRepository(
        mutableListOf(
            Producto(id = 1L, nombre = "Paracetamol 500mg", precio = 3.5, stock = 120),
            Producto(id = 3L, nombre = "Naproxeno 550mg", precio = 7.8, stock = 6)
        )
    )

    private fun nuevoViewModel(repositorio: FakeProductoRepository = repositorioConDos()) =
        DetalleProductoViewModel(
            obtenerProducto = ObtenerProductoUseCase(repositorio),
            compartidor = compartidor
        )

    @Test
    fun cargarMuestraElProductoConElPrecioFormateado() = runTest {

        val viewModel = nuevoViewModel()

        viewModel.cargar(3L)

        val contenido = assertIs<DetalleProductoUiState.Contenido>(viewModel.uiState.value)
        assertEquals("Naproxeno 550mg", contenido.producto.nombre)
        assertEquals(formatearSoles(7.8), contenido.producto.precio)
        assertEquals("6 unidades", contenido.producto.stock)
        assertEquals("Activo", contenido.producto.estado)
        assertTrue(contenido.producto.requiereReposicion)
    }

    @Test
    fun compartirEntregaAlCompartidorElTextoArmadoEnCodigoComun() = runTest {

        val viewModel = nuevoViewModel()
        viewModel.cargar(3L)

        viewModel.compartir()

        assertEquals(
            listOf("Naproxeno 550mg — ${formatearSoles(7.8)} · Stock: 6"),
            compartidor.textos
        )
    }

    @Test
    fun sinProductoCargadoNoSeComparteNada() = runTest {

        val viewModel = nuevoViewModel()

        viewModel.compartir()

        assertTrue(compartidor.textos.isEmpty())
    }

    @Test
    fun siElProductoNoExisteElDetalleMuestraElError() = runTest {

        val viewModel = nuevoViewModel()

        viewModel.cargar(99L)

        val error = assertIs<DetalleProductoUiState.Error>(viewModel.uiState.value)
        assertEquals("El producto ya no existe en el servidor.", error.mensaje)

        // Y no queda nada que compartir.
        viewModel.compartir()
        assertTrue(compartidor.textos.isEmpty())
    }

    @Test
    fun siFallaLaRedElDetalleNoQuedaEnCargando() = runTest {

        val repositorio = repositorioConDos().apply {
            fallaAlObtener = ErrorApiException(ErrorApi.SinConexion)
        }
        val viewModel = nuevoViewModel(repositorio)

        viewModel.cargar(1L)

        val error = assertIs<DetalleProductoUiState.Error>(viewModel.uiState.value)
        assertEquals("No se pudo conectar con el servidor. Revisa tu conexión.", error.mensaje)
    }

    @Test
    fun abrirOtroProductoReemplazaAlAnterior() = runTest {

        val viewModel = nuevoViewModel()

        viewModel.cargar(1L)
        viewModel.cargar(3L)
        viewModel.compartir()

        val contenido = assertIs<DetalleProductoUiState.Contenido>(viewModel.uiState.value)
        assertEquals(3L, contenido.producto.id)
        assertEquals(1, compartidor.textos.size)
        assertTrue(compartidor.textos.single().startsWith("Naproxeno 550mg"))
    }
}
