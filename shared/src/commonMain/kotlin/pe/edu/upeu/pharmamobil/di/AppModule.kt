package pe.edu.upeu.pharmamobil.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.remote.AvisosDeSesion
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobil.data.repository.ClienteRepositorioEnMemoria
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.data.local.MarcaDeSincronizacion
import pe.edu.upeu.pharmamobil.data.local.PerfilLocal
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.ProductoRemoto
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioOffline
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.repository.ClienteRepository
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.CerrarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.IniciarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObservarFinDeSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObtenerSesionGuardadaUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarClientesUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObservarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObservarUltimaSincronizacionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ReactivarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarClienteUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteViewModel
import pe.edu.upeu.pharmamobil.presentation.detalle.DetalleProductoViewModel
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobil.presentation.sesion.SesionViewModel
import pe.edu.upeu.pharmamobil.presentation.permiso.AvisosViewModel


/**
 * Categoria que se envia al crear un producto. El formulario aun no la pide
 * y PharmaSoft la exige; el id 1 existe en los datos de ejemplo del backend.
 */
const val CATEGORIA_POR_DEFECTO = 1L

val dataModule = module {
    // Sesion 7: un solo HttpClient; motor y URL base los aporta platformModule.
    // Sesion 10: el almacen del token tambien llega del platformModule.
    single { AvisosDeSesion() }
    // Sesion 11: una sola base local; el driver lo aporta el platformModule de cada plataforma.
    single { PharmaDb(get<CreadorDeDriver>().crear()) }
    single { crearHttpClient(engine = get(), config = get(), almacen = get(), avisos = get()) }
    // Sesion 11: el perfil local permite entrar con PharmaSoft apagado.
    single { PerfilLocal(get()) }
    single<AuthRepository> { AuthRepositorioRest(get(), get(), get(), get()) }
    single { ProductoApi(get()) }
    // Sesion 11: offline-first. La pantalla lee de la base local; PharmaSoft solo sincroniza.
    single { ProductoLocal(get()) }
    single { ProductoRemoto(get(), CATEGORIA_POR_DEFECTO) }
    // Actividad autonoma 11: con la marca de la ultima sincronizacion correcta.
    single { MarcaDeSincronizacion(get()) }
    // Sesion 13: con la telemetria (Firebase en Android) que aporta el platformModule.
    single<ProductoRepository> { ProductoRepositorioOffline(get(), get(), get(), telemetria = get()) }
    single<ClienteRepository> { ClienteRepositorioEnMemoria() }
}

val domainModule = module {
    factory { ObservarProductosUseCase(get()) }
    // Sesion 13: avisa con una notificacion cuando alguna fila queda fallida.
    factory { SincronizarProductosUseCase(get(), get()) }
    factory { ObservarUltimaSincronizacionUseCase(get()) }
    factory { ObtenerProductoUseCase(get()) }
    factory { RegistrarProductoUseCase(get()) }
    factory { ActualizarProductoUseCase(get()) }
    factory { EliminarProductoUseCase(get()) }
    factory { ReactivarProductoUseCase(get()) }
    factory { RegistrarClienteUseCase(get()) }
    factory { ListarClientesUseCase(get()) }
    // Sesion 10
    factory { IniciarSesionUseCase(get()) }
    factory { ObtenerSesionGuardadaUseCase(get()) }
    factory { CerrarSesionUseCase(get()) }
    factory { ObservarFinDeSesionUseCase(get()) }
}

val presentationModule = module {
    viewModel { ProductoViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ClienteViewModel(get(), get()) }
    // Sesion 9: el Compartidor lo aporta el platformModule de cada plataforma.
    viewModel { DetalleProductoViewModel(get(), get()) }
    // Sesion 10: decide si se muestra el login o la app.
    viewModel { SesionViewModel(get(), get(), get(), get()) }
    // Sesion 13: permiso de notificaciones (GestorPermisos y Notificador los aporta cada plataforma).
    viewModel { AvisosViewModel(get(), get()) }
}


expect val platformModule: Module

fun initKoin(configuracionAdicional: KoinApplication.() -> Unit = {}) {
    startKoin {
        configuracionAdicional()
        modules(
            dataModule,
            domainModule,
            presentationModule,
            platformModule
        )
    }
}
