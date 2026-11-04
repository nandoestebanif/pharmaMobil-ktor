package pe.edu.upeu.pharmamobil.platform

/**
 * Datos del sistema en el que corre la app. Cada plataforma los obtiene con su
 * propia API: android.os.Build en Android y UIDevice en iOS. Se muestran en la
 * pantalla «Acerca de».
 *
 * Es una clase expect y no una interfaz porque no necesita dependencias: se
 * construye sin parametros y el compilador exige el actual en cada plataforma.
 */
expect class InfoDispositivo() {

    /** Nombre del sistema operativo, por ejemplo «Android» o «iOS». */
    val sistema: String

    /** Version del sistema operativo, por ejemplo «16» o «26.5». */
    val version: String

    /** Modelo del equipo o del emulador/simulador. */
    val modelo: String
}
