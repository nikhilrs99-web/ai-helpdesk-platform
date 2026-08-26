<#
.SYNOPSIS
    Manual/regression test script for the Ticket API (ticket-service).

.DESCRIPTION
    Runs through the full set of scenarios covered in docs/api/tickets.md and the Day 10
    build log: auth, create, get, list, update, and both a legal and an illegal status
    change. Prints the HTTP status and body for each call so you can read exactly what
    happened.

.PREREQUISITES
    1. Docker infra running:      docker compose up -d   (from the repo root)
    2. ticket-service running:    mvn -pl services/ticket-service spring-boot:run
       (or run TicketServiceApplication directly from IntelliJ)

.USAGE
    Run the whole thing:          .\scripts\test-ticket-api.ps1
    Or open this file and run individual sections by hand in your own PowerShell
    session - copy a section below into your terminal to explore manually.
#>

$KeycloakUrl = "http://localhost:8080"
$Realm       = "helpdesk"
$ClientId    = "helpdesk-frontend"
$ApiBase     = "http://localhost:8081/api/tickets"

function Get-TestToken {
    param([string]$Username, [string]$Password)
    $body = @{
        client_id  = $ClientId
        grant_type = "password"
        username   = $Username
        password   = $Password
    }
    $response = Invoke-RestMethod -Uri "$KeycloakUrl/realms/$Realm/protocol/openid-connect/token" -Method Post -Body $body
    return $response.access_token
}

# Wraps Invoke-RestMethod so every call - success or failure - prints its result clearly
# and consistently (Write-Host, not bare pipeline output, so it always shows regardless of
# whether the caller captures the return value), and returns the parsed body for later steps.
function Show-Response {
    param([string]$Label, [scriptblock]$Request)
    Write-Host "`n=== $Label ===" -ForegroundColor Cyan
    try {
        $result = & $Request
        Write-Host "HTTP 2xx (success)" -ForegroundColor Green
        Write-Host ($result | ConvertTo-Json -Depth 6)
        return $result
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        Write-Host "HTTP $status" -ForegroundColor Yellow
        Write-Host $_.ErrorDetails.Message
        return $null
    }
}

# --- 1. Get a token for the test customer (see .env / docs/architecture/keycloak-setup.md) ---
$token = Get-TestToken -Username "test-customer" -Password "customer-local-dev-123"
$headers = @{ Authorization = "Bearer $token" }
Write-Host "Got token, length $($token.Length)" -ForegroundColor Green

# --- 2. No token -> expect 401 ---
Show-Response -Label "No token (expect 401)" -Request { Invoke-RestMethod -Uri $ApiBase -Method Get } | Out-Null

# --- 3. Create a ticket -> expect 201 ---
$newTicket = @{
    subject     = "Can't log in"
    description = "Password reset email never arrives"
    category    = "ACCESS"
} | ConvertTo-Json
$created = Show-Response -Label "Create ticket (expect 201)" -Request {
    Invoke-RestMethod -Uri $ApiBase -Method Post -Headers $headers -Body $newTicket -ContentType "application/json"
}

# --- 4. Create with invalid body -> expect 400 ---
$badTicket = @{ subject = ""; description = "x"; category = "ACCESS" } | ConvertTo-Json
Show-Response -Label "Create with blank subject (expect 400)" -Request {
    Invoke-RestMethod -Uri $ApiBase -Method Post -Headers $headers -Body $badTicket -ContentType "application/json"
} | Out-Null

if ($created) {
    $id = $created.id

    # --- 5. Get by id -> expect 200 ---
    Show-Response -Label "Get by id (expect 200)" -Request { Invoke-RestMethod -Uri "$ApiBase/$id" -Headers $headers } | Out-Null

    # --- 6. Get a non-existent id -> expect 404 ---
    Show-Response -Label "Get unknown id (expect 404)" -Request {
        Invoke-RestMethod -Uri "$ApiBase/00000000-0000-0000-0000-000000000000" -Headers $headers
    } | Out-Null

    # --- 7. List, paginated -> expect 200 ---
    Show-Response -Label "List page 0, size 5 (expect 200)" -Request {
        Invoke-RestMethod -Uri "$ApiBase`?page=0&size=5" -Headers $headers
    } | Out-Null

    # --- 8. Update subject/description -> expect 200 ---
    $update = @{ subject = "Can't log in - still broken"; description = "Tried again, still no email" } | ConvertTo-Json
    Show-Response -Label "Update ticket (expect 200)" -Request {
        Invoke-RestMethod -Uri "$ApiBase/$id" -Method Put -Headers $headers -Body $update -ContentType "application/json"
    } | Out-Null

    # --- 9. Legal status change: OPEN -> AI_TRIAGED -> expect 200 ---
    $legalChange = @{ status = "AI_TRIAGED" } | ConvertTo-Json
    Show-Response -Label "Change status OPEN->AI_TRIAGED (expect 200)" -Request {
        Invoke-RestMethod -Uri "$ApiBase/$id/status" -Method Patch -Headers $headers -Body $legalChange -ContentType "application/json"
    } | Out-Null

    # --- 10. Illegal status change: AI_TRIAGED -> RESOLVED -> expect 409 ---
    $illegalChange = @{ status = "RESOLVED" } | ConvertTo-Json
    Show-Response -Label "Change status AI_TRIAGED->RESOLVED (expect 409)" -Request {
        Invoke-RestMethod -Uri "$ApiBase/$id/status" -Method Patch -Headers $headers -Body $illegalChange -ContentType "application/json"
    } | Out-Null
}

Write-Host "`nDone." -ForegroundColor Green
