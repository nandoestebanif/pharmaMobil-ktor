package pe.edu.upeu.pharmamobil.platform

import java.text.NumberFormat
import java.util.Locale

/** Android: el formato de moneda lo resuelve java.text con la configuracion regional es-PE. */
actual fun formatearSoles(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE")).format(valor)
