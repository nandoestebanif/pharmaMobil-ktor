package pe.edu.upeu.pharmamobil.data.local

/** Doble de AlmacenSeguro para las pruebas: guarda el token en una variable. */
class AlmacenEnMemoria(var token: String? = null) : AlmacenSeguro {

    var vecesBorrado = 0
        private set

    override fun guardarToken(token: String) {
        this.token = token
    }

    override fun leerToken(): String? = token

    override fun borrarToken() {
        token = null
        vecesBorrado++
    }
}
