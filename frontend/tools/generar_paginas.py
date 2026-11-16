"""Genera los HTML "cáscara" de cada página: cada uno carga su módulo ES en js/pages/.

Uso: python -I frontend/tools/generar_paginas.py
Las páginas no contienen lógica ni scripts en línea (compatibles con la CSP del sitio).
"""
import pathlib

RAIZ = pathlib.Path(__file__).resolve().parent.parent
CHART = '<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>\n'

# (ruta html, título, módulo, usa Chart.js)
PAGINAS = [
    ("index.html", "Ingresar", "login", False),
    ("recupero.html", "Recuperar contraseña", "recupero", False),
    ("recupero-confirmar.html", "Nueva contraseña", "recupero", False),
    ("cambiar-password.html", "Cambiar contraseña", "cambiar-password", False),
    ("terminos.html", "Términos y condiciones", "terminos", False),
    ("admin/home.html", "Inicio", "admin-home", False),
    ("admin/profesionales.html", "Profesionales", "admin-profesionales", False),
    ("admin/profesional.html", "Profesional", "admin-profesional", False),
    ("admin/paciente-form.html", "Paciente", "admin-paciente-form", False),
    ("admin/pagos.html", "Pagos", "admin-pagos", False),
    ("admin/deudores.html", "Deudores", "admin-deudores", False),
    ("admin/aranceles.html", "Aranceles", "admin-aranceles", False),
    ("admin/opciones.html", "Opciones avanzadas", "admin-opciones", False),
    ("admin/bitacora.html", "Bitácora", "admin-bitacora", False),
    ("admin/accesos.html", "Registro de accesos", "admin-accesos", False),
    ("admin/notificaciones.html", "Notificaciones", "admin-notificaciones", False),
    ("admin/backups.html", "Backups", "admin-backups", False),
    ("admin/configuracion.html", "Configuración", "admin-configuracion", False),
    ("comun/calendario.html", "Calendario", "comun-calendario", False),
    ("comun/pacientes.html", "Pacientes", "comun-pacientes", False),
    ("comun/paciente.html", "Ficha del paciente", "comun-paciente", True),
    ("comun/reportes.html", "Reportes", "comun-reportes", True),
    ("comun/evolucion.html", "Evolución", "comun-evolucion", True),
    ("comun/alimentos.html", "Base de alimentos", "comun-alimentos", False),
    ("comun/ejercicios.html", "Base de ejercicios", "comun-ejercicios", False),
    ("profesional/home.html", "Inicio", "prof-home", False),
    ("profesional/novedades.html", "Novedades", "prof-novedades", False),
    ("profesional/agenda.html", "Agenda", "prof-agenda", False),
    ("profesional/plan-editor.html", "Plan alimentario", "prof-plan-editor", False),
    ("profesional/rutina-editor.html", "Rutina", "prof-rutina-editor", False),
    ("paciente/home.html", "Inicio", "pac-home", False),
    ("paciente/nutricion.html", "Nutrición", "pac-nutricion", False),
    ("paciente/mi-plan.html", "Mi plan alimentario", "pac-mi-plan", False),
    ("paciente/registro-dia.html", "Registro del día", "pac-registro-dia", False),
    ("paciente/observaciones.html", "Observaciones diarias", "pac-observaciones", True),
    ("paciente/turnos.html", "Mis turnos", "pac-turnos", False),
    ("paciente/turno-nuevo.html", "Nuevo turno", "pac-turno-nuevo", False),
    ("paciente/entrenamiento.html", "Entrenamiento", "pac-entrenamiento", False),
    ("paciente/mi-rutina.html", "Mi rutina", "pac-mi-rutina", False),
    ("paciente/molestia.html", "Registrar molestia", "pac-molestia", False),
    ("paciente/cuenta.html", "Mi cuenta", "pac-cuenta", False),
    ("paciente/perfil.html", "Mi perfil", "pac-perfil", False),
    ("paciente/contacto.html", "Contacto", "pac-contacto", False),
]

PLANTILLA = """<!doctype html>
<html lang="es-AR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="theme-color" content="#2E5E3A">
<title>{titulo} · MG Nutrición y Entrenamiento</title>
<link rel="icon" href="/assets/favicon.svg" type="image/svg+xml">
<link rel="stylesheet" href="/css/base.css">
{chart}<script type="module" src="/js/pages/{modulo}.js"></script>
</head>
<body>
<div id="app"><noscript>Necesitás habilitar JavaScript para usar el sistema.</noscript></div>
</body>
</html>
"""


def main():
    for ruta, titulo, modulo, chart in PAGINAS:
        destino = RAIZ / ruta
        destino.parent.mkdir(parents=True, exist_ok=True)
        destino.write_text(PLANTILLA.format(titulo=titulo, modulo=modulo, chart=CHART if chart else ""), encoding="utf-8")
    faltantes = [m for _, _, m, _ in PAGINAS if not (RAIZ / "js" / "pages" / f"{m}.js").exists()]
    print(f"{len(PAGINAS)} páginas generadas. Módulos faltantes: {sorted(set(faltantes))}")


if __name__ == "__main__":
    main()
