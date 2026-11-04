package pe.edu.upeu.pharmamobil.platform

/**
 * Sesion 11: escribe una linea del registro de depuracion (KtorHttp). En Android va a
 * la salida estandar, que Logcat muestra como System.out (igual que en la sesion 10). En
 * iOS va a NSLog: el registro unificado del sistema, que se puede leer del simulador con
 * «log stream» (la salida estandar de una app de iOS no se ve fuera de Xcode).
 */
expect fun escribirRegistro(linea: String)
