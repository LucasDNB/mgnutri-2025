"""Genera la colección Postman/Newman de pruebas de API (una carpeta por spec, tests por CA).

Uso: python -I postman/generar_coleccion.py
Ejecutar: npx newman run postman/mgnutri.postman_collection.json -e postman/local.postman_environment.json
Requiere una base recién creada (primer ingreso del admin con ADMIN_PASSWORD_INICIAL) o ajustar la variable adminPassword.
"""
import json
import pathlib

BASE = pathlib.Path(__file__).resolve().parent


def req(nombre, metodo, ruta, cuerpo=None, tests=(), auth="admin", pre=None):
    item = {
        "name": nombre,
        "request": {
            "method": metodo,
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "url": "{{baseUrl}}" + ruta,
        },
        "event": [{"listen": "test", "script": {"type": "text/javascript", "exec": list(tests)}}],
    }
    if auth:
        item["request"]["auth"] = {"type": "bearer", "bearer": [{"key": "token", "value": "{{token_" + auth + "}}", "type": "string"}]}
    else:
        item["request"]["auth"] = {"type": "noauth"}
    if cuerpo is not None:
        item["request"]["body"] = {"mode": "raw", "raw": json.dumps(cuerpo, ensure_ascii=False, indent=2)}
    if pre:
        item["event"].append({"listen": "prerequest", "script": {"type": "text/javascript", "exec": pre}})
    return item


def status(code, extra=()):
    return [f"pm.test('HTTP {code}', () => pm.response.to.have.status({code}));", *extra]


def code(c):
    return f"pm.test('code {c}', () => pm.expect(pm.response.json().code).to.eql('{c}'));"


carpetas = [
    {"name": "00-entorno", "item": [
        req("CA-00.1.2 salud", "GET", "/salud", tests=status(200, ["pm.test('bd OK', () => pm.expect(pm.response.json().bd).to.eql('OK'));"]), auth=None),
    ]},
    {"name": "13-seguridad", "item": [
        req("CA-13.1.3 credenciales inválidas", "POST", "/auth/login", {"username": "admin", "password": "incorrecta"},
            status(401, [code("CREDENCIALES_INVALIDAS")]), auth=None),
        req("CA-13.1.2 login admin", "POST", "/auth/login", {"username": "admin", "password": "{{adminPassword}}"},
            status(200, ["pm.collectionVariables.set('token_admin', pm.response.json().token);"]), auth=None),
        req("CA-13.1.6 primer ingreso → cambiar contraseña", "POST", "/auth/cambiar-password",
            {"actual": "{{adminPassword}}", "nueva": "{{adminPasswordNueva}}"},
            ["if (pm.response.code === 200) { pm.collectionVariables.set('token_admin', pm.response.json().token); pm.environment.set('adminPassword', pm.environment.get('adminPasswordNueva')); }",
             "pm.test('200 o 400 si ya se cambió', () => pm.expect([200, 400]).to.include(pm.response.code));"]),
        req("D-25 aceptar términos", "POST", "/terminos/aceptar", {"version": "1.0"}, status(200)),
        req("CA-13.6.1 sin token → 401", "GET", "/pacientes", tests=status(401), auth=None),
        req("me", "GET", "/me", tests=status(200, ["pm.test('rol ADMIN', () => pm.expect(pm.response.json().roles).to.include('ADMIN'));"])),
    ]},
    {"name": "02-profesionales", "item": [
        req("CA-02.1.3 matrícula obligatoria", "POST", "/profesionales",
            {"nombre": "Test", "apellido": "Sin Matricula", "tipoDocumento": "DNI", "nroDocumento": "{{$randomInt}}{{$randomInt}}11", "tipo": "NUTRICIONISTA"},
            status(400)),
        req("CA-02.1.2 alta nutricionista", "POST", "/profesionales",
            {"nombre": "Laura", "apellido": "Postman", "tipoDocumento": "DNI", "nroDocumento": "{{docProf}}", "tipo": "NUTRICIONISTA", "matricula": "MN 99",
             "telefonoWhatsapp": "1134567890"},
            status(201, ["pm.collectionVariables.set('profId', pm.response.json().id);"]),
            pre=["pm.collectionVariables.set('docProf', String(30000000 + Math.floor(Math.random() * 9999999)));"]),
        req("US-02.4 agenda", "PUT", "/profesionales/{{profId}}/disponibilidad",
            {"franjas": [{"diaSemana": d, "horaDesde": "08:00", "horaHasta": "20:00", "ubicacion": "Consultorio 1"} for d in range(1, 8)]}, status(200)),
        req("CA-02.4.1 franjas superpuestas", "PUT", "/profesionales/{{profId}}/disponibilidad",
            {"franjas": [{"diaSemana": 1, "horaDesde": "08:00", "horaHasta": "12:00"}, {"diaSemana": 1, "horaDesde": "11:00", "horaHasta": "13:00"}]}, status(400)),
    ]},
    {"name": "01-pacientes", "item": [
        req("CA-01.1.4 obligatorios", "POST", "/pacientes", {}, status(400, ["pm.test('errors[]', () => pm.expect(pm.response.json().errors).to.be.an('array'));"])),
        req("CA-01.1.2 alta paciente", "POST", "/pacientes",
            {"nombre": "Ana", "apellido": "Postman", "tipoDocumento": "DNI", "nroDocumento": "{{docPac}}", "fechaNacimiento": "1995-05-05", "sexo": "F",
             "telefonoWhatsapp": "11 4567-8901", "asignaciones": [{"profesionalId": "{{profId}}", "area": "NUTRICION"}]},
            status(201, ["pm.collectionVariables.set('pacId', pm.response.json().id);"]),
            pre=["pm.collectionVariables.set('docPac', String(40000000 + Math.floor(Math.random() * 9999999)));"]),
        req("CA-01.1.5 documento duplicado", "POST", "/pacientes",
            {"nombre": "Otra", "apellido": "Persona", "tipoDocumento": "DNI", "nroDocumento": "{{docPac}}", "fechaNacimiento": "1995-05-05", "sexo": "F",
             "telefonoWhatsapp": "1145678901"}, status(409, [code("DUPLICADO")])),
        req("CA-01.2.1 búsqueda sin acentos", "GET", "/pacientes?q=postman", tests=status(200, ["pm.test('encuentra', () => pm.expect(pm.response.json().totalElements).to.be.above(0));"])),
        req("CA-01.2.5 concurrencia", "PUT", "/pacientes/{{pacId}}",
            {"nombre": "Ana", "apellido": "Postman", "tipoDocumento": "DNI", "nroDocumento": "{{docPac}}", "fechaNacimiento": "1995-05-05", "sexo": "F",
             "telefonoWhatsapp": "1145678901", "version": 99}, status(409, [code("CONCURRENCIA")])),
    ]},
    {"name": "03-turnos", "item": [
        req("CA-03.1.2 huecos", "GET", "/profesionales/{{profId}}/huecos", tests=status(200, [
            "pm.collectionVariables.set('hueco', pm.response.json()[0].inicio);"])),
        req("RF-03 turno otorgado por el admin", "POST", "/turnos", {"pacienteId": "{{pacId}}", "profesionalId": "{{profId}}", "inicio": "{{hueco}}"},
            status(201, ["pm.collectionVariables.set('turnoId', pm.response.json().id);"])),
        req("CA-03.1.5 horario tomado", "POST", "/turnos", {"pacienteId": "{{pacId}}", "profesionalId": "{{profId}}", "inicio": "{{hueco}}"},
            ["pm.test('409 horario no disponible', () => pm.expect(pm.response.code).to.eql(409));"]),
        req("CA-03.4.2 cancelar sin motivo", "POST", "/turnos/{{turnoId}}/cancelar", {}, status(400)),
    ]},
    {"name": "07-pagos", "item": [
        req("CA-07.4.2 registrar deuda", "POST", "/pacientes/{{pacId}}/cargos", {"concepto": "Cuota", "periodo": "2026-01", "monto": 25000}, status(201)),
        req("CA-07.4.3 deuda duplicada", "POST", "/pacientes/{{pacId}}/cargos", {"concepto": "Cuota", "periodo": "2026-01", "monto": 25000},
            status(409, [code("DEUDA_DUPLICADA")])),
        req("CA-07.2.2 registrar pago", "POST", "/pacientes/{{pacId}}/pagos", {"monto": 10000, "medio": "EFECTIVO"},
            status(201, ["pm.test('comprobante', () => pm.expect(pm.response.json().nroComprobante).to.be.above(0));"])),
        req("US-07.1 saldo", "GET", "/pacientes/{{pacId}}/cuenta", tests=status(200, ["pm.test('saldo 15000', () => pm.expect(pm.response.json().saldo).to.eql(15000));"])),
    ]},
    {"name": "12-reportes", "item": [
        req(f"R {t}", "GET", f"/reportes/{t}", tests=status(200)) for t in ["turnos", "pagos", "rutinas", "planes"]
    ] + [req("R turnos xlsx", "GET", "/reportes/turnos?formato=xlsx", tests=status(200, [
        "pm.test('excel', () => pm.expect(pm.response.headers.get('Content-Type')).to.include('spreadsheetml'));"]))]},
    {"name": "14-bitacora", "item": [
        req("CA-14.2.2 consulta", "GET", "/bitacora?size=5", tests=status(200, ["pm.test('hay eventos', () => pm.expect(pm.response.json().totalElements).to.be.above(0));"])),
        req("CA-13.7.3 accesos", "GET", "/accesos?size=5", tests=status(200)),
    ]},
    {"name": "13-cierre", "item": [
        req("CA-13.4.1 logout", "POST", "/auth/logout", tests=status(204)),
        req("CA-13.4.3 token revocado", "GET", "/me", tests=status(401)),
    ]},
]

coleccion = {
    "info": {"name": "MG Nutrición y Entrenamiento · API", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
             "description": "Pruebas de contrato por spec (docs/04-convenciones-api.md)."},
    "item": carpetas,
    "variable": [{"key": k, "value": ""} for k in ["token_admin", "profId", "pacId", "docProf", "docPac", "hueco", "turnoId"]],
}
(BASE / "mgnutri.postman_collection.json").write_text(json.dumps(coleccion, ensure_ascii=False, indent=2), encoding="utf-8")
(BASE / "local.postman_environment.json").write_text(json.dumps({
    "name": "local", "values": [
        {"key": "baseUrl", "value": "http://localhost:8090/api/v1", "enabled": True},
        {"key": "adminPassword", "value": "Admin2025!", "enabled": True},
        {"key": "adminPasswordNueva", "value": "AdminNueva2025", "enabled": True}]}, ensure_ascii=False, indent=2), encoding="utf-8")
print("colección generada:", sum(len(c["item"]) for c in carpetas), "requests")
