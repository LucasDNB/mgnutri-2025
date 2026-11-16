"""Genera los diagramas del sistema (CA-00.5.1) a partir de la fuente de verdad:

- er.drawio      Modelo E-R leído del esquema real de SQL Server (migraciones Flyway aplicadas).
- clases.drawio  Diagrama de clases del dominio (entidades JPA) + página de capas por módulo, leído del código Java.
- README.md      Las mismas vistas en Mermaid (se renderizan en GitHub).

Uso (con la base levantada: docker compose up -d y la app ejecutada al menos una vez):
    python -I docs/diagramas/generar_diagramas.py [--servidor localhost,14333] [--base mgnutri] [--usuario sa] [--password ...]
"""
import argparse
import html
import pathlib
import re
import subprocess
import xml.etree.ElementTree as ET

RAIZ = pathlib.Path(__file__).resolve().parents[2]
SALIDA = pathlib.Path(__file__).resolve().parent
JAVA = RAIZ / "backend" / "src" / "main" / "java" / "ar" / "edu" / "usal" / "mgnutri"

# ----------------------------------------------------------------------------------------------- módulos
MODULOS_TABLAS = [
    ("Seguridad y configuración", "#dae8fc", "#6c8ebf",
     ["usuario", "usuario_rol", "administrador", "token_revocado", "token_recupero", "registro_acceso", "terminos_condiciones", "configuracion_sistema"]),
    ("Personas", "#d5e8d4", "#82b366", ["paciente", "profesional", "paciente_profesional"]),
    ("Agenda y turnos", "#fff2cc", "#d6b656", ["disponibilidad", "bloqueo_agenda", "turno", "turno_historial"]),
    ("Pagos", "#f8cecc", "#b85450", ["arancel", "cargo", "pago"]),
    ("Catálogo", "#e1d5e7", "#9673a6", ["grupo_alimento", "alimento", "equivalencia_manual", "grupo_muscular", "ejercicio", "ejercicio_grupo_secundario"]),
    ("Nutrición", "#d5e8d4", "#82b366", ["plan_alimentario", "comida_plan", "item_comida", "reemplazo_alimento", "registro_comida", "observacion_diaria"]),
    ("Entrenamiento", "#fff2cc", "#d6b656", ["rutina", "dia_rutina", "ejercicio_rutina", "reemplazo_ejercicio", "registro_entrenamiento"]),
    ("Interacción y evolución", "#dae8fc", "#6c8ebf", ["comentario", "molestia", "medicion_antropometrica", "evaluacion_fisica"]),
    ("Operación", "#f5f5f5", "#666666", ["bitacora_evento", "notificacion", "alerta", "backup_registro"]),
]
CIFRADAS = {("paciente", c) for c in ["telefono_whatsapp", "email", "domicilio", "antecedentes_salud", "observaciones"]} | \
    {("profesional", "telefono_whatsapp"), ("profesional", "email"), ("administrador", "telefono"), ("notificacion", "telefono"),
     ("notificacion", "parametros"), ("medicion_antropometrica", "observaciones")}


# ----------------------------------------------------------------------------------------------- esquema
def sql(args, consulta):
    cmd = ["sqlcmd", "-S", args.servidor, "-U", args.usuario, "-P", args.password, "-C", "-d", args.base, "-h", "-1", "-W", "-s", "|", "-Q",
           "SET NOCOUNT ON; " + consulta]
    salida = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace", check=True).stdout
    return [linea.split("|") for linea in salida.splitlines() if linea.strip()]


def leer_esquema(args):
    columnas = sql(args, """
        SELECT t.name, c.name, ty.name, c.max_length, c.precision, c.scale, c.is_nullable,
               CASE WHEN EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.indexes i ON i.object_id = ic.object_id AND i.index_id = ic.index_id
                                 WHERE i.is_primary_key = 1 AND ic.object_id = c.object_id AND ic.column_id = c.column_id) THEN 1 ELSE 0 END,
               CASE WHEN EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.indexes i ON i.object_id = ic.object_id AND i.index_id = ic.index_id
                                 WHERE i.is_unique = 1 AND i.is_primary_key = 0 AND i.has_filter = 0 AND ic.object_id = c.object_id AND ic.column_id = c.column_id
                                   AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id) = 1) THEN 1 ELSE 0 END
        FROM sys.tables t JOIN sys.columns c ON c.object_id = t.object_id JOIN sys.types ty ON ty.user_type_id = c.user_type_id
        WHERE t.name <> 'flyway_schema_history' ORDER BY t.name, c.column_id""")
    fks = sql(args, """
        SELECT tp.name, cp.name, tr.name, cp.is_nullable
        FROM sys.foreign_key_columns f
        JOIN sys.tables tp ON tp.object_id = f.parent_object_id JOIN sys.columns cp ON cp.object_id = f.parent_object_id AND cp.column_id = f.parent_column_id
        JOIN sys.tables tr ON tr.object_id = f.referenced_object_id ORDER BY tp.name""")
    tablas = {}
    for t, c, ty, largo, prec, esc, nulo, pk, unico in columnas:
        largo = int(largo)
        if ty in ("nvarchar", "nchar"):
            tipo = f"{ty.upper()}({'MAX' if largo == -1 else largo // 2})"
        elif ty in ("varchar", "char", "varbinary"):
            tipo = f"{ty.upper()}({'MAX' if largo == -1 else largo})"
        elif ty == "decimal":
            tipo = f"DECIMAL({prec},{esc})"
        elif ty in ("datetime2", "time"):
            tipo = f"{ty.upper()}({esc})"
        else:
            tipo = ty.upper()
        tablas.setdefault(t, []).append({"nombre": c, "tipo": tipo, "nulo": nulo == "1", "pk": pk == "1", "unico": unico == "1"})
    relaciones = [{"desde": a, "columna": b, "hacia": c, "nulo": d == "1"} for a, b, c, d in fks]
    for r in relaciones:
        for col in tablas[r["desde"]]:
            if col["nombre"] == r["columna"]:
                col["fk"] = r["hacia"]
                r["unico"] = col["unico"]
    return tablas, relaciones


# ----------------------------------------------------------------------------------------------- draw.io
class Drawio:
    def __init__(self):
        self.paginas = []

    def pagina(self, nombre):
        celdas = []
        self.paginas.append((nombre, celdas))
        return celdas

    def guardar(self, ruta):
        mxfile = ET.Element("mxfile", host="MG Nutrición · generar_diagramas.py", type="device")
        for i, (nombre, celdas) in enumerate(self.paginas):
            diagrama = ET.SubElement(mxfile, "diagram", id=f"p{i}", name=nombre)
            modelo = ET.SubElement(diagrama, "mxGraphModel", dx="1600", dy="900", grid="1", gridSize="10", guides="1", tooltips="1",
                                   connect="1", arrows="1", fold="1", page="0", pageScale="1", math="0", shadow="0")
            raiz = ET.SubElement(modelo, "root")
            ET.SubElement(raiz, "mxCell", id="0")
            ET.SubElement(raiz, "mxCell", id="1", parent="0")
            for c in celdas:
                raiz.append(c)
        ET.indent(mxfile)
        ET.ElementTree(mxfile).write(ruta, encoding="utf-8", xml_declaration=True)


def vertice(id_, etiqueta, x, y, w, h, estilo, padre="1"):
    c = ET.Element("mxCell", id=id_, value=etiqueta, style=estilo, vertex="1", parent=padre)
    ET.SubElement(c, "mxGeometry", x=str(x), y=str(y), width=str(w), height=str(h), **{"as": "geometry"})
    return c


def arista(id_, origen, destino, estilo, etiqueta=""):
    c = ET.Element("mxCell", id=id_, value=etiqueta, style=estilo, edge="1", parent="1", source=origen, target=destino)
    ET.SubElement(c, "mxGeometry", relative="1", **{"as": "geometry"})
    return c


ALTO_FILA = 15
ANCHO_TABLA = 270


def generar_er(tablas, relaciones):
    d = Drawio()
    celdas = d.pagina("Modelo E-R")
    celdas.append(vertice("titulo", "<b style='font-size:20px'>MG Nutrición y Entrenamiento · Modelo entidad-relación</b><br>"
                          "Generado desde el esquema SQL Server (Flyway V1–V14). 🔑 clave primaria · 🔗 clave foránea · 🔒 cifrado AES-256-GCM · "
                          "ᵁ único · ? admite nulo", 0, -90, 1400, 60, "text;html=1;align=left;verticalAlign=top;"))
    asignadas = {t for _, _, _, ts in MODULOS_TABLAS for t in ts}
    otras = sorted(set(tablas) - asignadas)
    modulos = MODULOS_TABLAS + ([("Otras", "#ffffff", "#999999", otras)] if otras else [])
    columnas_por_fila = 3
    x0, y0 = 0, 0
    alto_fila_modulos = 0
    for i, (nombre, relleno, borde, lista) in enumerate(modulos):
        lista = [t for t in lista if t in tablas]
        # tablas del módulo en dos columnas internas
        cols = [[], []]
        alturas = [40, 40]
        for t in lista:
            h = 30 + ALTO_FILA * len(tablas[t])
            k = 0 if alturas[0] <= alturas[1] else 1
            cols[k].append((t, alturas[k], h))
            alturas[k] += h + 30
        ancho = 2 * ANCHO_TABLA + 60
        alto = max(alturas) + 10
        if i % columnas_por_fila == 0 and i:
            y0 += alto_fila_modulos + 60
            x0 = 0
            alto_fila_modulos = 0
        mod_id = f"mod{i}"
        celdas.append(vertice(mod_id, nombre, x0, y0, ancho, alto,
                              f"swimlane;html=1;startSize=30;fillColor={relleno};strokeColor={borde};fontStyle=1;fontSize=14;rounded=1;arcSize=2;"))
        for k in (0, 1):
            for t, y, h in cols[k]:
                filas = []
                for c in tablas[t]:
                    icono = "🔑 " if c["pk"] else ("🔗 " if c.get("fk") else "")
                    extra = (" 🔒" if (t, c["nombre"]) in CIFRADAS else "") + (" ᵁ" if c["unico"] and not c["pk"] else "") + (" ?" if c["nulo"] else "")
                    nombre_col = f"<u>{c['nombre']}</u>" if c["pk"] else c["nombre"]
                    filas.append(f"{icono}{nombre_col}: <font color='#666666'>{html.escape(c['tipo'])}</font>{extra}")
                etiqueta = f"<div style='text-align:center;font-weight:bold;padding:3px;background:#2E5E3A;color:#fff'>{t}</div>" + "<br>".join(filas)
                celdas.append(vertice(f"t_{t}", etiqueta, 20 + k * (ANCHO_TABLA + 20), y, ANCHO_TABLA, h,
                                      "rounded=0;whiteSpace=wrap;html=1;align=left;verticalAlign=top;spacingLeft=4;spacingTop=0;fontSize=10;"
                                      "fillColor=#ffffff;strokeColor=#2E5E3A;overflow=hidden;", padre=mod_id))
        x0 += ancho + 60
        alto_fila_modulos = max(alto_fila_modulos, alto)
    for n, r in enumerate(relaciones):
        lado_padre = "ERzeroToOne" if r["nulo"] else "ERmandOne"
        lado_hijo = "ERzeroToOne" if r.get("unico") else "ERzeroToMany"
        celdas.append(arista(f"r{n}", f"t_{r['desde']}", f"t_{r['hacia']}",
                             f"edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;startArrow={lado_hijo};endArrow={lado_padre};"
                             "startFill=0;endFill=0;strokeColor=#5B665B;fontSize=9;jumpStyle=arc;", r["columna"]))
    d.guardar(SALIDA / "er.drawio")


# ----------------------------------------------------------------------------------------------- clases (Java)
REFERENCIAS_ID = {
    "pacienteId": "Paciente", "profesionalId": "Profesional", "nutricionistaId": "Profesional", "entrenadorId": "Profesional",
    "usuarioId": "Usuario", "destinatarioUsuarioId": "Usuario", "canceladoPorUsuarioId": "Usuario", "creadoPorUsuarioId": "Usuario",
    "alimentoId": "Alimento", "alimentoReemplazoId": "Alimento", "alimentoEquivalenteId": "Alimento",
    "ejercicioId": "Ejercicio", "ejercicioReemplazoId": "Ejercicio", "ejercicioRutinaId": "EjercicioRutina",
    "comidaPlanId": "ComidaPlan", "itemComidaId": "ItemComida", "planId": "PlanAlimentario", "rutinaId": "Rutina",
    "turnoId": "Turno", "arancelId": "Arancel",
}


def leer_entidades():
    clases = {}
    for archivo in sorted(JAVA.glob("*/domain/*.java")) + [JAVA / "common" / "EntidadAuditada.java"]:
        fuente = archivo.read_text(encoding="utf-8")
        modulo = archivo.parent.parent.name if archivo.parent.name == "domain" else "common"
        m = re.search(r"public (abstract )?(class|enum) (\w+)(?: extends (\w+))?", fuente)
        if not m:
            continue
        nombre = m.group(3)
        if m.group(2) == "enum":
            clases[nombre] = {"modulo": modulo, "tipo": "enum", "valores": valores_enum(fuente, fuente.index("{", m.end()) + 1), "atributos": [], "metodos": [],
                              "enums": {}, "extiende": None, "rel": []}
            continue
        if "@Entity" not in fuente and "@MappedSuperclass" not in fuente:
            continue
        enums = {}
        for e in list(re.finditer(r"public enum (\w+) \{", fuente))[::-1]:
            enums[e.group(1)] = valores_enum(fuente, e.end())
            fuente = fuente[:e.start()] + fuente[fin_bloque(fuente, e.end() - 1):]  # quita el enum anidado (sus campos no son de la entidad)
        enums = dict(reversed(list(enums.items())))
        atributos, rel = [], []
        for anot, tipo, campo in re.findall(r"((?:\s*@[^\n]+\n)*)\s*private ([\w<>, ]+?) (\w+)(?: = [^;]+)?;", fuente):
            if "static" in tipo:
                continue
            cifrado = "CryptoConverter" in anot
            atributos.append(("🔑 " if "@Id" in anot else "") + f"{campo}: {tipo}" + (" 🔒" if cifrado else ""))
            gen = re.search(r"<(\w+)>", tipo)
            if "@ManyToOne" in anot:
                rel.append(("asociacion", tipo, campo, "*", "1"))
            elif "@OneToMany" in anot and gen:
                rel.append(("composicion", gen.group(1), campo, "1", "*"))
            elif "@ManyToMany" in anot and gen:
                rel.append(("asociacion", gen.group(1), campo, "*", "*"))
            elif "@ElementCollection" in anot and gen:
                rel.append(("asociacion", gen.group(1), campo, "1", "*"))
            elif campo in REFERENCIAS_ID:
                rel.append(("referencia", REFERENCIAS_ID[campo], campo, "*", "1"))
        metodos = [f"{n}({a.strip() and '…'}): {t}" for t, n, a in re.findall(r"public (?:static )?([\w<>]+) (\w+)\(([^)]*)\) \{", fuente)
                   if not re.match(r"(get|set|is)[A-Z]", n) and n != nombre]
        clases[nombre] = {"modulo": modulo, "tipo": "abstracta" if "abstract" in (m.group(1) or "") else "entidad", "atributos": atributos,
                          "metodos": metodos, "enums": enums, "extiende": m.group(4), "rel": rel}
    # una relación bidireccional se dibuja una sola vez: se omite el @ManyToOne inverso de una composición
    for nombre, c in clases.items():
        c["rel"] = [r for r in c["rel"] if not (r[0] == "asociacion" and r[3] == "*" and r[4] == "1" and r[1] in clases
                                                and any(x[0] == "composicion" and x[1] == nombre for x in clases[r[1]]["rel"]))]
    return clases


def fin_bloque(texto, apertura):
    """Índice posterior a la llave que cierra la abierta en `apertura`."""
    nivel = 0
    for i in range(apertura, len(texto)):
        nivel += {"{": 1, "}": -1}.get(texto[i], 0)
        if nivel == 0:
            return i + 1
    return len(texto)


def valores_enum(texto, inicio):
    """Constantes de un enum: desde la llave hasta el primer ';' o '}' de nivel cero (ignora argumentos de constructor)."""
    cuerpo, nivel = [], 0
    for ch in texto[inicio:]:
        if nivel == 0 and ch in ";}":
            break
        nivel += {"(": 1, ")": -1}.get(ch, 0)
        cuerpo.append(ch if nivel == 0 and ch != ")" else "")
    return [v.strip() for v in "".join(cuerpo).split(",") if re.fullmatch(r"[A-Z][A-Z0-9_]*", v.strip())]


ORDEN_MODULOS = ["common", "seguridad", "configuracion", "pacientes", "profesionales", "turnos", "pagos", "catalogo", "nutricion",
                 "entrenamiento", "evolucion", "novedades", "notificaciones", "bitacora", "backup"]
TITULOS = {"common": "common", "seguridad": "seguridad", "configuracion": "configuracion", "pacientes": "pacientes", "profesionales": "profesionales",
           "turnos": "turnos", "pagos": "pagos", "catalogo": "catalogo", "nutricion": "nutricion", "entrenamiento": "entrenamiento",
           "evolucion": "evolucion", "novedades": "novedades", "notificaciones": "notificaciones", "bitacora": "bitacora", "backup": "backup"}


def etiqueta_clase(nombre, c):
    estereotipo = "«abstract»<br>" if c["tipo"] == "abstracta" else ("«enumeration»<br>" if c["tipo"] == "enum" else "«entity»<br>")
    cab = f"<div style='text-align:center;padding:2px;background:#2E5E3A;color:#fff'><i>{estereotipo}</i><b>{nombre}</b></div>"
    if c["tipo"] == "enum":
        return cab + "<br>".join(c["valores"])
    partes = [cab, "<br>".join(html.escape(a) for a in c["atributos"]) or "&nbsp;"]
    if c["metodos"]:
        partes.append("<hr>" + "<br>".join("+ " + html.escape(m) for m in c["metodos"]))
    for e, vals in c["enums"].items():
        partes.append(f"<hr><i>«enum» {e}</i>: {html.escape(', '.join(vals))}")
    return "".join(partes)


def alto_clase(c):
    if c["tipo"] == "enum":
        return 40 + 14 * len(c["valores"])
    lineas = lambda texto, por_linea=46: -(-len(texto) // por_linea)  # líneas que ocupa con el ajuste de texto (≈46 car. en 300 px)
    alto = 50 + 14 * sum(lineas(a) for a in c["atributos"])
    if c["metodos"]:
        alto += 18 + 14 * sum(lineas("+ " + m) for m in c["metodos"])
    for e, vals in c["enums"].items():
        alto += 18 + 14 * lineas(f"«enum» {e}: " + ", ".join(vals), 40)
    return alto


def generar_clases(clases):
    d = Drawio()
    celdas = d.pagina("Dominio")
    celdas.append(vertice("titulo", "<b style='font-size:20px'>MG Nutrición y Entrenamiento · Diagrama de clases del dominio</b><br>"
                          "Entidades JPA por módulo (paquete ar.edu.usal.mgnutri.&lt;módulo&gt;.domain). Rombo lleno: composición · "
                          "flecha: asociación JPA · punteada: referencia por id entre módulos · triángulo: herencia · 🔒 atributo cifrado",
                          0, -90, 1500, 60, "text;html=1;align=left;verticalAlign=top;"))
    ancho = 300
    x0, y0, alto_fila, por_fila = 0, 0, 0, 4
    i = 0
    for modulo in ORDEN_MODULOS:
        miembros = [n for n, c in clases.items() if c["modulo"] == modulo]
        if not miembros:
            continue
        cols = [[], []]
        alturas = [40, 40]
        for n in miembros:
            h = alto_clase(clases[n])
            k = 0 if alturas[0] <= alturas[1] else 1
            cols[k].append((n, alturas[k], h))
            alturas[k] += h + 30
        w = 2 * ancho + 60 if len(miembros) > 1 else ancho + 40
        h_mod = max(alturas) + 10
        if i % por_fila == 0 and i:
            y0 += alto_fila + 60
            x0, alto_fila = 0, 0
        mid = f"pkg_{modulo}"
        celdas.append(vertice(mid, f"«paquete» {TITULOS[modulo]}", x0, y0, w, h_mod,
                              "shape=folder;tabWidth=140;tabHeight=26;tabPosition=left;html=1;verticalAlign=top;align=left;spacingLeft=8;"
                              "fontStyle=1;fillColor=#FBF6EA;strokeColor=#7FA36B;container=1;collapsible=0;"))
        for k in (0, 1):
            for n, y, h in cols[k]:
                celdas.append(vertice(f"c_{n}", etiqueta_clase(n, clases[n]), 20 + k * (ancho + 20), y, ancho, h,
                                      "rounded=0;whiteSpace=wrap;html=1;align=left;verticalAlign=top;spacingLeft=4;fontSize=10;fillColor=#ffffff;"
                                      "strokeColor=#2E5E3A;overflow=hidden;", padre=mid))
        x0 += w + 60
        alto_fila = max(alto_fila, h_mod)
        i += 1
    n = 0
    for nombre, c in clases.items():
        if c.get("extiende") in clases:
            celdas.append(arista(f"h{n}", f"c_{nombre}", f"c_{c['extiende']}", "endArrow=block;endFill=0;html=1;strokeColor=#999999;edgeStyle=orthogonalEdgeStyle;"))
            n += 1
        for tipo, destino, campo, mult_origen, mult_destino in c["rel"]:
            if destino not in clases or destino == nombre and tipo == "referencia":
                continue
            if tipo == "composicion":
                estilo = "startArrow=diamondThin;startFill=1;endArrow=open;html=1;strokeColor=#2E5E3A;edgeStyle=orthogonalEdgeStyle;"
            elif tipo == "asociacion":
                estilo = "endArrow=open;html=1;strokeColor=#2E5E3A;edgeStyle=orthogonalEdgeStyle;"
            else:
                estilo = "endArrow=open;dashed=1;html=1;strokeColor=#999999;edgeStyle=orthogonalEdgeStyle;jumpStyle=arc;fontSize=9;"
            celdas.append(arista(f"a{n}", f"c_{nombre}", f"c_{destino}", estilo, f"{campo} {mult_origen}..{mult_destino}"))
            n += 1

    # ---- página 2: capas por módulo
    capas = d.pagina("Capas (MVC)")
    capas.append(vertice("titulo2", "<b style='font-size:20px'>Arquitectura en capas por módulo</b><br>controller → service → dao (Spring Data JPA / Hibernate). "
                         "Los controllers sólo manejan DTOs; las integraciones externas están detrás de puertos (MensajeriaPort, BackupProvider).",
                         0, -90, 1500, 60, "text;html=1;align=left;verticalAlign=top;"))
    x = 0
    y = 0
    for j, modulo in enumerate([m for m in ORDEN_MODULOS if (JAVA / m / "service").exists()]):
        if j and j % 5 == 0:
            y += 520
            x = 0
        clases_capa = {capa: sorted(p.stem for p in (JAVA / modulo / capa).glob("*.java")) for capa in ("controller", "service", "dao")}
        mid = f"m_{modulo}"
        capas.append(vertice(mid, modulo, x, y, 300, 480, "swimlane;html=1;startSize=28;fontStyle=1;fillColor=#E3ECDD;strokeColor=#2E5E3A;"))
        yy = 40
        prev = None
        for capa, color in (("controller", "#dae8fc"), ("service", "#d5e8d4"), ("dao", "#fff2cc")):
            lista = clases_capa[capa] or ["—"]
            h = 30 + 14 * len(lista)
            cid = f"{mid}_{capa}"
            capas.append(vertice(cid, f"<b>{capa}</b><br>" + "<br>".join(lista), 15, yy, 270, h,
                                 f"rounded=1;whiteSpace=wrap;html=1;align=left;verticalAlign=top;spacingLeft=6;fontSize=10;fillColor={color};", padre=mid))
            if prev:
                capas.append(arista(f"{cid}_e", prev, cid, "endArrow=block;html=1;"))
            prev = cid
            yy += h + 25
        x += 330
    d.guardar(SALIDA / "clases.drawio")


# ----------------------------------------------------------------------------------------------- Mermaid
def tipo_mermaid(t):
    return re.sub(r"[^A-Za-z0-9_]", "_", t).strip("_")


def generar_mermaid(tablas, relaciones, clases):
    er = ["erDiagram"]
    for r in relaciones:
        izq = "|o" if r["nulo"] else "||"
        der = "o|" if r.get("unico") else "o{"
        er.append(f'    {r["hacia"]} {izq}--{der} {r["desde"]} : "{r["columna"]}"')
    for t, cols in sorted(tablas.items()):
        er.append(f"    {t} {{")
        for c in cols:
            claves = ",".join(k for k, v in (("PK", c["pk"]), ("FK", c.get("fk")), ("UK", c["unico"] and not c["pk"])) if v)
            er.append(f"        {tipo_mermaid(c['tipo'])} {c['nombre']}{(' ' + claves) if claves else ''}")
        er.append("    }")
    cl = ["classDiagram", "    direction LR"]
    for nombre, c in clases.items():
        if c["tipo"] == "enum":
            cl.append(f"    class {nombre} {{\n        <<enumeration>>\n" + "\n".join(f"        {v}" for v in c["valores"]) + "\n    }")
            continue
        cuerpo = [f"        +{a.replace('🔑 ', '').replace(' 🔒', '').split(': ')[1]} {a.replace('🔑 ', '').split(': ')[0]}".replace("<", "~").replace(">", "~")
                  for a in c["atributos"]]
        cuerpo += [f"        +{m.split('(')[0]}()" for m in c["metodos"]]
        cl.append(f"    class {nombre} {{\n" + "\n".join(cuerpo) + "\n    }")
        if c["tipo"] == "abstracta":
            cl.append(f"    <<abstract>> {nombre}")
    for nombre, c in clases.items():
        if c.get("extiende") in clases:
            cl.append(f"    {c['extiende']} <|-- {nombre}")
        for tipo, destino, campo, mo, md in c["rel"]:
            if destino not in clases:
                continue
            flecha = "*--" if tipo == "composicion" else ("-->" if tipo == "asociacion" else "..>")
            cl.append(f'    {nombre} "{mo}" {flecha} "{md}" {destino} : {campo}')
    texto = f"""# Diagramas

Generados con [`generar_diagramas.py`](generar_diagramas.py) a partir del esquema real de SQL Server y de las entidades JPA,
por lo que se mantienen coherentes con [`../03-modelo-de-datos.md`](../03-modelo-de-datos.md) (CA-00.5.1).

| Archivo | Contenido | Cómo abrirlo |
|---|---|---|
| [`er.drawio`](er.drawio) | Modelo entidad-relación agrupado por módulo ({len(tablas)} tablas, {len(relaciones)} relaciones) | draw.io / diagrams.net |
| [`clases.drawio`](clases.drawio) | Página «Dominio»: {sum(1 for c in clases.values() if c['tipo'] != 'enum')} clases por paquete · Página «Capas (MVC)»: controller → service → dao por módulo | draw.io / diagrams.net |

Para regenerarlos (con la base levantada):

```bash
python -I docs/diagramas/generar_diagramas.py --password "<contraseña de sa>"
```

## Modelo E-R

```mermaid
{chr(10).join(er)}
```

## Clases del dominio

```mermaid
{chr(10).join(cl)}
```
"""
    (SALIDA / "README.md").write_text(texto, encoding="utf-8")


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--servidor", default="localhost,14333")
    p.add_argument("--base", default="mgnutri")
    p.add_argument("--usuario", default="sa")
    p.add_argument("--password", default="MgNutri_Local_2025!")
    args = p.parse_args()
    tablas, relaciones = leer_esquema(args)
    clases = leer_entidades()
    generar_er(tablas, relaciones)
    generar_clases(clases)
    generar_mermaid(tablas, relaciones, clases)
    for f in ("er.drawio", "clases.drawio"):
        ET.parse(SALIDA / f)  # valida XML bien formado
    print(f"{len(tablas)} tablas, {len(relaciones)} FK, {len(clases)} clases/enums -> {SALIDA}")


if __name__ == "__main__":
    main()
