package pe.edu.upeu.pharmamobil.domain.platform

/**
 * Doble del Compartidor para las pruebas: no abre nada, solo anota lo que le
 * pidieron compartir. Es posible porque la capacidad es una interfaz que se
 * inyecta, no un expect.
 */
class CompartidorFalso : Compartidor {

    val textos = mutableListOf<String>()

    override fun compartir(texto: String) {
        textos.add(texto)
    }
}
