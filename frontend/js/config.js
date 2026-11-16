// Configuración del frontend por entorno.
// Local: el backend sirve el frontend en el mismo origen → API_BASE vacío.
// Azure Static Web Apps: el pipeline reemplaza este archivo con la URL del App Service.
export const API_BASE = (globalThis.MGNUTRI_API_BASE ?? '') + '/api/v1';
