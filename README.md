# Laboratorio 7 – Portal de gestión comercial

Aplicación web interna para una distribuidora: catálogo de productos, clientes y emisión de pedidos de venta
(comprobante simplificado). Tecnologías: **Java 17, Spring Boot 3.3, Spring MVC, Thymeleaf, Spring Data JPA, MySQL 8**.

## Estructura funcional

- **Inicio** (`/`): título "Portal de gestión comercial", descripción y enlaces a las tres secciones.
- **Clientes** (`/customers`): CRUD sobre la tabla `customer` (nombre, tipo y número de documento).
- **Catálogo** (`/products`): CRUD sobre la tabla `product` (nombre, precio, stock).
- **Pedidos** (`/orders`): listado, detalle y registro de pedidos sobre `invoice` e `invoice_detail`.
- El menú es un único fragmento Thymeleaf (`templates/fragments/menu.html`) incluido en todas las vistas.

## Control de versiones (según el enunciado)

- `main`: permanece vacía (solo el commit inicial, sin archivos).
- `develop` → `feature/customer` → `feature/product`.
- Commits, en orden:
  1. `add structure and navigation to the customer flow` (rama `feature/customer`)
  2. `add customer management` (rama `feature/customer`)
  3. `add product catalog` (rama `feature/product`, creada desde `feature/customer`)
  4. `add order log` (rama `feature/product`)
  5. Merge de `feature/product` en `develop` (Pregunta 5).

## Base de datos

El script `sql/lab6.sql` crea la base `lab6` y sus 4 tablas (`customer`, `product`, `invoice`, `invoice_detail`),
sin cambiar nombres de tablas ni columnas. `sql/datos-ejemplo.sql` es opcional y carga datos de demostración.

La aplicación **no** crea ni modifica tablas (`spring.jpa.hibernate.ddl-auto=none`).

## Variables de entorno

- `DB_URL` (por defecto `jdbc:mysql://localhost:3306/lab6?...`)
- `DB_USER` (por defecto `root`)
- `DB_PASSWORD` (por defecto `root`)
- `PORT` (por defecto `8080`)

## Ejecutar en local

1. Crear la base: `mysql -u root -p < sql/lab6.sql` (y opcionalmente `sql/datos-ejemplo.sql`).
2. Ajustar las variables de entorno si tu usuario/clave de MySQL son distintos.
3. `mvn spring-boot:run` (o ejecutar `Laboratorio7Application` desde el IDE).
4. Abrir <http://localhost:8080>.

## Desplegar

### Opción A – Docker Compose (MySQL + aplicación)

```bash
docker compose up --build
```

Abre <http://localhost:8080>. La base `lab6` se crea automáticamente con `sql/lab6.sql`.

### Opción B – JAR en un servidor / VM en la nube

```bash
mvn clean package
export DB_URL="jdbc:mysql://HOST:3306/lab6?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima"
export DB_USER=usuario
export DB_PASSWORD=clave
java -jar target/laboratorio7.jar
```

### Opción C – Plataformas con Dockerfile (Render, Railway, etc.)

Usar el `Dockerfile` de la raíz, definir `DB_URL`, `DB_USER`, `DB_PASSWORD` apuntando a una MySQL gestionada
(con el script `sql/lab6.sql` ya ejecutado). La plataforma inyecta `PORT`. Health check: `/actuator/health`.

## Pruebas

`mvn test` ejecuta pruebas de integración con MockMvc sobre H2 en modo MySQL (perfil `test`, esquema idéntico a `lab6`).
