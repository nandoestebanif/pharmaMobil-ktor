package pe.edu.upeu.pharmamobil.presentation.sesion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobil.domain.model.ErrorSesionException
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.usecase.CerrarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.IniciarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObservarFinDeSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObtenerSesionGuardadaUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Doble del repositorio de sesion: sin red y con el resultado que pida cada prueba. */
private class FakeAuthRepository(
    var guardada: Sesion? = null,
    var falloAlRecuperar: ErrorSesion? = null,
    var falloAlIngresar: ErrorSesion? = null
) : AuthRepository {

    val fin = MutableSharedFlow<ErrorSesion>(extraBufferCapacity = 1)
    var cerrada = false
    var ultimasCredenciales: Credenciales? = null

    override suspend fun iniciarSesion(credenciales: Credenciales): Sesion {
        ultimasCredenciales = credenciales
        falloAlIngresar?.let { throw ErrorSesionException(it) }
        return SESION_ADMIN.copy(usuario = credenciales.usuario).also { guardada = it }
    }

    override suspend fun sesionGuardada(): Sesion? {
        falloAlRecuperar?.let { throw ErrorSesionException(it) }
        return guardada
    }

    override suspend fun cerrarSesion() {
        cerrada = true
        guardada = null
    }

    override val sesionTerminada: Flow<ErrorSesion> = fin
}

private val SESION_ADMIN = Sesion(
    token = "aaa.bbb.ccc",
    usuario = "admin",
    nombreCompleto = "Carlos Ramirez Torres",
    permisos = setOf("CATALOGO_LEER", "CATALOGO_ESCRIBIR"),
    roles = listOf("ADMIN")
)

@OptIn(ExperimentalCoroutinesApi::class)
class SesionViewModelTest {

    @BeforeTest
    fun instalarMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restaurarMain() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repositorio: FakeAuthRepository) = SesionViewModel(
        IniciarSesionUseCase(repositorio),
        ObtenerSesionGuardadaUseCase(repositorio),
        CerrarSesionUseCase(repositorio),
        ObservarFinDeSesionUseCase(repositorio)
    )

    @Test
    fun conUnTokenGuardadoEntraSinPedirCredenciales() = runTest {

        val vm = viewModel(FakeAuthRepository(guardada = SESION_ADMIN))

        val estado = vm.uiState.value
        assertFalse(estado.verificando)
        assertEquals(SESION_ADMIN, estado.sesion)
    }

    @Test
    fun sinTokenGuardadoMuestraElLogin() = runTest {

        val estado = viewModel(FakeAuthRepository()).uiState.value

        assertFalse(estado.verificando)
        assertNull(estado.sesion)
        assertNull(estado.aviso)
    }

    @Test
    fun unTokenGuardadoVencidoLlevaAlLoginConAviso() = runTest {

        val estado = viewModel(FakeAuthRepository(falloAlRecuperar = ErrorSesion.SesionExpirada)).uiState.value

        assertNull(estado.sesion)
        assertEquals("Tu sesión expiró. Vuelve a iniciar sesión.", estado.aviso)
    }

    @Test
    fun losCamposVaciosSeMarcanSinLlamarAlServidor() = runTest {

        val repositorio = FakeAuthRepository()
        val vm = viewModel(repositorio)

        vm.iniciarSesion()

        assertNotNull(vm.uiState.value.usuarioError)
        assertNotNull(vm.uiState.value.claveError)
        assertNull(repositorio.ultimasCredenciales)
    }

    @Test
    fun unLoginCorrectoAbreLaSesionYOlvidaLaContrasena() = runTest {

        val vm = viewModel(FakeAuthRepository())

        vm.onUsuarioChange("admin")
        vm.onClaveChange("Admin123")
        vm.iniciarSesion()

        val estado = vm.uiState.value
        assertEquals("admin", estado.sesion?.usuario)
        assertEquals("", estado.clave)
        assertFalse(estado.cargando)
    }

    @Test
    fun credencialesIncorrectasMuestranElError() = runTest {

        val vm = viewModel(FakeAuthRepository(falloAlIngresar = ErrorSesion.CredencialesInvalidas))

        vm.onUsuarioChange("admin")
        vm.onClaveChange("Incorrecta1")
        vm.iniciarSesion()

        assertNull(vm.uiState.value.sesion)
        assertEquals("Usuario o contraseña incorrectos.", vm.uiState.value.error)
    }

    @Test
    fun un401EnCualquierPantallaVuelveAlLoginConAviso() = runTest {

        val repositorio = FakeAuthRepository(guardada = SESION_ADMIN)
        val vm = viewModel(repositorio)

        repositorio.fin.emit(ErrorSesion.SesionExpirada)

        val estado = vm.uiState.value
        assertNull(estado.sesion)
        assertEquals("admin", estado.usuario)
        assertEquals("Tu sesión expiró. Vuelve a iniciar sesión.", estado.aviso)
    }

    @Test
    fun unUsuarioDeshabilitadoVuelveAlLoginConSuPropioAviso() = runTest {

        val repositorio = FakeAuthRepository(guardada = SESION_ADMIN)
        val vm = viewModel(repositorio)

        repositorio.fin.emit(ErrorSesion.UsuarioDeshabilitado)

        val estado = vm.uiState.value
        assertNull(estado.sesion)
        assertEquals("Tu usuario fue deshabilitado. Consulta con el administrador.", estado.aviso)
    }

    @Test
    fun cerrarSesionNoDejaNadaDeLaSesionAnterior() = runTest {

        val repositorio = FakeAuthRepository(guardada = SESION_ADMIN)
        val vm = viewModel(repositorio)
        vm.onClaveChange("Admin123")

        vm.cerrarSesion()

        val estado = vm.uiState.value
        assertNull(estado.sesion)
        assertEquals("", estado.usuario)
        assertEquals("", estado.clave)
        assertNull(estado.error)
    }

    @Test
    fun cerrarSesionBorraElTokenYVuelveAlLogin() = runTest {

        val repositorio = FakeAuthRepository(guardada = SESION_ADMIN)
        val vm = viewModel(repositorio)

        vm.cerrarSesion()

        assertTrue(repositorio.cerrada)
        assertNull(vm.uiState.value.sesion)
        assertEquals("Cerraste sesión.", vm.uiState.value.aviso)
    }
}
