"""
Sesion 10, paso 1: comprueba la seguridad de PharmaSoft desde Swagger UI con
un navegador real (Playwright) y guarda las tres capturas que pide la guia:

  sw01  GET /api/v1/productos sin autorizar        -> 401
  sw02  login admin + Authorize + el mismo GET     -> 200
  sw03  login almacen + Authorize + GET /clientes  -> 403

Uso: python swagger_capturas.py http://localhost:8080 carpeta_de_salida
"""
import json
import re
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE = sys.argv[1].rstrip("/")
SALIDA = Path(sys.argv[2])
SALIDA.mkdir(parents=True, exist_ok=True)
resumen = []


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
            "y": (peticion or respuesta)["y"] - 70 + page.evaluate("window.scrollY"),
            "width": arriba["width"],
            "height": respuesta["y"] + respuesta["height"] - (peticion or respuesta)["y"] + 80,
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


with sync_playwright() as p:
    navegador = p.chromium.launch()
    page = navegador.new_page(viewport={"width": 1280, "height": 900}, device_scale_factor=1.5)
    page.goto(f"{BASE}/swagger-ui.html")
    page.locator(".opblock").first.wait_for(timeout=60000)

    # 1. Sin autorizar: 401
    op = abrir(page, "get", "/api/v1/productos")
    codigo = ejecutar(page, op)
    captura(page, op, "sw01-productos-sin-token-401", f"GET /api/v1/productos sin token -> {codigo}")
    op.locator(".opblock-summary").first.click()

    # 2. Login admin, Authorize con el token (sin escribir Bearer) y el mismo GET: 200
    token_admin = login(page, "admin", "Admin123")
    autorizar(page, token_admin)
    op = abrir(page, "get", "/api/v1/productos")
    codigo = ejecutar(page, op)
    captura(page, op, "sw02-productos-admin-200", f"GET /api/v1/productos con token de admin -> {codigo}")
    op.locator(".opblock-summary").first.click()

    # 3. Login almacen y GET /clientes: 403
    token_almacen = login(page, "almacen", "Almacen123")
    autorizar(page, token_almacen)
    op = abrir(page, "get", "/api/v1/clientes")
    codigo = ejecutar(page, op)
    captura(page, op, "sw03-clientes-almacen-403", f"GET /api/v1/clientes con token de almacen -> {codigo}")

    page.screenshot(path=str(SALIDA / "sw00-swagger-completo.png"), full_page=False)
    navegador.close()

(SALIDA / "swagger-resumen.txt").write_text("\n".join(resumen) + "\n", encoding="utf-8")
