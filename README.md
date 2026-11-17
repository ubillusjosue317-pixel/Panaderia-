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

### 3. Definir las credenciales (obligatorio)

El proyecto **ya no trae usuario ni contraseña por defecto**. Antes de arrancar, en PowerShell
(solo dura mientras esa ventana esté abierta):

```powershell
$env:DB_USER = "tu_usuario_mysql"
$env:DB_PASSWORD = "tu_contraseña_mysql"
$env:ADMIN_PASSWORD = "UnaClaveSegura123"   # clave del administrador (opcional)
```

Si no defines `ADMIN_PASSWORD`, se genera una clave aleatoria y se muestra **una sola vez** en la
consola al crear el administrador (busca la línea `Clave temporal del administrador`).
Sin `DB_USER` y `DB_PASSWORD` la aplicación no arranca.

La base de datos `panaderia_piero_alonso` se crea sola, no hay que hacer nada en MySQL.

### 4. Arrancar

```powershell
.\mvnw.cmd spring-boot:run
```

La primera vez demora unos minutos porque descarga las librerías. Cuando aparezca
`Started PanaderiaApplication`, abrir **http://localhost:8080**

Para apagarlo: `Ctrl + C` en la ventana de PowerShell.

**Usuario administrador:** `admin@pieroalonso.pe` con la clave de `ADMIN_PASSWORD` (o la temporal de la consola).

### Verificación en 2 pasos

Cada cuenta (clientes y administrador) tiene **su propia clave** de **Google Authenticator** o
**Microsoft Authenticator**, y se vincula cuando la cuenta se crea, no en el primer inicio de sesión:

- **Clientes:** al registrarse, después de llenar el formulario aparece el QR. Se escanea, se escribe
  el código de 6 dígitos y recién ahí se crea la cuenta.
- **Administrador:** al crearse (primer arranque), la consola muestra una sola vez su clave. En la app:
  *Agregar cuenta > Ingresar clave de configuración*, con el correo del administrador y esa clave.
- En cada inicio de sesión se pide la contraseña y luego el código. Si fallas 5 veces, hay que volver a empezar.
- Una cuenta sin clave de Authenticator no puede entrar.

**Empezar de cero:** `database/reiniciar_cuentas.sql` borra todas las cuentas (y sus pedidos y mensajes).
Al reiniciar la app se crea de nuevo el administrador con clave y Authenticator nuevos.

### Medidas de seguridad

- **Bloqueo de cuenta:** 5 contraseñas incorrectas seguidas bloquean el correo 15 minutos.
- **Contraseñas:** mínimo 8 caracteres con mayúscula, minúscula, número y símbolo; se rechazan claves comunes (`Password#1`, `Admin#12345`, etc.).
- **Administrador:** en su primer ingreso (después del código del Authenticator) debe reemplazar la contraseña inicial; no puede abrir otra página hasta hacerlo.
- **Carrito:** máximo 20 unidades por producto. **Precio mínimo:** S/ 0.01.
- **Imágenes:** solo se aceptan archivos subidos (JPG/PNG/WEBP); no se admiten URLs externas.
- **Registro:** si el correo ya existe, la respuesta es igual a la de un registro nuevo.
- **Auditoría:** los cambios del administrador (productos y estados de pedido) quedan en la tabla `auditoria` (quién, cuándo y qué cambió).
- **Cabeceras y cookies:** CSP, X-Frame-Options, Referrer-Policy y Permissions-Policy; cookie de sesión `HttpOnly`, `SameSite=Lax` y `Secure`. Bootstrap y Bootstrap Icons se sirven desde el propio sitio (`static/vendor`), sin CDN.

### HTTPS

Para forzar HTTPS hace falta un certificado. Para pruebas, uno autofirmado:

```powershell
keytool -genkeypair -alias panaderia -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 365 -dname "CN=localhost"
$env:SSL_PASSWORD = "la_clave_que_pusiste"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=https"
```

Entonces se usa **https://localhost:8443** y todo http se redirige a https. No subir `keystore.p12` a GitHub.
