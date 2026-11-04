package pe.edu.upeu.pharmamobil.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

/**
 * iOS: el token se guarda en el Keychain del sistema como una contrasena
 * generica (servicio + cuenta). El Keychain lo cifra el propio iOS y sobrevive
 * al cierre de la app. Se usa la API C de Security.framework desde Kotlin/Native.
 */
@OptIn(ExperimentalForeignApi::class)
class AlmacenSeguroIos : AlmacenSeguro {

    /**
     * Respaldo en memoria por si el Keychain rechaza la escritura (por ejemplo,
     * una app sin firmar en el simulador devuelve errSecMissingEntitlement).
     * Asi la sesion funciona igual, aunque no sobreviva al cierre de la app.
     */
    private var respaldo: String? = null

    override fun guardarToken(token: String) {
        borrarToken()
        if (token.isEmpty()) return
        val datos = CFBridgingRetain(token.encodeToByteArray().aNSData())
        val consulta = consultaBase()
        CFDictionaryAddValue(consulta, kSecValueData, datos)
        CFDictionaryAddValue(consulta, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        val estado = SecItemAdd(consulta, null)
        if (estado != errSecSuccess) {
            println("Keychain => SecItemAdd devolvio $estado: el token queda solo en memoria")
            respaldo = token
        }
        liberar(consulta, datos)
    }

    override fun leerToken(): String? = memScoped {
        val consulta = consultaBase()
        CFDictionaryAddValue(consulta, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(consulta, kSecMatchLimit, kSecMatchLimitOne)
        val resultado = alloc<CFTypeRefVar>()
        val estado = SecItemCopyMatching(consulta, resultado.ptr)
        liberar(consulta)
        when (estado) {
            errSecSuccess -> (CFBridgingRelease(resultado.value) as? NSData)?.aByteArray()?.decodeToString()
            errSecItemNotFound -> respaldo
            else -> {
                println("Keychain => SecItemCopyMatching devolvio $estado")
                respaldo
            }
        }
    }

    override fun borrarToken() {
        respaldo = null
        val consulta = consultaBase()
        SecItemDelete(consulta)
        liberar(consulta)
    }

    /** Identifica el elemento: contrasena generica del servicio PharmaMobil, cuenta "jwt". */
    private fun consultaBase(): CFMutableDictionaryRef? {
        val consulta = CFDictionaryCreateMutable(
            null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr
        )
        val servicio = CFBridgingRetain(SERVICIO)
        val cuenta = CFBridgingRetain(CUENTA)
        CFDictionaryAddValue(consulta, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(consulta, kSecAttrService, servicio)
        CFDictionaryAddValue(consulta, kSecAttrAccount, cuenta)
        // El diccionario retiene sus valores: se sueltan las referencias propias.
        CFRelease(servicio)
        CFRelease(cuenta)
        return consulta
    }

    private fun liberar(vararg referencias: CFTypeRef?) {
        referencias.forEach { it?.let(::CFRelease) }
    }

    private companion object {
        const val SERVICIO = "pe.edu.upeu.pharmamobil.sesion"
        const val CUENTA = "jwt"
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.aNSData(): NSData = usePinned { fijado ->
    NSData.create(bytes = fijado.addressOf(0), length = size.convert())
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.aByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { fijado -> memcpy(fijado.addressOf(0), this.bytes, length) }
    return bytes
}
