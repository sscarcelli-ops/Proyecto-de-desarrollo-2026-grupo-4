# Proyecto de Desarrollo 2026 - Grupo 4

Plataforma de control de accesos para barrios privados.

| Parte | Tecnologías |
|---|---|
| `backend/` | Java 17, Spring Boot 4, Spring Data JPA, Spring Security, Flyway, PostgreSQL, springdoc-openapi |
| `frontend/` | React 19, TypeScript, Vite, Ant Design 6, React Router 7, TanStack Query, Axios |

## Requisitos

- JDK 17 o superior
- Node.js 20 o superior
- PostgreSQL 15 o superior con una base vacía llamada `accesos_db`

## Backend

```bash
cd backend
cp .env.example .env   # completar DB_PASSWORD
./mvnw spring-boot:run # en Windows: mvnw.cmd spring-boot:run
```

Al arrancar, Flyway aplica los scripts de `src/main/resources/db/migration` y Hibernate valida
las entidades contra las tablas (`ddl-auto: validate`). Prueba rápida: `GET http://localhost:8080/api/ping`.

Contrato de la API (Swagger, solo en `dev`): http://localhost:8080/swagger-ui.html

Perfiles: `dev` (por defecto, muestra el SQL) y `prod` (credenciales de la BD obligatorias).

### Migraciones

- Un cambio de esquema es un script nuevo. Un script ya aplicado **nunca** se edita.
- Nombre con fecha y hora: `V202610201030__crear_tabla_x.sql`.

## Autenticación (decisión del grupo)

- JWT **stateless** firmado (RNF-01). Contraseñas con bcrypt (RF-21).
- El frontend guarda el token en **`localStorage`** (compartido entre pestañas del mismo navegador, no con
  otro navegador ni con incógnito) y lo envía en cada petición con el header `Authorization: Bearer <token>`
  (interceptor de Axios en `shared/lib/httpClient.ts`). Sin cookies.
- El token sobrevive al cierre del navegador: cerrar sesión (RF-25) siempre borra el token. En dispositivos
  compartidos (garita), cada Guardia debe cerrar sesión al terminar su turno.
- Nunca usar `dangerouslySetInnerHTML` con datos del usuario: un XSS podría leer el token.
- Duración: **8 horas** (`JWT_EXPIRATION_SECONDS=28800`), una jornada de trabajo. No hay refresh token:
  al vencer, el backend responde 401 y el usuario vuelve a iniciar sesión.
- El estado del usuario (suspendido, Guardia deshabilitado, barrio inactivo) se valida en la BD en cada
  petición (US-43), así que un token vigente no permite operar a quien fue dado de baja.

## Errores de la API (formato único)

Todos los errores salen por `common/exception/GlobalExceptionHandler` con el mismo formato
(RFC 9457, `application/problem+json`):

```json
{
  "status": 422,
  "code": "BARRIO_SIN_OPERADOR",
  "detail": "No se puede activar un barrio sin Operador.",
  "instance": "/api/barrios/3/activar",
  "timestamp": "2026-10-22T22:30:00Z",
  "errors": [{ "field": "nombre", "message": "no debe estar vacío" }]
}
```

| Caso | Status | `code` |
|---|---|---|
| Bean Validation (`@Valid`) | 400 | `VALIDATION_ERROR` (+ `errors` por campo) |
| JSON mal formado | 400 | `MALFORMED_REQUEST` |
| Sin token / token vencido | 401 | `UNAUTHORIZED` |
| Sin permiso para la acción | 403 | `FORBIDDEN` |
| No existe | 404 | `NOT_FOUND` |
| Regla de negocio | 422 (por defecto) | el que defina la `BusinessException` |
| Archivo demasiado grande | 413 | `FILE_TOO_LARGE` |
| Error no previsto | 500 | `INTERNAL_ERROR` (el detalle queda solo en el log) |

Reglas del equipo:
- **Backend:** los Services lanzan `BusinessException("CODIGO", "Mensaje para el usuario")` o
  `NotFoundException`. Nunca se arma una respuesta de error a mano en un Controller.
- **Frontend:** usar `shared/lib/apiError.ts`: `getErrorMessage(error)` para mostrar el mensaje y
  `form.setFields(toFormFieldErrors(error))` para marcar los campos de un formulario de Ant Design.

## Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev            # http://localhost:5173
```

Estructura de `src/`:

- `app/`: providers globales (TanStack Query, Ant Design), `theme.ts` (paleta y estilos: **único lugar con
  colores**) y `layout/`: `PublicLayout` (login, activación, registro: tarjeta sobre fondo gris) y `AppLayout`
  (pantallas con sesión: menú lateral por rol; cada feature agrega su opción en `layout/menuItems.tsx`)
- `features/<dominio>/`: `api/` (llamadas HTTP + hooks de TanStack Query), `components/`, `pages/`.
  Dominios: `auth`, `barrios`, `operadores`, `guardias`, `perfiles`, `trabajadores`, `garita`, `reportes`
- `shared/`: `lib/` (cliente HTTP, token, errores), `components/` (tabla genérica, confirmaciones, filtros),
  `hooks/` y `types/`
- `routes/`: definición de rutas (React Router), constantes de paths y guards por rol y por estado del perfil
