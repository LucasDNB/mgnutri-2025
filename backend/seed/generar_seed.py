"""Genera V12__seed_catalogo.sql a partir de alimentos.csv y ejercicios.csv.

Uso: python -I generar_seed.py
Fuente de valores nutricionales: valores de referencia aproximados por 100 g de
USDA FoodData Central (SR Legacy) y Tabla de Composición de Alimentos de Argentina
(Argenfoods, UNLu). Son valores orientativos para el catálogo inicial; el profesional
puede ajustarlos desde la Base de datos de alimentos.
"""
import csv
import pathlib
import unicodedata

BASE = pathlib.Path(__file__).resolve().parent
SALIDA = BASE.parent / "src" / "main" / "resources" / "db" / "migration" / "V12__seed_catalogo.sql"


def norm(s: str) -> str:
    s = unicodedata.normalize("NFD", s)
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return " ".join(s.lower().split())


def q(s):
    if s is None or s == "":
        return "NULL"
    return "N'" + s.replace("'", "''") + "'"


def main():
    lineas = ["-- Semilla del catálogo (spec 10). Generado por backend/seed/generar_seed.py",
              "-- Fuente: USDA FoodData Central (SR Legacy) y Argenfoods (UNLu), valores aproximados por 100 g.", ""]
    with open(BASE / "alimentos.csv", encoding="utf-8") as f:
        filas = list(csv.DictReader(f))
    for r in filas:
        lineas.append(
            "INSERT INTO alimento (nombre, nombre_normalizado, grupo_alimento_id, kcal_100g, hc_100g, proteinas_100g, grasas_100g, fibra_100g, "
            "unidad_casera, gramos_unidad_casera, apto_desayuno, apto_colacion, apto_almuerzo, activo, created_at, created_by) "
            f"SELECT {q(r['nombre'])}, {q(norm(r['nombre']))}, g.id, {r['kcal']}, {r['hc']}, {r['prot']}, {r['grasas']}, {r['fibra']}, "
            f"{q(r['unidad'])}, {r['gramos'] or 'NULL'}, {1 if 'D' in r['aptitud'] else 0}, {1 if 'C' in r['aptitud'] else 0}, "
            f"{1 if 'A' in r['aptitud'] else 0}, 1, SYSUTCDATETIME(), 'SISTEMA' FROM grupo_alimento g WHERE g.nombre = {q(r['grupo'])};")
    lineas.append("")
    with open(BASE / "ejercicios.csv", encoding="utf-8") as f:
        ejercicios = list(csv.DictReader(f))
    for r in ejercicios:
        video = "https://www.youtube.com/results?search_query=" + r["nombre"].replace(" ", "+") + "+tecnica"
        lineas.append(
            "INSERT INTO ejercicio (nombre, nombre_normalizado, descripcion, grupo_muscular_principal_id, dificultad, equipamiento, video_url, activo, created_at, created_by) "
            f"SELECT {q(r['nombre'])}, {q(norm(r['nombre']))}, {q(r['descripcion'])}, g.id, '{r['dificultad']}', {q(r['equipamiento'])}, "
            f"{q(video)}, 1, SYSUTCDATETIME(), 'SISTEMA' FROM grupo_muscular g WHERE g.nombre = {q(r['grupo'])};")
    SALIDA.write_text("\n".join(lineas) + "\n", encoding="utf-8")
    print(f"{len(filas)} alimentos, {len(ejercicios)} ejercicios -> {SALIDA}")


if __name__ == "__main__":
    main()
