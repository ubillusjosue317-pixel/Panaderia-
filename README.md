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
**Microsoft Authenticator**, y se vincula al crear la cuenta (clientes) o en el primer inicio de sesión con la contraseña temporal (administrador):

- **Clientes:** al registrarse, después de llenar el formulario aparece el QR. Se escanea, se escribe
  el código de 6 dígitos y recién ahí se crea la cuenta.
- **Administrador:** se crea al arrancar la app por primera vez, con una contraseña temporal que sale una sola
  vez en la consola. En su **primer inicio de sesión** la pantalla de verificación le muestra el QR; lo escanea,
  escribe el primer código y desde ahí el QR ya no se vuelve a mostrar. Luego debe cambiar la contraseña.
- En cada inicio de sesión se pide la contraseña y luego el código. Si fallas 5 veces, hay que volver a empezar.
- Una cuenta sin clave de Authenticator no puede entrar.

**Empezar de cero:** `database/reiniciar_cuentas.sql` borra todas las cuentas (y sus pedidos y mensajes).
Al reiniciar la app se crea de nuevo el administrador con una contraseña temporal nueva.

### Funciones del sistema

**Clientes:** catálogo con filtro por categoría, página de cada producto con **reseñas** (1 a 5 estrellas), carrito (máx. 20 por producto y hasta donde alcance el **stock**), pedidos con recojo en tienda o delivery, **método de pago solo en delivery** (Efectivo, Yape / Plin o Tarjeta contra entrega), **subir el comprobante** de un pago con Yape / Plin, **cancelar** un pedido pendiente, **volver a pedir**, **mi perfil**, mensajes y **olvidé mi contraseña**.

**Administrador:** productos (precio, stock, imagen), pedidos (con método de pago y comprobante), mensajes, y **Ventas**: carga de ventas desde un Excel `.xlsx` (columnas Fecha, Producto, Cantidad, Precio; hay una plantilla descargable). Si alguna fila tiene errores no se carga nada.

### Medidas de seguridad

- **Bloqueo de cuenta:** 5 contraseñas incorrectas seguidas bloquean el correo 15 minutos. La sesión vence a los 10 minutos sin actividad.
- **Contraseñas:** mínimo 8 caracteres con mayúscula, minúscula, número y símbolo; se rechazan claves comunes (`Password#1`, `Admin#12345`, etc.).
- **Administrador:** en su primer ingreso (después del código del Authenticator) debe reemplazar la contraseña inicial; no puede abrir otra página hasta hacerlo.
- **Recuperar contraseña:** el enlace del correo lleva un token aleatorio de 256 bits, de **un solo uso**, que vence a los 30 minutos; en la base solo se guarda su hash. Al abrir el enlace, el token sale de la dirección (no queda en el historial ni en `Referer`). Siempre se responde igual exista o no el correo, hay un límite de 3 enlaces cada 15 minutos por correo, y la cuenta sigue pidiendo el código del Authenticator. Sin servidor de correo configurado, el enlace se escribe en la consola (modo desarrollo); para enviar correos reales define `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` (y `BASE_URL` con la dirección pública de la app).
- **Perfil:** el usuario siempre sale de la sesión, nunca de un número en la URL; `/perfil/{id}` con el id de otra persona da 403. El formulario no acepta cambiar correo, rol ni id.
- **Reseñas:** el texto se escapa al mostrarlo, así que un `<script>` se ve como texto y no se ejecuta. Una reseña por cliente y producto; solo el autor o el administrador la borran.
- **Stock:** el descuento al confirmar un pedido es una sola instrucción SQL atómica (`... where stock >= cantidad`), así que dos compras simultáneas no pueden vender la misma última unidad. Un pedido cancelado devuelve su stock.
- **Pedidos:** los precios y el total se calculan en el servidor desde la base de datos; lo que mande el navegador no los cambia.
- **Archivos subidos:** imágenes de productos y comprobantes se validan por su contenido real (primeros bytes), no por la extensión ni por el tipo que declara el navegador. Los comprobantes (`.jpg`, `.png`, `.pdf`, máx. 5 MB) se guardan con nombre aleatorio en una carpeta privada y solo los ve su dueño o el administrador. Los `.xlsx` de ventas se leen con límites de tamaño y de filas, y protección contra archivos comprimidos maliciosos.
- **Carrito:** máximo 20 unidades por producto. **Precio mínimo:** S/ 0.01.
- **Imágenes:** solo se aceptan archivos subidos (JPG/PNG/WEBP); no se admiten URLs externas.
- **Registro:** si el correo ya existe, la respuesta es igual a la de un registro nuevo.
- **Auditoría:** los cambios del administrador (productos, estados de pedido, cargas de ventas, cambios de clave) quedan en la tabla `auditoria` (quién, cuándo y qué cambió).
- **Cabeceras y cookies:** CSP, X-Frame-Options, nosniff, Referrer-Policy, Permissions-Policy y HSTS (por HTTPS); cookie de sesión `HttpOnly`, `SameSite=Lax` y `Secure`, que se borra al cerrar sesión. Bootstrap, los iconos y las fuentes se sirven desde el propio sitio (`static/vendor`): la página no carga nada de terceros.
- **Errores:** las páginas de error no muestran clases, líneas ni rutas del código.

### HTTPS

Para usar y forzar HTTPS hace falta un certificado. Para pruebas, uno autofirmado (el script usa `keytool`, que viene con Java):

```powershell
$env:SSL_PASSWORD = "elige_una_clave_para_el_certificado"
.\generar-certificado.ps1
$env:DB_USER = "tu_usuario_mysql"; $env:DB_PASSWORD = "tu_contraseña_mysql"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=https"
```

Entonces se usa **https://localhost:8443**. Quien abra `http://localhost:8080` es redirigido a https. El navegador avisará que el certificado no es de confianza (es normal con uno autofirmado): "Avanzado > Continuar". No subir `keystore.p12` a GitHub.
