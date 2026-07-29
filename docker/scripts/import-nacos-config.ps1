param(
  [string]$NacosAddr = "http://127.0.0.1:8848",
  [string]$Group = "DEFAULT_GROUP",
  [string]$ConfigDir = (Join-Path $PSScriptRoot "..\nacos-config")
)

$ErrorActionPreference = "Stop"
$ConfigDir = (Resolve-Path $ConfigDir).Path

Write-Host "Waiting for Nacos at $NacosAddr ..."
for ($i = 0; $i -lt 60; $i++) {
  try {
    $r = Invoke-WebRequest -Uri "$NacosAddr/nacos/" -UseBasicParsing -TimeoutSec 3
    if ($r.StatusCode -ge 200) { break }
  } catch {
    Start-Sleep -Seconds 2
  }
  if ($i -eq 59) { throw "Nacos not ready" }
}

Get-ChildItem -Path $ConfigDir -Filter "*.yml" | ForEach-Object {
  $dataId = $_.Name
  $content = Get-Content -Raw -Path $_.FullName
  $body = @{
    dataId  = $dataId
    group   = $Group
    type    = "yaml"
    content = $content
  }
  Write-Host "Publishing $dataId ..."
  Invoke-RestMethod -Method Post -Uri "$NacosAddr/nacos/v1/cs/configs" -Body $body | Out-Null
}

Write-Host "Done. Open $NacosAddr/nacos  (no auth) to verify configs."
