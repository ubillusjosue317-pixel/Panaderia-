# Panadería Piero Alonso - Sistema web de gestión de pedidos

**Curso:** Curso Integrador I: Sistemas Software
**Universidad:** Universidad Tecnológica del Perú (UTP) - Sede Los Olivos
**Docente:** Toledo Bravo Marisabel
**Grupo:** Mesa N° 1

## Integrantes

- Ipanaque Chang Micaela Luciana
- Lara León Kiara Ximena
- Murillo Quispe Emily Ariana
- Ostoz Diaz David German
- Principe Quintana Bionda Tatiana
- Ubillus Torres Josue Elias (jefe de proyecto)

## Descripción

La panadería registra sus pedidos en un cuaderno, lo que genera pérdida de información,
pedidos duplicados y demoras. Este sistema permite que los clientes vean el catálogo
(precios y promociones) y hagan pedidos desde el celular o la PC, y que el personal
gestione los productos, el estado de los pedidos y los reportes.

## Tecnologías

- Java 21 + Spring Boot 3.5
- Spring MVC + Thymeleaf
- Bootstrap 5
- Spring Data JPA (Hibernate)
- MySQL 8
- Spring Security (BCrypt + roles)
- Lombok
- JUnit 5

## Arquitectura

```
Navegador -> Vista (Thymeleaf) -> Controller -> Service -> Repository (JPA) -> MySQL
```

```
src/main/java/com/utp/panaderia
├── config/        SecurityConfig, DatosIniciales
├── controller/    controladores publicos y admin/
├── dto/           formularios (registro, producto)
├── model/         entidades (7 tablas)
├── repository/    repositorios JPA
├── security/      login con Spring Security
├── session/       carrito y favoritos (por sesion)
├── service/       interfaces + impl/
└── util/          clases de apoyo
```

## Cómo ejecutar

1. Tener MySQL corriendo en el puerto 3306.
2. Cambiar usuario y contraseña de MySQL en `src/main/resources/application.properties`
   (por defecto `root` / `root`).
3. Ejecutar:

```bash
mvnw.cmd spring-boot:run
```

4. Entrar a http://localhost:8080

La BD `panaderia_piero_alonso` se crea sola. También se puede crear con el script
`database/panaderia_piero_alonso.sql`.

**Usuario administrador:** `admin@pieroalonso.pe` / `Admin12345`

### Sin MySQL (H2)

Crear `src/main/resources/application-local.properties` (no se sube al repo):

```properties
spring.datasource.url=jdbc:h2:file:./data/panaderia;MODE=MySQL;DATABASE_TO_LOWER=TRUE
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver
```

y ejecutar:

```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

## Avance

- [x] Modelo de base de datos
- [x] Registro e inicio de sesión
- [x] Roles (cliente / administrador)
- [x] Inicio, Explorar (búsqueda y filtro por categoría)
- [x] Favoritos y carrito de compras
- [x] Confirmar pedido (recojo en tienda o delivery)
- [x] Historial de pedidos del cliente
- [x] Mantenimiento de productos (admin)
- [ ] Cambio de estado de pedidos (admin)
- [ ] Promociones
- [ ] Reportes
