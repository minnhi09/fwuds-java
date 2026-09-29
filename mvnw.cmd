@echo off
setlocal
set "PROJECT_DIR=%~dp0"
set "MAVEN_VERSION=3.9.9"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
if "%JAVA_HOME%"=="" (
  where java >nul 2>nul || (
    echo ERROR: Java 21 was not found. Install a JDK and set JAVA_HOME.
    exit /b 1
  )
) else if not exist "%JAVA_HOME%\bin\java.exe" (
  echo ERROR: JAVA_HOME does not point to a valid JDK: %JAVA_HOME%
  exit /b 1
)
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  echo Downloading Apache Maven %MAVEN_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $u='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip'; $z=Join-Path $env:TEMP 'apache-maven-%MAVEN_VERSION%-bin.zip'; Invoke-WebRequest -Uri $u -OutFile $z; $sha=[Security.Cryptography.SHA256]::Create(); try{$actual=([BitConverter]::ToString($sha.ComputeHash([IO.File]::ReadAllBytes($z)))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}; if($actual -ne '4ec3f26fb1a692473aea0235c300bd20f0f9fe741947c82c1234cefd76ac3a3c'){throw ('Maven archive checksum mismatch: '+$actual)}; $parent=Split-Path '%MAVEN_HOME%'; $null=New-Item -ItemType Directory -Force -Path $parent; Expand-Archive -LiteralPath $z -DestinationPath $parent -Force"
  if errorlevel 1 exit /b 1
)
call "%MAVEN_HOME%\bin\mvn.cmd" -f "%PROJECT_DIR%pom.xml" %*
