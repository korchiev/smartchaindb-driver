package com.bigchaindb.smartchaindb.driver;

import com.bigchaindb.builders.BigchainDbConfigBuilder;
import com.bigchaindb.model.MetaData;
import com.bigchaindb.model.Transaction;

import java.security.KeyPair;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Load Testing Driver for SHACL Validation Cache Performance
 * 
 * Sends real transactions with configurable:
 * - Transaction count
 * - Data payload size
 * - Transaction types
 * - Concurrency level
 * - Delay between transactions
 * 
 * Usage:
 *   java LoadTestDriver <mode> <count> [options]
 * 
 * Modes:
 *   - single: Single transaction test
 *   - batch: Batch of unique transactions
 *   - marketplace: Full marketplace flow
 *   - stress: High-load stress test
 *   - cache: Cache effectiveness test (same TX repeated)
 * 
 * @author BigchainDB Performance Testing Team
 */
public class LoadTestDriver {
    
    // Configuration
    private static final String BIGCHAINDB_URL = System.getenv().getOrDefault("BIGCHAINDB_URL", "http://localhost:9984/");
    private static final int DEFAULT_COUNT = 10;
    private static final int DEFAULT_PAYLOAD_SIZE_KB = 1; // KB
    private static final int DEFAULT_DELAY_MS = 100;
    private static final int DEFAULT_THREADS = 1;
    
    // Statistics
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failureCount = new AtomicInteger(0);
    private static final List<Long> validationTimes = new CopyOnWriteArrayList<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    
    public static void main(String[] args) throws Exception {
        printHeader();
        
        // Parse arguments
        String mode = args.length > 0 ? args[0] : "batch";
        int count = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_COUNT;
        int payloadSizeKB = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_PAYLOAD_SIZE_KB;
        int delayMs = args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_DELAY_MS;
        int threads = args.length > 4 ? Integer.parseInt(args[4]) : DEFAULT_THREADS;
        
        System.out.println("Configuration:");
        System.out.println("  Mode: " + mode);
        System.out.println("  Count: " + count);
        System.out.println("  Payload Size: " + payloadSizeKB + " KB");
        System.out.println("  Delay: " + delayMs + " ms");
        System.out.println("  Threads: " + threads);
        System.out.println("  BigchainDB URL: " + BIGCHAINDB_URL);
        System.out.println();
        
        // Setup connection
        setConfig();
        
        // Run test
        long startTime = System.currentTimeMillis();
        
        switch (mode.toLowerCase()) {
            case "single":
                testSingleTransaction(payloadSizeKB);
                break;
            case "batch":
                testBatchTransactions(count, payloadSizeKB, delayMs);
                break;
            case "marketplace":
                testMarketplaceFlow(count, payloadSizeKB, delayMs);
                break;
            case "stress":
                testStressLoad(count, payloadSizeKB, threads);
                break;
            case "cache":
                testCacheEffectiveness(count, payloadSizeKB, delayMs);
                break;
            default:
                System.out.println("Unknown mode: " + mode);
                printUsage();
                return;
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Print statistics
        printStatistics(totalTime);
        
        // Fetch server metrics
        System.out.println("\nFetching server metrics...");
        fetchServerMetrics();
    }
    
    /**
     * Test 1: Single Transaction
     */
    private static void testSingleTransaction(int payloadSizeKB) throws Exception {
        printTestHeader("Single Transaction Test");
        
        KeyPair keys = getKeys();
        BigchainDBJavaDriver driver = new BigchainDBJavaDriver();
        
        Map<String, Object> assetData = generateLargeAssetData(payloadSizeKB);
        MetaData metadata = generateLargeMetadata(payloadSizeKB);
        
        System.out.println("Creating CREATE transaction...");
        System.out.println("  Asset data size: ~" + estimateSize(assetData) + " bytes");
        System.out.println("  Metadata size: ~" + estimateSize(metadata.getMetadata()) + " bytes");
        
        long start = System.nanoTime();
        try {
            String txId = Transactions.doCreate(driver, assetData, metadata, keys);
            long duration = (System.nanoTime() - start) / 1_000_000; // Convert to ms
            
            System.out.println("✓ Transaction created: " + txId);
            System.out.println("  Time: " + duration + "ms");
            
            successCount.incrementAndGet();
            validationTimes.add(duration);
        } catch (Exception e) {
            long duration = (System.nanoTime() - start) / 1_000_000;
            System.out.println("✗ Transaction failed after " + duration + "ms: " + e.getMessage());
            failureCount.incrementAndGet();
            throw e;
        }
    }
    
    /**
     * Test 2: Batch Transactions (Unique)
     */
    private static void testBatchTransactions(int count, int payloadSizeKB, int delayMs) throws Exception {
        printTestHeader("Batch Transactions Test (" + count + " unique)");
        
        KeyPair keys = getKeys();
        BigchainDBJavaDriver driver = new BigchainDBJavaDriver();
        
        List<String> createdIds = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            System.out.printf("[%d/%d] Creating transaction...\n", i + 1, count);
            
            Map<String, Object> assetData = generateLargeAssetData(payloadSizeKB);
            assetData.put("batchIndex", i);
            assetData.put("timestamp", LocalDateTime.now(Clock.systemUTC()).format(formatter));
            
            MetaData metadata = generateLargeMetadata(payloadSizeKB);
            
            long start = System.nanoTime();
            try {
                String txId = Transactions.doCreate(driver, assetData, metadata, keys);
                long duration = (System.nanoTime() - start) / 1_000_000;
                
                System.out.println("  ✓ " + txId + " - " + duration + "ms");
                
                createdIds.add(txId);
                successCount.incrementAndGet();
                validationTimes.add(duration);
                
                if (delayMs > 0 && i < count - 1) {
                    Thread.sleep(delayMs);
                }
            } catch (Exception e) {
                long duration = (System.nanoTime() - start) / 1_000_000;
                System.out.println("  ✗ Failed after " + duration + "ms: " + e.getMessage());
                failureCount.incrementAndGet();
            }
        }
        
        System.out.println("\nCreated " + createdIds.size() + " transactions successfully.");
    }
    
    /**
     * Test 3: Marketplace Flow (CREATE → ADVERTISEMENT → BUY_OFFER → SELL)
     */
    private static void testMarketplaceFlow(int count, int payloadSizeKB, int delayMs) throws Exception {
        printTestHeader("Marketplace Flow Test (" + count + " flows)");
        
        for (int i = 0; i < count; i++) {
            System.out.printf("\n[Flow %d/%d]\n", i + 1, count);
            
            KeyPair sellerKeys = getKeys();
            KeyPair buyerKeys = getKeys();
            KeyPair escrowKeys = sellerKeys; // Seller acts as escrow
            BigchainDBJavaDriver driver = new BigchainDBJavaDriver();
            
            try {
                // Step 1: CREATE asset
                System.out.println("  [1/4] CREATE asset...");
                Map<String, Object> assetData = generateLargeAssetData(payloadSizeKB);
                assetData.put("flowIndex", i);
                MetaData createMetadata = generateLargeMetadata(payloadSizeKB / 2);
                
                long start = System.nanoTime();
                String createId = Transactions.doCreate(driver, assetData, createMetadata, sellerKeys);
                long duration = (System.nanoTime() - start) / 1_000_000;
                
                System.out.println("    ✓ " + createId + " - " + duration + "ms");
                successCount.incrementAndGet();
                validationTimes.add(duration);
                
                Thread.sleep(delayMs);
                
                // Step 2: ADVERTISEMENT
                System.out.println("  [2/4] ADVERTISEMENT...");
                start = System.nanoTime();
                String adId = Simulation.createAdvertisement(driver, sellerKeys, createId);
                duration = (System.nanoTime() - start) / 1_000_000;
                
                if (adId != null) {
                    System.out.println("    ✓ " + adId + " - " + duration + "ms");
                    successCount.incrementAndGet();
                    validationTimes.add(duration);
                } else {
                    System.out.println("    ✗ Failed");
                    failureCount.incrementAndGet();
                    continue;
                }
                
                Thread.sleep(delayMs);
                
                // Step 3: BUY_OFFER
                System.out.println("  [3/4] BUY_OFFER...");
                start = System.nanoTime();
                String buyOfferId = Simulation.createBuyOffer(driver, buyerKeys, escrowKeys, createId, adId);
                duration = (System.nanoTime() - start) / 1_000_000;
                
                if (buyOfferId != null) {
                    System.out.println("    ✓ " + buyOfferId + " - " + duration + "ms");
                    successCount.incrementAndGet();
                    validationTimes.add(duration);
                } else {
                    System.out.println("    ✗ Failed");
                    failureCount.incrementAndGet();
                    continue;
                }
                
                Thread.sleep(delayMs);
                
                // Step 4: SELL
                System.out.println("  [4/4] SELL...");
                start = System.nanoTime();
                String sellId = Simulation.createSell(driver, sellerKeys, buyerKeys, escrowKeys, createId, buyOfferId);
                duration = (System.nanoTime() - start) / 1_000_000;
                
                if (sellId != null) {
                    System.out.println("    ✓ " + sellId + " - " + duration + "ms");
                    successCount.incrementAndGet();
                    validationTimes.add(duration);
                } else {
                    System.out.println("    ✗ Failed");
                    failureCount.incrementAndGet();
                }
                
                Thread.sleep(delayMs);
                
            } catch (Exception e) {
                System.out.println("    ✗ Flow failed: " + e.getMessage());
                failureCount.incrementAndGet();
            }
        }
    }
    
    /**
     * Test 4: Stress Test (Multi-threaded)
     */
    private static void testStressLoad(int count, int payloadSizeKB, int threads) throws Exception {
        printTestHeader("Stress Test (" + count + " TXs, " + threads + " threads)");
        
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            final int index = i;
            Future<?> future = executor.submit(() -> {
                try {
                    KeyPair keys = getKeys();
                    BigchainDBJavaDriver driver = new BigchainDBJavaDriver();
                    
                    Map<String, Object> assetData = generateLargeAssetData(payloadSizeKB);
                    assetData.put("stressIndex", index);
                    assetData.put("thread", Thread.currentThread().getName());
                    
                    MetaData metadata = generateLargeMetadata(payloadSizeKB / 2);
                    
                    long start = System.nanoTime();
                    String txId = Transactions.doCreate(driver, assetData, metadata, keys);
                    long duration = (System.nanoTime() - start) / 1_000_000;
                    
                    System.out.printf("[%d/%d] ✓ %s - %dms (thread: %s)\n", 
                        index + 1, count, txId, duration, Thread.currentThread().getName());
                    
                    successCount.incrementAndGet();
                    validationTimes.add(duration);
                    
                } catch (Exception e) {
                    System.out.printf("[%d/%d] ✗ Failed: %s\n", index + 1, count, e.getMessage());
                    failureCount.incrementAndGet();
                }
            });
            futures.add(future);
        }
        
        // Wait for all to complete
        for (Future<?> future : futures) {
            future.get();
        }
        
        executor.shutdown();
    }
    
    /**
     * Test 5: Cache Effectiveness (Same TX repeated)
     */
    private static void testCacheEffectiveness(int count, int payloadSizeKB, int delayMs) throws Exception {
        printTestHeader("Cache Effectiveness Test (Same TX " + count + "x)");
        
        System.out.println("NOTE: This tests cache by creating one TX and then checking");
        System.out.println("      if subsequent identical asset data is cached server-side.");
        System.out.println();
        
        KeyPair keys = getKeys();
        BigchainDBJavaDriver driver = new BigchainDBJavaDriver();
        
        // Create one transaction with fixed data
        Map<String, Object> assetData = generateLargeAssetData(payloadSizeKB);
        assetData.put("cacheTest", "fixedData");
        assetData.put("timestamp", "2025-10-04T12:00:00.000"); // Fixed timestamp
        
        MetaData metadata = new MetaData();
        metadata.setMetaData("requestCreationTimestamp", "2025-10-04T12:00:00.000");
        metadata.setMetaData("cacheTest", "fixedMetadata");
        
        System.out.println("Creating " + count + " transactions with similar data...");
        
        for (int i = 0; i < count; i++) {
            System.out.printf("[%d/%d] ", i + 1, count);
            
            long start = System.nanoTime();
            try {
                // Each transaction is slightly different (different keys/signatures)
                // but asset structure is the same
                String txId = Transactions.doCreate(driver, assetData, metadata, keys);
                long duration = (System.nanoTime() - start) / 1_000_000;
                
                String cacheStatus = (i == 0) ? "MISS (expected)" : "POSSIBLE HIT";
                System.out.printf("✓ %dms - %s\n", duration, cacheStatus);
                
                successCount.incrementAndGet();
                validationTimes.add(duration);
                
                if (delayMs > 0 && i < count - 1) {
                    Thread.sleep(delayMs);
                }
            } catch (Exception e) {
                long duration = (System.nanoTime() - start) / 1_000_000;
                System.out.printf("✗ Failed after %dms: %s\n", duration, e.getMessage());
                failureCount.incrementAndGet();
            }
        }
        
        // Analyze cache effectiveness
        if (validationTimes.size() > 1) {
            long firstTime = validationTimes.get(0);
            double avgSubsequent = validationTimes.stream()
                .skip(1)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
            
            System.out.println("\nCache Analysis:");
            System.out.println("  First validation: " + firstTime + "ms");
            System.out.println("  Avg subsequent: " + String.format("%.2f", avgSubsequent) + "ms");
            if (avgSubsequent > 0) {
                System.out.println("  Speedup: " + String.format("%.2fx", firstTime / avgSubsequent));
            }
        }
    }
    
    // ===== Helper Methods =====
    
    /**
     * Generate large asset data with configurable size
     */
    private static Map<String, Object> generateLargeAssetData(int sizeKB) {
        Map<String, Object> assetData = new TreeMap<>();
        
        // Core fields (required)
        assetData.put("machineIdentifier", "LOAD_TEST_" + UUID.randomUUID().toString().substring(0, 8));
        assetData.put("capability", Arrays.asList("Welding", "Cutting", "Assembly", "Painting"));
        
        // Add large data to reach target size
        int targetBytes = sizeKB * 1024;
        StringBuilder largeData = new StringBuilder();
        String chunk = "LoadTest_" + "x".repeat(100); // 109 bytes per chunk
        
        while (largeData.length() < targetBytes) {
            largeData.append(chunk);
        }
        
        assetData.put("largeDataField", largeData.toString());
        assetData.put("specifications", generateSpecifications());
        assetData.put("features", generateFeatures());
        
        return assetData;
    }
    
    /**
     * Generate large metadata with configurable size
     */
    private static MetaData generateLargeMetadata(int sizeKB) {
        MetaData metadata = new MetaData();
        
        metadata.setMetaData("requestCreationTimestamp", LocalDateTime.now(Clock.systemUTC()).format(formatter));
        metadata.setMetaData("testRun", "LoadTest_" + System.currentTimeMillis());
        metadata.setMetaData("environment", "Performance Testing");
        
        // Add large metadata
        int targetBytes = sizeKB * 1024;
        StringBuilder largeMetadata = new StringBuilder();
        String chunk = "Metadata_" + "x".repeat(100);
        
        while (largeMetadata.length() < targetBytes) {
            largeMetadata.append(chunk);
        }
        
        metadata.setMetaData("largeMetadataField", largeMetadata.toString());
        metadata.setMetaData("additionalInfo", generateAdditionalInfo());
        
        return metadata;
    }
    
    private static Map<String, Object> generateSpecifications() {
        Map<String, Object> specs = new TreeMap<>();
        specs.put("model", "Industrial-X" + new Random().nextInt(1000));
        specs.put("year", 2020 + new Random().nextInt(5));
        specs.put("weight", (500 + new Random().nextInt(1000)) + " kg");
        specs.put("power", (5 + new Random().nextInt(20)) + " kW");
        specs.put("dimensions", "200x150x100 cm");
        specs.put("certification", Arrays.asList("ISO9001", "CE", "UL"));
        return specs;
    }
    
    private static List<String> generateFeatures() {
        return Arrays.asList(
            "Automated operation",
            "High precision",
            "Energy efficient",
            "Easy maintenance",
            "Safety certified",
            "IoT enabled",
            "Remote monitoring",
            "Predictive maintenance"
        );
    }
    
    private static Map<String, Object> generateAdditionalInfo() {
        Map<String, Object> info = new TreeMap<>();
        info.put("vendor", "LoadTest Industries");
        info.put("warranty", "24 months");
        info.put("support", "24/7 Technical Support");
        info.put("training", "Included");
        return info;
    }
    
    private static int estimateSize(Object obj) {
        return obj.toString().getBytes().length;
    }
    
    private static KeyPair getKeys() {
        net.i2p.crypto.eddsa.KeyPairGenerator edDsaKpg = new net.i2p.crypto.eddsa.KeyPairGenerator();
        return edDsaKpg.generateKeyPair();
    }
    
    private static void setConfig() {
        BigchainDbConfigBuilder.baseUrl(BIGCHAINDB_URL).setup();
    }
    
    // ===== Statistics & Reporting =====
    
    private static void printStatistics(long totalTime) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("LOAD TEST RESULTS");
        System.out.println("=".repeat(80));
        
        int total = successCount.get() + failureCount.get();
        System.out.println("\nTransaction Statistics:");
        System.out.println("  Total: " + total);
        System.out.println("  Successful: " + successCount.get());
        System.out.println("  Failed: " + failureCount.get());
        System.out.println("  Success Rate: " + String.format("%.2f%%", (successCount.get() * 100.0 / total)));
        
        System.out.println("\nTiming Statistics:");
        System.out.println("  Total time: " + totalTime + "ms (" + String.format("%.2f", totalTime / 1000.0) + "s)");
        
        if (!validationTimes.isEmpty()) {
            LongSummaryStatistics stats = validationTimes.stream()
                .mapToLong(Long::longValue)
                .summaryStatistics();
            
            System.out.println("  Min time: " + stats.getMin() + "ms");
            System.out.println("  Max time: " + stats.getMax() + "ms");
            System.out.println("  Avg time: " + String.format("%.2f", stats.getAverage()) + "ms");
            System.out.println("  Median time: " + calculateMedian(validationTimes) + "ms");
            
            double throughput = (successCount.get() * 1000.0) / totalTime;
            System.out.println("  Throughput: " + String.format("%.2f", throughput) + " TPS");
        }
        
        System.out.println("\n" + "=".repeat(80));
    }
    
    private static long calculateMedian(List<Long> times) {
        List<Long> sorted = times.stream().sorted().collect(Collectors.toList());
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size/2 - 1) + sorted.get(size/2)) / 2;
        } else {
            return sorted.get(size/2);
        }
    }
    
    private static void fetchServerMetrics() {
        try {
            System.out.println("To view server-side metrics, run:");
            System.out.println("  curl http://localhost:9984/api/v1/metrics/validation | jq");
        } catch (Exception e) {
            System.out.println("Could not fetch server metrics: " + e.getMessage());
        }
    }
    
    // ===== UI Helpers =====
    
    private static void printHeader() {
        System.out.println("\n" + "╔" + "=".repeat(78) + "╗");
        System.out.println("║" + " SHACL Validation Cache - Load Test Driver ".center(78) + "║");
        System.out.println("╚" + "=".repeat(78) + "╝\n");
    }
    
    private static void printTestHeader(String title) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(title);
        System.out.println("=".repeat(80) + "\n");
    }
    
    private static void printUsage() {
        System.out.println("\nUsage:");
        System.out.println("  java LoadTestDriver <mode> [count] [payloadKB] [delayMs] [threads]");
        System.out.println("\nModes:");
        System.out.println("  single       - Single transaction test");
        System.out.println("  batch        - Batch of unique transactions");
        System.out.println("  marketplace  - Full marketplace flow (CREATE→AD→OFFER→SELL)");
        System.out.println("  stress       - Multi-threaded stress test");
        System.out.println("  cache        - Cache effectiveness test");
        System.out.println("\nExamples:");
        System.out.println("  java LoadTestDriver single");
        System.out.println("  java LoadTestDriver batch 50 2 100");
        System.out.println("  java LoadTestDriver marketplace 10 5 500");
        System.out.println("  java LoadTestDriver stress 100 1 0 4");
        System.out.println("  java LoadTestDriver cache 10 1 100");
    }
}


