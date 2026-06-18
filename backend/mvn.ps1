# Simple Maven Bootstrap Script for Windows PowerShell
# Downloads Maven 3.9.6 and runs build commands

param(
    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$MavenArgs
)

$MAVEN_VERSION = "3.9.6"
$MAVEN_URL = "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip"
$M2_DIR = "$PSScriptRoot\.m2"
$MAVEN_HOME = "$M2_DIR\apache-maven-$MAVEN_VERSION"

# Create .m2 directory if it doesn't exist
if (!(Test-Path $M2_DIR)) {
    New-Item -ItemType Directory -Path $M2_DIR | Out-Null
}

# Download Maven if not already present
if (!(Test-Path "$MAVEN_HOME\bin\mvn.cmd")) {
    Write-Host "Downloading Maven $MAVEN_VERSION..."
    $MAVEN_ZIP = "$M2_DIR\maven-$MAVEN_VERSION.zip"
    
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    (New-Object Net.WebClient).DownloadFile($MAVEN_URL, $MAVEN_ZIP)
    
    Write-Host "Extracting Maven..."
    Expand-Archive -Path $MAVEN_ZIP -DestinationPath $M2_DIR -Force
    
    Remove-Item $MAVEN_ZIP -Force -ErrorAction SilentlyContinue
    Write-Host "Maven installed at: $MAVEN_HOME"
}

# Run Maven with full path in quotes
$MAVEN_CMD = "$MAVEN_HOME\bin\mvn.cmd"
Write-Host "Running Maven: $MAVEN_CMD $($MavenArgs -join ' ')"

if (Test-Path $MAVEN_CMD) {
    & cmd /c "`"$MAVEN_CMD`" $($MavenArgs -join ' ')"
} else {
    Write-Error "Maven executable not found at $MAVEN_CMD"
    exit 1
}

