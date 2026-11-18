"""Convierte los manuales (Markdown) a PDF: portada, índice, numeración de páginas e imágenes.

Uso:  python -I docs/manuales/generar_pdf.py [manual-usuario.md manual-sistema.md ...]
Requiere Node (usa `npx marked@14.1.4`) y Chrome o Edge instalados.

Extensiones al Markdown:
- Encabezado YAML con titulo / subtitulo / version (portada).
- `![texto](img/x.jpg){.celular}` muestra la captura angosta (pantallas de teléfono).
"""
import html
import pathlib
import re
import shutil
import subprocess
import sys
import tempfile
import unicodedata

DIR = pathlib.Path(__file__).resolve().parent
NAVEGADORES = [r"C:\Program Files\Google\Chrome\Application\chrome.exe", r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
               "google-chrome", "chromium", "msedge"]

CSS = """
@page { size: A4; margin: 18mm 16mm 18mm 16mm;
  @bottom-right { content: counter(page); font: 9pt 'Segoe UI', Arial, sans-serif; color: #6b6b6b; }
  @bottom-left { content: '__PIE__'; font: 9pt 'Segoe UI', Arial, sans-serif; color: #6b6b6b; } }
@page :first { @bottom-right { content: none; } @bottom-left { content: none; } }
body { font: 10.5pt/1.5 'Segoe UI', Arial, sans-serif; color: #1f2a1f; }
.portada { height: 250mm; display: flex; flex-direction: column; justify-content: center; page-break-after: always; }
.portada .logo { width: 34mm; height: 34mm; border-radius: 50%; background: #2E5E3A; color: #fff; display: grid;
  place-items: center; font: 800 26pt 'Segoe UI', Arial; margin-bottom: 14mm; }
.portada h1 { font-size: 30pt; color: #2E5E3A; margin: 0; border: 0; page-break-before: avoid; }
.portada .sub { font-size: 15pt; color: #4d5d4d; margin-top: 4mm; }
.portada .ver { margin-top: 18mm; color: #6b6b6b; }
nav.indice { page-break-after: always; }
nav.indice h2 { color: #2E5E3A; }
nav.indice ol { padding-left: 1.2em; } nav.indice ol ol { padding-left: 1.4em; font-size: 9.5pt; }
nav.indice a { color: #1f2a1f; text-decoration: none; }
h1 { color: #2E5E3A; font-size: 20pt; border-bottom: 2px solid #2E5E3A; padding-bottom: 2mm; page-break-before: always; margin-top: 0; }
h2 { color: #2E5E3A; font-size: 14pt; margin-top: 7mm; page-break-after: avoid; }
h3 { color: #3c6b47; font-size: 11.5pt; margin-top: 5mm; page-break-after: avoid; }
p, li { orphans: 3; widows: 3; }
a { color: #2E5E3A; }
code { background: #f1efe6; padding: 0 3px; border-radius: 3px; font-size: 9.5pt; }
pre { background: #f6f4ec; border: 1px solid #e3dfd0; border-radius: 6px; padding: 3mm; font-size: 8.5pt; white-space: pre-wrap; page-break-inside: avoid; }
pre code { background: none; padding: 0; }
table { border-collapse: collapse; width: 100%; margin: 3mm 0; font-size: 9.5pt; page-break-inside: avoid; }
th { background: #2E5E3A; color: #fff; text-align: left; padding: 1.5mm 2mm; }
td { border-bottom: 1px solid #e3dfd0; padding: 1.5mm 2mm; vertical-align: top; }
blockquote { margin: 3mm 0; padding: 2mm 4mm; background: #fdf3dc; border-left: 4px solid #b7791f; }
img { display: block; max-width: 100%; max-height: 120mm; margin: 3mm auto 5mm; border: 1px solid #d9d4c3; border-radius: 4px;
  page-break-inside: avoid; }
img.celular { max-width: 62mm; max-height: 130mm; }
img.diagrama { max-height: 245mm; border: 0; page-break-before: always; page-break-after: always; }
"""


def slug(texto):
    t = unicodedata.normalize("NFKD", re.sub(r"<[^>]+>", "", texto)).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "-", t).strip("-")


def navegador():
    for n in NAVEGADORES:
        if pathlib.Path(n).exists() or shutil.which(n):
            return n
    raise SystemExit("No se encontró Chrome ni Edge")


def convertir(origen: pathlib.Path):
    texto = origen.read_text(encoding="utf-8")
    meta = {}
    m = re.match(r"---\n(.*?)\n---\n", texto, re.S)
    if m:
        meta = dict(re.findall(r"^(\w+):\s*(.+)$", m.group(1), re.M))
        texto = texto[m.end():]
    # imágenes con clase {.celular}
    texto = re.sub(r"!\[([^\]]*)\]\(([^)]+)\)\{\.(\w+)\}", lambda x: f'<img class="{x.group(3)}" alt="{html.escape(x.group(1))}" src="{x.group(2)}">', texto)
    with tempfile.TemporaryDirectory() as tmp:
        md = pathlib.Path(tmp) / "entrada.md"
        md.write_text(texto, encoding="utf-8")
        npx = "npx.cmd" if sys.platform == "win32" else "npx"
        cuerpo = subprocess.run([npx, "-y", "marked@14.1.4", "-i", str(md), "--gfm"], capture_output=True, text=True, encoding="utf-8", check=True).stdout
    # ids en los títulos (los enlaces internos usan el mismo slug que GitHub)
    indice = []

    def con_id(x):
        nivel, contenido = x.group(1), x.group(2)
        ident = slug(contenido)
        if nivel in ("1", "2"):
            indice.append((int(nivel), ident, re.sub(r"<[^>]+>", "", contenido)))
        return f'<h{nivel} id="{ident}">{contenido}</h{nivel}>'

    cuerpo = re.sub(r"<h([1-3])>(.*?)</h\1>", con_id, cuerpo)
    # índice: capítulos (h1) con sus secciones (h2)
    items, abierto = [], False
    for nivel, ident, titulo in indice:
        if nivel == 1:
            if abierto:
                items.append("</ol></li>")
            items.append(f'<li><a href="#{ident}">{html.escape(titulo)}</a><ol>')
            abierto = True
        else:
            items.append(f'<li><a href="#{ident}">{html.escape(titulo)}</a></li>')
    if abierto:
        items.append("</ol></li>")
    titulo = meta.get("titulo", origen.stem)
    pie = f"{meta.get('titulo', '')} · MG Nutrición y Entrenamiento".replace("'", "")
    documento = f"""<!doctype html><html lang="es-AR"><head><meta charset="utf-8"><title>{html.escape(titulo)}</title>
<style>{CSS.replace('__PIE__', pie)}</style></head><body>
<section class="portada"><div class="logo">MG</div><h1>{html.escape(titulo)}</h1>
<div class="sub">{html.escape(meta.get('subtitulo', ''))}</div><div class="ver">Versión {html.escape(meta.get('version', ''))}</div></section>
<nav class="indice"><h2>Contenido</h2><ol>{''.join(items)}</ol></nav>
{cuerpo}</body></html>"""
    pagina = origen.with_suffix(".html")
    pagina.write_text(documento, encoding="utf-8")
    pdf = origen.with_suffix(".pdf")
    with tempfile.TemporaryDirectory() as perfil:
        subprocess.run([navegador(), "--headless=new", "--disable-gpu", f"--user-data-dir={perfil}", "--no-pdf-header-footer",
                        "--virtual-time-budget=10000", f"--print-to-pdf={pdf}", pagina.as_uri()], check=True, capture_output=True, timeout=300)
    pagina.unlink()
    print(f"{pdf.name}: {pdf.stat().st_size // 1024} KB")


if __name__ == "__main__":
    archivos = sys.argv[1:] or ["manual-usuario.md", "manual-sistema.md"]
    for a in archivos:
        ruta = (DIR / a) if not pathlib.Path(a).is_absolute() else pathlib.Path(a)
        if ruta.exists():
            convertir(ruta)
