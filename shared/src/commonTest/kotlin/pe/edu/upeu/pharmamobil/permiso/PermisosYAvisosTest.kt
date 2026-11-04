package pe.edu.upeu.pharmamobil.permiso

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.data.repository.FakeProductoRepository
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.decidirEstado
import pe.edu.upeu.pharmamobil.domain.port.sinDatosSensibles
import pe.edu.upeu.pharmamobil.domain.sync.ResumenSync
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarProductosUseCase
import pe.edu.upeu.pharmamobil.presentation.permiso.AvisosViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Sesion 13: el gestor de permisos, los avisos y la telemetria, con dobles en commonTest. */
@OptIn(ExperimentalCoroutinesApi::class)
class PermisosYAvisosTest {

    @BeforeTest fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun despues() = Dispatchers.resetMain()

    @Test
    fun laTablaDelPaso5DistingueLosTresEstados() {
        // Primera vez: nunca se pidio y no procede explicar -> se puede pedir.
        assertEquals(EstadoPermiso.Denegado, decidirEstado(concedido = false, yaSePidio = false, procedeExplicar = false))
        // Denego una vez: procede explicar -> se puede volver a pedir.
        assertEquals(EstadoPermiso.Denegado, decidirEstado(concedido = false, yaSePidio = true, procedeExplicar = true))
        // Denego definitivamente: ya se pidio y Android ya no explica -> solo Ajustes.
        assertEquals(EstadoPermiso.DenegadoParaSiempre, decidirEstado(concedido = false, yaSePidio = true, procedeExplicar = false))
        assertEquals(EstadoPermiso.Concedido, decidirEstado(concedido = true, yaSePidio = true, procedeExplicar = false))
    }

    @Test
    fun laTarjetaExplicaPrimeroYPideSoloAlPulsarActivar() = runTest {
        val gestor = GestorPermisosFalso(actual = EstadoPermiso.Denegado, alPedir = EstadoPermiso.Concedido)
        val vm = AvisosViewModel(gestor, NotificadorFalso())

        vm.refrescar()
        assertEquals(EstadoPermiso.Denegado, vm.estado.value)
        assertEquals(0, gestor.solicitudes, "consultar no lanza el dialogo")

        vm.activar()
        assertEquals(EstadoPermiso.Concedido, vm.estado.value)
        assertEquals(1, gestor.solicitudes)
    }

    @Test
    fun bloqueadoParaSiempreOfreceIrAAjustesYAlVolverSeActualiza() = runTest {
        val gestor = GestorPermisosFalso(actual = EstadoPermiso.DenegadoParaSiempre)
        val vm = AvisosViewModel(gestor, NotificadorFalso())
        vm.refrescar()
        assertEquals(EstadoPermiso.DenegadoParaSiempre, vm.estado.value)

        vm.abrirAjustes()
        gestor.actual = EstadoPermiso.Concedido   // el usuario lo activo en Ajustes
        vm.refrescar()                              // al volver a la app (onResume)

        assertEquals(1, gestor.ajustesAbiertos)
        assertEquals(EstadoPermiso.Concedido, vm.estado.value)
    }

    @Test
    fun conElPermisoSePuedeEnviarUnAvisoDePrueba() = runTest {
        val notificador = NotificadorFalso()
        val vm = AvisosViewModel(GestorPermisosFalso(actual = EstadoPermiso.Concedido), notificador)
        vm.probar()
        assertEquals(1, notificador.pruebas)
        assertTrue(vm.avisoEnviado.value)
    }

    @Test
    fun sincronizarAvisaSoloCuandoAparecenFilasFallidas() = runTest {
        val repo = FakeProductoRepository()
        val notificador = NotificadorFalso()
        val caso = SincronizarProductosUseCase(repo, notificador)

        repo.resumen = ResumenSync(enviadas = 2, nuevasFallidas = 0, pendientes = 0, fallidas = 0)
        caso()
        repo.resumen = ResumenSync(enviadas = 0, nuevasFallidas = 1, pendientes = 0, fallidas = 2)
        caso()

        assertEquals(listOf(2L), notificador.fallidosAvisados)
    }

    @Test
    fun laTelemetriaNuncaEnviaElTokenNiDatosPersonales() {
        val limpio = mapOf("pantalla" to "productos", "token" to "eyJ...", "usuario" to "admin", "codigo" to "409")
            .sinDatosSensibles()
        assertEquals(setOf("pantalla", "codigo"), limpio.keys)
        assertFalse("eyJ..." in limpio.values)
    }
}
