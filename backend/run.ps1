# Loads backend/.env (gitignored -- see .env.example) into the process environment, then runs the
# packaged jar. application.yml has no working default for DB_PASSWORD on purpose, so this (or an
# equivalent) is required for local dev -- a real deployment should set these via its own secret
# store instead of this file.
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#=][^=]*)=(.*)$') {
            [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim())
        }
    }
} else {
    Write-Warning "backend/.env not found -- copy .env.example to .env and fill in DB_PASSWORD."
}

& java -jar (Join-Path $PSScriptRoot "target/backend-0.0.1-SNAPSHOT.jar")
