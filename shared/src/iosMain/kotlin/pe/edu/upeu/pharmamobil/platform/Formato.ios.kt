package pe.edu.upeu.pharmamobil.platform

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle

/** iOS: el formato de moneda lo resuelve Foundation con la configuracion regional es_PE. */
actual fun formatearSoles(valor: Double): String {
    val formateador = NSNumberFormatter()
    formateador.numberStyle = NSNumberFormatterCurrencyStyle
    formateador.locale = NSLocale("es_PE")
    return formateador.stringFromNumber(NSNumber(valor))
        ?: "S/ $valor"
}
