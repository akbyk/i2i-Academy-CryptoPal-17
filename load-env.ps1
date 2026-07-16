Get-Content .env | ForEach-Object {
    if ($_ -and -not $_.StartsWith("#")) {
        $name, $value = $_.Split("=", 2)
        if ($name -and $value) {
            [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim())
        }
    }
}
[System.Environment]::SetEnvironmentVariable("POSTGRES_URL", "jdbc:postgresql://localhost:5432/cryptopal")
[System.Environment]::SetEnvironmentVariable("REDIS_HOST", "localhost")
Write-Host "Environment variables loaded with localhost overrides!" -ForegroundColor Green
