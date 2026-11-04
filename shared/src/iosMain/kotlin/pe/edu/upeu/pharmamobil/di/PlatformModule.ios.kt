package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguroIos
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriverIos
import pe.edu.upeu.pharmamobil.data.remote.ConfiguracionApi
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorIos
import pe.edu.upeu.pharmamobil.data.telemetria.TelemetriaConsola
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.platform.GestorPermisosIos
import pe.edu.upeu.pharmamobil.platform.NotificadorIos
import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi

actual val platformModule: Module = module {
    // Motor Darwin (NSURLSession).
    single<HttpClientEngine> { Darwin.create() }
    // Sesion 10: el registro HTTP solo en una compilacion de depuracion.
    single { ConfiguracionApi(urlBase = urlDePharmaSoft(), registroHttp = esDepuracion()) }
    // Sesion 9: capacidad nativa de compartir (hoja de compartir de UIKit).
    single<Compartidor> { CompartidorIos() }
    // Sesion 10: el token va al Keychain. A diferencia de Android, no recibe nada.
    single<AlmacenSeguro> { AlmacenSeguroIos() }
    // Sesion 11: base local con el driver nativo de SQLite.
    single<CreadorDeDriver> { CreadorDeDriverIos() }
    // Sesion 13: permisos, avisos y telemetria (version minima en iOS, ver AvisosIos.kt).
    single<GestorPermisos> { GestorPermisosIos() }
    single<Notificador> { NotificadorIos() }
    single<Telemetria> { TelemetriaConsola() }
}

@OptIn(ExperimentalNativeApi::class)
private fun esDepuracion(): Boolean = Platform.isDebugBinary

private const val URL_DEL_SIMULADOR = "http://localhost:8080/api/v1/"

/**
 * URL base de PharmaSoft. El simulador comparte la red de la Mac, asi que
 * localhost llega al backend. Un iPhone fisico necesita la IP de la PC: se
 * escribe al compilar en la clave PharmaSoftUrl del Info.plist.
 */
private fun urlDePharmaSoft(): String {
    val configurada = (NSBundle.mainBundle.objectForInfoDictionaryKey("PharmaSoftUrl") as? String)
        ?.trim()
        .orEmpty()
    if (configurada.isEmpty()) return URL_DEL_SIMULADOR
    return if (configurada.endsWith("/")) configurada else "$configurada/"
}
