package com.bigchaindb.smartchaindb.driver;


import com.bigchaindb.builders.BigchainDbConfigBuilder;
import com.bigchaindb.model.Connection;
import com.bigchaindb.model.GenericCallback;
import com.bigchaindb.model.MetaData;
import com.bigchaindb.model.Transaction;
import okhttp3.Response;

import java.security.KeyPair;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Random;

/**
 * simple usage of BigchainDB Java driver (https://github.com/bigchaindb/java-bigchaindb-driver)
 * to create TXs on BigchainDB network
 *
 * @author dev@bigchaindb.com
 */
public class BigchainDBJavaDriver {

    /**
     * Driver method
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {

        /* -
        // GJoin: Uncomment this block if group manager is set up. Make sure GROUP_SIGNATURE flag
        // in DriverConstants is set to true
        String out = new Scanner(new URL(DriverConstants.GROUP_MANAGER_ENDPOINT + "?id=1&usk=1")
                .openStream(), "UTF-8").useDelimiter("\\A").next();
        String[] parts = out.split("\\|");
        String payloadString = "{transaction: test, tester: sen}";
        String secretKey='"'+parts[0].replaceAll(",","comma") +'"';
        String publicKey= '"'+parts[1].replaceAll(",","comma") + '"';
        String jsonPayload='"'+payloadString.replaceAll(",","comma")+ '"';
        */

        setConfig();
        KeyPair keys = BigchainDBJavaDriver.getKeys();
        // KeyPair transferKeys = BigchainDBJavaDriver.getKeys();
        BigchainDBJavaDriver driver = new BigchainDBJavaDriver();

        // Run async ConsumerDriver
        //new ConsumerDriver(keys).run();

        // Execute your transactions
        Transaction rfq1 = null;
        Transaction bid1 = null;
        List<String> rfq_ids = new ArrayList<>();
        List<String> create_ids = new ArrayList<>();
        List<String> win_bid_ids = new ArrayList<>();
        List<String> advertisement_ids = new ArrayList<>();
        Transaction winningBid = null;
        // int rfq_count = 10;
        int create_count = 1;
        // int bid_count = 250;
        int bids_per_rfq = 1;
        int advertisement_count = 1;
        int counter = 0;
        
        // //create rfqs 
        // for(int j = 0; j < rfq_count; j++) {
        //     Transaction rfq = Simulation.createRFQ(driver, keys, null);
        //     rfq_ids.add(rfq.getId());
        // }
        
        // create assests seperately 
        for(int k = 0; k < create_count; k++) {
            String createId = Simulation.createCreate(driver, keys);
            create_ids.add(createId);
        }
        
        Thread.sleep(5000);
        // ===== ADVERTISEMENT TRANSACTION VALIDATION DEMO =====
        System.out.println("\n=== ADVERTISEMENT Transaction Validation Demo ===");
        
        // Test Case 1: SUCCESSFUL ADVERTISEMENT (valid CREATE transaction)
        System.out.println("\n1. Testing SUCCESSFUL ADVERTISEMENT...");
        String validCreateId = create_ids.get(0); // Use first created asset
        String successfulAdId = Simulation.createAdvertisement(driver, keys, validCreateId);
        if (successfulAdId != null) {
            advertisement_ids.add(successfulAdId);
            System.out.println("✅ SUCCESS: ADVERTISEMENT created with ID: " + successfulAdId);
        } else {
            System.out.println("❌ FAILED: Could not create ADVERTISEMENT");
        }

         Thread.sleep(5000);

        // === BUY_OFFER and SELL demo ===
        System.out.println("\n=== BUY_OFFER and SELL Demo ===");
        KeyPair buyerKeys = BigchainDBJavaDriver.getKeys();
        // For atomic swap demo, seller acts as escrow (in production, use separate escrow service)
        KeyPair escrowKeys = keys; // Use seller as escrow
        String buyOfferId = Simulation.createBuyOffer(driver, buyerKeys, escrowKeys, validCreateId, successfulAdId);
        if (buyOfferId != null) {
            System.out.println("✅ SUCCESS: BUY_OFFER created with ID: " + buyOfferId);
        } else {
            System.out.println("❌ FAILED: Could not create BUY_OFFER");
        }

        Thread.sleep(5000);

        // Attempt SELL based on the buy offer
        String sellId = Simulation.createSell(driver, keys, buyerKeys, escrowKeys, validCreateId, buyOfferId);
        if (sellId != null) {
            System.out.println("✅ SUCCESS: SELL created with ID: " + sellId);
        } else {
            System.out.println("❌ FAILED: Could not create SELL");
        }
        
        // // Test Case 2: FAILING ADVERTISEMENT (duplicate OPEN ad for same asset)
        // System.out.println("\n2. Testing FAILING ADVERTISEMENT (duplicate OPEN ad)...");
        // try {
        //     String duplicateAdId = Simulation.createAdvertisement(driver, keys, validCreateId);
        //     if (duplicateAdId != null) {
        //         System.out.println("❌ UNEXPECTED: Duplicate ADVERTISEMENT was created: " + duplicateAdId);
        //     } else {
        //         System.out.println("✅ EXPECTED: Duplicate ADVERTISEMENT correctly rejected");
        //     }
        // } catch (Exception e) {
        //     System.out.println("✅ EXPECTED: Duplicate ADVERTISEMENT correctly rejected - " + e.getMessage());
        // }
        
        // // Test Case 3: FAILING ADVERTISEMENT (invalid CREATE transaction ID)
        // System.out.println("\n3. Testing FAILING ADVERTISEMENT (invalid CREATE transaction)...");
        // try {
        //     String invalidCreateId = "invalid-create-transaction-id-12345";
        //     String invalidAdId = Simulation.createAdvertisement(driver, keys, invalidCreateId);
        //     if (invalidAdId != null) {
        //         System.out.println("❌ UNEXPECTED: ADVERTISEMENT with invalid CREATE was created: " + invalidAdId);
        //     } else {
        //         System.out.println("✅ EXPECTED: ADVERTISEMENT with invalid CREATE correctly rejected");
        //     }
        // } catch (Exception e) {
        //     System.out.println("✅ EXPECTED: ADVERTISEMENT with invalid CREATE correctly rejected - " + e.getMessage());
        // }
        
        // // Test Case 4: FAILING ADVERTISEMENT (wrong owner)
        // System.out.println("\n4. Testing FAILING ADVERTISEMENT (wrong owner)...");
        // try {
        //     KeyPair differentKeys = BigchainDBJavaDriver.getKeys(); // Different keypair
        //     String wrongOwnerAdId = Simulation.createAdvertisement(driver, differentKeys, validCreateId);
        //     if (wrongOwnerAdId != null) {
        //         System.out.println("❌ UNEXPECTED: ADVERTISEMENT with wrong owner was created: " + wrongOwnerAdId);
        //     } else {
        //         System.out.println("✅ EXPECTED: ADVERTISEMENT with wrong owner correctly rejected");
        //     }
        // } catch (Exception e) {
        //     System.out.println("✅ EXPECTED: ADVERTISEMENT with wrong owner correctly rejected - " + e.getMessage());
        // }
        
        // // Create additional successful advertisements for other assets
        // System.out.println("\n5. Creating additional successful ADVERTISEMENTs...");
        // for(int k = 1; k < advertisement_count; k++) {
        //     String createId = create_ids.get(k);
        //     String advertisementId = Simulation.createAdvertisement(driver, keys, createId);
        //     if (advertisementId != null) {
        //         advertisement_ids.add(advertisementId);
        //         System.out.println("✅ ADVERTISEMENT " + k + " created: " + advertisementId);
        //     }
        // }
        
        // System.out.println("\n=== ADVERTISEMENT Validation Demo Complete ===");
        // System.out.println("Total successful ADVERTISEMENTs: " + advertisement_ids.size());
        
        // for(int i = 0; i  < rfq_ids.size() ; i++){
        //     String rfqId = rfq_ids.get(i);
        //     List<Transaction> bids = new ArrayList<>();
        //     for(int j = 0;  j < bids_per_rfq; j++) {
        //         String createId = create_ids.get(counter);
        //         counter ++;
        //         bids.add(Simulation.createBid(driver, keys, rfqId, createId));
        //     }
        //     Random rand = new Random();
        //     winningBid = bids.get(rand.nextInt(bids.size()));
        //     win_bid_ids.add(winningBid.getId());}

        // for(int j = 0; j < rfq_ids.size() ; j++){
        //     String rfq_Id = rfq_ids.get(j);
        //     MetaData metaData = new MetaData();
        //     metaData.setMetaData("requestCreationTimestamp", LocalDateTime.now(Clock.systemUTC()).toString());
        //     Transactions.doAccept(driver, win_bid_ids.get(j), rfq_Id, metaData, keys);
        // }
    

        //Simulation.createBid(driver, keys, rfq.getId());

        // MetaData metaData = new MetaData();
        // metaData.setMetaData("requestCreationTimestamp", LocalDateTime.now(Clock.systemUTC()).toString());
        // Transactions.doAccept(driver, bid1.getId(), rfq1.getId(), metaData, keys);
    }

    /**
     * configures connection url and credentials
     */
    public static void setConfig() {
        // Single-Node Setup
        BigchainDbConfigBuilder.baseUrl("http://localhost:9984/").setup();

        // Multi-Node Setup
        // List<Connection> connections = new ArrayList<>();
        // for (String url : DriverConstants.VALIDATOR_NODES) {
        //     Map<String, Object> attributes = new TreeMap<>();
        //     attributes.put("baseUrl", url);
        //     connections.add(new Connection(attributes));
        // }


        //  BigchainDbConfigBuilder
        //         .addConnections(connections)
        //         .setTimeout(60000)
        //         .setup();
    }

    /**
     * generates EdDSA keypair to sign and verify transactions
     *
     * @return KeyPair
     */
    public static KeyPair getKeys() {
        net.i2p.crypto.eddsa.KeyPairGenerator edDsaKpg = new net.i2p.crypto.eddsa.KeyPairGenerator();
        return edDsaKpg.generateKeyPair();
    }

    public GenericCallback handleServerResponse(String operation, MetaData metaData, String txId) {

        return new GenericCallback() {
            public void transactionMalformed(Response response) {
                System.out.println("Malformed: " + response.message());
            }

            public void pushedSuccessfully(Response response) {
                System.out.println("Transaction successfully posted");
            }

            public void otherError(Response response) {
                System.out.println("Other error: " + response.message());
            }
        };
    }
}
