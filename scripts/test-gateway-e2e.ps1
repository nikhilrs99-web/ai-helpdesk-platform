<#
.SYNOPSIS
    End-to-end smoke test for the full request path: Keycloak -> API Gateway -> ticket-service.

.DESCRIPTION
    Unlike test-ticket-api.ps1 (which talks to ticket-service directly on :8081 and exercises
    its business rules in depth), this script goes through the gateway on :8000. Its job is
    narrower: prove that a browser-issued JWT (iss=http://localhost:8080/...) is actually
    accepted by both the gateway and ticket-service, and that the gateway's routes reach the
    right containers. This is the path that broke when the JWK set couldn't be fetched over
    Docker's internal network - see the issuer-uri / jwk-set-uri split in each service's
    application.yml.

.PREREQUISITES
    Full stack running:   docker compose up -d --build   (from the repo root)

.USAGE
    .\scripts\test-gateway-e2e.ps1
#>

$KeycloakUrl = "http://localhost:8080"
$Realm       = "helpdesk"
$ClientId    = "helpdesk-frontend"
$GatewayBase = "http://localhost:8000/api/tickets"

Write-Host "Authenticating with Keycloak as test-customer..." -ForegroundColor Cyan
$tokenResponse = Invoke-RestMethod -Uri "$KeycloakUrl/realms/$Realm/protocol/openid-connect/token" -Method Post -Body @{
    client_id  = $ClientId
    grant_type = "password"
    username   = "test-customer"
    password   = "customer-local-dev-123"
}
$headers = @{ Authorization = "Bearer $($tokenResponse.access_token)" }
Write-Host "Got JWT (iss should be $KeycloakUrl/realms/$Realm)" -ForegroundColor Green

$ticket = @{
    subject     = "Cannot access the VPN"
    description = "VPN client says connection refused after the latest Windows update."
    category    = "ACCESS"
} | ConvertTo-Json

Write-Host "`nPOST $GatewayBase (through the gateway, expect 201)..." -ForegroundColor Cyan
try {
    $created = Invoke-RestMethod -Uri $GatewayBase -Method Post -Headers $headers -Body $ticket -ContentType "application/json"
    Write-Host "Ticket created via gateway:" -ForegroundColor Green
    $created | ConvertTo-Json -Depth 6 | Write-Host
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    Write-Host "FAILED - HTTP $status" -ForegroundColor Red
    Write-Host $_.ErrorDetails.Message
    exit 1
}
