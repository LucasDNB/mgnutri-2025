# Constitución del proyecto · MG Nutrición y Entrenamiento

Principios no negociables que todo spec, plan, tarea y línea de código debe respetar. Derivan de la ERS v1.0.0 (+ cambios v1.0.1 del SDP), el SDP IEEE 1058 rev. 1 y el Sprint Planning v1.0.0.

## Artículo I · Trazabilidad ante todo
1. Toda funcionalidad nace de un RF y uno o más CU de la ERS. Si no hay RF/CU, no se implementa (o se registra primero como decisión en `docs/01-decisiones.md`).
2. Cada spec declara los RF, CU y paquete de trabajo que cubre. Cada commit referencia el spec (`[spec-04] CU-11 …`).
3. Lo que el PdS menciona y la ERS no baja (listas de compra, recetario, hidratación) **no se implementa**.

## Artículo II · El sistema no reemplaza al profesional
1. No realiza diagnósticos ni asesoramiento automatizado (PdS).
2. Todo lo "automático" (plan generado, equivalencias, alternativas) es una **propuesta** que un profesional confirma o que el paciente elige dentro de límites definidos por el profesional.

## Artículo III · Stack fijo
| Capa | Tecnología obligatoria |
|---|---|
| Backend | Java SE 17, Spring Boot 3.x, Maven |
| Persistencia | Hibernate (JPA) con patrón DAO, SQL Server / Azure SQL Database, Flyway |
| API | REST JSON propia, versión `/api/v1` |
| Frontend | HTML5 + CSS3 + JavaScript (módulos ES), desacoplado, sin framework; Chart.js como única librería |
| Infra | Azure App Service (backend), Azure Static Web Apps (frontend), Azure SQL, Azure Blob Storage (backups) |
| Integraciones | WhatsApp Cloud API (Meta) |
| Herramientas | Git + GitHub, Postman/Newman, IntelliJ IDEA, VS Code, Draw.io |

Agregar una dependencia requiere que sea gratuita y con licencia compatible con uso académico (SDP §3.1).

## Artículo IV · Arquitectura en capas
1. `controller` → `service` → `dao`. Un controller nunca toca un DAO; un DAO nunca contiene reglas de negocio.
2. Los controllers sólo reciben/devuelven **DTOs**; las entidades JPA no salen de la capa de servicio.
3. Paquetes por módulo de dominio (`pacientes`, `profesionales`, `turnos`, `nutricion`, `entrenamiento`, `evolucion`, `pagos`, `catalogo`, `notificaciones`, `reportes`, `seguridad`, `bitacora`, `backup`), cada uno con sus subpaquetes `controller/service/dao/domain/dto`.
4. Integraciones externas (WhatsApp, Azure Backup) detrás de interfaces (puertos) con adaptadores intercambiables; nunca se las llama desde un controller.

## Artículo V · Seguridad y datos personales (Ley 25.326)
1. Todo endpoint, salvo login, recupero de contraseña y contacto público, requiere autenticación.
2. Autorización por rol **y** por pertenencia: un profesional sólo accede a pacientes asignados; un paciente sólo a sus propios datos. Se valida en el servicio, no sólo en la UI.
3. Contraseñas sólo hasheadas (BCrypt). Datos sensibles cifrados en columna. Nunca se loguean contraseñas, tokens ni datos de salud.
4. Sin aceptación de términos y condiciones no hay acceso.

## Artículo VI · Auditoría obligatoria
1. Toda alta, baja, modificación, generación de reporte, cambio de contraseña o de configuración registra un evento en bitácora (RF-17 / CU-35).
2. Todo login, logout e intento fallido se registra en el registro de accesos (RF-16).
3. Ambos registros son de sólo lectura, inmodificables e ineliminables, con retención mínima de 6 meses.

## Artículo VII · Calidad verificable
1. Cada criterio de aceptación de un spec tiene al menos un test automatizado (unitario o de integración) o un caso en la colección Postman.
2. Cobertura mínima de la capa `service`: 70 % de líneas.
3. Validación duplicada: el frontend valida para usabilidad, el backend valida siempre (Bean Validation + reglas de servicio).
4. Errores con formato único (Problem Details RFC 7807) y mensajes en español aptos para mostrar al usuario.
5. Objetivos de rendimiento (ERS 3.4): p90 < 5 s en servidor para operaciones frecuentes, 50 usuarios concurrentes, 500 pacientes activos con historial.

## Artículo VIII · Usabilidad
1. Mobile-first y responsive (escritorio, tablet, celular).
2. Cada rol ve un home con sus accesos más usados (mockups de la ERS 3.9.1).
3. Ayuda contextual en campos y funciones (ERS 3.8).
4. Un usuario nuevo comprende las funciones básicas en ≤ 30 minutos.
5. Textos de UI en español rioplatense neutro, sin jerga técnica.

## Artículo IX · Operación
1. Esquema versionado con Flyway; ninguna migración destruye datos existentes.
2. Configuración por variables de entorno / App Settings; ningún secreto en el repositorio.
3. Despliegue continuo desde GitHub (rama `main`) a Azure.
4. Backups: automáticos de Azure SQL + manual/programado desde el sistema; RTO ≤ 2 h.

## Gobierno
Esta constitución prevalece sobre cualquier spec o plan. Modificarla requiere registrar la decisión en `docs/01-decisiones.md` con su justificación.

**Versión**: 1.0.0 · **Ratificada**: 2026-11-15
