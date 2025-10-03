package com.bigchaindb.smartchaindb.driver;

import com.bigchaindb.model.MetaData;
import com.bigchaindb.model.Transaction;

import java.security.KeyPair;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive examples for creating marketplace transactions in SmartChainDB.
 * This class demonstrates how to create ADVERTISEMENT, BUY_OFFER, SELL, 
 * REQUEST_RETURN, and ACCEPT_RETURN transactions using the Java driver.
 * 
 * @author SmartChainDB Team
 */
public class MarketplaceTransactionExamples {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static String repeatChar(char ch, int count) {
        char[] arr = new char[count];
        java.util.Arrays.fill(arr, ch);
        return new String(arr);
    }

    /**
     * Example: Creating an ADVERTISEMENT transaction
     * 
     * @param driver BigchainDB driver instance
     * @param assetId ID of the asset to advertise
     * @param advertiserKeys Keys of the advertiser
     * @return Transaction ID of the created advertisement
     */
    public static String createAdvertisementExample(BigchainDBJavaDriver driver, String assetId, KeyPair advertiserKeys) throws Exception {
        System.out.println("\n=== ADVERTISEMENT Transaction Example ===");
        
        // Create metadata for advertisement
        MetaData metaData = new MetaData();
        metaData.setMetaData("status", "OPEN");
        metaData.setMetaData("advertiser_public_key", ((net.i2p.crypto.eddsa.EdDSAPublicKey) advertiserKeys.getPublic()).toString());
        metaData.setMetaData("price", "1000.00");
        metaData.setMetaData("description", "High-quality digital asset for sale");
        metaData.setMetaData("category", "Digital Art");
        metaData.setMetaData("condition", "New");
        metaData.setMetaData("expiry_date", LocalDateTime.now(Clock.systemUTC()).plusDays(30).format(ISO_FORMATTER));
        metaData.setMetaData("contact_info", "advertiser@example.com");
        metaData.setMetaData("location", "New York, NY");
        
        // Create advertisement transaction
        String advertisementId = Transactions.doAdvertisement(driver, assetId, metaData, advertiserKeys);
        
        if (advertisementId != null) {
            System.out.println("Advertisement transaction created successfully");
            System.out.println("   Transaction ID: " + advertisementId);
            System.out.println("   Asset ID: " + assetId);
            System.out.println("   Status: " + metaData.getMetadata().get("status"));
            System.out.println("   Price: " + metaData.getMetadata().get("price"));
        } else {
            System.out.println(" Failed to create advertisement transaction");
        }
        
        return advertisementId;
    }

    /**
     * Example: Creating a BUY_OFFER transaction with escrow
     * 
     * @param driver BigchainDB driver instance
     * @param assetId ID of the asset being offered for
     * @param advertisementId ID of the advertisement being responded to
     * @param buyerKeys Keys of the buyer
     * @param escrowKeys Keys of the escrow account
     * @return Transaction ID of the created buy offer
     */
    public static String createBuyOfferExample(BigchainDBJavaDriver driver, String assetId, String advertisementId, 
                                             KeyPair buyerKeys, KeyPair escrowKeys) throws Exception {
        System.out.println("\n=== BUY_OFFER Transaction Example ===");
        
        // Create metadata for buy offer
        MetaData metaData = new MetaData();
        metaData.setMetaData("buyer_public_key", ((net.i2p.crypto.eddsa.EdDSAPublicKey) buyerKeys.getPublic()).toString());
        metaData.setMetaData("offer_amount", "1000.00");
        metaData.setMetaData("offer_currency", "USD");
        metaData.setMetaData("offer_timestamp", LocalDateTime.now(Clock.systemUTC()).format(ISO_FORMATTER));
        metaData.setMetaData("offer_expiry", LocalDateTime.now(Clock.systemUTC()).plusDays(7).format(ISO_FORMATTER));
        metaData.setMetaData("escrow_public_key", ((net.i2p.crypto.eddsa.EdDSAPublicKey) escrowKeys.getPublic()).toString());
        metaData.setMetaData("offer_notes", "Interested in purchasing this asset");
        
        // Create buy offer transaction
        String buyOfferId = Transactions.doBuyOffer(driver, assetId, advertisementId, metaData, buyerKeys, escrowKeys);
        
        if (buyOfferId != null) {
            System.out.println(" Buy offer transaction created successfully");
            System.out.println("   Transaction ID: " + buyOfferId);
            System.out.println("   Asset ID: " + assetId);
            System.out.println("   Advertisement ID: " + advertisementId);
            System.out.println("   Offer Amount: " + metaData.getMetadata().get("offer_amount"));
            System.out.println("   Offer Currency: " + metaData.getMetadata().get("offer_currency"));
        } else {
            System.out.println("Failed to create buy offer transaction");
        }
        
        return buyOfferId;
    }

    /**
     * Example: Creating a SELL transaction with atomic transfers
     * 
     * @param driver BigchainDB driver instance
     * @param assetId ID of the asset being sold
     * @param buyOfferId ID of the buy offer being accepted
     * @param sellerKeys Keys of the seller
     * @param buyerKeys Keys of the buyer
     * @return Transaction ID of the created sell transaction
     */
    public static String createSellExample(BigchainDBJavaDriver driver, String assetId, String buyOfferId, 
                                         KeyPair sellerKeys, KeyPair buyerKeys) throws Exception {
        System.out.println("\n=== SELL Transaction Example ===");
        
        // Create metadata for sell transaction
        MetaData metaData = new MetaData();
        metaData.setMetaData("seller_public_key", ((net.i2p.crypto.eddsa.EdDSAPublicKey) sellerKeys.getPublic()).toString());
        metaData.setMetaData("buyer_public_key", ((net.i2p.crypto.eddsa.EdDSAPublicKey) buyerKeys.getPublic()).toString());
        metaData.setMetaData("sale_amount", "1000.00");
        metaData.setMetaData("sale_currency", "USD");
        metaData.setMetaData("sale_timestamp", LocalDateTime.now(Clock.systemUTC()).format(ISO_FORMATTER));
        metaData.setMetaData("sale_notes", "Asset sold as agreed in buy offer");
        
        // Create sell transaction
        String sellId = Transactions.doSell(driver, assetId, buyOfferId, metaData, sellerKeys, buyerKeys);
        
        if (sellId != null) {
            System.out.println(" Sell transaction created successfully");
            System.out.println("   Transaction ID: " + sellId);
            System.out.println("   Asset ID: " + assetId);
            System.out.println("   Buy Offer ID: " + buyOfferId);
            System.out.println("   Sale Amount: " + metaData.getMetadata().get("sale_amount"));
            System.out.println("   Sale Currency: " + metaData.getMetadata().get("sale_currency"));
        } else {
            System.out.println(" Failed to create sell transaction");
        }
        
        return sellId;
    }

    /**
     * Example: Creating a REQUEST_RETURN transaction
     * 
     * @param driver BigchainDB driver instance
     * @param assetId ID of the asset being returned
     * @param sellTransactionId ID of the sell transaction being disputed
     * @param buyerKeys Keys of the buyer (requester)
     * @return Transaction ID of the created return request
     */
    public static String createRequestReturnExample(BigchainDBJavaDriver driver, String assetId, String sellTransactionId, 
                                                   KeyPair buyerKeys) throws Exception {
        System.out.println("\n=== REQUEST_RETURN Transaction Example ===");
        
        // Create metadata for return request
        MetaData metaData = new MetaData();
        metaData.setMetaData("requester_public_key", ((net.i2p.crypto.eddsa.EdDSAPublicKey) buyerKeys.getPublic()).toString());
        metaData.setMetaData("return_reason", "Item not as described in advertisement");
        metaData.setMetaData("return_request_timestamp", LocalDateTime.now(Clock.systemUTC()).format(ISO_FORMATTER));
        
        // Create return policy details
        Map<String, Object> returnPolicyDetails = new HashMap<>();
        returnPolicyDetails.put("return_window_days", 30);
        returnPolicyDetails.put("return_status", "PENDING");
        returnPolicyDetails.put("return_condition", "Item must be in original condition");
        metaData.setMetaData("return_policy_details", returnPolicyDetails);
        
        metaData.setMetaData("additional_notes", "The item received does not match the description");
        
        // Create request return transaction
        String requestReturnId = Transactions.doRequestReturn(driver, assetId, sellTransactionId, metaData, buyerKeys);
        
        if (requestReturnId != null) {
            System.out.println("Request return transaction created successfully");
            System.out.println("   Transaction ID: " + requestReturnId);
            System.out.println("   Asset ID: " + assetId);
            System.out.println("   Sell Transaction ID: " + sellTransactionId);
            System.out.println("   Return Reason: " + metaData.getMetadata().get("return_reason"));
            System.out.println("   Return Status: " + ((Map<?, ?>) metaData.getMetadata().get("return_policy_details")).get("return_status"));
        } else {
            System.out.println(" Failed to create request return transaction");
        }
        
        return requestReturnId;
    }

    /**
     * Example: Creating an ACCEPT_RETURN transaction
     * 
     * @param driver BigchainDB driver instance
     * @param assetId ID of the asset being returned
     * @param requestReturnId ID of the return request being accepted
     * @param sellerKeys Keys of the seller (accepter)
     * @return Transaction ID of the created accept return transaction
     */
    public static String createAcceptReturnExample(BigchainDBJavaDriver driver, String assetId, String requestReturnId, 
                                                  KeyPair sellerKeys) throws Exception {
        System.out.println("\n=== ACCEPT_RETURN Transaction Example ===");
        
        // Create metadata for accept return
        MetaData metaData = new MetaData();
        metaData.setMetaData("accepter_public_key", ((net.i2p.crypto.eddsa.EdDSAPublicKey) sellerKeys.getPublic()).toString());
        metaData.setMetaData("return_acceptance_timestamp", LocalDateTime.now(Clock.systemUTC()).format(ISO_FORMATTER));
        
        // Create refund details
        Map<String, Object> refundDetails = new HashMap<>();
        refundDetails.put("refund_amount", "1000.00");
        refundDetails.put("refund_currency", "USD");
        refundDetails.put("refund_method", "Escrow return");
        metaData.setMetaData("refund_details", refundDetails);
        
        metaData.setMetaData("return_processing_notes", "Return accepted, processing refund through escrow");
        
        // Create accept return transaction
        String acceptReturnId = Transactions.doAcceptReturn(driver, assetId, requestReturnId, metaData, sellerKeys);
        
        if (acceptReturnId != null) {
            System.out.println("Accept return transaction created successfully");
            System.out.println("   Transaction ID: " + acceptReturnId);
            System.out.println("   Asset ID: " + assetId);
            System.out.println("   Request Return ID: " + requestReturnId);
            System.out.println("   Refund Amount: " + ((Map<?, ?>) metaData.getMetadata().get("refund_details")).get("refund_amount"));
            System.out.println("   Refund Currency: " + ((Map<?, ?>) metaData.getMetadata().get("refund_details")).get("refund_currency"));
            System.out.println("   Refund Method: " + ((Map<?, ?>) metaData.getMetadata().get("refund_details")).get("refund_method"));
        } else {
            System.out.println("Failed to create accept return transaction");
        }
        
        return acceptReturnId;
    }

    /**
     * Demonstrates the complete marketplace transaction flow
     * 
     * @param driver BigchainDB driver instance
     */
    public static void demonstrateMarketplaceFlow(BigchainDBJavaDriver driver) throws Exception {
        System.out.println("🚀 SmartChainDB Marketplace Transaction Flow Demo");
        System.out.println(repeatChar('=', 60));
        
        // Generate keypairs for all participants
        KeyPair advertiserKeys = BigchainDBJavaDriver.getKeys();
        KeyPair buyerKeys = BigchainDBJavaDriver.getKeys();
        KeyPair escrowKeys = BigchainDBJavaDriver.getKeys();
        
        System.out.println("📋 Generated Keypairs:");
        System.out.println("   Advertiser: " + ((net.i2p.crypto.eddsa.EdDSAPublicKey) advertiserKeys.getPublic()).toString());
        System.out.println("   Buyer: " + ((net.i2p.crypto.eddsa.EdDSAPublicKey) buyerKeys.getPublic()).toString());
        System.out.println("   Escrow: " + ((net.i2p.crypto.eddsa.EdDSAPublicKey) escrowKeys.getPublic()).toString());
        
        // Step 1: Create an asset first (simulate with a dummy asset ID)
        String assetId = "asset_" + System.currentTimeMillis();
        System.out.println("\n📦 Asset ID: " + assetId);
        
        // Step 2: Create advertisement
        String advertisementId = createAdvertisementExample(driver, assetId, advertiserKeys);
        
        // Step 3: Create buy offer
        String buyOfferId = null;
        if (advertisementId != null) {
            buyOfferId = createBuyOfferExample(driver, assetId, advertisementId, buyerKeys, escrowKeys);
        }
        
        // Step 4: Create sell transaction
        String sellId = null;
        if (buyOfferId != null) {
            sellId = createSellExample(driver, assetId, buyOfferId, advertiserKeys, buyerKeys);
        }
        
        // Step 5: Create return request (optional)
        String requestReturnId = null;
        if (sellId != null) {
            requestReturnId = createRequestReturnExample(driver, assetId, sellId, buyerKeys);
        }
        
        // Step 6: Accept return (optional)
        String acceptReturnId = null;
        if (requestReturnId != null) {
            acceptReturnId = createAcceptReturnExample(driver, assetId, requestReturnId, advertiserKeys);
        }
        
        // Summary
        System.out.println("\n" + repeatChar('=', 60));
        System.out.println("📊 Marketplace Transaction Flow Summary:");
        System.out.println(repeatChar('=', 60));
        
        String[] transactionTypes = {"ADVERTISEMENT", "BUY_OFFER", "SELL", "REQUEST_RETURN", "ACCEPT_RETURN"};
        String[] transactionIds = {advertisementId, buyOfferId, sellId, requestReturnId, acceptReturnId};
        
        for (int i = 0; i < transactionTypes.length; i++) {
            if (transactionIds[i] != null) {
                System.out.println("" + transactionTypes[i] + ": " + transactionIds[i]);
            } else {
                System.out.println("" + transactionTypes[i] + ": Failed to create");
            }
        }
        
        System.out.println("\n🎉 Marketplace transaction flow demonstration completed!");
        System.out.println("\n💡 Key Features Demonstrated:");
        System.out.println("   • Advertisement listing with metadata");
        System.out.println("   • Buy offer with escrow mechanism");
        System.out.println("   • Atomic sell transaction (asset + payment)");
        System.out.println("   • Return request with policy details");
        System.out.println("   • Return acceptance with refund processing");
    }

    /**
     * Demonstrates validation errors for each transaction type
     */
    public static void demonstrateValidationExamples() {
        System.out.println("\n=== Transaction Validation Examples ===");
        
        // Test invalid advertisement metadata
        System.out.println("\n--- Testing Invalid Advertisement Metadata ---");
        try {
            MetaData invalidMetaData = new MetaData();
            invalidMetaData.setMetaData("status", "INVALID_STATUS"); // Invalid status
            invalidMetaData.setMetaData("advertiser_public_key", "invalid_key");
            
            // This would fail validation on the server side
            System.out.println("Invalid metadata would be caught by server validation");
        } catch (Exception e) {
            System.out.println("Caught expected validation error: " + e.getMessage());
        }
        
        // Test invalid buy offer amount
        System.out.println("\n--- Testing Invalid Buy Offer Amount ---");
        try {
            MetaData invalidMetaData = new MetaData();
            invalidMetaData.setMetaData("offer_amount", "-100.00"); // Negative amount
            
            // This would fail validation on the server side
            System.out.println("Invalid amount would be caught by server validation");
        } catch (Exception e) {
            System.out.println("Caught expected validation error: " + e.getMessage());
        }
        
        System.out.println("\n💡 Validation Notes:");
        System.out.println("   • All validation is performed on the server side");
        System.out.println("   • Client-side validation can be added for better UX");
        System.out.println("   • Server validation ensures data integrity and security");
    }

    /**
     * Main method to run all examples
     */
    public static void main(String[] args) {
        try {
            // Initialize driver
            BigchainDBJavaDriver.setConfig();
            BigchainDBJavaDriver driver = new BigchainDBJavaDriver();
            
            // Run marketplace flow demonstration
            demonstrateMarketplaceFlow(driver);
            
            // Demonstrate validation examples
            demonstrateValidationExamples();
            
        } catch (Exception e) {
            System.err.println(" Error running marketplace examples: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
