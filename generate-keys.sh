#!/bin/bash

echo "=========================================="
echo "  Telus Challenge - Key Generation"
echo "=========================================="
echo ""

# Create sftp-keys directory if it doesn't exist
mkdir -p sftp-keys

# Generate SSH keys
echo "1. Generating SSH key pair (RSA-4096)..."
ssh-keygen -t rsa -b 4096 -f sftp-keys/telus_consumer_key -N "" -C "telus-consumer@telus.com"
echo "   ✓ SSH keys generated"

# Copy public key to authorized_keys
echo ""
echo "2. Creating authorized_keys file..."
cp sftp-keys/telus_consumer_key.pub sftp-keys/authorized_keys
echo "   ✓ authorized_keys created"

# Generate encryption key
echo ""
echo "3. Generating AES-256 encryption key..."
openssl rand -base64 32 > sftp-keys/encryption_key.txt
echo "   ✓ Encryption key generated"

# Display encryption key for docker-compose.yml
echo ""
echo "=========================================="
echo "  Keys Generated Successfully!"
echo "=========================================="
echo ""
echo "📋 Next Steps:"
echo ""
echo "1. Update docker-compose.yml with the encryption key:"
echo "   SFTP_ENCRYPTION_KEY=$(cat sftp-keys/encryption_key.txt)"
echo ""
echo "2. Start the services:"
echo "   docker-compose up --build -d"
echo ""
echo "⚠️  IMPORTANT: Keep these keys secure and NEVER commit them to git!"
echo ""
