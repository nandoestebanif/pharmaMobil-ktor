package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb

/** iOS: SQLite del sistema (el plugin enlaza libsqlite3); el archivo va en Application Support. */
class CreadorDeDriverIos : CreadorDeDriver {

    override fun crear(): SqlDriver = NativeSqliteDriver(
        schema = PharmaDb.Schema,
        name = NOMBRE_DE_LA_BASE
    )
}
