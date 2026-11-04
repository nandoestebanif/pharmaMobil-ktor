"""
Sesion 10, seccion 6 de la guia: verificacion por rol contra PharmaSoft.
Cada usuario inicia sesion y repite las seis operaciones con su token.
Las escrituras usan datos validos (y unicos por usuario) para que un 200/201
signifique que el servidor realmente autorizo y guardo.

Uso: python matriz_roles.py http://localhost:8080 carpeta_de_salida
Deja matriz-roles.json y matriz-roles.md.
"""
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

BASE = sys.argv[1].rstrip("/") + "/api/v1"
SALIDA = Path(sys.argv[2])
SALIDA.mkdir(parents=True, exist_ok=True)

USUARIOS = [("admin", "Admin123"), ("vendedor", "Vendedor123"), ("almacen", "Almacen123")]


def pedir(metodo, ruta, token=None, cuerpo=None):
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(BASE + ruta, data=datos, method=metodo)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, json.loads(r.read() or b"{}")
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read() or b"{}")
        except ValueError:
            return e.code, {}


operaciones = [
    ("Listar productos", lambda t, u: pedir("GET", "/productos?pagina=0&tamanio=5", t)),
    ("Registrar producto", lambda t, u: pedir("POST", "/productos", t,
        {"nombre": f"Producto de {u}", "precio": 2.5, "stock": 10, "estado": True, "categoriaId": 1})),
    ("Listar clientes", lambda t, u: pedir("GET", "/clientes?pagina=0&tamanio=5", t)),
    ("Registrar venta", lambda t, u: pedir("POST", "/ventas", t,
        {"clienteId": 1, "detalles": [{"productoId": 1, "cantidad": 1}]})),
    ("Ver reportes", lambda t, u: pedir("GET", "/reportes/ventas-por-categoria", t)),
    ("Consultar /auth/me", lambda t, u: pedir("GET", "/auth/me", t)),
]

resultado = {}
for usuario, clave in USUARIOS:
    codigo, cuerpo = pedir("POST", "/auth/login", cuerpo={"username": usuario, "password": clave})
    token = cuerpo.get("token")
    fila = {"login": codigo}
    for nombre, op in operaciones:
        estado, respuesta = op(token, usuario)
        fila[nombre] = {"codigo": estado, "mensaje": respuesta.get("message") if isinstance(respuesta, dict) else None}
    resultado[usuario] = fila

(SALIDA / "matriz-roles.json").write_text(json.dumps(resultado, ensure_ascii=False, indent=2), encoding="utf-8")
lineas = ["| Operación | admin | vendedor | almacen |", "|---|---|---|---|"]
for nombre, _ in operaciones:
    lineas.append(f"| {nombre} | " + " | ".join(str(resultado[u][nombre]["codigo"]) for u, _ in USUARIOS) + " |")
(SALIDA / "matriz-roles.md").write_text("\n".join(lineas) + "\n", encoding="utf-8")
print("\n".join(lineas))
