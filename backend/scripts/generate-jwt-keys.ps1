<#
.SYNOPSIS
    Generates a 2048-bit RSA key pair for JWT RS256 signing.

.DESCRIPTION
    Produces PKCS8 private key (-----BEGIN PRIVATE KEY-----) and
    SubjectPublicKeyInfo public key (-----BEGIN PUBLIC KEY-----) in PEM format,
    compatible with Java's PKCS8EncodedKeySpec / X509EncodedKeySpec and
    the application's JwtService.

    Works on Windows PowerShell 5.1 (.NET Framework 4.x) and PowerShell 7+
    (.NET 5+). No embedded Java source code. No external tools required.

.PARAMETER OutputDir
    Directory where jwt-private.pem and jwt-public.pem are written.
    Defaults to <backend-root>/secrets/

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\generate-jwt-keys.ps1
    powershell -ExecutionPolicy Bypass -File scripts\generate-jwt-keys.ps1 -OutputDir C:\my-secrets
#>

[CmdletBinding()]
param(
    [string]$OutputDir = ""
)

$ErrorActionPreference = "Stop"

$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Definition
$BackendDir = Split-Path -Parent $ScriptDir

if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $BackendDir "secrets"
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$PrivateKeyPath = Join-Path $OutputDir "jwt-private.pem"
$PublicKeyPath  = Join-Path $OutputDir "jwt-public.pem"

Write-Host ""
Write-Host "=== JWT RSA-2048 Key Generation ===" -ForegroundColor Cyan
Write-Host "Output directory  : $OutputDir"
Write-Host "PowerShell version: $($PSVersionTable.PSVersion)"
Write-Host ""

$privateKeyPem = $null
$publicKeyPem  = $null

if ($PSVersionTable.PSVersion.Major -ge 7) {
    Write-Host "Using native .NET 5+ RSA export methods." -ForegroundColor DarkGray
    $rsa = [System.Security.Cryptography.RSA]::Create(2048)
    try {
        $privateKeyPem = $rsa.ExportPkcs8PrivateKeyPem()
        $publicKeyPem  = $rsa.ExportSubjectPublicKeyInfoPem()
    } finally {
        $rsa.Dispose()
    }
} else {
    Write-Host "Using .NET Framework RSA with DER encoder (PS 5.1)." -ForegroundColor DarkGray

    # Load C# DER encoder only once per session
    if (-not ([System.Management.Automation.PSTypeName]'RsaPemExporter').Type) {
        $csharp = @"
using System;
using System.Security.Cryptography;
using System.Text;

public static class RsaPemExporter
{
    private static readonly byte[] RsaOid =
        new byte[] { 0x2A, 0x86, 0x48, 0x86, 0xF7, 0x0D, 0x01, 0x01, 0x01 };

    public static string ExportPrivateKeyPem(RSAParameters p)
    {
        return WrapPem("PRIVATE KEY", BuildPkcs8(p));
    }

    public static string ExportPublicKeyPem(RSAParameters p)
    {
        return WrapPem("PUBLIC KEY", BuildSpki(p));
    }

    private static string WrapPem(string label, byte[] der)
    {
        string b64 = Convert.ToBase64String(der);
        var sb = new StringBuilder();
        sb.Append("-----BEGIN ").Append(label).AppendLine("-----");
        for (int i = 0; i < b64.Length; i += 64)
            sb.AppendLine(b64.Substring(i, Math.Min(64, b64.Length - i)));
        sb.Append("-----END ").Append(label).Append("-----");
        return sb.ToString();
    }

    private static byte[] BuildPkcs8(RSAParameters p)
    {
        byte[] pkcs1 = BuildPkcs1(p);
        byte[] algId = Seq(Oid(RsaOid), Null());
        return Seq(Int0(), algId, OctetStr(pkcs1));
    }

    private static byte[] BuildPkcs1(RSAParameters p)
    {
        return Seq(Int0(), UInt(p.Modulus), UInt(p.Exponent),
                   UInt(p.D), UInt(p.P), UInt(p.Q),
                   UInt(p.DP), UInt(p.DQ), UInt(p.InverseQ));
    }

    private static byte[] BuildSpki(RSAParameters p)
    {
        byte[] rsaPub = Seq(UInt(p.Modulus), UInt(p.Exponent));
        byte[] algId  = Seq(Oid(RsaOid), Null());
        return Seq(algId, BitStr(rsaPub));
    }

    private static byte[] Tlv(byte tag, byte[] val)
    {
        byte[] lenBytes = EncodeLen(val.Length);
        byte[] buf = new byte[1 + lenBytes.Length + val.Length];
        buf[0] = tag;
        Array.Copy(lenBytes, 0, buf, 1, lenBytes.Length);
        Array.Copy(val, 0, buf, 1 + lenBytes.Length, val.Length);
        return buf;
    }

    private static byte[] EncodeLen(int n)
    {
        if (n < 0x80)  return new byte[] { (byte)n };
        if (n < 0x100) return new byte[] { 0x81, (byte)n };
        return new byte[] { 0x82, (byte)(n >> 8), (byte)(n & 0xFF) };
    }

    private static byte[] Cat(params byte[][] parts)
    {
        int sz = 0; foreach (var x in parts) sz += x.Length;
        byte[] buf = new byte[sz]; int pos = 0;
        foreach (var x in parts) { Array.Copy(x, 0, buf, pos, x.Length); pos += x.Length; }
        return buf;
    }

    private static byte[] UInt(byte[] v)
    {
        if (v[0] >= 0x80) v = Cat(new byte[] { 0x00 }, v);
        return Tlv(0x02, v);
    }

    private static byte[] Int0()               { return new byte[] { 0x02, 0x01, 0x00 }; }
    private static byte[] Seq(params byte[][] c){ return Tlv(0x30, Cat(c)); }
    private static byte[] OctetStr(byte[] d)   { return Tlv(0x04, d); }
    private static byte[] Oid(byte[] o)        { return Tlv(0x06, o); }
    private static byte[] Null()               { return new byte[] { 0x05, 0x00 }; }
    private static byte[] BitStr(byte[] d)     { return Tlv(0x03, Cat(new byte[]{0x00}, d)); }
}
"@
        Add-Type -Language CSharp -TypeDefinition $csharp
    }

    $rsa = New-Object System.Security.Cryptography.RSACryptoServiceProvider(2048)
    try {
        $p = $rsa.ExportParameters($true)
        $privateKeyPem = [RsaPemExporter]::ExportPrivateKeyPem($p)
        $publicKeyPem  = [RsaPemExporter]::ExportPublicKeyPem($p)
    } finally {
        $rsa.Dispose()
    }
}

# Write PEM files
$privateKeyPem | Set-Content -Path $PrivateKeyPath -Encoding UTF8 -NoNewline
$publicKeyPem  | Set-Content -Path $PublicKeyPath  -Encoding UTF8 -NoNewline

Write-Host "Private key : $PrivateKeyPath" -ForegroundColor Green
Write-Host "Public key  : $PublicKeyPath"  -ForegroundColor Green

# Structural validation (works on PS 5.1 and PS 7)
Write-Host ""
Write-Host "--- Structural validation ---" -ForegroundColor Yellow

function Test-PemStructure {
    param([string]$Path, [string]$Label)
    $raw = Get-Content $Path -Raw
    if (-not $raw.Contains("-----BEGIN $Label-----")) { throw "Missing BEGIN header in $Path" }
    if (-not $raw.Contains("-----END $Label-----"))   { throw "Missing END footer in $Path" }
    $b64 = $raw -replace '-----[A-Z ]+-----','' -replace '\s',''
    $der = [System.Convert]::FromBase64String($b64)
    if ($der[0] -ne 0x30) { throw "Expected DER SEQUENCE (0x30) in $Path, got 0x$($der[0].ToString('X2'))" }
    if ($der.Length -lt 256) { throw "Key too short ($($der.Length) bytes) in $Path" }
    Write-Host "  [OK] $Label : $($der.Length) bytes, DER SEQUENCE verified" -ForegroundColor Green
}

Test-PemStructure -Path $PrivateKeyPath -Label "PRIVATE KEY"
Test-PemStructure -Path $PublicKeyPath  -Label "PUBLIC KEY"

# Roundtrip load+sign validation (PS 7 / .NET 5+ only)
if ($PSVersionTable.PSVersion.Major -ge 7) {
    Write-Host ""
    Write-Host "--- Roundtrip validation (PS 7 / .NET 5+) ---" -ForegroundColor Yellow

    try {
        $privB64 = ((Get-Content $PrivateKeyPath -Raw) -replace '-----[A-Z ]+-----','' -replace '\s','')
        $pubB64  = ((Get-Content $PublicKeyPath  -Raw) -replace '-----[A-Z ]+-----','' -replace '\s','')
        $privDer = [System.Convert]::FromBase64String($privB64)
        $pubDer  = [System.Convert]::FromBase64String($pubB64)

        $rsaPriv = [System.Security.Cryptography.RSA]::Create()
        $nc = 0
        $rsaPriv.ImportPkcs8PrivateKey([System.ReadOnlySpan[byte]]$privDer, [ref]$nc)
        Write-Host "  [OK] ImportPkcs8PrivateKey succeeded ($nc bytes consumed)" -ForegroundColor Green

        $rsaPub = [System.Security.Cryptography.RSA]::Create()
        $rsaPub.ImportSubjectPublicKeyInfo([System.ReadOnlySpan[byte]]$pubDer, [ref]$nc)
        Write-Host "  [OK] ImportSubjectPublicKeyInfo succeeded ($nc bytes consumed)" -ForegroundColor Green

        $data = [System.Text.Encoding]::UTF8.GetBytes("placement-jwt-validation-token")
        $alg  = [System.Security.Cryptography.HashAlgorithmName]::SHA256
        $pad  = [System.Security.Cryptography.RSASignaturePadding]::Pkcs1
        $sig  = $rsaPriv.SignData($data, $alg, $pad)
        $ok   = $rsaPub.VerifyData($data, $sig, $alg, $pad)
        if (-not $ok) { throw "Sign/verify mismatch - keys are not a valid pair" }
        Write-Host "  [OK] Sign + verify roundtrip passed - key pair is valid" -ForegroundColor Green

        $rsaPriv.Dispose()
        $rsaPub.Dispose()
    } catch {
        Write-Host "  [FAIL] $_" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "=== Generation complete ===" -ForegroundColor Cyan
Write-Host "IMPORTANT: Never commit jwt-private.pem to version control." -ForegroundColor Red
Write-Host ""
Write-Host "For .env.prod paste (newlines as literal \n):" -ForegroundColor Cyan
Write-Host ""

$privEnv = ((Get-Content $PrivateKeyPath -Raw) -replace "`r`n","`n" -replace "`n","\\n").TrimEnd('\','n')
$pubEnv  = ((Get-Content $PublicKeyPath  -Raw) -replace "`r`n","`n" -replace "`n","\\n").TrimEnd('\','n')

Write-Host "JWT_PRIVATE_KEY_PEM=$privEnv"
Write-Host ""
Write-Host "JWT_PUBLIC_KEY_PEM=$pubEnv"
Write-Host ""
