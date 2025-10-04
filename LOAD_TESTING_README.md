# Load Testing with Java Driver

**Real-world transaction simulation with configurable data sizes and concurrency**

---

## 🎯 What This Does

Sends **actual transactions** through BigchainDB using the Java driver to test SHACL validation cache performance with:

✅ **Configurable transaction counts**  
✅ **Variable payload sizes (1KB - 100KB+)**  
✅ **Multiple transaction types** (CREATE, ADVERTISEMENT, BUY_OFFER, SELL)  
✅ **Concurrent/parallel testing**  
✅ **Real-world marketplace flows**  
✅ **Cache effectiveness measurements**  

---

## 🚀 Quick Start

### Prerequisites

1. **BigchainDB services running:**
   ```bash
   cd ../smartchaindb
   docker-compose up -d
   ```

2. **Java & Maven installed:**
   ```bash
   java -version  # Java 8+
   mvn -version   # Maven 3.6+
   ```

### Run Tests

**Linux/Mac:**
```bash
cd smartchaindb-driver
chmod +x run_load_test.sh
./run_load_test.sh all
```

**Windows:**
```cmd
cd smartchaindb-driver
run_load_test.bat all
```

---

## 📋 Test Modes

### 1. Single Transaction Test

Tests one CREATE transaction with large payload.

```bash
# Linux/Mac
./run_load_test.sh single

# Windows
run_load_test.bat single
```

**What it does:**
- Creates 1 CREATE transaction
- Uses 1KB payload (default)
- Measures validation time
- Shows transaction ID

**Expected output:**
```
Creating CREATE transaction...
  Asset data size: ~1024 bytes
  Metadata size: ~1024 bytes
✓ Transaction created: abc123...
  Time: 52ms
```

---

### 2. Batch Transactions Test

Sends multiple **unique** transactions in sequence.

```bash
# 50 transactions, 2KB payload each
./run_load_test.sh batch 50 2

# Windows
run_load_test.bat batch 50 2
```

**Parameters:**
- `count`: Number of transactions (default: 10)
- `payloadKB`: Payload size in KB (default: 1)
- `delayMs`: Delay between TXs in milliseconds (default: 100)

**What it does:**
- Creates N unique transactions
- Each with different asset data
- Sequential sending with configurable delay
- Measures each validation time

**Expected output:**
```
[1/50] Creating transaction...
  ✓ abc123... - 52ms
[2/50] Creating transaction...
  ✓ def456... - 48ms
...
Created 50 transactions successfully.
```

**Use case:** Test system throughput with unique data

---

### 3. Marketplace Flow Test

Runs complete marketplace flow: CREATE → ADVERTISEMENT → BUY_OFFER → SELL

```bash
# 5 complete flows, 5KB payloads, 1000ms delay between steps
./run_load_test.sh marketplace 5 5 1000

# Windows
run_load_test.bat marketplace 5 5 1000
```

**Parameters:**
- `count`: Number of complete flows (default: 10)
- `payloadKB`: Payload size for CREATE (default: 1)
- `delayMs`: Delay between steps (default: 100)

**What it does:**
- Creates asset (CREATE)
- Advertises it (ADVERTISEMENT)
- Buyer makes offer (BUY_OFFER)
- Seller accepts (SELL)
- Repeats N times

**Expected output:**
```
[Flow 1/5]
  [1/4] CREATE asset...
    ✓ abc123... - 52ms
  [2/4] ADVERTISEMENT...
    ✓ def456... - 48ms
  [3/4] BUY_OFFER...
    ✓ ghi789... - 50ms
  [4/4] SELL...
    ✓ jkl012... - 55ms
```

**Use case:** Test real-world marketplace scenarios

---

### 4. Stress Test (Multi-threaded)

High-load testing with concurrent transactions.

```bash
# 100 transactions, 1KB each, 4 parallel threads
./run_load_test.sh stress 100 1 0 4

# Windows
run_load_test.bat stress 100 1 0 4
```

**Parameters:**
- `count`: Total transactions (default: 10)
- `payloadKB`: Payload size (default: 1)
- `delayMs`: 0 for no delay (default: 0)
- `threads`: Number of parallel threads (default: 1)

**What it does:**
- Spawns N threads
- Each sends transactions concurrently
- No delay between transactions
- Maximum throughput test

**Expected output:**
```
[1/100] ✓ abc123... - 52ms (thread: pool-1-thread-1)
[2/100] ✓ def456... - 48ms (thread: pool-1-thread-2)
[3/100] ✓ ghi789... - 50ms (thread: pool-1-thread-3)
...
```

**Use case:** Test system under high concurrent load

---

### 5. Cache Effectiveness Test

Tests server-side caching by sending similar transactions.

```bash
# 10 transactions with similar data
./run_load_test.sh cache 10 1 100

# Windows
run_load_test.bat cache 10 1 100
```

**Parameters:**
- `count`: Number of transactions (default: 10)
- `payloadKB`: Payload size (default: 1)
- `delayMs`: Delay between TXs (default: 100)

**What it does:**
- Creates transactions with same asset structure
- Different signatures (so not duplicates)
- Measures if server-side caching helps

**Expected output:**
```
[1/10] ✓ 52ms - MISS (expected)
[2/10] ✓ 48ms - POSSIBLE HIT
[3/10] ✓ 47ms - POSSIBLE HIT
...

Cache Analysis:
  First validation: 52ms
  Avg subsequent: 47.5ms
  Speedup: 1.09x
```

**Use case:** Verify SHACL cache is working

---

### 6. Run All Tests

Runs all test scenarios in sequence.

```bash
./run_load_test.sh all

# Windows
run_load_test.bat all
```

**What it runs:**
1. Single transaction (1 TX, 1KB)
2. Small batch (10 TXs, 1KB)
3. Cache test (5 TXs)
4. Marketplace flow (3 flows, 2KB)
5. Larger batch (20 TXs, 5KB)

**Duration:** ~5-10 minutes

---

## 📊 Understanding Results

### Transaction Statistics

```
Transaction Statistics:
  Total: 50
  Successful: 48
  Failed: 2
  Success Rate: 96.00%
```

| Metric | Meaning |
|--------|---------|
| Total | Attempted transactions |
| Successful | Validated and accepted |
| Failed | Rejected by validation |
| Success Rate | % of successful TXs |

---

### Timing Statistics

```
Timing Statistics:
  Total time: 5234ms (5.23s)
  Min time: 42ms
  Max time: 158ms
  Avg time: 52.34ms
  Median time: 48ms
  Throughput: 9.17 TPS
```

| Metric | Good Value | Meaning |
|--------|------------|---------|
| Avg time | < 60ms | Average validation time |
| Min time | < 50ms | Fastest validation (cache hit) |
| Max time | < 200ms | Slowest validation |
| Throughput | > 10 TPS | Transactions per second |

---

### Performance Interpretation

**Excellent Performance:**
```
Avg time: 45ms
Min time: 35ms
Max time: 80ms
Throughput: 15 TPS
```
✓ System is healthy  
✓ Cache is working  
✓ No bottlenecks

**Poor Performance:**
```
Avg time: 250ms
Min time: 200ms
Max time: 500ms
Throughput: 2 TPS
```
✗ Check SHACL service  
✗ Check MongoDB  
✗ Check network

---

## 🔧 Configuring Tests

### Payload Sizes

| Size | Use Case | Example |
|------|----------|---------|
| 1KB | Normal transactions | `./run_load_test.sh batch 10 1` |
| 5KB | Medium data | `./run_load_test.sh batch 10 5` |
| 10KB | Large data | `./run_load_test.sh batch 10 10` |
| 50KB+ | Stress test | `./run_load_test.sh batch 10 50` |

**Note:** Larger payloads = slower validation (more data to process)

---

### Delays

| Delay | Effect | Use Case |
|-------|--------|----------|
| 0ms | No delay | Maximum throughput test |
| 100ms | Slight delay | Realistic user pace |
| 500ms | Moderate | Give server time to breathe |
| 1000ms+ | Long | Allow block commits |

```bash
# No delay - maximum speed
./run_load_test.sh batch 50 1 0

# 1 second delay - allow commits
./run_load_test.sh marketplace 5 2 1000
```

---

### Concurrency

| Threads | Load | Use Case |
|---------|------|----------|
| 1 | Sequential | Normal testing |
| 2-4 | Moderate | Realistic concurrent users |
| 8-16 | High | Stress testing |

```bash
# Single thread
./run_load_test.sh stress 100 1 0 1

# 4 threads - moderate load
./run_load_test.sh stress 100 1 0 4

# 16 threads - high stress
./run_load_test.sh stress 200 1 0 16
```

---

## 📈 Real-World Scenarios

### Scenario 1: Normal Operations
**Simulate typical usage with 50 TPS:**

```bash
./run_load_test.sh batch 100 2 20
```

Expected:
- Success rate: > 95%
- Avg time: 40-60ms
- Throughput: 40-60 TPS

---

### Scenario 2: Peak Load
**Simulate traffic spike:**

```bash
./run_load_test.sh stress 200 1 0 8
```

Expected:
- Success rate: > 90%
- Avg time: 60-100ms
- Throughput: 20-40 TPS

---

### Scenario 3: Marketplace Activity
**Simulate real marketplace with 20 simultaneous trades:**

```bash
./run_load_test.sh marketplace 20 5 500
```

Expected:
- All 4 steps complete for each flow
- Success rate: > 95%
- Total time: ~3-5 minutes

---

### Scenario 4: Large Documents
**Test with 50KB payloads:**

```bash
./run_load_test.sh batch 20 50 500
```

Expected:
- Success rate: > 90%
- Avg time: 100-200ms (larger data)
- Throughput: 5-10 TPS

---

## 🐛 Troubleshooting

### Error: "BigchainDB is not accessible"

**Problem:** Services not running

**Fix:**
```bash
cd ../smartchaindb
docker-compose up -d
docker-compose ps  # Check status
```

---

### Error: "Compilation failed"

**Problem:** Missing dependencies or Java version

**Fix:**
```bash
mvn clean install
java -version  # Ensure Java 8+
```

---

### Error: "Transaction failed: 400 Bad Request"

**Problem:** Invalid transaction structure

**Check:**
- Transaction schemas match server expectations
- All required fields present
- Valid signatures

---

### High failure rate (> 10%)

**Possible causes:**
1. Server overloaded
2. SHACL service slow
3. MongoDB slow
4. Network issues

**Debug:**
```bash
# Check server logs
cd ../smartchaindb
docker-compose logs -f bigchaindb

# Check SHACL service
docker-compose logs -f shacleng

# Check system resources
docker stats
```

---

### Slow performance (> 100ms avg)

**Check:**
1. **SHACL service:**
   ```bash
   docker-compose logs shacleng | tail -50
   ```

2. **MongoDB:**
   ```bash
   docker-compose exec mongodb mongo --eval "db.serverStatus().connections"
   ```

3. **System resources:**
   ```bash
   docker stats
   ```

**Tune:**
- Reduce payload size
- Increase delay between TXs
- Reduce concurrency

---

## 📊 Viewing Server-Side Metrics

After running tests, check cache performance:

```bash
# Detailed metrics
curl http://localhost:9984/api/v1/metrics/validation | jq '.'

# Quick summary
curl -s http://localhost:9984/api/v1/metrics/validation | jq '.metrics | {total_validations, cache_hit_rate_percent, avg_time_ms: .performance.avg_time_ms}'
```

**Expected metrics after load test:**
```json
{
  "total_validations": 150,
  "cache_hit_rate_percent": 45.5,
  "avg_time_ms": 48.2
}
```

---

## 🎯 Best Practices

### 1. Start Small
```bash
# Begin with single transaction
./run_load_test.sh single

# Then small batch
./run_load_test.sh batch 10 1
```

### 2. Gradually Increase Load
```bash
# 10 → 50 → 100 → 200
./run_load_test.sh batch 10 1
./run_load_test.sh batch 50 1
./run_load_test.sh batch 100 1
```

### 3. Monitor Server Health
```bash
# Watch logs during test
docker-compose logs -f bigchaindb shacleng
```

### 4. Clean Between Tests
```bash
# Clear cache
curl -X POST http://localhost:9984/api/v1/metrics/validation/cache/clear

# Reset metrics
curl -X POST http://localhost:9984/api/v1/metrics/validation/reset
```

---

## ✅ Success Criteria

Tests are successful if:

1. ✅ **Success rate > 95%**
2. ✅ **Avg validation time < 60ms** (for 1KB payloads)
3. ✅ **Throughput > 10 TPS** (single-threaded)
4. ✅ **No server crashes** or errors
5. ✅ **Cache hit rate > 40%** (for cache test)

---

## 📚 Related Documentation

- **Python Tests:** `../smartchaindb/tests/performance/README.md`
- **Cache Implementation:** `../smartchaindb/CACHE_IMPLEMENTATION_COMPLETE.md`
- **Testing Guide:** `../smartchaindb/CACHE_TESTING_GUIDE.md`

---

## 🎉 Summary

**Real-world load testing with:**
- ✅ Actual transaction sending
- ✅ Configurable payloads (1KB - 100KB+)
- ✅ Multiple test scenarios
- ✅ Concurrent execution
- ✅ Full marketplace flows
- ✅ Performance metrics

**Run it now:**
```bash
./run_load_test.sh all
```

---

*Last Updated: October 4, 2025*
*Status: ✅ READY FOR USE*


