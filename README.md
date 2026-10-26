# Proyecto de Desarrollo 2026 - Grupo 4

Plataforma de control de accesos para barrios privados.

| Parte | Tecnologías |
|---|---|
| `backend/` | Java 17, Spring Boot 4, Spring Data JPA, Spring Security, Flyway, PostgreSQL, springdoc-openapi |
| `frontend/` | React 19, TypeScript, Vite, Ant Design 6, React Router 7, TanStack Query, Axios |

## Requisitos

- JDK 17 o superior
- Node.js 20 o superior
- Docker (para PostgreSQL). Alternativa: PostgreSQL 15+ instalado, con una base vacía `accesos_db`

## Ramas (gitflow)

- `main`: solo recibe merges desde `dev` en los hitos (demo interna, entregas). Nadie commitea directo.
- `dev`: integración. Cada US sale de acá y vuelve acá por pull request revisado por otro integrante.
- `feature/US-xx-descripcion`: una rama por 1 o 2 US, menos de una semana de vida. Traer `dev` a diario.

## Base de datos (Docker)

```bash
cp backend/.env.example backend/.env           # una sola vez; DB_PASSWORD puede quedar como está
docker compose --env-file backend/.env up -d   # PostgreSQL 17 en el puerto DB_PORT (5432 por defecto)
```

- Si ya tenés PostgreSQL instalado usando el 5432, poné `DB_PORT=5433` en `backend/.env`.
- El contenedor crea dos bases: `accesos_db` (la de trabajo) y `accesos_vacia` (siempre sin datos de
  prueba, para probar la activación del Administrador, US-01: `DB_NAME=accesos_vacia`).
- `docker compose down -v` borra los datos; al volver a levantar, las bases se crean vacías.

## Backend

```bash
cd backend
./mvnw spring-boot:run # en Windows: mvnw.cmd spring-boot:run
```

Al arrancar, Flyway aplica los scripts de `src/main/resources/db/migration` y Hibernate valida
las entidades contra las tablas (`ddl-auto: validate`). Prueba rápida: `GET http://localhost:8080/api/ping`.

Contrato de la API (Swagger, solo en `dev`): http://localhost:8080/swagger-ui.html

Perfiles (`SPRING_PROFILES_ACTIVE` en `backend/.env`):

| Perfil | Para qué |
|---|---|
| `dev` | Por defecto. Muestra el SQL y tolera migraciones mergeadas fuera de orden |
| `dev,seed` | `dev` + datos de prueba del camino feliz (`db/seed`) |
| `prod` | Credenciales de la BD obligatorias, sin Swagger |

### Migraciones

- Un cambio de esquema es un script nuevo en `db/migration`. Un script ya aplicado **nunca** se edita.
- Nombre con fecha y hora: `V202610201030__crear_tabla_x.sql`.
- Las bases con seed ya tienen filas: una columna nueva `NOT NULL` necesita un `DEFAULT` o completar los datos
  en el mismo script.

### Datos de prueba (seed)

Con `SPRING_PROFILES_ACTIVE=dev,seed`, Flyway también aplica `src/main/resources/db/seed`. Sin ese perfil la
base queda sin usuarios. Todos los usuarios tienen la contraseña **`Prueba123`**.

| Usuario | Rol / estado | Qué permite probar |
|---|---|---|
| `admin@seed.test` | Administrador | Gestión de barrios, Operadores y solicitudes |
| `operador@seed.test` | Operador de *Barrio Los Álamos* (Activo) | Guardias, habilitaciones, historial |
| `operador.libre@seed.test` | Operador sin barrio ("Disponible") | Asignar Operador a *Barrio San Jorge* (Inactivo). No puede iniciar sesión hasta tener barrio: "No tenés un barrio asignado…" (RN-15) |
| `guardia@seed.test` | Guardia de *Barrio Los Álamos* | Garita |
| `empresa@seed.test` | Empresa aprobada y habilitada en *Los Álamos* | Trabajadores, documentación, historial |
| `mono@seed.test` | Monotributista aprobado, **no** habilitado en *Los Álamos* | Motivo "no habilitado en el barrio" |
| `mono.pendiente@seed.test` | Solicitud pendiente | Aprobar / ver estado de la solicitud |
| `mono.rechazado@seed.test` | Solicitud rechazada con motivo | Ver motivo / corregir y reenviar |
| `mono.suspendido@seed.test` | Monotributista suspendido | Login rechazado: "Tu cuenta se encuentra suspendida" (US-04) |
| `guardia.deshabilitado@seed.test` | Guardia deshabilitado de *Los Álamos* | Login rechazado: "Tu usuario se encuentra deshabilitado" (US-04) |
| `guardia.inactivo@seed.test` | Guardia de *San Jorge* (Inactivo) | Login rechazado: "Tu barrio se encuentra inactivo" (US-04) |
| `operador.inactivo@seed.test` | Operador de *Barrio Las Lomas* (Inactivo) | Login rechazado: "Tu barrio se encuentra inactivo" (RN-15) |

Trabajadores de la Empresa (DNI → caso): `30111222` vigente y **con ingreso abierto** (mismo DNI que el
Monotributista: dos vínculos), `30777888` vigente listo para ingresar (sin foto), `30333444` ART vencida,
`30555666` pendiente de carga, `30999000` dado de baja.

Para agregar datos: un archivo nuevo y fechado por US (`V<fecha>__seed_<tema>.sql`), sin `id` escritos a mano
(son `GENERATED ALWAYS`): las relaciones se arman buscando por email, CUIT o DNI. Ver el seed base como ejemplo.

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
- **Listados paginados (RNF-08):** recibir `Pageable` y devolver `PageResponse<T>` (`common/paging`);
  en el frontend, el tipo `PageResponse<T>` de `shared/types/page.ts`. `page` empieza en 0.
- **Frontend:** usar `shared/lib/apiError.ts`: `getErrorMessage(error)` para mostrar el mensaje y
  `form.setFields(toFormFieldErrors(error))` para marcar los campos de un formulario de Ant Design.

## Tipos de la API (generados desde Swagger)

Los tipos de TypeScript de los DTO del backend **no se escriben a mano**: se generan desde Swagger.

```bash
# con el backend corriendo en el 8080
cd frontend
npm run api:types      # regenera src/shared/api/schema.d.ts
```

- Correrlo cada vez que se crea o cambia un DTO en el backend, y **commitear `schema.d.ts`** junto con ese
  cambio (así el build no necesita el backend levantado).
- Uso: `import type { ApiSchema } from 'shared/api/types'` y `ApiSchema<'BarrioResponse'>`.
- Si alguien cambia un DTO, al regenerar los tipos el frontend marca en rojo cada lugar que quedó desactualizado.
- Los `XxxResponse` tienen todos sus campos obligatorios (`OpenApiConfig`). Si un campo puede venir `null`,
  declararlo en el record con `@Schema(types = {"string", "null"})`. En los `XxxRequest`, lo obligatorio sale de
  Bean Validation (`@NotNull`, `@NotBlank`...).

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
