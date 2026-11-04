"""
Sesion 11, paso 6 de la guia: comprueba en Swagger UI (navegador real, Playwright)
que el producto creado SIN RED en la app llego a PharmaSoft con un id real (positivo).

  sw11-producto-sincronizado  GET /api/v1/productos/{id} con el token de admin -> 200

Uso: python swagger_s11.py http://localhost:8080 carpeta_de_salida "Omeprazol 20mg"
"""
import json
import sys
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE = sys.argv[1].rstrip("/")
SALIDA = Path(sys.argv[2])
NOMBRE = sys.argv[3]
SALIDA.mkdir(parents=True, exist_ok=True)
resumen = []


def buscar_id(nombre):
    """Id que PharmaSoft le asigno al producto (consulta directa, sin la app)."""
    datos = json.dumps({"username": "admin", "password": "Admin123"}).encode()
    req = urllib.request.Request(BASE + "/api/v1/auth/login", data=datos, headers={"Content-Type": "application/json"})
    token = json.loads(urllib.request.urlopen(req).read())["token"]
    req = urllib.request.Request(BASE + "/api/v1/productos?pagina=0&tamanio=100", headers={"Authorization": "Bearer " + token})
    contenido = json.loads(urllib.request.urlopen(req).read())["contenido"]
    for p in contenido:
        if p["nombre"].strip().lower() == nombre.lower():
            return p["id"]
    raise SystemExit(f"{nombre} no esta en PharmaSoft: la sincronizacion no lo subio")


def bloque(page, metodo, ruta):
    """El bloque de la operacion (metodo + ruta exacta) en Swagger UI."""
    return page.locator(f".opblock-{metodo}").filter(
        has=page.locator(f'.opblock-summary-path[data-path="{ruta}"]')
    ).first


def abrir(page, metodo, ruta):
    op = bloque(page, metodo, ruta)
    op.scroll_into_view_if_needed()
    if "is-open" not in (op.get_attribute("class") or ""):
        op.locator(".opblock-summary").first.click()
    # El bloque se despliega con una animacion: se espera el boton antes de leerlo.
    boton = op.locator(".try-out__btn").first
    boton.wait_for(timeout=20000)
    if boton.inner_text().strip().lower().startswith("try"):
        boton.click()
    op.locator(".execute").first.wait_for(timeout=20000)
    return op


def ejecutar(page, op):
    op.locator(".execute").first.click()
    op.locator(".live-responses-table tbody .response-col_status").first.wait_for(timeout=20000)
    page.wait_for_timeout(600)
    codigo = op.locator(".live-responses-table tbody .response-col_status").first.inner_text().strip()
    return codigo.split()[0]


def cuerpo(op):
    return op.locator(".live-responses-table .response-col_description pre").first.inner_text()


def autorizar(page, token):
    page.locator("button.authorize").first.click()
    dialogo = page.locator(".dialog-ux .modal-ux")
    dialogo.wait_for()
    salir = dialogo.locator("button", has_text="Logout")
    if salir.count():
        salir.first.click()
    dialogo.locator("input").first.fill(token)
    dialogo.locator("button.authorize").first.click()
    dialogo.locator("button", has_text="Close").first.click()


def captura(page, op, nombre, texto):
    """Recorta desde la cabecera de la operacion hasta la respuesta del servidor
    (la URL pedida, el codigo y el cuerpo), sin los esquemas de documentacion."""
    tabla = op.locator(".live-responses-table").first
    tabla.scroll_into_view_if_needed()
    page.wait_for_timeout(400)
    cabecera = op.locator(".opblock-summary").first.bounding_box()
    peticion = op.locator(".request-url").first.bounding_box()
    respuesta = tabla.bounding_box()
    # La cabecera queda arriba fuera de vista: se arma con dos recortes del documento.
    arriba = op.bounding_box()
    page.screenshot(
        path=str(SALIDA / f"{nombre}.png"),
        full_page=True,
        clip={
            "x": arriba["x"],
            "y": (peticion or respuesta)["y"] - 38 + page.evaluate("window.scrollY"),  # sin el curl con el token
            "width": arriba["width"],
            "height": respuesta["y"] + respuesta["height"] - (peticion or respuesta)["y"] + 48,
        },
    )
    page.screenshot(
        path=str(SALIDA / f"{nombre}-cabecera.png"),
        full_page=True,
        clip={"x": arriba["x"], "y": cabecera["y"] + page.evaluate("window.scrollY"),
              "width": arriba["width"], "height": cabecera["height"]},
    )
    resumen.append(f"{nombre}: {texto}")
    print(nombre, texto)


def login(page, usuario, clave):
    op = abrir(page, "post", "/api/v1/auth/login")
    editor = op.locator("textarea.body-param__text").first
    editor.fill(json.dumps({"username": usuario, "password": clave}, indent=2))
    codigo = ejecutar(page, op)
    token = json.loads(cuerpo(op))["token"]
    resumen.append(f"login {usuario}: HTTP {codigo}")
    # Se pliega para que la siguiente captura quede limpia.
    op.locator(".opblock-summary").first.click()
    return token



ID = buscar_id(NOMBRE)
print(f"{NOMBRE} -> id {ID} en PharmaSoft")
resumen.append(f"{NOMBRE}: id {ID} en PharmaSoft")

with sync_playwright() as p:
    navegador = p.chromium.launch()
    page = navegador.new_page(viewport={"width": 1280, "height": 900}, device_scale_factor=1.5)
    page.goto(f"{BASE}/swagger-ui.html")
    page.locator(".opblock").first.wait_for(timeout=60000)

    token = login(page, "admin", "Admin123")
    autorizar(page, token)

    op = abrir(page, "get", "/api/v1/productos/{id}")
    op.locator("tr[data-param-name='id'] input").first.fill(str(ID))
    codigo = ejecutar(page, op)
    texto = cuerpo(op)
    captura(page, op, "sw11-producto-sincronizado", f"GET /api/v1/productos/{ID} -> HTTP {codigo}")
    (SALIDA / "sw11-respuesta.json").write_text(texto, encoding="utf-8")
    navegador.close()

(SALIDA / "swagger-s11-resumen.txt").write_text("\n".join(resumen) + "\n", encoding="utf-8")
assert ID > 0, "el id debe ser el real del servidor (positivo)"
