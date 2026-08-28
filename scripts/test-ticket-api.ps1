<#
.SYNOPSIS
    Manual/regression test script for the Ticket API (ticket-service).

.DESCRIPTION
    Runs through the full set of scenarios covered in docs/api/tickets.md: auth, create
    (including Day 11's routing and Day 12's per-category metadata requirements), get,
    list, update, and status changes - all under Day 13's authorization rules (owner vs
    agent/admin, and list scoping). Prints the HTTP status and body for each call so you
    can read exactly what happened.

    Needs three Keycloak identities to actually prove the Day 13 rules: test-customer
    (owns the tickets used below), test-agent (elevated - can touch anything), and
    test-customer-2 (a second, unrelated customer - proves cross-owner denial with a real
    token, not just an assertion). All three are local-only fixtures defined in
    infrastructure/docker/keycloak/realm-export/helpdesk-realm.json.

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

# A JWT's payload is just base64url - decode it locally to read the "sub" claim without
# needing to call anything, so the list-scoping checks below can assert against the real
# identity a token represents instead of just trusting the endpoint blindly.
function Get-JwtSubject {
    param([string]$Token)
    $payload = $Token.Split('.')[1].Replace('-', '+').Replace('_', '/')
    switch ($payload.Length % 4) { 2 { $payload += '==' } 3 { $payload += '=' } }
    $json = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload))
    return ($json | ConvertFrom-Json).sub
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

# --- 1. Tokens for all three identities (see docs/architecture/keycloak-setup.md) ---
$customerToken  = Get-TestToken -Username "test-customer"   -Password "customer-local-dev-123"
$customer2Token = Get-TestToken -Username "test-customer-2" -Password "customer2-local-dev-123"
$agentToken     = Get-TestToken -Username "test-agent"      -Password "agent-local-dev-123"
$customerHeaders  = @{ Authorization = "Bearer $customerToken" }
$customer2Headers = @{ Authorization = "Bearer $customer2Token" }
$agentHeaders     = @{ Authorization = "Bearer $agentToken" }
$customerSub = Get-JwtSubject -Token $customerToken
Write-Host "Got all three tokens. test-customer sub = $customerSub" -ForegroundColor Green

# --- 2. No token -> expect 401 ---
Show-Response -Label "No token (expect 401)" -Request { Invoke-RestMethod -Uri $ApiBase -Method Get } | Out-Null

# --- 3. Create a ticket as test-customer (ACCESS - no metadata required) -> expect 201 ---
$newTicket = @{
    subject     = "Can't log in"
    description = "Password reset email never arrives"
    category    = "ACCESS"
} | ConvertTo-Json
$created = Show-Response -Label "Create ACCESS ticket (expect 201, routedTeam=support)" -Request {
    Invoke-RestMethod -Uri $ApiBase -Method Post -Headers $customerHeaders -Body $newTicket -ContentType "application/json"
}

# --- 4. Create with invalid body -> expect 400 ---
$badTicket = @{ subject = ""; description = "x"; category = "ACCESS" } | ConvertTo-Json
Show-Response -Label "Create with blank subject (expect 400)" -Request {
    Invoke-RestMethod -Uri $ApiBase -Method Post -Headers $customerHeaders -Body $badTicket -ContentType "application/json"
} | Out-Null

# --- 5. Create a BUG ticket with no metadata -> expect 400 naming the missing keys (Factory pattern, Day 12) ---
$bugNoMetadata = @{ subject = "App crashes on save"; description = "Stack trace attached"; category = "BUG" } | ConvertTo-Json
Show-Response -Label "Create BUG with no metadata (expect 400: browser, appVersion)" -Request {
    Invoke-RestMethod -Uri $ApiBase -Method Post -Headers $customerHeaders -Body $bugNoMetadata -ContentType "application/json"
} | Out-Null

# --- 6. Create a BUG ticket with metadata -> expect 201, routedTeam=engineering ---
$bugWithMetadata = @{
    subject     = "App crashes on save"
    description = "Stack trace attached"
    category    = "BUG"
    metadata    = @{ browser = "Chrome"; appVersion = "1.2.3" }
} | ConvertTo-Json
Show-Response -Label "Create BUG with metadata (expect 201, routedTeam=engineering)" -Request {
    Invoke-RestMethod -Uri $ApiBase -Method Post -Headers $customerHeaders -Body $bugWithMetadata -ContentType "application/json"
} | Out-Null

# --- 7. Create a BILLING ticket with only invoiceId -> expect 201, currency defaulted to USD ---
$billing = @{
    subject     = "Overcharged this month"
    description = "Billed twice"
    category    = "BILLING"
    metadata    = @{ invoiceId = "INV-9001" }
} | ConvertTo-Json
Show-Response -Label "Create BILLING with invoiceId only (expect 201, currency=USD default)" -Request {
    Invoke-RestMethod -Uri $ApiBase -Method Post -Headers $customerHeaders -Body $billing -ContentType "application/json"
} | Out-Null

if ($created) {
    $id = $created.id

    # --- 8. Owner reads their own ticket -> expect 200 ---
    Show-Response -Label "Get by id as owner (expect 200)" -Request {
        Invoke-RestMethod -Uri "$ApiBase/$id" -Headers $customerHeaders
    } | Out-Null

    # --- 9. A different customer reads someone else's ticket -> expect 403 (Day 13) ---
    Show-Response -Label "Get by id as non-owner customer (expect 403)" -Request {
        Invoke-RestMethod -Uri "$ApiBase/$id" -Headers $customer2Headers
    } | Out-Null

    # --- 10. An agent reads any ticket -> expect 200 ---
    Show-Response -Label "Get by id as agent (expect 200)" -Request {
        Invoke-RestMethod -Uri "$ApiBase/$id" -Headers $agentHeaders
    } | Out-Null

    # --- 11. Unknown id as agent -> expect 404 ---
    Show-Response -Label "Get unknown id as agent (expect 404)" -Request {
        Invoke-RestMethod -Uri "$ApiBase/00000000-0000-0000-0000-000000000000" -Headers $agentHeaders
    } | Out-Null

    # --- 12. Unknown id as customer -> expect 403, not 404 (ownership can't be proven either way - see docs/api/tickets.md) ---
    Show-Response -Label "Get unknown id as customer (expect 403, masked)" -Request {
        Invoke-RestMethod -Uri "$ApiBase/00000000-0000-0000-0000-000000000000" -Headers $customerHeaders
    } | Out-Null

    # --- 13. List as the owning customer -> expect only their own tickets ---
    $customerList = Show-Response -Label "List as customer (expect only their own tickets)" -Request {
        Invoke-RestMethod -Uri "$ApiBase`?page=0&size=50" -Headers $customerHeaders
    }
    if ($customerList) {
        $foreign = $customerList.content | Where-Object { $_.requesterId -ne $customerSub }
        if ($foreign) {
            Write-Host "FAIL: list as customer returned a ticket not owned by them!" -ForegroundColor Red
        } else {
            Write-Host "OK: all $($customerList.content.Count) tickets in the list belong to test-customer" -ForegroundColor Green
        }
    }

    # --- 14. List as an agent -> expect every ticket, not just one requester's ---
    $agentList = Show-Response -Label "List as agent (expect every ticket)" -Request {
        Invoke-RestMethod -Uri "$ApiBase`?page=0&size=50" -Headers $agentHeaders
    }
    if ($agentList -and $customerList) {
        Write-Host "OK: agent sees $($agentList.totalElements) total vs customer's $($customerList.totalElements) own" -ForegroundColor Green
    }

    # --- 15. Owner updates their own ticket -> expect 200 ---
    $update = @{ subject = "Can't log in - still broken"; description = "Tried again, still no email" } | ConvertTo-Json
    Show-Response -Label "Update as owner (expect 200)" -Request {
        Invoke-RestMethod -Uri "$ApiBase/$id" -Method Put -Headers $customerHeaders -Body $update -ContentType "application/json"
    } | Out-Null

    # --- 16. A different customer tries to update someone else's ticket -> expect 403 ---
    $hijack = @{ subject = "HIJACKED"; description = "should not be allowed" } | ConvertTo-Json
    Show-Response -Label "Update as non-owner customer (expect 403)" -Request {
        Invoke-RestMethod -Uri "$ApiBase/$id" -Method Put -Headers $customer2Headers -Body $hijack -ContentType "application/json"
    } | Out-Null

    # --- 17. Owner tries to change status -> expect 403 (agent/admin only, Day 13) ---
    $legalChange = @{ status = "AI_TRIAGED" } | ConvertTo-Json
    Show-Response -Label "Change status as owner (expect 403 - customers can't drive workflow)" -Request {
        Invoke-RestMethod -Uri "$ApiBase/$id/status" -Method Patch -Headers $customerHeaders -Body $legalChange -ContentType "application/json"
    } | Out-Null

    # --- 18. Agent makes a legal status change: OPEN -> AI_TRIAGED -> expect 200 ---
    Show-Response -Label "Change status as agent, OPEN->AI_TRIAGED (expect 200)" -Request {
        Invoke-RestMethod -Uri "$ApiBase/$id/status" -Method Patch -Headers $agentHeaders -Body $legalChange -ContentType "application/json"
    } | Out-Null

    # --- 19. Agent attempts an illegal status change: AI_TRIAGED -> RESOLVED -> expect 409 ---
    $illegalChange = @{ status = "RESOLVED" } | ConvertTo-Json
    Show-Response -Label "Change status as agent, AI_TRIAGED->RESOLVED (expect 409)" -Request {
        Invoke-RestMethod -Uri "$ApiBase/$id/status" -Method Patch -Headers $agentHeaders -Body $illegalChange -ContentType "application/json"
    } | Out-Null
}

Write-Host "`nDone." -ForegroundColor Green
