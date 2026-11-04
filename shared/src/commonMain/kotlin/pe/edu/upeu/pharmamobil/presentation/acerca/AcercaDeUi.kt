package pe.edu.upeu.pharmamobil.presentation.acerca

import pe.edu.upeu.pharmamobil.platform.InfoDispositivo
import pe.edu.upeu.pharmamobil.platform.formatearSoles

/** Monto de ejemplo para mostrar como formatea la moneda cada plataforma. */
internal const val MONTO_DE_MUESTRA = 24.5

/** Lo que muestra la pantalla «Acerca de», ya convertido a texto. */
data class AcercaDeUi(
    val sistema: String,
    val version: String,
    val modelo: String,
    val precioDeMuestra: String,
    val separador: String
)

/**
 * Junta las dos capacidades sin estado del proyecto: los datos del equipo
 * (InfoDispositivo) y el formato de moneda (formatearSoles). Las dos son
 * expect/actual; esta funcion es comun y no sabe en que plataforma corre.
 */
fun InfoDispositivo.aUi(): AcercaDeUi {
    val precio = formatearSoles(MONTO_DE_MUESTRA)
    return AcercaDeUi(
        sistema = sistema,
        version = version,
        modelo = modelo,
        precioDeMuestra = precio,
        separador = describirSeparador(precio)
    )
}

/**
 * Describe lo que hay entre el simbolo «S/» y la primera cifra. Es justo lo
 * que cambia entre plataformas, y a simple vista no se distingue un espacio
 * normal de uno de no separacion.
 */
fun describirSeparador(precioFormateado: String): String {

    val primeraCifra = precioFormateado.indexOfFirst { it.isDigit() }
    val inicio = precioFormateado.indexOf("S/").let { if (it < 0) 0 else it + 2 }

    if (primeraCifra < 0 || primeraCifra < inicio) return "No se reconoce el formato"

    val entreMedio = precioFormateado.substring(inicio, primeraCifra)
    if (entreMedio.isEmpty()) return "Ninguno: el símbolo va pegado al monto"

    return entreMedio.map { caracter -> "${codigoUnicode(caracter)} (${nombreDe(caracter)})" }
        .joinToString(" + ")
}

private fun codigoUnicode(caracter: Char): String =
    "U+" + caracter.code.toString(16).uppercase().padStart(4, '0')

private fun nombreDe(caracter: Char): String = when (caracter) {
    ' ' -> "espacio"
    ' ' -> "espacio de no separación"
    ' ' -> "espacio fino de no separación"
    else -> "otro carácter"
}
