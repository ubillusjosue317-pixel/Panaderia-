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
y hagan pedidos desde el celular o la PC, y que el personal gestione los productos.

## Tecnologías

- Java 21 + Spring Boot 3.5
- Spring MVC + Thymeleaf
- Bootstrap 5 + CSS propio (responsive)
- Spring Data JPA (Hibernate)
- MySQL 8
- Spring Security (BCrypt + roles + verificación en 2 pasos TOTP)
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

## Cómo ejecutarlo en Windows

### 1. Instalar (una sola vez)

| Programa | Dónde descargarlo | Nota |
|----------|-------------------|------|
| Java JDK 21 | https://adoptium.net (Temurin 21, instalador .msi) | Marcar "Set JAVA_HOME" durante la instalación |
| MySQL 8 | https://dev.mysql.com/downloads/installer/ (MySQL Installer) | Elegir "Server only" y anotar la contraseña de `root` |
| Git | https://git-scm.com/download/win | Opciones por defecto |

No hace falta instalar Maven, el proyecto trae `mvnw.cmd`.

Para revisar que Java quedó bien, en PowerShell: `java -version` (debe decir 21 o mayor).

### 2. Descargar el proyecto

```powershell
git clone https://github.com/ubillusjosue317-pixel/Panaderia-.git
cd Panaderia-
```

### 3. Poner la contraseña de MySQL

El proyecto usa el usuario `root` con contraseña `root`. Si tu MySQL tiene otra contraseña,
escríbela en PowerShell antes de arrancar (solo dura mientras esa ventana esté abierta):

```powershell
$env:DB_PASSWORD = "tu_contraseña"
```

O cámbiala en `src/main/resources/application.properties` (línea `spring.datasource.password`),
pero **sin subir ese cambio** a GitHub.

La base de datos `panaderia_piero_alonso` se crea sola, no hay que hacer nada en MySQL.

### 4. Arrancar

```powershell
.\mvnw.cmd spring-boot:run
```

La primera vez demora unos minutos porque descarga las librerías. Cuando aparezca
`Started PanaderiaApplication`, abrir **http://localhost:8080**

Para apagarlo: `Ctrl + C` en la ventana de PowerShell.

**Usuario administrador:** `admin@pieroalonso.pe` / `Admin12345`

### Verificación en 2 pasos

Todos los usuarios (clientes y administrador) deben ingresar un código de 6 dígitos después de la contraseña.
Hay que tener en el celular **Google Authenticator** o **Microsoft Authenticator** (gratis en Play Store / App Store).

1. La primera vez que inicias sesión aparece un código QR: escanéalo con la app.
2. Escribe el código de 6 dígitos que muestra la app. Desde ahí, cada inicio de sesión pide el código.
3. Si fallas 5 veces se cierra la sesión y hay que volver a poner la contraseña.

La hora del celular y de la PC deben estar correctas (automáticas), si no el código sale como incorrecto.

Si alguien cambia de celular o borra la app, se reinicia su verificación desde MySQL y la próxima vez le aparece otro QR:

```sql
UPDATE usuario SET totp_secreto = NULL WHERE email = 'correo@del.usuario';
```

Cada base de datos es independiente: si abres el proyecto en otra PC, el administrador escanea un QR nuevo ahí.

### Panel del administrador

El administrador no ve las opciones de compra (Explorar, carrito, Favoritos). Su menú tiene:

- **Productos**: crear y editar productos, cambiar el precio directo desde la tabla y subir la foto desde la PC.
- **Pedidos**: todos los pedidos de los clientes, filtrados por estado; se cambia el estado (Pendiente → En preparación → Listo → Entregado).
- **Mensajería**: consultas que escriben los clientes desde su sección *Mensajes*; el administrador las responde y el cliente ve la respuesta ahí mismo.

### Imágenes de los productos

Las fotos de los productos de ejemplo están dentro del proyecto en `src/main/resources/static/img/productos/`,
por eso se ven en cualquier PC.

Las fotos que sube el administrador desde el formulario se guardan en la carpeta `uploads/` (al lado de `pom.xml`)
y no se suben a GitHub, igual que la base de datos: cada PC tiene sus propios productos y sus propias fotos.

### Verlo desde el celular

Con la PC y el celular en el mismo Wi-Fi:

1. En PowerShell, `ipconfig` y copiar la "Dirección IPv4" (por ejemplo `192.168.1.25`).
2. En el celular abrir `http://192.168.1.25:8080`
3. Si no carga, Windows pedirá permiso de firewall para Java la primera vez: aceptar en "Redes privadas".

### Probar sin MySQL (opcional)

Crear el archivo `src/main/resources/application-local.properties` con:

```properties
spring.datasource.url=jdbc:h2:file:./data/panaderia;MODE=MySQL;DATABASE_TO_LOWER=TRUE
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver
```

y arrancar con:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

### Pruebas

```powershell
.\mvnw.cmd test
```

### Problemas comunes

| Mensaje | Solución |
|---------|----------|
| `java no se reconoce como un comando` | Reinstalar el JDK marcando "Set JAVA_HOME" y abrir una nueva ventana de PowerShell |
| `Access denied for user 'root'` | La contraseña de MySQL no es `root`: usar `$env:DB_PASSWORD` (paso 3) |
| `Communications link failure` | MySQL no está encendido: abrir "Servicios" de Windows e iniciar `MySQL80` |
| `Port 8080 was already in use` | Ya hay otra copia corriendo: cerrarla con `Ctrl + C` |
| `release version 21 not supported` | Hay un Java antiguo instalado: instalar el JDK 21 |

## Avance

- [x] Modelo de base de datos
- [x] Registro e inicio de sesión
- [x] Verificación en 2 pasos (app autenticadora)
- [x] Roles (cliente / administrador)
- [x] Inicio, Explorar (búsqueda y filtro por categoría)
- [x] Favoritos y carrito de compras
- [x] Confirmar pedido (recojo en tienda o delivery)
- [x] Historial de pedidos del cliente
- [x] Mantenimiento de productos (admin)
- [x] Diseño para celular y computadora
- [x] Cambio de estado de pedidos (admin)
- [x] Mensajería entre clientes y administrador
- [x] Subida de imágenes de productos
- [ ] Promociones
- [ ] Reportes
