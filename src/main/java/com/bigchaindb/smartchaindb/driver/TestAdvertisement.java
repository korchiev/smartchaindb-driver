package com.bigchaindb.smartchaindb.driver;

import com.bigchaindb.model.MetaData;
import java.security.KeyPair;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

public class TestAdvertisement {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Testing ADVERTISEMENT Transaction ===");
        
        // Setup driver
        BigchainDBJavaDriver.setConfig();
        BigchainDBJavaDriver driver = new BigchainDBJavaDriver();
        KeyPair advertiserKeys = BigchainDBJavaDriver.getKeys();
        
        // Create asset data
        Map<String, Object> assetData = new TreeMap<>();
        assetData.put("type", "Digital Asset");
        assetData.put("name", "Test Digital Art");
        assetData.put("description", "A beautiful digital artwork");
        assetData.put("category", "Digital Art");
        
        // Create advertisement metadata
        MetaData adMetaData = new MetaData();
        adMetaData.setMetaData("status", "OPEN");
        adMetaData.setMetaData("advertiser_public_key", 
            ((net.i2p.crypto.eddsa.EdDSAPublicKey) advertiserKeys.getPublic()).toString());
        adMetaData.setMetaData("price", "1000.00");
        adMetaData.setMetaData("description", "High-quality digital asset for sale");
        adMetaData.setMetaData("category", "Digital Art");
        adMetaData.setMetaData("condition", "New");
        adMetaData.setMetaData("expiry_date", LocalDateTime.now(Clock.systemUTC()).plusDays(30).format(ISO));
        adMetaData.setMetaData("contact_info", "advertiser@example.com");
        adMetaData.setMetaData("location", "New York, NY");
        adMetaData.setMetaData("requestCreationTimestamp", LocalDateTime.now(Clock.systemUTC()).toString());
        
        try {
            // Create asset first, then advertise it
            String adTxId = Transactions.doCreateThenAdvertise(driver, assetData, adMetaData, advertiserKeys);
            
            if (adTxId != null) {
                System.out.println("SUCCESS: ADVERTISEMENT transaction created with ID: " + adTxId);
            } else {
                System.out.println("ERROR: Failed to create ADVERTISEMENT transaction");
            }
            
        } catch (Exception e) {
            System.out.println("ERROR: Exception occurred: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== Test Complete ===");
    }
}

