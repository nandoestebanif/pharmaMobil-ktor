package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.db.SqlDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb

/**
 * Sesion 11: driver en memoria para las pruebas. JDBC solo existe en la JVM, por eso
 * cada plataforma de prueba aporta el suyo (androidHostTest: JdbcSqliteDriver;
 * iosTest: inMemoryDriver nativo). Cada llamada devuelve una base NUEVA y vacia.
 */
expect fun crearDriverDePrueba(): SqlDriver

/** Una base por prueba: si se comparte, la segunda prueba hereda los datos de la primera. */
fun baseEnMemoria(): PharmaDb = PharmaDb(crearDriverDePrueba())

/** Doble del CreadorDeDriver para el grafo de Koin en las pruebas. */
class CreadorDeDriverEnMemoria : CreadorDeDriver {
    override fun crear(): SqlDriver = crearDriverDePrueba()
}
