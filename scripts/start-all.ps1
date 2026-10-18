# ============================================================
#  商品管理系统 - 一键启动（中间件 + 后端 + 前端）
#  用法： powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1
# ============================================================
$ErrorActionPreference = "Continue"
$Root = Split-Path -Parent $PSScriptRoot
$MW   = Join-Path $Root "_middleware"
$Node = Join-Path $MW "node"
$M2Settings = Join-Path $MW "m2-settings.xml"

Write-Host "############ 1/3 中间件 ############" -ForegroundColor Cyan
& (Join-Path $PSScriptRoot "start-middleware.ps1")

Write-Host ""
Write-Host "############ 2/3 后端 (Spring Boot :8090) ############" -ForegroundColor Cyan
Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/c", "mvn -B -s `"$M2Settings`" spring-boot:run" `
    -WorkingDirectory (Join-Path $Root "backend")
Write-Host "等待后端就绪 ..." -ForegroundColor DarkGray
for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 3
    if (Test-NetConnection -ComputerName 127.0.0.1 -Port 8090 -InformationLevel Quiet -WarningAction SilentlyContinue) { break }
}
$backend = Test-NetConnection -ComputerName 127.0.0.1 -Port 8090 -InformationLevel Quiet -WarningAction SilentlyContinue
Write-Host ("后端 8090: " + $(if ($backend) { "OPEN  http://localhost:8090" } else { "closed（请查看后端窗口日志）" })) -ForegroundColor Green

Write-Host ""
Write-Host "############ 3/3 前端 (Vite :5173) ############" -ForegroundColor Cyan
Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/c", "set `"PATH=$Node;%PATH%`" && npm run dev" `
    -WorkingDirectory (Join-Path $Root "frontend")
Start-Sleep -Seconds 12
$frontend = Test-NetConnection -ComputerName 127.0.0.1 -Port 5173 -InformationLevel Quiet -WarningAction SilentlyContinue
Write-Host ("前端 5173: " + $(if ($frontend) { "OPEN  http://localhost:5173" } else { "closed（请查看前端窗口日志）" })) -ForegroundColor Green

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host " 全部启动完成" -ForegroundColor Cyan
Write-Host "   前端页面 : http://localhost:5173" -ForegroundColor White
Write-Host "   后端接口 : http://localhost:8090" -ForegroundColor White
Write-Host "   演示账号 : admin / admin123（超级管理员）" -ForegroundColor White
Write-Host "              product / product123（商品管理员）" -ForegroundColor White
Write-Host "              viewer / viewer123（只读访客）" -ForegroundColor White
Write-Host "==============================================" -ForegroundColor Cyan
