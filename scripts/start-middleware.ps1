# ============================================================
#  商品管理系统 - 中间件启动脚本
#  启动 Redis、RabbitMQ、Elasticsearch（PostgreSQL 为 Windows 服务，通常已在运行）
#  用法： powershell -ExecutionPolicy Bypass -File scripts\start-middleware.ps1
# ============================================================
$ErrorActionPreference = "Continue"

$Root = Split-Path -Parent $PSScriptRoot
$MW   = Join-Path $Root "_middleware"

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host " 启动中间件" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

function Test-Port($port) {
    return (Test-NetConnection -ComputerName 127.0.0.1 -Port $port -InformationLevel Quiet -WarningAction SilentlyContinue)
}

# ---------------------------------------------------------- Redis
if (Test-Port 6379) {
    Write-Host "[跳过] Redis 已在运行 (6379)" -ForegroundColor Yellow
} else {
    Write-Host "[启动] Redis ..." -ForegroundColor Green
    Start-Process -FilePath (Join-Path $MW "redis\redis-server.exe") `
        -ArgumentList "redis.windows.conf" `
        -WorkingDirectory (Join-Path $MW "redis") -WindowStyle Minimized
    Start-Sleep -Seconds 3
}
$ping = & (Join-Path $MW "redis\redis-cli.exe") -p 6379 ping 2>&1
Write-Host ("       redis-cli ping -> " + $ping) -ForegroundColor Green

# ---------------------------------------------------------- RabbitMQ
if (Test-Port 5672) {
    Write-Host "[跳过] RabbitMQ 已在运行 (5672)" -ForegroundColor Yellow
} else {
    Write-Host "[启动] RabbitMQ ..." -ForegroundColor Green
    $env:ERLANG_HOME   = Join-Path $MW "erlang"
    $env:RABBITMQ_BASE = Join-Path $MW "rabbitmq-data"
    $env:HOME          = Join-Path $MW "erlang-home"
    $env:USERPROFILE   = Join-Path $MW "erlang-home"
    $env:HOMEDRIVE     = "C:"
    $env:HOMEPATH      = "\" + ((Join-Path $MW "erlang-home") -replace '^[A-Za-z]:', '')
    Start-Process -FilePath (Join-Path $MW "rabbitmq\sbin\rabbitmq-server.bat") `
        -WorkingDirectory (Join-Path $MW "rabbitmq\sbin") -WindowStyle Minimized
    Write-Host "       等待 RabbitMQ 就绪（约 30-60 秒）..." -ForegroundColor DarkGray
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 3
        if ((Test-Port 5672) -and (Test-Port 15672)) { break }
    }
}
Write-Host ("       AMQP 5672  : " + $(if (Test-Port 5672) { "OPEN" } else { "closed" })) -ForegroundColor Green
Write-Host ("       管理页 15672: " + $(if (Test-Port 15672) { "OPEN" } else { "closed" })) -ForegroundColor Green

# ---------------------------------------------------------- Elasticsearch
if (Test-Port 9200) {
    Write-Host "[跳过] Elasticsearch 已在运行 (9200)" -ForegroundColor Yellow
} else {
    Write-Host "[启动] Elasticsearch ..." -ForegroundColor Green
    # ES 默认把临时文件放在系统 %TEMP% 下，可能因权限失败，这里强制指向项目内目录
    $env:ES_TMPDIR = Join-Path $MW "es-tmp"
    New-Item -ItemType Directory -Force -Path $env:ES_TMPDIR | Out-Null
    Start-Process -FilePath (Join-Path $MW "elasticsearch\bin\elasticsearch.bat") `
        -WorkingDirectory (Join-Path $MW "elasticsearch") -WindowStyle Minimized
    Write-Host "       等待 Elasticsearch 就绪（约 20-40 秒）..." -ForegroundColor DarkGray
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 3
        if (Test-Port 9200) { break }
    }
}
Write-Host ("       Elasticsearch 9200: " + $(if (Test-Port 9200) { "OPEN" } else { "closed" })) -ForegroundColor Green

# ---------------------------------------------------------- PostgreSQL
Write-Host ("       PostgreSQL 5432   : " + $(if (Test-Port 5432) { "OPEN" } else { "closed（请确认 PostgreSQL 服务已启动）" })) -ForegroundColor Green

Write-Host ""
Write-Host "中间件状态检查完成。" -ForegroundColor Cyan
Write-Host "浏览器验证： http://localhost:9200    http://localhost:15672 (guest/guest)" -ForegroundColor Cyan
