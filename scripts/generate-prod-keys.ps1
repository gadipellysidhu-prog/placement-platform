# generate-prod-keys.ps1
# Generates an RSA key pair using the bundled JDK and writes single-line
# base64 DER values into .env.prod (no PEM headers, no newlines).
# JwtService.decodePem() accepts this format directly.
#
# Usage (run from the project root):
#   .\scripts\generate-prod-keys.ps1
#   .\scripts\generate-prod-keys.ps1 -EnvFile ".env.prod" -KeySize 4096

param(
    [string]$EnvFile = ".env.prod",
    [int]$KeySize = 2048
)

$ErrorActionPreference = "Stop"

$scriptDir   = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$java = [System.IO.Path]::GetFullPath((Join-Path $projectRoot "..\..\..\jdk\jdk-17.0.19+10\bin\java.exe"))

if (-not (Test-Path $java)) {
    Write-Error "Bundled JDK not found at: $java"
    exit 1
}
Write-Host "JDK: $java"

# Write Java source file without BOM
$javaFile = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), "PlacementGenerateKeys.java")
$q = [char]34
$n = [char]10

$src = "import java.security.*;" + $n +
       "import java.util.Base64;" + $n +
       "public class PlacementGenerateKeys {" + $n +
       "    public static void main(String[] args) throws Exception {" + $n +
       "        KeyPairGenerator kpg = KeyPairGenerator.getInstance(${q}RSA${q});" + $n +
       "        kpg.initialize($KeySize);" + $n +
       "        KeyPair kp = kpg.generateKeyPair();" + $n +
       "        System.out.println(${q}PRIVATE_B64=${q} + Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()));" + $n +
       "        System.out.println(${q}PUBLIC_B64=${q} + Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()));" + $n +
       "    }" + $n +
       "}"

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText($javaFile, $src, $utf8NoBom)

Write-Host "Generating RSA $KeySize-bit key pair..."
$output = & $java $javaFile 2>&1

$privateLine = $output | Where-Object { $_ -match "^PRIVATE_B64=" } | Select-Object -First 1
$publicLine  = $output | Where-Object { $_ -match "^PUBLIC_B64="  } | Select-Object -First 1

if (-not $privateLine -or -not $publicLine) {
    Write-Error "Key generation failed:`n$($output -join "`n")"
    exit 1
}

$privateB64 = ($privateLine -replace "^PRIVATE_B64=", "").Trim()
$publicB64  = ($publicLine  -replace "^PUBLIC_B64=",  "").Trim()

Write-Host "Private key: $($privateB64.Length) chars (PKCS#8 DER base64)"
Write-Host "Public key:  $($publicB64.Length) chars (X.509 DER base64)"

$envPath    = [System.IO.Path]::Combine($projectRoot, $EnvFile)
$examplePath = [System.IO.Path]::Combine($projectRoot, ".env.example")

if (Test-Path $envPath) {
    $content = [System.IO.File]::ReadAllText($envPath)
    if ($content -match "JWT_PRIVATE_KEY_PEM=") {
        $content = [System.Text.RegularExpressions.Regex]::Replace($content, "(?m)^JWT_PRIVATE_KEY_PEM=.*$", "JWT_PRIVATE_KEY_PEM=$privateB64")
    } else {
        $content = $content.TrimEnd() + "`nJWT_PRIVATE_KEY_PEM=$privateB64`n"
    }
    if ($content -match "JWT_PUBLIC_KEY_PEM=") {
        $content = [System.Text.RegularExpressions.Regex]::Replace($content, "(?m)^JWT_PUBLIC_KEY_PEM=.*$", "JWT_PUBLIC_KEY_PEM=$publicB64")
    } else {
        $content = $content.TrimEnd() + "`nJWT_PUBLIC_KEY_PEM=$publicB64`n"
    }
    [System.IO.File]::WriteAllText($envPath, $content, $utf8NoBom)
    Write-Host "Updated: $envPath"
} elseif (Test-Path $examplePath) {
    $content = [System.IO.File]::ReadAllText($examplePath)
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "(?m)^JWT_PRIVATE_KEY_PEM=.*$", "JWT_PRIVATE_KEY_PEM=$privateB64")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "(?m)^JWT_PUBLIC_KEY_PEM=.*$",  "JWT_PUBLIC_KEY_PEM=$publicB64")
    [System.IO.File]::WriteAllText($envPath, $content, $utf8NoBom)
    Write-Host "Created: $envPath (from .env.example)"
    Write-Host "REQUIRED: Set DB_PASSWORD and CORS_ALLOWED_ORIGINS in $envPath"
} else {
    $minimal = "DB_NAME=placement_prod`n" +
               "DB_USERNAME=placement_user`n" +
               "DB_PASSWORD=CHANGE_ME`n" +
               "CORS_ALLOWED_ORIGINS=http://localhost:3000`n" +
               "SERVER_PORT=8081`n" +
               "APP_VERSION=1.0.0`n" +
               "JWT_PRIVATE_KEY_PEM=$privateB64`n" +
               "JWT_PUBLIC_KEY_PEM=$publicB64`n"
    [System.IO.File]::WriteAllText($envPath, $minimal, $utf8NoBom)
    Write-Host "Created: $envPath (minimal template)"
    Write-Host "REQUIRED: Set DB_PASSWORD and CORS_ALLOWED_ORIGINS in $envPath"
}

Remove-Item $javaFile -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "Keys written. Edit $EnvFile to set DB_PASSWORD and CORS_ALLOWED_ORIGINS."
Write-Host ""
Write-Host "Deploy:"
Write-Host "  docker compose -f docker-compose.prod.yml --env-file $EnvFile up -d"
