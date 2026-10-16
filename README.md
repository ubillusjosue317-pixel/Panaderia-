# Sistema web de gestión de pedidos – Panadería Piero Alonso

Proyecto del curso Integrador I: Sistemas Software (UTP) – Grupo 1.

Aplicación web para que los clientes de la panadería revisen el catálogo y hagan pedidos
en línea, y para que el personal gestione productos, pedidos y reportes sin usar el cuaderno.

## Tecnologías

| Capa | Tecnología |
|------|-----------|
| Presentación | HTML5, CSS3, Bootstrap 5, Thymeleaf |
| Controladores y servicios | Java 21 + Spring Boot 3.5 (Spring MVC) |
| Acceso a datos | Spring Data JPA (Hibernate) |
| Base de datos | MySQL 8 |
| Seguridad | Spring Security, contraseñas con BCrypt, control por roles |
| Pruebas | JUnit 5, MockMvc, H2 en memoria |

## Cómo ejecutarlo

Requisitos: JDK 21 o superior y MySQL corriendo en `localhost:3306`.
No hace falta instalar Maven, el proyecto trae el wrapper (`mvnw`).

1. Revisar el usuario y contraseña de MySQL en `src/main/resources/application.properties`
   (por defecto `root` / `root`) o definir las variables de entorno `DB_USER` y `DB_PASSWORD`.
2. Ejecutar:

   ```bash
   ./mvnw spring-boot:run        # Linux / Mac / Git Bash
   mvnw.cmd spring-boot:run      # Windows (CMD o PowerShell)
   ```

3. Abrir <http://localhost:8080>

La base de datos `panaderia_piero_alonso` se crea sola la primera vez. También se cargan los roles,
estados de pedido, categorías, algunos productos de ejemplo y el usuario administrador:

- **Correo:** `admin@pieroalonso.pe`
- **Contraseña:** `Admin12345` (se puede cambiar con la variable `ADMIN_PASSWORD`)

### Probar sin MySQL (perfil local con H2)

Para desarrollar en una máquina sin MySQL se puede usar H2. Crear el archivo
`src/main/resources/application-local.properties` (no se sube al repositorio) con:

```properties
spring.datasource.url=jdbc:h2:file:./data/panaderia;MODE=MySQL;DATABASE_TO_LOWER=TRUE
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver
```

y ejecutar con el perfil `local`:

```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Los datos quedan en la carpeta `data/`; si se borra, la BD se vuelve a crear con los datos iniciales.

Si se prefiere crear la BD a mano, el script está en [database/panaderia_piero_alonso.sql](database/panaderia_piero_alonso.sql).

Para correr las pruebas: `./mvnw test`

## Estructura

```
src/main/java/com/pieroalonso/panaderia
├── config/         Seguridad y carga de datos iniciales
├── controller/     Controladores web (catálogo, login/registro)
│   └── admin/      Controladores del panel de administración
├── dto/            Formularios con sus validaciones
├── model/          Entidades JPA (las 7 tablas del modelo)
├── repository/     Repositorios de Spring Data
├── security/       Carga del usuario para el inicio de sesión
└── service/        Lógica de negocio

src/main/resources
├── templates/      Vistas Thymeleaf
└── static/         CSS e imágenes
```

## Avance (≈ 30 %)

**Listo**

- [x] Configuración del proyecto (Spring Boot, MySQL, Thymeleaf, Bootstrap)
- [x] Modelo de datos completo: ROL, USUARIO, CATEGORIA, PRODUCTO, ESTADO_PEDIDO, PEDIDO, DETALLE_PEDIDO
- [x] Registro de clientes con validaciones e inicio de sesión (RNF01)
- [x] Contraseñas con BCrypt y acceso por roles (cliente / administrador)
- [x] Catálogo público con búsqueda y filtro por categoría (RF01)
- [x] Panel de administración: registrar, editar y ocultar productos (RF04, parcial)
- [x] Pruebas unitarias e integración de lo implementado

**Pendiente**

- [ ] Carrito de compras y confirmación del pedido (RF05)
- [ ] Registro del pedido con recojo en tienda o delivery (RF02, RNF02)
- [ ] Seguimiento y cambio de estado de pedidos (RF03)
- [ ] Historial de pedidos del cliente
- [ ] Gestión de categorías y promociones (RF04)
- [ ] Reportes: ventas, productos más pedidos, historial por cliente
- [ ] Subida de imágenes de productos
- [ ] Despliegue en hosting
