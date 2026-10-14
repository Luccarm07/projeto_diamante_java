# Requires PowerShell 7+ (ForEach-Object -Parallel).
# Fires 50 simultaneous POST /orders for the flash-sale dish (id 1) and counts the status codes.
param([string]$BaseUrl = "http://localhost:8080")

Write-Host "Stock before:" (Invoke-RestMethod "$BaseUrl/dishes/1").stock
$codes = 1..50 | ForEach-Object -Parallel {
    $r = Invoke-WebRequest -Uri "$($using:BaseUrl)/orders" -Method Post `
        -ContentType "application/json" -Body '{"dishId":1,"quantity":1}' -SkipHttpErrorCheck
    $r.StatusCode
} -ThrottleLimit 50
$codes | Group-Object | Sort-Object Name | Format-Table @{n='Status';e={$_.Name}}, Count
Write-Host "Stock after:" (Invoke-RestMethod "$BaseUrl/dishes/1").stock
