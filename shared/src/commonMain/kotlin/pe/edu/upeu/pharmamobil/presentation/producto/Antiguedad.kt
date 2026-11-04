package pe.edu.upeu.pharmamobil.presentation.producto

/**
 * Actividad autonoma 11: cuanto tiempo paso desde la ultima sincronizacion correcta, en
 * lenguaje natural. Sin ese dato el usuario no sabe si mira datos de hace cinco minutos o
 * de hace tres dias.
 *
 * @param ultima instante de la ultima sincronizacion correcta (ms), o null si nunca hubo una.
 * @param ahora instante actual (ms).
 */
fun textoDeAntiguedad(ultima: Long?, ahora: Long): String {
    if (ultima == null) return "Nunca sincronizado"
    val minutos = (ahora - ultima).coerceAtLeast(0) / 60_000
    val horas = minutos / 60
    val dias = horas / 24
    return when {
        minutos < 1 -> "Actualizado hace un momento"
        minutos < 60 -> "Actualizado hace ${plural(minutos, "minuto", "minutos")}"
        horas < 24 -> "Actualizado hace ${plural(horas, "hora", "horas")}"
        else -> "Actualizado hace ${plural(dias, "día", "días")}"
    }
}

private fun plural(n: Long, uno: String, varios: String) = if (n == 1L) "1 $uno" else "$n $varios"
