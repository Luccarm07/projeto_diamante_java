# Requires PowerShell 7+. Sends 300 reviews at once, waits for the flush and shows the ranking.
param([string]$OrderUrl = "http://localhost:8080", [string]$ReviewUrl = "http://localhost:8083")

$codes = 1..300 | ForEach-Object -Parallel {
    $dish = ($_ % 5) + 1; $rating = ($_ % 5) + 1
    $body = "{`"dishId`":$dish,`"rating`":$rating,`"comment`":`"load test`"}"
    (Invoke-WebRequest -Uri "$($using:OrderUrl)/reviews" -Method Post `
        -ContentType "application/json" -Body $body -SkipHttpErrorCheck).StatusCode
} -ThrottleLimit 50
$codes | Group-Object | Format-Table @{n='Status';e={$_.Name}}, Count
Write-Host "Waiting for the scheduled flush..."
Start-Sleep -Seconds 6
Invoke-RestMethod "$ReviewUrl/reviews/ranking" | Format-Table
