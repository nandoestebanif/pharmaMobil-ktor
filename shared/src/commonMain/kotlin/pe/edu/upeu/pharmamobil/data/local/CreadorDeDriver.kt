package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * Sesion 11: abre la base local. El esquema y las consultas son comunes; lo
 * unico que cambia por plataforma es quien abre el archivo (mismo patron que
 * AlmacenSeguro de la sesion 10): AndroidSqliteDriver en Android,
 * NativeSqliteDriver en iOS y un driver en memoria en las pruebas.
 */
interface CreadorDeDriver {
    fun crear(): SqlDriver
}

/** Nombre del archivo de la base en el dispositivo. */
const val NOMBRE_DE_LA_BASE = "pharma.db"
