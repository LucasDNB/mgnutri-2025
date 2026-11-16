"""Exporta cada página de er.drawio y clases.drawio a PNG (para el informe) sin necesitar draw.io instalado.

Arma una página HTML temporal con el visor oficial de diagrams.net (viewer-static.min.js) y la captura con
Chrome/Edge headless al tamaño exacto del diagrama, con escala 2x para que el texto se lea impreso.

Uso:  python -I docs/diagramas/exportar_png.py [--escala 2] [--navegador "C:/Program Files/Google/Chrome/Application/chrome.exe"]
Salida: docs/diagramas/png/<archivo>-<página>.png
"""
import argparse
import html
import json
import pathlib
import shutil
import subprocess
import tempfile
import unicodedata
import xml.etree.ElementTree as ET

DIR = pathlib.Path(__file__).resolve().parent
SALIDA = DIR / "png"
NAVEGADORES = [r"C:\Program Files\Google\Chrome\Application\chrome.exe",
               r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
               "google-chrome", "chromium", "msedge"]
MARGEN = 40


def limites(diagrama):
    """Caja que contiene todos los vértices de primer nivel de una página."""
    xs, ys, xs2, ys2 = [], [], [], []
    for c in diagrama.iter("mxCell"):
        g = c.find("mxGeometry")
        if c.get("vertex") == "1" and c.get("parent") == "1" and g is not None:
            x, y = float(g.get("x", 0)), float(g.get("y", 0))
            xs.append(x); ys.append(y)
            xs2.append(x + float(g.get("width", 0))); ys2.append(y + float(g.get("height", 0)))
    return min(xs), min(ys), max(xs2), max(ys2)


def slug(texto):
    t = unicodedata.normalize("NFKD", texto).encode("ascii", "ignore").decode().lower()
    return "".join(ch if ch.isalnum() else "-" for ch in t).strip("-").replace("--", "-")


def navegador(preferido):
    for n in ([preferido] if preferido else []) + NAVEGADORES:
        if pathlib.Path(n).exists() or shutil.which(n):
            return n
    raise SystemExit("No se encontró Chrome/Edge; indicá la ruta con --navegador")


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--escala", type=float, default=2)
    p.add_argument("--navegador")
    args = p.parse_args()
    exe = navegador(args.navegador)
    SALIDA.mkdir(exist_ok=True)
    tmp = pathlib.Path(tempfile.mkdtemp(prefix="mgnutri-png-"))
    try:
        for archivo in ("er.drawio", "clases.drawio"):
            texto = (DIR / archivo).read_text(encoding="utf-8")
            for i, diagrama in enumerate(ET.fromstring(texto).findall("diagram")):
                x0, y0, x1, y1 = limites(diagrama)
                ancho, alto = int(x1 - x0) + 2 * MARGEN, int(y1 - y0) + 2 * MARGEN
                config = {"xml": texto, "page": i, "toolbar": "", "nav": False, "resize": False, "border": MARGEN, "zoom": 1,
                          "highlight": "none", "lightbox": False}
                pagina = tmp / f"p{i}.html"
                pagina.write_text(
                    "<!doctype html><meta charset=utf-8><style>html,body{margin:0;background:#fff}</style>"
                    f"<div class=mxgraph style='width:{ancho}px;height:{alto}px' data-mxgraph=\"{html.escape(json.dumps(config))}\"></div>"
                    "<script src='https://viewer.diagrams.net/js/viewer-static.min.js'></script>", encoding="utf-8")
                destino = SALIDA / f"{archivo.split('.')[0]}-{slug(diagrama.get('name'))}.png"
                subprocess.run([exe, "--headless=new", "--disable-gpu", "--hide-scrollbars", f"--user-data-dir={tmp / 'perfil'}",
                                f"--force-device-scale-factor={args.escala}", f"--window-size={ancho},{alto}",
                                "--virtual-time-budget=20000", f"--screenshot={destino}", pagina.as_uri()],
                               check=True, capture_output=True, timeout=180)
                print(f"{destino.relative_to(DIR.parents[1])}  ({ancho}x{alto} px @ {args.escala}x)")
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
