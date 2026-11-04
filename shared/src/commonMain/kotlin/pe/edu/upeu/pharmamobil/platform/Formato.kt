package pe.edu.upeu.pharmamobil.platform

/**
 * Formatea un monto como moneda del Peru (soles).
 *
 * PharmaSoft envia el precio como numero; la forma correcta de mostrarlo
 * depende del sistema operativo (simbolo, separadores, espacios), asi que
 * commonMain solo declara que la funcion debe existir. Cada plataforma
 * aporta su `actual` y el compilador exige que esten todos.
 */
expect fun formatearSoles(valor: Double): String
