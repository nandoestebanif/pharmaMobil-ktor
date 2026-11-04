package pe.edu.upeu.pharmamobil.di

import pe.edu.upeu.pharmamobil.data.remote.ConfiguracionApi
import pe.edu.upeu.pharmamobil.data.local.AlmacenEnMemoria
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriverEnMemoria
import pe.edu.upeu.pharmamobil.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.presentation.sesion.SesionViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.Koin
import pe.edu.upeu.pharmamobil.db.PharmaDb
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.permiso.GestorPermisosFalso
import pe.edu.upeu.pharmamobil.permiso.NotificadorFalso
import pe.edu.upeu.pharmamobil.permiso.TelemetriaFalsa
import pe.edu.upeu.pharmamobil.data.repository.ClienteRepositorioEnMemoria
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioOffline
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.domain.platform.CompartidorFalso
import pe.edu.upeu.pharmamobil.domain.repository.ClienteRepository
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarClientesUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObservarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ReactivarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarClienteUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteViewModel
import pe.edu.upeu.pharmamobil.presentation.detalle.DetalleProductoViewModel
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

/**
 * Comprueba que el grafo se ensambla sin arrancar la aplicacion: si una
 * definicion falta o esta declarada con el tipo equivocado, falla aqui.
 *
 * Se cargan los cuatro modulos, incluido presentationModule: construir un
 * ViewModel es justo lo que se rompe al cambiar un constructor, y antes era
 * lo unico que el grafo no cubria. Como los ViewModel arrancan una carga en
 * su init, hace falta un Dispatchers.Main de prueba.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppModuleTest {

    @BeforeTest
    fun instalarMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun detenerKoin() {
        esperarALosViewModel()
        stopKoin()
        Dispatchers.resetMain()
    }

    /** ViewModel creados en la prueba: se cancelan al terminar (ver esperarALosViewModel). */
    private val creados = mutableListOf<ViewModel>()

    private inline fun <reified T : ViewModel> Koin.viewModel(): T = get<T>().also { creados += it }

    /**
     * Sesion 10 (autonoma): la carga del init pasa por Dispatchers.IO (almacen cifrado)
     * y vuelve a Main. Si la prueba termina antes, vuelve cuando el Main de prueba ya
     * no existe y la excepcion aparece en OTRA prueba (en CI fallo CrudProductoUseCasesTest).
     * Se cancelan y se espera a que terminen ANTES de quitar el Main de prueba.
     */
    private fun esperarALosViewModel() {
        runTest { creados.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() } }
        creados.clear()
    }

    /**
     * Sesion 9: el Compartidor real de Android necesita un Context que en una
     * prueba no existe. Como es una interfaz inyectada, basta registrar un
     * doble despues del platformModule: la ultima definicion gana.
     *
     * Lo mismo con el motor HTTP: los ViewModel cargan datos en su init, y con
     * el motor real la prueba salia a la red. MockEngine responde una pagina vacia.
     */
    private val doblesDePlataforma = module {
        single<Compartidor> { CompartidorFalso() }
        // Sesion 10: el almacen real de Android necesita un Context.
        single<AlmacenSeguro> { AlmacenEnMemoria() }
        // Sesion 11: la base local en memoria (el driver real necesita un Context o iOS).
        single<CreadorDeDriver> { CreadorDeDriverEnMemoria() }
        // Sesion 13: permisos, avisos y telemetria reales necesitan Context / Activity / Firebase.
        single<GestorPermisos> { GestorPermisosFalso() }
        single<Notificador> { NotificadorFalso() }
        single<Telemetria> { TelemetriaFalsa() }
        // La configuracion de Android consulta el Context para saber si es depuracion.
        single { ConfiguracionApi(urlBase = "http://localhost:8080/api/v1/") }
        single<HttpClientEngine> {
            MockEngine {
                respond(
                    content = PAGINA_VACIA,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }
    }

    private fun grafoCompleto(): Koin = startKoin {
        modules(dataModule, domainModule, presentationModule, platformModule, doblesDePlataforma)
    }.koin

    @Test
    fun resuelveLosRepositoriosPorSuInterfazDeDominio() {

        val koin = grafoCompleto()

        // Sesion 11: offline-first; la pantalla lee de la base local.
        assertIs<ProductoRepositorioOffline>(koin.get<ProductoRepository>())
        assertIs<ClienteRepositorioEnMemoria>(koin.get<ClienteRepository>())
    }

    @Test
    fun losRepositoriosSonUnicosEnTodaLaAplicacion() {

        val koin = grafoCompleto()

        assertSame(
            koin.get<ProductoRepository>(),
            koin.get<ProductoRepository>()
        )
        assertSame(
            koin.get<ClienteRepository>(),
            koin.get<ClienteRepository>()
        )
    }

    @Test
    fun resuelveLosCasosDeUsoConSusRepositorios() {

        val koin = grafoCompleto()

        koin.get<RegistrarProductoUseCase>()
        koin.get<ObservarProductosUseCase>()
        koin.get<SincronizarProductosUseCase>()
        koin.get<ObtenerProductoUseCase>()
        koin.get<ActualizarProductoUseCase>()
        koin.get<EliminarProductoUseCase>()
        koin.get<ReactivarProductoUseCase>()
        koin.get<RegistrarClienteUseCase>()
        koin.get<ListarClientesUseCase>()
    }

    @Test
    fun resuelveLosViewModelConSusCasosDeUso() {

        val koin = grafoCompleto()

        koin.viewModel<ProductoViewModel>()
        koin.viewModel<ClienteViewModel>()
    }

    @Test
    fun resuelveLaSesionConSuRepositorioYSuViewModel() {

        val koin = grafoCompleto()

        assertIs<AuthRepositorioRest>(koin.get<AuthRepository>())
        assertIs<AlmacenEnMemoria>(koin.get<AlmacenSeguro>())
        koin.viewModel<SesionViewModel>()
    }

    @Test
    fun abreLaBaseLocalConElDriverRegistrado() {

        val koin = grafoCompleto()

        // Sesion 11: si el esquema o el driver estuvieran mal, fallaria aqui.
        val base = koin.get<PharmaDb>()
        assertSame(base, koin.get<PharmaDb>())
        assertEquals(0, base.productoQueries.listarTodos().executeAsList().size)
    }

    @Test
    fun elViewModelDelDetalleRecibeElCompartidorRegistrado() {

        val koin = grafoCompleto()

        assertIs<CompartidorFalso>(koin.get<Compartidor>())
        koin.viewModel<DetalleProductoViewModel>()
    }

    private companion object {
        const val PAGINA_VACIA =
            """{"contenido":[],"pagina":0,"tamanio":20,"totalElementos":0,"totalPaginas":0,"ultima":true}"""
    }
}
