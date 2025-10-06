# Telus Challenge - Executive Summary
## Secure ETL Data Pipeline with Apache Camel

**Author:** Alan Farias  
**Repository:** [github.com/AlanFarias97/telus-challenge](https://github.com/AlanFarias97/telus-challenge)  
**Technologies:** Java 21, Spring Boot, Apache Camel, Kafka, Docker

---

## 📋 Project Overview

Production-ready ETL (Extract, Transform, Load) pipeline for user data processing with **enterprise-grade security features**. The system extracts data from a REST API, validates and transforms it, and securely uploads encrypted files to SFTP using SSH key authentication.

### Key Features

✅ **Resilient API Extraction** - Paginated extraction with retry logic and persistent state  
✅ **Data Validation & Transformation** - JSON schema validation, business rules, and enrichment  
✅ **Kafka-Based Messaging** - Decoupled architecture for scalability and resilience  
✅ **Database Metadata Storage** - Complete audit trail with SQLite  
✅ **AES-256-GCM Encryption** - All files encrypted before upload (MANDATORY)  
✅ **SSH Key Authentication** - RSA-4096 for secure SFTP access (MANDATORY)  
✅ **Docker Orchestration** - Complete containerized environment  
✅ **Dead Letter Queue** - Invalid records handled separately for debugging  

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                 PRODUCER (Deployment 1)                 │
│                                                         │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐        │
│  │ Phase 1  │───▶│ Phase 2  │───▶│  Kafka   │        │
│  │Extractor │    │Transform │    │ Producer │        │
│  └──────────┘    └──────────┘    └──────────┘        │
│       │               │                │               │
│       ▼               ▼                ▼               │
│  raw_users/   processed_users/   Kafka Topic          │
│                  dlq/                                  │
└─────────────────────────────────────────────────────────┘
                        │
                        │ Apache Kafka
                        ▼
┌─────────────────────────────────────────────────────────┐
│                 CONSUMER (Deployment 2)                 │
│                                                         │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐          │
│  │  Kafka   │──▶│ SQLite   │──▶│ Encrypt  │          │
│  │ Consumer │   │ Metadata │   │AES-256-GCM│          │
│  └──────────┘   └──────────┘   └──────────┘          │
│                                      │                  │
│                                      ▼                  │
│                            ┌──────────────┐            │
│                            │ SFTP Upload  │            │
│                            │ (SSH Keys)   │            │
│                            └──────────────┘            │
└─────────────────────────────────────────────────────────┘
```

---

## 🔐 Security Implementation (MANDATORY Requirements)

### 1. File Encryption (AES-256-GCM)
- **Algorithm:** AES-256 with Galois/Counter Mode
- **When:** All files encrypted before SFTP upload
- **Guarantees:** 
  - Confidentiality (data cannot be read without key)
  - Integrity (tampering detected)
  - Authentication (verifies data source)

### 2. SSH Key Authentication (RSA-4096)
- **Key Type:** RSA-4096 bit
- **Authentication:** Public/private key pair (no passwords)
- **Security:** Private key mounted read-only in container

---

## 🚀 Quick Start Guide

### Step 1: Clone Repository
```bash
git clone https://github.com/AlanFarias97/telus-challenge.git
cd telus-challenge
```

### Step 2: Generate Security Keys
```bash
# Linux/Mac
./generate-keys.sh

# Windows
.\generate-keys.ps1
```

### Step 3: Update Configuration
Edit `docker-compose.yml` and set the encryption key from `sftp-keys/encryption_key.txt`:
```yaml
SFTP_ENCRYPTION_KEY=<your-generated-key>
```

### Step 4: Start Services
```bash
docker-compose up --build -d
```

### Step 5: Trigger Extraction
```bash
curl -X POST http://localhost:8080/api/extraction/trigger
```

### Step 6: Verify (wait 30 seconds)
```bash
# Check encrypted files in SFTP
docker exec telus-sftp ls -lh /home/testuser/uploads/*.enc

# Check logs
docker-compose logs telus-consumer | grep "File encryption"
```

**Expected Results:**
- ✅ Multiple `.enc` files in SFTP
- ✅ Log: "File encryption enabled (AES-256-GCM)"
- ✅ Log: "Using SSH Key Authentication"
- ✅ Database records with `uploaded_to_sftp = 1`

---

## 📊 Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Language | Java 21 | Modern Java features |
| Framework | Spring Boot 3.x | Application foundation |
| Integration | Apache Camel 4.7 | ETL orchestration |
| Messaging | Apache Kafka | Asynchronous communication |
| Database | SQLite | Metadata persistence |
| Encryption | AES-256-GCM | File encryption |
| Authentication | SSH RSA-4096 | SFTP security |
| Containerization | Docker Compose | Service orchestration |

---

## 📈 Data Flow Explained

### Phase 1: Extraction
- Fetches users from REST API (`https://dummyjson.com/users`)
- Handles pagination (100 users per page)
- Persistent state for recovery
- Output: `raw_users/records_*.jsonl`

### Phase 2: Transformation
- Validates against JSON schema
- Applies business rules (age, email, etc.)
- Enriches with department codes
- Splits valid/invalid records
- Output: `processed_users/etl_*.jsonl` + `dlq/invalid_*.jsonl`

### Phase 3: Kafka Messaging
- Sends file metadata to Kafka topic
- Includes record counts (valid/invalid/total)
- Decouples producer from consumer

### Phase 4: Database Persistence
- Consumer stores file metadata in SQLite
- Tracks processing timestamps
- Records upload status

### Phase 5: Encryption
- AES-256-GCM encryption applied
- Random IV per file
- Original files remain on producer
- Output: `*.jsonl.enc`

### Phase 6: SFTP Upload
- SSH key authentication
- Uploads encrypted files only
- Automatic cleanup of temporary files
- Updates database status

---

## 🎯 Key Achievements

✅ **Complete 3-Phase Pipeline** - Extract, Transform, Load  
✅ **Production-Ready Security** - Encryption + SSH keys  
✅ **Scalable Architecture** - Independent producer/consumer  
✅ **Resilient Design** - Retries, DLQ, persistent state  
✅ **Full Documentation** - Professional English README  
✅ **Automated Setup** - Key generation scripts  
✅ **Docker Environment** - One-command deployment  
✅ **Audit Trail** - Complete metadata in database  

---

## 📚 Documentation & Resources

- **Full README:** [github.com/AlanFarias97/telus-challenge](https://github.com/AlanFarias97/telus-challenge)
- **Architecture Details:** See README.md sections on data flow
- **Security Guide:** `sftp-keys/README.md`
- **Troubleshooting:** README includes common issues and solutions
- **API Reference:** Producer exposes REST endpoint for manual triggers

---

## 🔍 Verification Commands

### Check All Services Running
```bash
docker-compose ps
# Expected: 6 services "Up"
```

### Check Encrypted Files
```bash
docker exec telus-sftp ls -lh /home/testuser/uploads/*.enc | wc -l
# Expected: Multiple files
```

### Check Database Records
```bash
docker exec telus-consumer sh -c "sqlite3 /app/data/telus.db \
  'SELECT filename, total_records, uploaded_to_sftp \
   FROM processed_files ORDER BY created_at DESC LIMIT 5;'"
```

### Check Kafka Messages
```bash
docker exec telus-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic processed-users \
  --from-beginning --max-messages 2
```

---

## 📝 Project Statistics

- **Lines of Code:** 4,086+
- **Java Classes:** 35+
- **Docker Services:** 6 (Producer, Consumer, Kafka, Zookeeper, SFTP, SQLite)
- **Security Features:** 2 mandatory (Encryption + SSH Keys)
- **Test Coverage:** Manual verification with detailed steps

---

## 💡 Design Decisions

### Why Kafka?
- **Decoupling:** Producer and Consumer are completely independent
- **Resilience:** Messages persist if Consumer crashes
- **Scalability:** Multiple consumers can process in parallel

### Why JSONL Files?
- **Data Lake Pattern:** Immutable raw files for audit
- **Reprocessing:** Can re-run transformations with different logic
- **Performance:** Sequential writes faster than database inserts

### Why Separate Producer/Consumer?
- **Independent Scaling:** Different resource needs
- **Fault Isolation:** SFTP issues don't block extraction
- **Clear Separation:** Single responsibility principle

---

## 🏆 Conclusion

This implementation demonstrates a **production-ready ETL pipeline** with enterprise-grade security features. The system successfully meets all MANDATORY requirements (encryption and SSH key authentication) while providing a scalable, resilient architecture suitable for real-world data processing scenarios.

The codebase is fully documented in English, includes comprehensive testing instructions, and can be deployed with a single command using Docker Compose.

---

**For questions or evaluation, see the full documentation in the GitHub repository.**

**Repository:** https://github.com/AlanFarias97/telus-challenge
