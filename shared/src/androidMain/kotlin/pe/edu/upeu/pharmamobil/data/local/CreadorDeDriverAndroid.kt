package pe.edu.upeu.pharmamobil.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb

/** Android: el archivo queda en data/data/<paquete>/databases/pharma.db. */
class CreadorDeDriverAndroid(
    private val contexto: Context
) : CreadorDeDriver {

    override fun crear(): SqlDriver = AndroidSqliteDriver(
        schema = PharmaDb.Schema,
        context = contexto,
        name = NOMBRE_DE_LA_BASE
    )
}
