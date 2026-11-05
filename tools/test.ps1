param([string]$JdkPath='C:\Program Files\Java\jdk-17')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
  & npm.cmd --prefix band test
  if($LASTEXITCODE){throw 'Band tests failed'}
  New-Item -ItemType Directory -Force -Path vendor,test/build | Out-Null
  if(-not (Test-Path vendor/json.jar)){Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/json/json/20250517/json-20250517.jar' -OutFile vendor/json.jar}
  & (Join-Path $JdkPath 'bin/javac.exe') -encoding UTF-8 -cp vendor/json.jar -d test/build test/stubs/android/content/Context.java test/stubs/android/content/SharedPreferences.java test/stubs/android/content/res/AssetManager.java android/app/src/main/java/com/bandcalendar/sync/CalendarCore.java android/app/src/main/java/com/bandcalendar/sync/CalendarRepository.java test/CalendarTest.java test/RepositoryTest.java
  if($LASTEXITCODE){throw 'JVM test compilation failed'}
  & (Join-Path $JdkPath 'bin/java.exe') -cp ('test/build'+[IO.Path]::PathSeparator+'vendor/json.jar') CalendarTest
  if($LASTEXITCODE){throw 'JVM tests failed'}
  & (Join-Path $JdkPath 'bin/java.exe') -cp ('test/build'+[IO.Path]::PathSeparator+'vendor/json.jar') RepositoryTest
  if($LASTEXITCODE){throw 'Repository tests failed'}
  & node tools/test-parity.cjs
  if($LASTEXITCODE){throw 'Java/JS parity failed'}
}finally{Pop-Location}
