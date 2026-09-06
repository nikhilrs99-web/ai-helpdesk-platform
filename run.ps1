Write-Host "Checking if Docker is running..."
docker info > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker is not running! Please start Docker Desktop and try again." -ForegroundColor Red
    exit 1
}

Write-Host "Docker is running. Building and starting the AI Helpdesk Platform..." -ForegroundColor Green
docker-compose up --build -d

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Platform is starting up! This may take a few minutes." -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Frontend UI: http://localhost:3000"
Write-Host "API Gateway: http://localhost:8000"
Write-Host "Keycloak:    http://localhost:8080"
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "To view logs, run: docker-compose logs -f"
