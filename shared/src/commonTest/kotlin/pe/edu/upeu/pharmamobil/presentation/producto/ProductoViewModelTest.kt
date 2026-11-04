package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.data.repository.FakeProductoRepository
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoUiState.Operacion
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObservarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ReactivarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObservarUltimaSincronizacionUseCase
import pe.edu.upeu.pharmamobil.platform.formatearSoles
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * viewModelScope corre sobre Dispatchers.Main, que en una prueba no existe:
 * setMain lo sustituye por un dispatcher de prueba antes de cada caso.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelTest {

    @BeforeTest
    fun instalarMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restaurarMain() {
        Dispatchers.resetMain()
    }

    private fun nuevoViewModel(
        repositorio: FakeProductoRepository = FakeProductoRepository()
    ) = ProductoViewModel(
        observarProductos = ObservarProductosUseCase(repositorio),
        sincronizarProductos = SincronizarProductosUseCase(repositorio),
        obtenerProducto = ObtenerProductoUseCase(repositorio),
        registrarProducto = RegistrarProductoUseCase(repositorio),
        actualizarProducto = ActualizarProductoUseCase(repositorio),
        eliminarProducto = EliminarProductoUseCase(repositorio),
        reactivarProducto = ReactivarProductoUseCase(repositorio),
        observarUltimaSincronizacion = ObservarUltimaSincronizacionUseCase(repositorio)
    )

    private fun repositorioConDos() = FakeProductoRepository(
        mutableListOf(
            Producto(id = 1L, nombre = "Paracetamol", precio = 12.5, stock = 50, categoriaId = 2),
            Producto(id = 2L, nombre = "Ibuprofeno", precio = 5.2, stock = 80, categoriaId = 1)
        )
    )

    private fun ProductoViewModel.nombresEnPantalla(): List<String> =
        assertIs<ProductoUiState.Fase.ConProductos>(uiState.value.fase).productos.map { it.nombre }

    @Test
    fun arrancaEnSinProductosCuandoElInventarioEstaVacio() = runTest {

        val viewModel = nuevoViewModel()

        assertEquals(ProductoUiState.Fase.SinProductos, viewModel.uiState.value.fase)
    }

    @Test
    fun muestraElInventarioConElPrecioYaFormateado() = runTest {

        val repositorio = FakeProductoRepository(
            mutableListOf(
                Producto(id = 1L, nombre = "Paracetamol", precio = 12.5, stock = 5)
            )
        )

        val fase = assertIs<ProductoUiState.Fase.ConProductos>(
            nuevoViewModel(repositorio).uiState.value.fase
        )

        // Sesion 9: el precio lo formatea cada plataforma (formatearSoles), asi que
        // la prueba compara con ese mismo formateador y no con un texto fijo.
        assertEquals(formatearSoles(12.5), fase.productos.first().precio)
        assertEquals("5 u.", fase.productos.first().stock)
        assertTrue(fase.productos.first().requiereReposicion)
    }

    // ---------- Sesion 11: offline-first ----------

    @Test
    fun sinConexionMuestraLaListaGuardadaConElAvisoDeRed() = runTest {

        val repositorio = FakeProductoRepository()
            .conBaseLocal(listOf(Producto(id = 1L, nombre = "Paracetamol", precio = 3.5, stock = 120)))
            .apply { fallaAlSincronizar = ErrorApiException(ErrorApi.SinConexion) }

        val viewModel = nuevoViewModel(repositorio)

        assertEquals(listOf("Paracetamol"), viewModel.nombresEnPantalla())
        assertEquals("Sin conexión: mostrando datos guardados", viewModel.uiState.value.avisoRed)
    }

    @Test
    fun primeraAperturaSinRedEsUnEstadoVacioInformativoYNoUnError() = runTest {

        // Autonoma 11, escenario 4: base local vacia y sin red.
        val repositorio = FakeProductoRepository()
            .apply { fallaAlSincronizar = ErrorApiException(ErrorApi.SinConexion) }

        val estado = nuevoViewModel(repositorio).uiState.value

        assertEquals(ProductoUiState.Fase.SinDatosSinConexion, estado.fase)
        assertEquals("Sin conexión con PharmaSoft", estado.avisoRed)
        assertNull(estado.ultimaSincronizacion)
    }

    @Test
    fun laPantallaRecibeLaHoraDeLaUltimaSincronizacionCorrecta() = runTest {

        val repositorio = repositorioConDos().apply { reloj = 1_792_000_000_000L }

        val viewModel = nuevoViewModel(repositorio)

        assertEquals(1_792_000_000_000L, viewModel.uiState.value.ultimaSincronizacion)
    }

    @Test
    fun registrarNoVuelveAPedirLaLista() = runTest {

        val repositorio = repositorioConDos()
        val viewModel = nuevoViewModel(repositorio)
        val sincronizacionesAlAbrir = repositorio.sincronizaciones

        viewModel.onNombreChange("Clorfenamina")
        viewModel.onPrecioChange("2.80")
        viewModel.onStockChange("25")
        viewModel.guardar()

        // La lista se actualizo sola (el Flow emitio) sin otra sincronizacion.
        assertTrue("Clorfenamina" in viewModel.nombresEnPantalla())
        assertEquals(sincronizacionesAlAbrir, repositorio.sincronizaciones)
    }

    @Test
    fun pasaAFaseErrorCuandoElRepositorioFalla() = runTest {

        val repositorio = FakeProductoRepository().apply {
            fallaAlSincronizar = IllegalStateException("Sin conexión")
        }

        val fase = assertIs<ProductoUiState.Fase.Error>(
            nuevoViewModel(repositorio).uiState.value.fase
        )

        assertEquals("Sin conexión", fase.mensaje)
    }

    @Test
    fun losErroresDeValidacionCaenEnElFormularioNoEnLaFase() = runTest {

        val viewModel = nuevoViewModel()

        viewModel.onNombreChange("")
        viewModel.onPrecioChange("abc")
        viewModel.onStockChange("-1")
        viewModel.guardar()

        val estado = viewModel.uiState.value

        assertEquals("El nombre es obligatorio", estado.formulario.nombreError)
        assertEquals("El precio debe ser un número válido", estado.formulario.precioError)
        assertEquals("El stock no puede ser negativo", estado.formulario.stockError)
        assertEquals(ProductoUiState.Fase.SinProductos, estado.fase)
        assertNull(estado.mensajeExito)
    }

    @Test
    fun registrarLimpiaElFormularioYRecargaElInventario() = runTest {

        val viewModel = nuevoViewModel()

        viewModel.onNombreChange("Paracetamol")
        viewModel.onPrecioChange("12.50")
        viewModel.onStockChange("5")
        viewModel.guardar()

        val estado = viewModel.uiState.value
        val fase = assertIs<ProductoUiState.Fase.ConProductos>(estado.fase)

        assertEquals("Paracetamol", fase.productos.single().nombre)
        assertEquals("", estado.formulario.nombre)
        assertEquals("", estado.formulario.precio)
        assertEquals("", estado.formulario.stock)
        assertEquals(
            "Producto \"Paracetamol\" registrado correctamente",
            estado.mensajeExito
        )
    }

    // ---------- Sesion 8: transiciones de estado de las operaciones ----------

    @Test
    fun alEliminarLaListaSigueVisibleYLaOperacionPasaPorEnCurso() = runTest {

        val repositorio = repositorioConDos()
        val viewModel = nuevoViewModel(repositorio)
        val compuerta = CompletableDeferred<Unit>().also { repositorio.compuerta = it }

        viewModel.eliminar(1L)

        // Mientras el servidor responde: operacion en curso, lista intacta (no vuelve a Cargando).
        assertEquals(
            Operacion.EnCurso(Operacion.Tipo.Eliminar, productoId = 1L),
            viewModel.uiState.value.operacion
        )
        assertEquals(listOf("Paracetamol", "Ibuprofeno"), viewModel.nombresEnPantalla())

        compuerta.complete(Unit)

        // Al terminar: operacion inactiva, lista recargada desde el repositorio y aviso de exito.
        assertEquals(Operacion.Inactiva, viewModel.uiState.value.operacion)
        assertEquals(listOf("Ibuprofeno"), viewModel.nombresEnPantalla())
        assertEquals("Producto eliminado", viewModel.uiState.value.mensajeExito)
    }

    @Test
    fun siEliminarFallaSeAvisaSinPerderLaLista() = runTest {

        val repositorio = repositorioConDos().apply {
            fallaAlEliminar = ErrorApiException(ErrorApi.SinConexion)
        }
        val viewModel = nuevoViewModel(repositorio)

        viewModel.eliminar(1L)

        val estado = viewModel.uiState.value
        assertEquals(
            Operacion.Fallida("No se pudo conectar con el servidor. Revisa tu conexión."),
            estado.operacion
        )
        assertEquals(listOf("Paracetamol", "Ibuprofeno"), viewModel.nombresEnPantalla())
        assertNull(estado.mensajeExito)
    }

    @Test
    fun editarCargaElFormularioYGuardarActualizaConservandoLaCategoria() = runTest {

        val repositorio = repositorioConDos()
        val viewModel = nuevoViewModel(repositorio)

        viewModel.editar(1L)

        val formulario = viewModel.uiState.value.formulario
        assertEquals(1L, formulario.editandoId)
        assertEquals("Paracetamol", formulario.nombre)
        assertEquals("12.5", formulario.precio)
        assertEquals("50", formulario.stock)

        viewModel.onNombreChange("Paracetamol 500mg")
        viewModel.onStockChange("45")
        viewModel.guardar()

        val estado = viewModel.uiState.value
        assertEquals(Operacion.Inactiva, estado.operacion)
        assertNull(estado.formulario.editandoId)
        assertEquals("", estado.formulario.nombre)
        assertEquals("Producto \"Paracetamol 500mg\" actualizado correctamente", estado.mensajeExito)
        assertEquals(listOf("Paracetamol 500mg", "Ibuprofeno"), viewModel.nombresEnPantalla())
        assertEquals(
            Producto(id = 1L, nombre = "Paracetamol 500mg", precio = 12.5, stock = 45, categoriaId = 2),
            repositorio.obtener(1L)
        )
    }

    @Test
    fun el400DelServidorSeMuestraBajoElCampoYNoComoAvisoGenerico() = runTest {

        val repositorio = repositorioConDos().apply {
            fallaAlRegistrar = ErrorApiException(
                ErrorApi.Validacion(mapOf("nombre" to "El nombre debe tener entre 3 y 150 caracteres"))
            )
        }
        val viewModel = nuevoViewModel(repositorio)

        viewModel.onNombreChange("Ab")
        viewModel.onPrecioChange("4.50")
        viewModel.onStockChange("10")
        viewModel.guardar()

        val estado = viewModel.uiState.value
        assertEquals("El nombre debe tener entre 3 y 150 caracteres", estado.formulario.nombreError)
        assertNull(estado.formulario.precioError)
        assertEquals(Operacion.Inactiva, estado.operacion)
        // El formulario conserva lo escrito y la lista sigue en pantalla.
        assertEquals("Ab", estado.formulario.nombre)
        assertEquals(listOf("Paracetamol", "Ibuprofeno"), viewModel.nombresEnPantalla())
    }

    @Test
    fun el409MuestraElMensajeDelServidorComoOperacionFallida() = runTest {

        val repositorio = repositorioConDos().apply {
            fallaAlRegistrar = ErrorApiException(
                ErrorApi.Conflicto("Ya existe un producto con el nombre Paracetamol")
            )
        }
        val viewModel = nuevoViewModel(repositorio)

        viewModel.onNombreChange("Paracetamol")
        viewModel.onPrecioChange("4.50")
        viewModel.onStockChange("10")
        viewModel.guardar()

        assertEquals(
            Operacion.Fallida("Ya existe un producto con el nombre Paracetamol"),
            viewModel.uiState.value.operacion
        )

        viewModel.descartarError()

        assertEquals(Operacion.Inactiva, viewModel.uiState.value.operacion)
    }

    @Test
    fun siElProductoYaNoExisteAlEditarSeAvisaYSeRefrescaLaLista() = runTest {

        val repositorio = repositorioConDos()
        val viewModel = nuevoViewModel(repositorio)

        // Otro usuario lo elimina en el servidor mientras la app aun lo muestra.
        repositorio.eliminar(1L)
        viewModel.editar(1L)

        val estado = viewModel.uiState.value
        assertEquals(Operacion.Fallida("El producto ya no existe en el servidor."), estado.operacion)
        assertNull(estado.formulario.editandoId)
        assertEquals(listOf("Ibuprofeno"), viewModel.nombresEnPantalla())
    }

    @Test
    fun mientrasSeRegistraLaFaseNoVuelveACargando() = runTest {

        val repositorio = repositorioConDos()
        val viewModel = nuevoViewModel(repositorio)
        val compuerta = CompletableDeferred<Unit>().also { repositorio.compuerta = it }

        viewModel.onNombreChange("Loratadina")
        viewModel.onPrecioChange("2.40")
        viewModel.onStockChange("60")
        viewModel.guardar()

        assertEquals(Operacion.EnCurso(Operacion.Tipo.Crear), viewModel.uiState.value.operacion)
        assertEquals(listOf("Paracetamol", "Ibuprofeno"), viewModel.nombresEnPantalla())

        // Un segundo toque mientras hay una operacion en curso se ignora.
        viewModel.eliminar(2L)
        assertEquals(Operacion.EnCurso(Operacion.Tipo.Crear), viewModel.uiState.value.operacion)

        compuerta.complete(Unit)

        assertEquals(listOf("Paracetamol", "Ibuprofeno", "Loratadina"), viewModel.nombresEnPantalla())
    }

    @Test
    fun prepararNuevoDejaElFormularioEnBlancoYSinAvisos() = runTest {

        val viewModel = nuevoViewModel(repositorioConDos())

        // Queda un producto en edicion y un aviso de exito de una operacion anterior...
        viewModel.eliminar(2L)
        viewModel.editar(1L)
        assertEquals(1L, viewModel.uiState.value.formulario.editandoId)

        // ...y el usuario abre el formulario para registrar otro.
        viewModel.prepararNuevo()

        val estado = viewModel.uiState.value
        assertEquals(FormularioProducto(), estado.formulario)
        assertEquals(Operacion.Inactiva, estado.operacion)
        assertNull(estado.mensajeExito)

        // Lo que se guarde ahora es un alta, no una actualizacion del producto 1.
        viewModel.onNombreChange("Aspirina 100mg")
        viewModel.onPrecioChange("1.5")
        viewModel.onStockChange("40")
        viewModel.guardar()

        assertEquals(
            "Producto \"Aspirina 100mg\" registrado correctamente",
            viewModel.uiState.value.mensajeExito
        )
        assertTrue("Paracetamol" in viewModel.nombresEnPantalla())
    }

    @Test
    fun cancelarLaEdicionLimpiaElFormulario() = runTest {

        val viewModel = nuevoViewModel(repositorioConDos())

        viewModel.editar(2L)
        viewModel.cancelarEdicion()

        assertEquals(FormularioProducto(), viewModel.uiState.value.formulario)
    }

    // ---------- Productos dados de baja ----------

    private fun repositorioConUnaBaja() = FakeProductoRepository(
        mutableListOf(
            Producto(id = 1L, nombre = "Paracetamol", precio = 12.5, stock = 50, categoriaId = 2, activo = false),
            Producto(id = 2L, nombre = "Ibuprofeno", precio = 5.2, stock = 80, categoriaId = 1)
        )
    )

    @Test
    fun elInventarioSeparaLosActivosDeLosDadosDeBaja() = runTest {

        val viewModel = nuevoViewModel(repositorioConUnaBaja())

        assertEquals(listOf("Ibuprofeno"), viewModel.nombresEnPantalla())
        assertEquals(listOf("Paracetamol"), viewModel.uiState.value.dadosDeBaja.map { it.nombre })
    }

    @Test
    fun reactivarDevuelveElProductoAlInventarioActivo() = runTest {

        val viewModel = nuevoViewModel(repositorioConUnaBaja())

        viewModel.verDadosDeBaja(true)
        viewModel.reactivar(1L)

        val estado = viewModel.uiState.value
        assertEquals(Operacion.Inactiva, estado.operacion)
        assertEquals(listOf("Paracetamol", "Ibuprofeno"), viewModel.nombresEnPantalla())
        assertTrue(estado.dadosDeBaja.isEmpty())
        assertEquals("Producto \"Paracetamol\" reactivado", estado.mensajeExito)
        // Sin mas productos de baja, la pantalla regresa al inventario activo.
        assertEquals(false, estado.viendoBajas)
    }

    @Test
    fun el409PorUnNombreDadoDeBajaSugiereReactivarlo() = runTest {

        val repositorio = repositorioConUnaBaja().apply {
            fallaAlRegistrar = ErrorApiException(
                ErrorApi.Conflicto("Ya existe un producto con el nombre Paracetamol")
            )
        }
        val viewModel = nuevoViewModel(repositorio)

        viewModel.onNombreChange("paracetamol")
        viewModel.onPrecioChange("4.50")
        viewModel.onStockChange("10")
        viewModel.guardar()

        assertEquals(
            Operacion.Fallida(
                "Ya existe un producto dado de baja con ese nombre. Reactívalo desde \"De baja\"."
            ),
            viewModel.uiState.value.operacion
        )
    }

    // ---------- Actividad autonoma S8: transiciones pedidas en la ficha ----------

    @Test
    fun laCargaPasaDeCargandoAConProductosConLaListaEsperada() = runTest {

        val repositorio = repositorioConDos()
        val compuerta = CompletableDeferred<Unit>().also { repositorio.compuertaAlSincronizar = it }

        val viewModel = nuevoViewModel(repositorio)

        // Mientras el servidor no responde, la pantalla esta en Cargando.
        assertEquals(ProductoUiState.Fase.Cargando, viewModel.uiState.value.fase)

        compuerta.complete(Unit)

        assertEquals(listOf("Paracetamol", "Ibuprofeno"), viewModel.nombresEnPantalla())
    }

    @Test
    fun elErrorDeValidacionNoCambiaLaFaseAError() = runTest {

        val repositorio = repositorioConDos().apply {
            fallaAlRegistrar = ErrorApiException(
                ErrorApi.Validacion(
                    mapOf(
                        "nombre" to "El nombre debe tener entre 3 y 150 caracteres",
                        "precio" to "El precio debe ser mayor que cero",
                        "stock" to "El stock no puede ser negativo"
                    )
                )
            )
        }
        val viewModel = nuevoViewModel(repositorio)

        viewModel.onNombreChange("Ab")
        viewModel.onPrecioChange("0.001")
        viewModel.onStockChange("10")
        viewModel.guardar()

        val estado = viewModel.uiState.value
        assertEquals("El nombre debe tener entre 3 y 150 caracteres", estado.formulario.nombreError)
        assertEquals("El precio debe ser mayor que cero", estado.formulario.precioError)
        assertEquals("El stock no puede ser negativo", estado.formulario.stockError)
        assertIs<ProductoUiState.Fase.ConProductos>(estado.fase)
        assertEquals(Operacion.Inactiva, estado.operacion)
    }

    @Test
    fun el409AlEliminarRefrescaLaListaYMuestraElMensajeDelServidor() = runTest {

        val repositorio = repositorioConDos()
        val viewModel = nuevoViewModel(repositorio)

        // Otro cliente da de baja el producto 1 mientras la app aun lo muestra como activo...
        repositorio.actualizar(repositorio.obtener(1L).copy(activo = false))
        // ...y el servidor rechaza la segunda eliminacion con 409.
        repositorio.fallaAlEliminar = ErrorApiException(
            ErrorApi.Conflicto("El producto Paracetamol ya se encuentra inactivo")
        )

        viewModel.eliminar(1L)

        val estado = viewModel.uiState.value
        assertEquals(Operacion.Fallida("El producto Paracetamol ya se encuentra inactivo"), estado.operacion)
        // La lista se refresco: el producto paso a "De baja".
        assertEquals(listOf("Ibuprofeno"), viewModel.nombresEnPantalla())
        assertEquals(listOf("Paracetamol"), estado.dadosDeBaja.map { it.nombre })
    }

    @Test
    fun el409AlEliminarNoSeConfundeConUnNombreDuplicadoDelFormulario() = runTest {

        val repositorio = repositorioConDos()
        val viewModel = nuevoViewModel(repositorio)

        // El usuario tiene abierto en el formulario el mismo producto que va a eliminar.
        viewModel.editar(1L)
        repositorio.actualizar(repositorio.obtener(1L).copy(activo = false))
        repositorio.fallaAlEliminar = ErrorApiException(
            ErrorApi.Conflicto("El producto Paracetamol ya se encuentra inactivo")
        )

        viewModel.eliminar(1L)

        // Aunque el nombre del formulario coincide con una baja, el aviso es el del servidor.
        assertEquals(
            Operacion.Fallida("El producto Paracetamol ya se encuentra inactivo"),
            viewModel.uiState.value.operacion
        )
    }

    @Test
    fun siElViewModelSeCancelaDuranteUnaOperacionNoSeMuestraNingunError() = runTest {

        val repositorio = repositorioConDos()
        val viewModel = nuevoViewModel(repositorio)
        repositorio.compuerta = CompletableDeferred()

        viewModel.eliminar(1L)
        assertEquals(
            Operacion.EnCurso(Operacion.Tipo.Eliminar, productoId = 1L),
            viewModel.uiState.value.operacion
        )

        // La Activity se destruye: viewModelScope se cancela con la operacion a medias.
        viewModel.viewModelScope.cancel()

        // La CancellationException se relanza: no se convierte en Operacion.Fallida
        // ni se intenta actualizar el estado despues de la cancelacion.
        val estado = viewModel.uiState.value
        assertEquals(Operacion.EnCurso(Operacion.Tipo.Eliminar, productoId = 1L), estado.operacion)
        assertNull(estado.mensajeExito)
        assertEquals(listOf("Paracetamol", "Ibuprofeno"), viewModel.nombresEnPantalla())
    }
}
