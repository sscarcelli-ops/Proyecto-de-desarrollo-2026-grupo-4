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
- Solo para usar la app sin desarrollar: alcanza con Docker (ver "Ambientes")

## Ramas (gitflow)

- `main`: solo recibe merges desde `dev` en los hitos (demo interna, entregas). Nadie commitea directo.
- `dev`: integración. Cada US sale de acá y vuelve acá por pull request revisado por otro integrante.
- `feature/US-xx-descripcion`: una rama por 1 o 2 US, menos de una semana de vida. Traer `dev` a diario.

## Ambientes (Docker)

Un archivo de variables por ambiente, todos con la misma imagen (T-12). Para todo en Docker la máquina **solo
necesita Docker**: el backend y el frontend se compilan adentro de Docker, no hace falta Java ni Node.

| Ambiente | Para qué | Comando |
|---|---|---|
| **Prueba, con datos** | Ejecutar los casos de prueba | `docker compose --env-file docker/prueba.env up -d --build` |
| **Prueba, vacía** | Camino feliz desde cero: activar el primer Administrador (US-01) | `docker compose --env-file docker/prueba-vacia.env up -d --build` |
| **Producción** | Instalación real | `docker compose --env-file docker/produccion.env up -d --build` |
| **Desarrollo** | Programar: solo la base; backend y frontend desde el IDE | `docker compose --env-file backend/.env up -d postgres` |

Qué tiene cada uno:

| | Desarrollo | Prueba, con datos | Prueba, vacía | Producción |
|---|---|---|---|---|
| Archivo | `backend/.env` | `docker/prueba.env` | `docker/prueba-vacia.env` | `docker/produccion.env` |
| Dónde corre | Base en Docker; backend y front en el IDE | Todo en Docker | Todo en Docker | Todo en Docker |
| Base | `accesos_db` o `accesos_vacia`, a elección | `accesos_db` | `accesos_vacia` | `accesos_db` |
| Datos de prueba (seed) | Opcional (`dev,seed`) | ✅ | — | — |
| Primer Administrador | El del seed, o se activa | El del seed | Se activa con `GRUPO4-ACTIVAR-2026` | Se activa con un código propio |
| Credenciales (base, JWT) | Las de ejemplo | Las de ejemplo | Las de ejemplo | Propias: **no arranca con las de ejemplo** |
| Swagger | ✅ | ✅ | ✅ | — |
| SQL en el log | ✅ | — | — | — |
| Log en archivo | Opcional (`LOG_FILE`) | Volumen `logs` | Volumen `logs` | Volumen `logs` |

- **Sin `--env-file`**, `docker compose up -d --build` levanta Prueba con datos.
- La app queda en http://localhost:5173, la API en http://localhost:8080 y Swagger en
  http://localhost:8080/swagger-ui.html. La activación está en http://localhost:5173/activar.
- **Pasar de una prueba a la otra:** ejecutar el otro comando. Usan bases distintas en el mismo contenedor y cada
  una conserva sus datos.
- **Repetir el camino feliz desde cero:** `docker compose down -v` y volver a levantar. Borra las dos bases; la del
  seed se vuelve a cargar sola.
- **Producción:** copiar `docker/produccion.env.example` como `docker/produccion.env` (no se versiona) y reemplazar
  los valores `CAMBIAR-...`. Va en su propia máquina; si ahí se usó un ambiente de prueba, antes `docker compose
  down -v` (la base conserva la contraseña con la que se creó).
- **Este compose es para instalaciones de prueba o locales: no exponerlo a internet.** No tiene HTTPS y publica los
  puertos de la base y del backend.
- Otros comandos: `docker compose ps` (estado), `docker compose logs -f backend` (logs en vivo), `docker compose
  down` (apagar), `docker compose rm -sf backend frontend` (apagar la app y dejar la base para el IDE).
- Los archivos subidos quedan en el volumen `archivos` y los logs en el volumen `logs` (`/data/logs/accesos.log`).
- **Cómo llega el frontend al backend:** el frontend no tiene la dirección del backend. Llama a `/api` en su misma
  dirección y nginx reenvía esos pedidos al contenedor del backend (`BACKEND_URL` en `docker-compose.yml`). En
  desarrollo hace lo mismo Vite (`frontend/vite.config.ts`). Así la misma compilación sirve en cualquier máquina.
- La primera vez tarda varios minutos (descarga las imágenes y las dependencias); después, `--build` solo
  recompila lo que cambió.

### Desarrollo

```bash
cp backend/.env.example backend/.env                   # una sola vez; DB_PASSWORD puede quedar como está
docker compose --env-file backend/.env up -d postgres  # PostgreSQL 17 en el puerto DB_PORT (5432 por defecto)
```

- Si ya tenés PostgreSQL instalado usando el 5432, poné `DB_PORT=5433` en `backend/.env`.
- **Ojo:** `docker compose up -d` sin `postgres` levanta todo (Prueba, con datos) y ocupa los puertos 8080 y 5173
  del backend y el frontend del IDE.
- Para levantar un ambiente de prueba en tu máquina con tu puerto y tu contraseña, agregar tu `.env` antes:
  `docker compose --env-file backend/.env --env-file docker/prueba.env up -d --build`. Apagar antes el backend y el
  frontend del IDE.
- Base vacía en desarrollo: `DB_NAME=accesos_vacia` en `backend/.env`. La activación del Administrador (US-01)
  carga al arrancar el código de `ACTIVACION_TOKEN` (`GRUPO4-ACTIVAR-2026`), que vence a los
  `ACTIVACION_VIGENCIA_DIAS` y sirve una sola vez. Para probarlo de nuevo, usar otro valor y reiniciar (o borrar los
  datos de `accesos_vacia`).

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
| `operador.robles@seed.test` | Operador de *Barrio Los Robles* (Activo, segundo barrio) | Aislamiento entre barrios (US-42) |
| `guardia.robles@seed.test` | Guardia de *Los Robles* | Garita de otro barrio: no ve lo de *Los Álamos* (US-42). Sofía Sur (DNI 31222333) entra habilitada |
| `empresa.sur@seed.test` | Empresa aprobada (*Jardines del Sur S.R.L.*), habilitada solo en *Los Robles*, con un trabajador | Aislamiento entre Empresas (US-41): no ve los trabajadores de *Limpiezas del Norte* |

Trabajadores de la Empresa (DNI → caso): `30111222` vigente y **con ingreso abierto** (mismo DNI que el
Monotributista: dos vínculos), `30777888` vigente listo para ingresar (sin foto), `30333444` ART vencida,
`30555666` pendiente de carga, `30999000` dado de baja.

Para agregar datos: un archivo nuevo y fechado por US (`V<fecha>__seed_<tema>.sql`), sin `id` escritos a mano
(son `GENERATED ALWAYS`): las relaciones se arman buscando por email, CUIT o DNI. Ver el seed base como ejemplo.

### 📋 Datos de Prueba de Garita (Seed Oficial - Barrio Los Álamos)

**Usuario Guardia de prueba:**
- **Email:** `guardia@seed.test`
- **Contraseña:** `Password1!`
- **Barrio:** Barrio Los Álamos (ID: 1)

| DNI | Nombre y Apellido | Vínculo / Empresa | Documentación | Estado Inicial | Caso de prueba que cubre |
|---|---|---|---|---|---|
| `30444555` | **Luis Habilitado** | Limpiezas del Norte S.A. | ART y Seguro vigentes (nov 2027) | Habilitado (Afuera) | **Ingreso exitoso (US-39 Caso 1)** y concurrencia. Pantalla verde `ACCESO HABILITADO`. |
| `30111222` | **Juan Pérez** | 1. Limpiezas del Norte S.A.<br>2. Juan Pérez (Monotributista) | ART y Seguro vigentes | Con ingreso activo (Adentro) | **Egreso exitoso (US-40 Caso 1)** y bloqueo por doble ingreso activo (US-39 Caso 3). Pantalla amarilla directa `REGISTRAR EGRESO`. |
| `30333444` | **Carlos Vencido** | Limpiezas del Norte S.A. | ART vencida (expiró hace 10 días) | Inhabilitado | **Rechazo por documentación vencida (US-39 Caso 2)**. Pantalla roja: *"documentación vencida (ART)"*. |
| `30777888` | **Ana Vigente** | Limpiezas del Norte S.A. | ART y Seguro vigentes | Habilitada (Sin foto) | **Múltiples ingresos en el día (US-39 Caso 4)** y aviso de **DNI físico por falta de foto (US-39 Caso 6)**. |
| `30555666` | **Marta Pendiente** | Limpiezas del Norte S.A. | Sin documentación | Inhabilitada | **Rechazo por falta de papeles (US-38)**. Pantalla roja: *"documentación pendiente de carga"*. |
| `30999000` | **Bruno Baja** | Limpiezas del Norte S.A. | Documentos vigentes | Dado de baja en vínculo | **Rechazo por trabajador dado de baja (US-38)**. Pantalla roja: *"baja en ese vínculo"*. |
| `32555661` | **Sergio Suspendido** | Sergio Suspendido (Monotributista) | Documentos vigentes | Perfil Suspendido | **Rechazo por empleador suspendido (US-38)**. Pantalla roja: *"Monotributista suspendido"*. |
| `31222333` | **Sofía Sur** | Jardines del Sur S.R.L. | Documentos vigentes | Pertenece a Los Robles | **Rechazo por aislamiento de barrios (US-38 / US-42)**. Pantalla roja: *"no habilitada en el barrio"*. |

## Autenticación (decisión del grupo)

- JWT **stateless** firmado (RNF-01). Contraseñas con bcrypt (RF-21).
- El frontend guarda el token en **`localStorage`** (compartido entre pestañas del mismo navegador, no con
  otro navegador ni con incógnito) y lo envía en cada petición con el header `Authorization: Bearer <token>`
  (interceptor de Axios en `shared/lib/httpClient.ts`). Sin cookies.
- El token sobrevive al cierre del navegador: cerrar sesión (RF-25) siempre borra el token. En dispositivos
  compartidos (garita), cada Guardia debe cerrar sesión al terminar su turno.
- Nunca usar `dangerouslySetInnerHTML` con datos del usuario: un XSS podría leer el token.
- **Política de contraseñas (US-44):** todo DTO que **define** una contraseña (registro, alta de Operador y Guardia,
  cambio, recupero, activación) la marca con `@PasswordSegura` (`common/security`): mínimo 8 caracteres, una
  mayúscula, una minúscula y un número, y como máximo 72 bytes (límite de bcrypt). El login no la usa.
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

## Logs (T-10)

Sirven para saber qué pasó en una instalación sin tener que reproducir el problema. Quedan activos también en `prod`;
lo que cambia es el nivel de detalle, con variables de `backend/.env` (`LOG_LEVEL`, `LOG_LEVEL_APP`, `LOG_LEVEL_SQL`,
`LOG_FILE`; ver `.env.example`).

```
WARN [6ecc564d u:5] ... GlobalExceptionHandler : Acceso denegado por permisos
WARN [6ecc564d u:5] ... RegistroPeticionesFilter : GET /api/barrios -> 403 (15 ms)
```

- Cada línea lleva `[número de pedido u:id de usuario]`: así se siguen todas las líneas de un mismo pedido y se sabe
  quién lo hizo. Cada pedido a la API termina con una línea de método, ruta (sin parámetros), estado y duración; los
  401 y 403 van como WARN.
- Ya se registran: login rechazado con el motivo, sesión rechazada con el motivo (token vencido o alterado, sesión
  cerrada, cuenta sin acceso), acceso denegado por permisos, login, logout, cambio de contraseña, aprobación y rechazo
  de perfiles, y los errores inesperados (500) con la causa completa.
- **Nunca se registran** contraseñas, tokens, códigos de activación ni datos personales (email, DNI, nombre): para
  identificar a alguien, su id. Tampoco el contenido de los pedidos.
- Niveles: `ERROR` solo para errores inesperados, `WARN` para rechazos de seguridad, `INFO` para acciones que cambian
  datos importantes (aprobar, dar de baja, activar...). Usar `private static final Logger log` (o `@Slf4j`) y
  parámetros con `{}`, nunca concatenando texto.

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
npm install
npm run dev            # http://localhost:5173
```

No hace falta `frontend/.env`: el frontend llama a `/api` y Vite lo reenvía a `http://localhost:8080`. Si el backend
corre en otra dirección o puerto, copiar `.env.example` a `.env` y cambiar `BACKEND_URL` (ver el archivo).

```bash
# o, sin crear el archivo, solo para esta vez:
BACKEND_URL=http://localhost:8081 npm run dev
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
