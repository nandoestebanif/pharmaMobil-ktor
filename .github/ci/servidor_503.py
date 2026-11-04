"""
Sesion 13: servidor de prueba que responde 503 «Servicio no disponible» a todo, en el
mismo puerto que PharmaSoft. Sirve para provocar un fallo TRANSITORIO real (la app suma un
intento y espera antes de seguir), distinto de «sin red» (connection refused).
Uso: python servidor_503.py 8080
"""
import json
import sys
from datetime import datetime
from http.server import BaseHTTPRequestHandler, HTTPServer


class Respuesta503(BaseHTTPRequestHandler):
    def _responder(self):
        cuerpo = json.dumps({
            "timestamp": datetime.now().isoformat(timespec="seconds"),
            "status": 503, "error": "Service Unavailable",
            "message": "Servicio no disponible", "path": self.path,
        }).encode()
        self.send_response(503)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(cuerpo)))
        self.end_headers()
        self.wfile.write(cuerpo)
        print(f"{datetime.now():%H:%M:%S.%f} {self.command} {self.path} -> 503", flush=True)

    do_GET = do_POST = do_PUT = do_DELETE = _responder

    def log_message(self, *args):
        pass


HTTPServer(("0.0.0.0", int(sys.argv[1])), Respuesta503).serve_forever()
