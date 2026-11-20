# Genera un certificado de PRUEBA (autofirmado) para usar HTTPS en tu computadora.
#
# Uso (en PowerShell, dentro de la carpeta del proyecto):
#   $env:SSL_PASSWORD = "elige_una_clave_para_el_certificado"
#   .\generar-certificado.ps1
#
# Crea keystore.p12 (esta en .gitignore: NO lo subas a GitHub). Luego arranca con:
#   $env:DB_USER = "..."; $env:DB_PASSWORD = "..."
#   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=https"
# y abre https://localhost:8443 (el navegador avisara que el certificado no es de confianza:
# es normal con un certificado autofirmado; elige "Avanzado > Continuar").

if (-not $env:SSL_PASSWORD -or $env:SSL_PASSWORD.Length -lt 6) {
    Write-Host "Define primero la clave del certificado (minimo 6 caracteres):" -ForegroundColor Yellow
    Write-Host '  $env:SSL_PASSWORD = "tu_clave"' -ForegroundColor Yellow
    exit 1
}

if (Test-Path "keystore.p12") {
    Write-Host "Ya existe keystore.p12. Borralo si quieres generar uno nuevo." -ForegroundColor Yellow
    exit 1
}

# keytool viene con el JDK; si no esta en el PATH se busca en JAVA_HOME
$keytool = "keytool"
if (-not (Get-Command keytool -ErrorAction SilentlyContinue)) {
    $casa = $env:JAVA_HOME
    if (-not $casa -and (Get-Command java -ErrorAction SilentlyContinue)) {
        # si JAVA_HOME no esta definida, java dice donde esta instalado
        $linea = (& java -XshowSettings:properties -version 2>&1 | Select-String "java.home") | Select-Object -First 1
        if ($linea) { $casa = ($linea.ToString() -split "=", 2)[1].Trim() }
    }
    if ($casa -and (Test-Path "$casa\bin\keytool.exe")) {
        $keytool = "$casa\bin\keytool.exe"
    } else {
        Write-Host "No encontre keytool. Instala el JDK o define JAVA_HOME." -ForegroundColor Red
        exit 1
    }
}

& $keytool -genkeypair -alias panaderia -keyalg RSA -keysize 2048 -storetype PKCS12 `
    -keystore keystore.p12 -validity 365 -storepass $env:SSL_PASSWORD -keypass $env:SSL_PASSWORD `
    -dname "CN=localhost, OU=Panaderia, O=Piero Alonso, C=PE" `
    -ext "SAN=dns:localhost,ip:127.0.0.1"

if ($LASTEXITCODE -eq 0) {
    Write-Host "Listo: se creo keystore.p12" -ForegroundColor Green
}
