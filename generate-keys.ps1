# Telus Challenge - Key Generation Script (PowerShell)

Write-Host "==========================================" -ForegroundColor Green
Write-Host "  Telus Challenge - Key Generation" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""

# Create sftp-keys directory if it doesn't exist
if (!(Test-Path "sftp-keys")) {
    New-Item -ItemType Directory -Path "sftp-keys" | Out-Null
}

# Generate SSH keys
Write-Host "1. Generating SSH key pair (RSA-4096)..." -ForegroundColor Cyan
ssh-keygen -t rsa -b 4096 -f sftp-keys/telus_consumer_key -N '""' -C "telus-consumer@telus.com" 2>&1 | Out-Null
Write-Host "   ✓ SSH keys generated" -ForegroundColor Green

# Copy public key to authorized_keys
Write-Host ""
Write-Host "2. Creating authorized_keys file..." -ForegroundColor Cyan
Copy-Item -Path "sftp-keys\telus_consumer_key.pub" -Destination "sftp-keys\authorized_keys"
Write-Host "   ✓ authorized_keys created" -ForegroundColor Green

# Generate encryption key
Write-Host ""
Write-Host "3. Generating AES-256 encryption key..." -ForegroundColor Cyan
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$key = [Convert]::ToBase64String($bytes)
$key | Out-File -FilePath "sftp-keys\encryption_key.txt" -Encoding ASCII
Write-Host "   ✓ Encryption key generated" -ForegroundColor Green

# Display encryption key
Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "  Keys Generated Successfully!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Next Steps:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Update docker-compose.yml with the encryption key:" -ForegroundColor White
Write-Host "   SFTP_ENCRYPTION_KEY=$key" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. Start the services:" -ForegroundColor White
Write-Host "   docker-compose up --build -d" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  IMPORTANT: Keep these keys secure and NEVER commit them to git!" -ForegroundColor Red
Write-Host ""
