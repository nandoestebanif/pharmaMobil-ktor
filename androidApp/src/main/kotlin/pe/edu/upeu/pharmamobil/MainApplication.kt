package pe.edu.upeu.pharmamobil

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import pe.edu.upeu.pharmamobil.di.initKoin

/**
 * Arranca Koin una sola vez, antes que cualquier Activity. Se declara en el
 * manifiesto con android:name=".MainApplication".
 */
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger()
            androidContext(this@MainApplication)
            // Sesion 14: la URL de PharmaSoft elegida al compilar (local.properties). Vacia =
            // automatica: el platformModule decide entre emulador y adb reverse.
            properties(mapOf("urlServidor" to getString(R.string.servidor_url)))
        }
    }
}
