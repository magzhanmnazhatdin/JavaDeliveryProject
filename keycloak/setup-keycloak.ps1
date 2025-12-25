# Keycloak Setup Script for Delivery Platform (PowerShell)
# This script imports the delivery-realm configuration into Keycloak

$KEYCLOAK_URL = "http://localhost:8180"
$ADMIN_USER = "admin"
$ADMIN_PASSWORD = "admin"
$REALM_FILE = "$PSScriptRoot\delivery-realm.json"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Keycloak Setup for Delivery Platform" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# Wait for Keycloak to be ready
Write-Host "Waiting for Keycloak to be ready..."
$ready = $false
$attempts = 0
while (-not $ready -and $attempts -lt 30) {
    try {
        $response = Invoke-RestMethod -Uri "$KEYCLOAK_URL/health/ready" -Method Get -ErrorAction Stop
        $ready = $true
        Write-Host "Keycloak is ready!" -ForegroundColor Green
    } catch {
        Write-Host "Keycloak is not ready yet. Waiting... (attempt $($attempts + 1)/30)"
        Start-Sleep -Seconds 5
        $attempts++
    }
}

if (-not $ready) {
    Write-Host "ERROR: Keycloak did not become ready in time." -ForegroundColor Red
    exit 1
}

# Get admin token
Write-Host "Getting admin access token..."
$tokenBody = @{
    username = $ADMIN_USER
    password = $ADMIN_PASSWORD
    grant_type = "password"
    client_id = "admin-cli"
}

try {
    $tokenResponse = Invoke-RestMethod -Uri "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" `
        -Method Post `
        -ContentType "application/x-www-form-urlencoded" `
        -Body $tokenBody

    $TOKEN = $tokenResponse.access_token
    Write-Host "Admin token obtained successfully!" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Failed to get admin token. Please check admin credentials." -ForegroundColor Red
    Write-Host $_.Exception.Message
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $TOKEN"
    "Content-Type" = "application/json"
}

# Check if realm already exists
Write-Host "Checking if delivery-realm already exists..."
try {
    $realmCheck = Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/delivery-realm" `
        -Method Get `
        -Headers $headers `
        -ErrorAction Stop

    Write-Host "Realm 'delivery-realm' already exists. Deleting it first..." -ForegroundColor Yellow
    Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/delivery-realm" `
        -Method Delete `
        -Headers $headers
    Write-Host "Existing realm deleted." -ForegroundColor Green
} catch {
    Write-Host "Realm does not exist. Proceeding with import..." -ForegroundColor Gray
}

# Import realm
Write-Host "Importing delivery-realm..."
try {
    $realmJson = Get-Content -Path $REALM_FILE -Raw

    Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms" `
        -Method Post `
        -Headers $headers `
        -Body $realmJson `
        -ErrorAction Stop

    Write-Host "Realm 'delivery-realm' imported successfully!" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Failed to import realm." -ForegroundColor Red
    Write-Host $_.Exception.Message
    exit 1
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Keycloak Admin Console: $KEYCLOAK_URL" -ForegroundColor White
Write-Host "Realm: delivery-realm" -ForegroundColor White
Write-Host ""
Write-Host "Test Users:" -ForegroundColor Yellow
Write-Host "  - customer1 / password123 (CUSTOMER role)"
Write-Host "  - restaurant_owner1 / password123 (RESTAURANT_OWNER role)"
Write-Host "  - courier1 / password123 (COURIER role)"
Write-Host "  - admin / admin123 (ADMIN role)"
Write-Host ""
Write-Host "OAuth2 Endpoints:" -ForegroundColor Yellow
Write-Host "  - Token: $KEYCLOAK_URL/realms/delivery-realm/protocol/openid-connect/token"
Write-Host "  - Auth: $KEYCLOAK_URL/realms/delivery-realm/protocol/openid-connect/auth"
Write-Host "  - JWKS: $KEYCLOAK_URL/realms/delivery-realm/protocol/openid-connect/certs"
Write-Host ""

# Test getting a token for customer1
Write-Host "Testing token retrieval for customer1..." -ForegroundColor Yellow
$testTokenBody = @{
    username = "customer1"
    password = "password123"
    grant_type = "password"
    client_id = "delivery-app"
}

try {
    $testTokenResponse = Invoke-RestMethod -Uri "$KEYCLOAK_URL/realms/delivery-realm/protocol/openid-connect/token" `
        -Method Post `
        -ContentType "application/x-www-form-urlencoded" `
        -Body $testTokenBody

    Write-Host "Token test successful! Access token received." -ForegroundColor Green
    Write-Host ""
    Write-Host "Sample Access Token (first 50 chars):" -ForegroundColor Gray
    Write-Host $testTokenResponse.access_token.Substring(0, [Math]::Min(50, $testTokenResponse.access_token.Length)) + "..."
} catch {
    Write-Host "Token test failed: $($_.Exception.Message)" -ForegroundColor Red
}
