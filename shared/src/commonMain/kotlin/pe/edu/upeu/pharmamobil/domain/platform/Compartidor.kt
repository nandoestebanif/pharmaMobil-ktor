package pe.edu.upeu.pharmamobil.domain.platform

/**
 * Capacidad de compartir un texto con otras aplicaciones del dispositivo.
 *
 * Es una interfaz y no un expect porque cada plataforma necesita algo que
 * commonMain no conoce (un Context en Android, un controlador de vista en
 * iOS). La implementacion se inyecta con Koin desde el platformModule, y en
 * las pruebas se sustituye por un doble.
 */
interface Compartidor {
    fun compartir(texto: String)
}
