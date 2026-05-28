Set-Location "$PSScriptRoot\..\web"
$env:FRONTEND_HOST = if ($env:FRONTEND_HOST) { $env:FRONTEND_HOST } else { '0.0.0.0' }
$env:FRONTEND_PORT = if ($env:FRONTEND_PORT) { $env:FRONTEND_PORT } else { '8102' }
npm run dev
