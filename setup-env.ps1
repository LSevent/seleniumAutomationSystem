$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$JavaHome = Join-Path $ProjectRoot ".tools\jdk-17.0.19+10"
$MavenHome = Join-Path $ProjectRoot ".tools\apache-maven-3.9.11"

if (-not (Test-Path (Join-Path $JavaHome "bin\java.exe"))) {
    throw "Java was not found at $JavaHome"
}

if (-not (Test-Path (Join-Path $MavenHome "bin\mvn.cmd"))) {
    throw "Maven was not found at $MavenHome"
}

$env:JAVA_HOME = $JavaHome
$env:MAVEN_HOME = $MavenHome
$env:Path = "$JavaHome\bin;$MavenHome\bin;$env:Path"

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "MAVEN_HOME=$env:MAVEN_HOME"
Write-Host "Current PowerShell session is ready. Try: java -version; mvn -version; mvn clean test"
