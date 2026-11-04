package pe.edu.upeu.pharmamobil.platform

import platform.Foundation.NSLog

actual fun escribirRegistro(linea: String) {
    // "%@" evita que un "%" del mensaje se interprete como formato.
    NSLog("%@", linea)
}
