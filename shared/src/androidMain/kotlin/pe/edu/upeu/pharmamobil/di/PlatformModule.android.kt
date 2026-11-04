package pe.edu.upeu.pharmamobil.di

import android.content.pm.ApplicationInfo
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguroAndroid
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriverAndroid
import pe.edu.upeu.pharmamobil.data.remote.ConfiguracionApi
import pe.edu.upeu.pharmamobil.data.remote.EscenarioPrueba
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import pe.edu.upeu.pharmamobil.permiso.GestorPermisosAndroid
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid
import pe.edu.upeu.pharmamobil.platform.NotificadorAndroid

actual val platformModule: Module = module {
    // Motor OkHttp (requiere el permiso INTERNET en el AndroidManifest).
    single<HttpClientEngine> { OkHttp.create() }
    // 10.0.2.2 = localhost de la PC vista desde el emulador.
    // En celular fisico reemplazar por la IP local de la PC.
    single {
        ConfiguracionApi(
            urlBase = "http://10.0.2.2:8080/api/v1/",
            // Bitacoras S7/S8: cambiar a TIEMPO_AGOTADO o JSON_ESTRICTO
            // para provocar cada escenario. Dejar en NINGUNO para el uso normal.
            escenario = EscenarioPrueba.NINGUNO,
            // Sesion 10: el registro HTTP solo en una compilacion de depuracion.
            registroHttp = (androidContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        )
    }
    // Sesion 9: capacidad nativa de compartir. androidContext() es el Context de
    // la aplicacion que MainApplication le entrega a Koin al arrancar.
    single<Compartidor> { CompartidorAndroid(androidContext()) }
    // Sesion 10: el token se guarda cifrado. Igual que el Compartidor, necesita un Context.
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
    // Sesion 11: quien abre la base local (pharma.db). Tambien necesita el Context.
    single<CreadorDeDriver> { CreadorDeDriverAndroid(androidContext()) }
    // Sesion 13: permisos en tiempo de ejecucion. Uno solo para toda la app: MainActivity
    // le entrega su puente (el lanzador del dialogo) en onCreate.
    single { GestorPermisosAndroid(androidContext()) }
    single<GestorPermisos> { get<GestorPermisosAndroid>() }
    // Sesion 13: notificaciones del sistema (solo si el usuario concedio el permiso).
    single<Notificador> { NotificadorAndroid(androidContext()) }
}
