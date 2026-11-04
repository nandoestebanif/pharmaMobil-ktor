package pe.edu.upeu.pharmamobil.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
import androidx.security.crypto.MasterKey

/**
 * Android: EncryptedSharedPreferences cifra la clave y el valor con una llave
 * AES-256 que custodia el Android Keystore, fuera del alcance de la app.
 * Un SharedPreferences comun dejaria el token en texto plano en el disco.
 */
// security-crypto 1.1.0 marca estas clases como obsoletas (Google recomienda
// migrar a DataStore + Tink), pero siguen siendo la API que pide la guia.
@Suppress("DEPRECATION")
class AlmacenSeguroAndroid(contexto: Context) : AlmacenSeguro {

    private val prefs: SharedPreferences by lazy {
        val llave = MasterKey.Builder(contexto)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            contexto,
            ARCHIVO,
            llave,
            PrefKeyEncryptionScheme.AES256_SIV,
            PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun guardarToken(token: String) {
        prefs.edit().putString(CLAVE, token).apply()
    }

    override fun leerToken(): String? = prefs.getString(CLAVE, null)

    override fun borrarToken() {
        prefs.edit().remove(CLAVE).apply()
    }

    private companion object {
        const val ARCHIVO = "sesion_segura"
        const val CLAVE = "jwt"
    }
}
