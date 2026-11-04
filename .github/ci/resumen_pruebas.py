"""
Resume los XML de JUnit de :shared:testAndroidHostTest: total y, aparte, las pruebas
de la sesion 11 (offline-first) una por una con su resultado.
Uso: python resumen_pruebas.py carpeta_xml salida.txt
"""
import glob
import re
import sys
import xml.etree.ElementTree as ET

CLASES_S11 = ("ProductoRepositorioOfflineTest", "ProductoLocalTest")
EXTRA_S11 = ("sinConexionMuestraLaListaGuardadaConElAvisoDeRed", "registrarNoVuelveAPedirLaLista",
             "sinServidorSeEntraConElPerfilDeLaUltimaSesion", "cerrarSesionBorraTambienElPerfilLocal",
             "abreLaBaseLocalConElDriverRegistrado")
# Actividad autonoma: la suite de 6 pruebas y lo nuevo de "Actualizado hace..." / primera apertura sin red
CLASES_AUTONOMA = ("SuiteOfflineTest", "AntiguedadTest")
CLASES_S13 = ("SincronizacionTest", "PermisosYAvisosTest")
EXTRA_AUTONOMA = ("primeraAperturaSinRedEsUnEstadoVacioInformativoYNoUnError",
                  "laPantallaRecibeLaHoraDeLaUltimaSincronizacionCorrecta")

total = fallos = 0
lineas = []
autonoma = []
s13 = []
for archivo in sorted(glob.glob(sys.argv[1] + "/*.xml")):
    raiz = ET.parse(archivo).getroot()
    for caso in raiz.iter("testcase"):
        total += 1
        fallo = caso.find("failure") is not None or caso.find("error") is not None
        fallos += fallo
        clase = caso.get("classname").rsplit(".", 1)[-1]
        nombre = caso.get("name")
        linea = f"  {'FALLO' if fallo else 'PASO '} {clase} > {nombre}  ({float(caso.get('time')):.3f} s)"
        if clase in CLASES_S11 or nombre in EXTRA_S11:
            lineas.append(linea)
        if clase in CLASES_AUTONOMA or nombre in EXTRA_AUTONOMA:
            autonoma.append(linea)
        if clase in CLASES_S13:
            s13.append(linea)

with open(sys.argv[2], "w", encoding="utf-8") as salida:
    salida.write(f"$ ./gradlew :shared:testAndroidHostTest\n")
    salida.write(f"{total} pruebas, {fallos} con fallo\n\nPruebas de la sesion 11 (offline-first):\n")
    salida.write("\n".join(lineas) + "\n")
    salida.write("\nActividad autonoma (suite offline, antiguedad, primera apertura):\n")
    salida.write("\n".join(sorted(autonoma, key=lambda l: ("SuiteOffline" not in l, l))) + "\n")
    salida.write("\nSesion 13 (sincronizacion refinada, permisos y avisos):\n")
    salida.write("\n".join(sorted(s13)) + "\n")
print(open(sys.argv[2], encoding="utf-8").read())
