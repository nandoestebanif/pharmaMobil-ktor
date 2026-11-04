#!/usr/bin/env python3
"""Sesion 10 (autonoma): los 6 escenarios de token contra PharmaSoft real.

Requiere PharmaSoft con tokens de 1 minuto y la base H2 en archivo
(backend.sh 1 archivo), para poder deshabilitar un usuario con SQL.
Salida: escenarios-token.json y escenarios-token.txt en $EVID (o ./).
"""
import json, os, subprocess, sys, time, urllib.request, urllib.error, base64
from datetime import datetime

BASE = os.environ.get("BASE", "http://localhost:8080")
EVID = os.environ.get("EVID", ".")
RUTA = "/api/v1/productos"
H2SH = os.environ.get("H2_SH", os.path.join(os.path.dirname(os.path.abspath(__file__)), "h2.sh"))
espera = int(os.environ.get("ESPERA_EXPIRA", "70"))
lineas, filas = [], []


def log(s=""):
    print(s, flush=True)
    lineas.append(s)


def llamar(ruta, auth=None, metodo="GET", cuerpo=None):
    h = {"Accept": "application/json"}
    if auth is not None:
        h["Authorization"] = auth
    if cuerpo is not None:
        h["Content-Type"] = "application/json"
        cuerpo = json.dumps(cuerpo).encode()
    rq = urllib.request.Request(BASE + ruta, data=cuerpo, headers=h, method=metodo)
    try:
        with urllib.request.urlopen(rq, timeout=20) as r:
            return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


def login(u, p):
    c, b = llamar("/api/v1/auth/login", metodo="POST", cuerpo={"username": u, "password": p})
    assert c == 200, (c, b)
    return json.loads(b)["token"]


def recortar(t):
    return t[:12] + "..." + t[-6:]


def mensaje(b):
    try:
        return json.loads(b).get("message") or ""
    except Exception:
        return b[:80]


def sql(q):
    r = subprocess.run(["bash", H2SH, q], capture_output=True, text=True)
    return "\n".join(l for l in r.stdout.splitlines() if l.strip())


def escenario(n, nombre, descripcion, auth_txt, auth):
    ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    c, b = llamar(RUTA, auth)
    m = mensaje(b)
    log(f"[{ts}] Escenario {n}: {nombre}")
    log(f"  {descripcion}")
    log(f"  > GET {RUTA}   Authorization: {auth_txt}")
    log(f"  < HTTP {c}  message: \"{m}\"")
    log()
    filas.append({"n": n, "nombre": nombre, "hora": ts, "descripcion": descripcion,
                  "peticion": f"GET {RUTA}", "authorization": auth_txt, "http": c, "message": m})


log(f"PharmaSoft: {BASE}  |  tokens de 1 minuto  |  {datetime.now():%Y-%m-%d}")
log()
tv = login("vendedor", "Vendedor123")
payload = json.loads(base64.urlsafe_b64decode(tv.split(".")[1] + "==="))
log(f"login vendedor OK: iat={payload['iat']} exp={payload['exp']} (vida {payload['exp'] - payload['iat']} s)")
log()

# 1. valido
escenario(1, "Token valido", "Token recien emitido, sin modificar.", f"Bearer {recortar(tv)}", f"Bearer {tv}")

# 3. alterado: cambia un caracter de la firma
firma = tv.rsplit(".", 1)
car = "A" if firma[1][5] != "A" else "B"
alt = firma[0] + "." + firma[1][:5] + car + firma[1][6:]
escenario(3, "Token alterado",
          f"Se cambia un caracter de la firma (el 6.º: «{firma[1][5]}» por «{car}») del token valido.",
          f"Bearer eyJhbGciOiJI...(firma: {firma[1][:5]}[{car}]{firma[1][6:11]}...)", f"Bearer {alt}")

# 4. sin header
escenario(4, "Sin header Authorization", "Peticion sin cabecera Authorization.", "(sin cabecera)", None)

# 5. prefijo minuscula
escenario(5, "Prefijo incorrecto", "Token valido pero con el prefijo «bearer» en minuscula.",
          f"bearer {recortar(tv)}", f"bearer {tv}")

# 6. usuario deshabilitado con la sesion abierta
tv2 = login("vendedor", "Vendedor123")
c0, _ = llamar(RUTA, f"Bearer {tv2}")
log(f"Antes de deshabilitar: GET {RUTA} con token de vendedor -> HTTP {c0}")
log("SQL> UPDATE usuarios SET estado = FALSE WHERE username = 'vendedor';")
sql("UPDATE usuarios SET estado = FALSE WHERE username = 'vendedor'")
log(sql("SELECT username, estado FROM usuarios WHERE username = 'vendedor'"))
log()
escenario(6, "Usuario deshabilitado", "El mismo token (aun vigente) de un usuario con estado = false.",
          f"Bearer {recortar(tv2)}", f"Bearer {tv2}")
c, b = llamar("/api/v1/auth/login", metodo="POST", cuerpo={"username": "vendedor", "password": "Vendedor123"})
log(f"Login con el usuario deshabilitado -> HTTP {c}  message: \"{mensaje(b)}\"")
filas.append({"n": "6b", "nombre": "Login de usuario deshabilitado", "http": c, "message": mensaje(b)})
log()
log("SQL> UPDATE usuarios SET estado = TRUE WHERE username = 'vendedor';")
sql("UPDATE usuarios SET estado = TRUE WHERE username = 'vendedor'")
log(sql("SELECT username, estado FROM usuarios WHERE username = 'vendedor'"))
log()
c, _ = llamar(RUTA, f"Bearer {tv2}")
log(f"Usuario reactivado: el mismo token vuelve a dar HTTP {c}")
log()

# 2. expirado (al final: hay que esperar)
log(f"Esperando {espera} s para que venza el token del escenario 1 ...")
time.sleep(espera)
escenario(2, "Token expirado", f"El token del escenario 1 despues de {espera} s (vida de 60 s).",
          f"Bearer {recortar(tv)}", f"Bearer {tv}")

json.dump(filas, open(os.path.join(EVID, "escenarios-token.json"), "w"), ensure_ascii=False, indent=1)
open(os.path.join(EVID, "escenarios-token.txt"), "w").write("\n".join(lineas) + "\n")
