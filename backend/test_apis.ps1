$BaseUrl = "http://localhost:5000" # Update port if different

Write-Host "--- Testing Registration ---" -ForegroundColor Cyan
$regBody = @{
    companyName = "Test Company"
    contactPhone = "1234567890"
    email = "admin@test.com"
    password = "Password123"
} | ConvertTo-Json

try {
    $regRes = Invoke-RestMethod -Uri "$BaseUrl/api/auth/register" -Method Post -Body $regBody -ContentType "application/json"
    $regRes | Format-List
} catch {
    Write-Error "Registration failed: $_"
}

Write-Host "`n--- Testing Login ---" -ForegroundColor Cyan
$loginBody = @{
    email = "admin@test.com"
    password = "Password123"
} | ConvertTo-Json

try {
    $loginRes = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $loginRes | Format-List
} catch {
    Write-Error "Login failed: $_"
}
