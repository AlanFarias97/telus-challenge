# SFTP Keys Directory

This directory contains SSH keys for SFTP authentication and encryption keys for file encryption.

## ⚠️ IMPORTANT: Security Notice

**The actual keys are NOT included in this repository for security reasons.**

You need to generate them before running the project.

## 🔑 How to Generate SSH Keys

Run the following command from the project root:

```bash
# Generate RSA-4096 SSH key pair
ssh-keygen -t rsa -b 4096 -f sftp-keys/telus_consumer_key -N "" -C "telus-consumer@telus.com"

# Copy public key to authorized_keys
cp sftp-keys/telus_consumer_key.pub sftp-keys/authorized_keys
```

**On Windows (PowerShell):**
```powershell
ssh-keygen -t rsa -b 4096 -f sftp-keys/telus_consumer_key -N '""' -C "telus-consumer@telus.com"
Copy-Item -Path sftp-keys\telus_consumer_key.pub -Destination sftp-keys\authorized_keys
```

## 🔐 How to Generate Encryption Key

```bash
# Linux/Mac
openssl rand -base64 32 > sftp-keys/encryption_key.txt
```

**On Windows (PowerShell):**
```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes) | Out-File -FilePath sftp-keys\encryption_key.txt -Encoding ASCII
```

## 📁 Expected Files After Generation

After running the commands above, this directory should contain:

- `telus_consumer_key` - Private SSH key (RSA-4096) - **KEEP SECRET**
- `telus_consumer_key.pub` - Public SSH key
- `authorized_keys` - Copy of public key for SFTP server
- `encryption_key.txt` - AES-256 encryption key (Base64 encoded) - **KEEP SECRET**

## 🚀 Quick Setup Script

```bash
# Run this script from the project root to generate all keys
./generate-keys.sh
```

Or manually run the commands above.

## 🔒 Security Best Practices

1. **NEVER commit private keys to git**
2. **NEVER share encryption keys**
3. In production, use:
   - AWS KMS or Azure Key Vault for key management
   - HashiCorp Vault for secrets management
   - Kubernetes Secrets for container orchestration
4. Rotate keys regularly (every 90 days recommended)
5. Use different keys for each environment (dev/staging/prod)

## 📝 Notes

- Keys are automatically excluded from git via `.gitignore`
- The encryption key is used for AES-256-GCM encryption
- SSH keys are used for SFTP authentication (no passwords)
- All keys are mounted as read-only volumes in Docker containers
