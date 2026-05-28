Set-Location "$PSScriptRoot\..\web"
$frontendHost = if ($env:FRONTEND_HOST) { $env:FRONTEND_HOST } else { '0.0.0.0' }
$frontendPort = if ($env:FRONTEND_PORT) { $env:FRONTEND_PORT } else { '8102' }
npm run dev -- --host $frontendHost --port $frontendPort
