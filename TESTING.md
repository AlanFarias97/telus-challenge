# Testing Guide

## 📋 Overview

This project includes unit tests for critical components:
- **FileEncryptionUtil**: AES-256-GCM encryption/decryption
- **UserValidationProcessor**: Business rule validation
- **DepartmentEnrichmentProcessor**: Data enrichment

## 🧪 Running Tests

### Option 1: Using Maven (requires Java 21)

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=FileEncryptionUtilTest
./mvnw test -Dtest=UserValidationProcessorTest
./mvnw test -Dtest=DepartmentEnrichmentProcessorTest

# Run with coverage
./mvnw test jacoco:report
```

### Option 2: Using Docker (recommended)

```bash
# Build and run tests inside Docker
docker run --rm \
  -v $(pwd):/app \
  -w /app \
  eclipse-temurin:21-jdk-alpine \
  sh -c "./mvnw test"
```

### Option 3: Using IDE

1. Open project in IntelliJ IDEA / Eclipse / VSCode
2. Navigate to test class
3. Right-click → Run Test

## 📊 Test Coverage

### FileEncryptionUtilTest (8 tests)
- ✅ Key generation (AES-256)
- ✅ Key serialization/deserialization
- ✅ File encryption
- ✅ File decryption
- ✅ Encrypt → Decrypt round-trip
- ✅ Different keys produce different results
- ✅ Wrong key fails decryption
- ✅ Large file encryption (1KB+)

### UserValidationProcessorTest (9 tests)
- ✅ Valid user passes validation
- ✅ Invalid email format rejected
- ✅ Age < 18 rejected
- ✅ Age > 120 rejected
- ✅ Missing required fields rejected
- ✅ Empty email rejected
- ✅ Null email rejected
- ✅ Boundary age 18 accepted
- ✅ Boundary age 120 accepted

### DepartmentEnrichmentProcessorTest (8 tests)
- ✅ Known department enriched with code
- ✅ All 14 departments mapped correctly
- ✅ Unknown department → "UNKNOWN" code
- ✅ Null department → "UNKNOWN" code
- ✅ Empty department → "UNKNOWN" code
- ✅ Case-insensitive matching
- ✅ Original user data preserved
- ✅ Department code added

**Total: 25 unit tests**

## 🎯 Test Results (Expected)

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.challenge.telus.utils.FileEncryptionUtilTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Running com.challenge.telus.processors.UserValidationProcessorTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Running com.challenge.telus.processors.DepartmentEnrichmentProcessorTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

## 🔧 Troubleshooting

### Java version mismatch

**Error:** `release version 21 not supported`

**Solution:** Use Docker to run tests (see Option 2 above) or install Java 21:

```bash
# Check Java version
java -version

# Should show: openjdk version "21.x.x"
```

### Missing dependencies

```bash
# Clean and rebuild
./mvnw clean install -DskipTests

# Then run tests
./mvnw test
```

### Cannot find schema/csv files

Tests use `@TempDir` for temporary files and `classpath:` resources. Make sure:
- `src/main/resources/schemas/user-validation-schema.json` exists
- `src/main/resources/data/departments.csv` exists

## 📝 Test Structure

```
src/test/java/com/challenge/telus/
├── utils/
│   └── FileEncryptionUtilTest.java       # Encryption tests
└── processors/
    ├── UserValidationProcessorTest.java   # Validation tests
    └── DepartmentEnrichmentProcessorTest.java  # Enrichment tests
```

## 🚀 CI/CD Integration

These tests can be integrated into CI/CD pipelines:

```yaml
# Example GitHub Actions
- name: Run Tests
  run: ./mvnw test
  
- name: Upload Coverage
  uses: codecov/codecov-action@v3
```

## 📚 Future Test Improvements

Potential additions for comprehensive testing:
- Integration tests with Testcontainers (Kafka, SQLite)
- E2E tests with actual SFTP server
- Performance tests for large file processing
- Camel route tests with `CamelTestSupport`
- Mock-based tests for external API calls

---

**Note:** These tests focus on critical business logic (encryption, validation, enrichment) to demonstrate testing capabilities while keeping execution time reasonable.
