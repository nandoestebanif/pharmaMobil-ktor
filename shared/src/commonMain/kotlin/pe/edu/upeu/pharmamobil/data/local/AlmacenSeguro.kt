package pe.edu.upeu.pharmamobil.data.local

/**
 * Donde vive el token entre un arranque y otro. Cada plataforma usa su
 * almacen cifrado: EncryptedSharedPreferences en Android y el Keychain en iOS.
 *
 * Es una interfaz y no un expect porque la version de Android necesita un
 * Context en el constructor y la de iOS no recibe nada (mismo criterio que
 * el Compartidor de la sesion 9). Se registra en el platformModule.
 */
interface AlmacenSeguro {
    fun guardarToken(token: String)
    fun leerToken(): String?
    fun borrarToken()
}
