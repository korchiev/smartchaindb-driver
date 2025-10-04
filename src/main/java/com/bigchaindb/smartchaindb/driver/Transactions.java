package com.bigchaindb.smartchaindb.driver;

import com.bigchaindb.builders.BigchainDbTransactionBuilder;
import com.bigchaindb.constants.Operations;
import com.bigchaindb.model.FulFill;
import com.bigchaindb.model.MetaData;
import com.bigchaindb.model.Transaction;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;

import java.io.IOException;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Transactions {

    /**
     * performs CREATE transactions on BigchainDB network
     *
     * @param assetData data to store as asset
     * @param metaData  data to store as metadata
     * @param keys      keys to sign and verify transaction
     * @return id of CREATED asset
     */
    public static String doCreate(BigchainDBJavaDriver driver, Map<String, Object> assetData, MetaData metaData, KeyPair keys) throws Exception {

        Transaction transaction = null;
        try {
            BigchainDbTransactionBuilder.IBuild builder = BigchainDbTransactionBuilder
                    .init()
                    .addAssets(assetData, TreeMap.class)
                    .addMetaData(metaData)
                    .operation(Operations.CREATE)
                    .buildAndSign((EdDSAPublicKey) keys.getPublic(), (EdDSAPrivateKey) keys.getPrivate());

            transaction = builder.sendTransaction(driver.handleServerResponse("CREATE", null, null));
            System.out.println("(*) CREATE Transaction sent.. - " + transaction.getId());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return transaction != null ? transaction.getId() : null;
    }

    /**
     * performs TRANSFER operations on CREATED assets
     *
     * @param txId     id of transaction/asset
     * @param metaData data to append for this transaction
     * @param keys     keys to sign and verify transactions
     */
    public static void doTransfer(BigchainDBJavaDriver driver, String txId, MetaData metaData, KeyPair keys, KeyPair transferKeys) throws Exception {

        Map<String, String> assetData = new TreeMap<String, String>();
        assetData.put("id", txId);

        try {

            FulFill fulfill = new FulFill();
            fulfill.setOutputIndex(0);
            fulfill.setTransactionId(txId);

            BigchainDbTransactionBuilder.IBuild builder = BigchainDbTransactionBuilder
                    .init()
                    .addInput(null, fulfill, (EdDSAPublicKey) keys.getPublic())
                    .addOutput("1", (EdDSAPublicKey) transferKeys.getPublic())
                    .addAssets(txId, String.class)
                    .addMetaData(metaData)
                    .operation(Operations.TRANSFER)
                    .buildAndSign((EdDSAPublicKey) keys.getPublic(), (EdDSAPrivateKey) keys.getPrivate());

            Transaction transaction = builder.sendTransaction(driver.handleServerResponse("TRANSFER", null, null));
            System.out.println("(*) TRANSFER Transaction sent.. - " + transaction.getId());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String doPreRequest(BigchainDBJavaDriver driver, MetaData metaData, KeyPair keys,
                                      String skey, String pkey, String payload) throws Exception {

        Transaction transaction = null;
        Map<String, String> assetData = new TreeMap<String, String>() {{
            put("", "");
        }};

        List<String> capabilityList = (List<String>) metaData.getMetadata().get("capability");
        metaData.setMetaData("inferredCapabilities", Simulation.mediate(capabilityList));

        try {
            BigchainDbTransactionBuilder.IBuild builder;
            if (DriverConstants.GROUP_SIGNATURE) {
                builder = BigchainDbTransactionBuilder
                        .init()
                        .addAssets(assetData, TreeMap.class)
                        .addMetaData(metaData)
                        .operation(Operations.REQUEST_FOR_QUOTE)
                        .buildAndSignG(skey, pkey, payload);
            } else {
                builder = BigchainDbTransactionBuilder
                        .init()
                        .addAssets(assetData, TreeMap.class)
                        .addMetaData(metaData)
                        .operation(Operations.REQUEST_FOR_QUOTE)
                        .buildAndSign((EdDSAPublicKey) keys.getPublic(), (EdDSAPrivateKey) keys.getPrivate());
            }

            transaction = builder.sendTransaction(driver.handleServerResponse("PRE_REQUEST", null, null));
            System.out.println("(*) PRE-REQUEST Transaction sent.. - " + transaction.getId());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return transaction != null ? transaction.getId() : null;
    }

    public static String doInterest(BigchainDBJavaDriver driver, String txId, String preRequestId, MetaData metaData, KeyPair keys) throws Exception {

        Map<String, String> assetData = new TreeMap<String, String>();
        assetData.put("id", txId);
        assetData.put("pre_request_id", preRequestId);

        try {
            Transaction transaction = BigchainDbTransactionBuilder
                    .init()
                    .addAssets(assetData, TreeMap.class)
                    .addMetaData(metaData)
                    .operation(Operations.INTEREST)
                    .buildAndSign((EdDSAPublicKey) keys.getPublic(), (EdDSAPrivateKey) keys.getPrivate())
                    .sendTransaction(driver.handleServerResponse("INTEREST", null, null));

            System.out.println("(*) INTEREST Transaction sent.. - " + transaction.getId());
            return transaction.getId();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Transaction doRequest(BigchainDBJavaDriver driver, String _txId, MetaData metaData, KeyPair keys,
                                        String skey, String pkey, String payload) throws Exception {

        Transaction transaction = null;
        Map<String, String> assetData = new TreeMap<String, String>() {{
            put("pre_request_id", _txId);
            put("deadline", "2021-02-21");
        }};

        List<String> capabilityList = (List<String>) metaData.getMetadata().get("capability");
        //metaData.setMetaData("inferredCapabilities", Simulation.mediate(capabilityList));

        try {
            BigchainDbTransactionBuilder.IBuild builder;
            if (DriverConstants.GROUP_SIGNATURE) {
                builder = BigchainDbTransactionBuilder
                        .init()
                        .addAssets(assetData, TreeMap.class)
                        .addMetaData(metaData)
                        .operation(Operations.REQUEST_FOR_QUOTE)
                        .buildAndSignG(skey, pkey, payload);
            } else {
                builder = BigchainDbTransactionBuilder
                        .init()
                        .addAssets(assetData, TreeMap.class)
                        .addMetaData(metaData)
                        .operation(Operations.REQUEST_FOR_QUOTE)
                        .buildAndSign((EdDSAPublicKey) keys.getPublic(), (EdDSAPrivateKey) keys.getPrivate());
            }

            String txId = builder.getTransaction().getId();
            transaction = builder.sendTransaction(driver.handleServerResponse("REQUEST_FOR_QUOTE", metaData, txId));
            System.out.println("(*) REQUEST Transaction sent.. - " + txId);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return transaction;
    }

    public static Transaction doBid(BigchainDBJavaDriver driver, String txId, String rfqTxId, MetaData metaData, KeyPair keys) throws Exception {

        Transaction transaction = null;
        Map<String, String> assetData = new TreeMap<String, String>();
        assetData.put("id", txId);
        assetData.put("rfq_id", rfqTxId);

        try {

            FulFill fulfill = new FulFill();
            fulfill.setOutputIndex(0);
            fulfill.setTransactionId(txId);

            BigchainDbTransactionBuilder.IBuild builder = BigchainDbTransactionBuilder
                    .init()
                    .addInput(null, fulfill, (EdDSAPublicKey) keys.getPublic())
                    .addOutput("1", DriverConstants.SMARTCHAINDB_PUBKEY)
                    .addAssets(assetData, TreeMap.class)
                    .addMetaData(metaData)
                    .operation(Operations.BID)
                    .buildAndSign((EdDSAPublicKey) keys.getPublic(), (EdDSAPrivateKey) keys.getPrivate());

            transaction = builder.sendTransaction(driver.handleServerResponse("BID", metaData, null));
            System.out.println("(*) BID Transaction sent.. - " + transaction.getId());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return transaction;
    }


    public static void doAccept(BigchainDBJavaDriver driver, String winningBidTxId, String rfqTxId, MetaData metaData, KeyPair keys) throws Exception {

        Map<String, String> assetData = new TreeMap<String, String>();
        assetData.put("rfq_id", rfqTxId);
        assetData.put("winner_bid_id", winningBidTxId);

        try {
            Transaction transaction = BigchainDbTransactionBuilder
                    .init()
                    .addAssets(assetData, TreeMap.class)
                    .addMetaData(metaData)
                    .operation(Operations.ACCEPT)
                    .buildAndSign((EdDSAPublicKey) keys.getPublic(), (EdDSAPrivateKey) keys.getPrivate())
                    .sendTransaction(driver.handleServerResponse("ACCEPT", metaData, null));

            System.out.println("(*) ACCEPT Transaction sent.. - " + transaction.getId());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates an ADVERTISEMENT transaction for listing an asset for sale
     *
     * @param driver      BigchainDB driver instance
     * @param assetId     ID of the asset to advertise
     * @param metaData    Advertisement metadata (status, advertiser_public_key, price, etc.)
     * @param keys        Keys to sign the transaction
     * @return Transaction ID of the created advertisement
     */
    public static String doAdvertisement(BigchainDBJavaDriver driver, String assetId, MetaData metaData, KeyPair keys) throws Exception {
        Transaction transaction = null;
        
        try {
            // For ADVERTISEMENT, we don't fulfill anything - just announce availability
            // Similar to CREATE transactions, ADVERTISEMENT transactions have no inputs to fulfill
            
            BigchainDbTransactionBuilder.IBuild builder = BigchainDbTransactionBuilder
                    .init()
                    .addInput(null, null, (EdDSAPublicKey) keys.getPublic()) // null fulfill = no fulfillment
                    .addAssets(assetId, String.class) // set asset.id (not asset.data)
                    .addMetaData(metaData)
                    .operation(Operations.ADVERTISEMENT)
                    .buildAndSign((EdDSAPublicKey) keys.getPublic(), (EdDSAPrivateKey) keys.getPrivate());
            
            transaction = builder.sendTransaction(driver.handleServerResponse("ADVERTISEMENT", metaData, null));
            System.out.println("(*) ADVERTISEMENT Transaction sent.. - " + transaction.getId());
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return transaction != null ? transaction.getId() : null;
    }

    /**
     * Convenience: creates a new asset via CREATE and immediately ADVERTISES it.
     * Ensures the ADVERTISEMENT references a valid CREATE transaction id.
     *
     * @param driver      BigchainDB driver instance
     * @param assetData   Asset payload to be used in the CREATE transaction
     * @param adMetaData  Advertisement metadata
     * @param keys        Keys to sign the transactions
     * @return The ADVERTISEMENT transaction id, or null if creation failed
     */
    public static String doCreateThenAdvertise(BigchainDBJavaDriver driver,
                                               Map<String, Object> assetData,
                                               MetaData adMetaData,
                                               KeyPair keys) throws Exception {
        // 1) CREATE the asset
        MetaData emptyCreateMeta = new MetaData();
        String createdAssetTxId = doCreate(driver, assetData, emptyCreateMeta, keys);
        if (createdAssetTxId == null) {
            return null;
        }

        // 2) ADVERTISE referencing the created asset id
        return doAdvertisement(driver, createdAssetTxId, adMetaData, keys);
    }
    
    /**
     * Creates a BUY_OFFER transaction with escrow mechanism
     *
     * @param driver          BigchainDB driver instance
     * @param assetId         ID of the asset being offered for
     * @param advertisementId ID of the advertisement being responded to
     * @param metaData        Buy offer metadata (buyer_public_key, offer_amount, escrow_public_key, etc.)
     * @param keys            Keys to sign the transaction
     * @param escrowKeys      Escrow account keys
     * @return Transaction ID of the created buy offer
     */
    public static String doBuyOffer(BigchainDBJavaDriver driver, String assetId, String advertisementId, 
                                   MetaData metaData, KeyPair keys, KeyPair escrowKeys) throws Exception {
        Transaction transaction = null;
        
        // Asset data for buy offer - need asset.id and asset.data.advertisement_id
        Map<String, Object> assetData = new TreeMap<String, Object>();
        assetData.put("id", assetId);
        assetData.put("advertisement_id", advertisementId); // This will go under asset.data
        
        try {
            // Create input for buyer's payment asset
            FulFill fulfill = new FulFill();
            fulfill.setOutputIndex(0);
            // Use payment_asset_id from metadata if provided, otherwise use assetId (old behavior)
            String paymentAssetId = (String) metaData.getMetadata().get("payment_asset_id");
            fulfill.setTransactionId(paymentAssetId != null ? paymentAssetId : assetId);
            
            // Get offer amount from metadata (must be an integer string per schema)
            String offerAmount = metaData.getMetadata().get("offer_amount").toString();
            
            BigchainDbTransactionBuilder.IBuild builder = BigchainDbTransactionBuilder
                    .init()
                    .addInput(null, fulfill, (EdDSAPublicKey) keys.getPublic())
                    .addOutput(offerAmount, (EdDSAPublicKey) escrowKeys.getPublic())
                    .addAssets(assetData, Map.class)
                    .addMetaData(metaData)
                    .operation(Operations.BUY_OFFER)
                    .buildAndSign((EdDSAPublicKey) keys.getPublic(), (EdDSAPrivateKey) keys.getPrivate());
            
            transaction = builder.sendTransaction(driver.handleServerResponse("BUY_OFFER", metaData, null));
            System.out.println("(*) BUY_OFFER Transaction sent.. - " + transaction.getId());
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return transaction != null ? transaction.getId() : null;
    }
    
    /**
     * Creates a SELL transaction with atomic transfers
     *
     * @param driver      BigchainDB driver instance
     * @param assetId     ID of the asset being sold
     * @param buyOfferId  ID of the buy offer being accepted
     * @param metaData    Sell metadata (seller_public_key, buyer_public_key, sale_amount, etc.)
     * @param keys        Keys to sign the transaction
     * @param buyerKeys   Buyer's keys for asset transfer
     * @return Transaction ID of the created sell transaction
     */
    public static String doSell(BigchainDBJavaDriver driver, String assetId, String buyOfferId, 
                               MetaData metaData, KeyPair sellerKeys, KeyPair buyerKeys, KeyPair escrowKeys) throws Exception {
        Transaction transaction = null;
        
        // Asset data for sell - need asset.id and asset.data.buy_offer_id
        Map<String, Object> assetData = new TreeMap<String, Object>();
        assetData.put("id", assetId);
        assetData.put("buy_offer_id", buyOfferId); // This will go under asset.data
        
        try {
            // Note: SELL has one input (seller's asset) and two outputs (asset→buyer, payment→seller)
            // The escrowed payment from BUY_OFFER is validated but not spent as a UTXO input
            // This is a simplified atomic swap where the payment transfer is implicit
            
            // Input: Seller's asset
            FulFill assetFulfill = new FulFill();
            assetFulfill.setOutputIndex(0);
            assetFulfill.setTransactionId(assetId);
            
            // Get sale amount from metadata (must be an integer string per schema)
            String saleAmount = metaData.getMetadata().get("sale_amount").toString();
            
            BigchainDbTransactionBuilder.IBuild builder = BigchainDbTransactionBuilder
                    .init()
                    .addInput(null, assetFulfill, (EdDSAPublicKey) sellerKeys.getPublic())  // Seller signs for asset
                    .addOutput("1", (EdDSAPublicKey) buyerKeys.getPublic())     // Output 1: Asset → Buyer
                    .addOutput(saleAmount, (EdDSAPublicKey) sellerKeys.getPublic()) // Output 2: Payment → Seller
                    .addAssets(assetData, Map.class)
                    .addMetaData(metaData)
                    .operation(Operations.SELL)
                    .buildAndSign((EdDSAPublicKey) sellerKeys.getPublic(), (EdDSAPrivateKey) sellerKeys.getPrivate());
            
            transaction = builder.sendTransaction(driver.handleServerResponse("SELL", metaData, null));
            System.out.println("(*) SELL Transaction sent.. - " + transaction.getId());
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return transaction != null ? transaction.getId() : null;
    }
    
    /**
     * Creates a REQUEST_RETURN transaction
     *
     * @param driver            BigchainDB driver instance
     * @param assetId           ID of the asset being returned
     * @param sellTransactionId ID of the sell transaction being disputed
     * @param metaData          Return request metadata (requester_public_key, return_reason, etc.)
     * @param keys              Keys to sign the transaction
     * @return Transaction ID of the created return request
     */
    public static String doRequestReturn(BigchainDBJavaDriver driver, String assetId, String sellTransactionId, 
                                        MetaData metaData, KeyPair keys) throws Exception {
        Transaction transaction = null;
        
        // Asset data for request return
        Map<String, String> assetData = new TreeMap<String, String>();
        assetData.put("id", assetId);
        assetData.put("sell_transaction_id", sellTransactionId);
        
        try {
            // Create input for the asset being returned
            FulFill fulfill = new FulFill();
            fulfill.setOutputIndex(0);
            fulfill.setTransactionId(sellTransactionId);
            
            BigchainDbTransactionBuilder.IBuild builder = BigchainDbTransactionBuilder
                    .init()
                    .addInput(null, fulfill, (EdDSAPublicKey) keys.getPublic())
                    .addAssets(assetData, TreeMap.class)
                    .addMetaData(metaData)
                    .operation(Operations.REQUEST_RETURN)
                    .buildAndSign((EdDSAPublicKey) keys.getPublic(), (EdDSAPrivateKey) keys.getPrivate());
            
            transaction = builder.sendTransaction(driver.handleServerResponse("REQUEST_RETURN", metaData, null));
            System.out.println("(*) REQUEST_RETURN Transaction sent.. - " + transaction.getId());
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return transaction != null ? transaction.getId() : null;
    }
    
    /**
     * Creates an ACCEPT_RETURN transaction
     *
     * @param driver            BigchainDB driver instance
     * @param assetId           ID of the asset being returned
     * @param requestReturnId   ID of the return request being accepted
     * @param metaData          Return acceptance metadata (accepter_public_key, refund_details, etc.)
     * @param keys              Keys to sign the transaction
     * @return Transaction ID of the created accept return transaction
     */
    public static String doAcceptReturn(BigchainDBJavaDriver driver, String assetId, String requestReturnId, 
                                       MetaData metaData, KeyPair keys) throws Exception {
        Transaction transaction = null;
        
        // Asset data for accept return
        Map<String, String> assetData = new TreeMap<String, String>();
        assetData.put("id", assetId);
        assetData.put("request_return_id", requestReturnId);
        
        try {
            // Create input for processing the return
            FulFill fulfill = new FulFill();
            fulfill.setOutputIndex(0);
            fulfill.setTransactionId(requestReturnId);
            
            BigchainDbTransactionBuilder.IBuild builder = BigchainDbTransactionBuilder
                    .init()
                    .addInput(null, fulfill, (EdDSAPublicKey) keys.getPublic())
                    .addAssets(assetData, TreeMap.class)
                    .addMetaData(metaData)
                    .operation(Operations.ACCEPT_RETURN)
                    .buildAndSign((EdDSAPublicKey) keys.getPublic(), (EdDSAPrivateKey) keys.getPrivate());
            
            transaction = builder.sendTransaction(driver.handleServerResponse("ACCEPT_RETURN", metaData, null));
            System.out.println("(*) ACCEPT_RETURN Transaction sent.. - " + transaction.getId());
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return transaction != null ? transaction.getId() : null;
    }

    private static Transaction validateTransaction(Map<String, String> assetData, MetaData metaData, KeyPair keys, Operations operation) throws Exception {

        BigchainDbTransactionBuilder.IBuild builder = BigchainDbTransactionBuilder
                .init()
                .addAssets(assetData, TreeMap.class)
                .addMetaData(metaData)
                .operation(operation)
                .buildAndSign((EdDSAPublicKey) keys.getPublic(), (EdDSAPrivateKey) keys.getPrivate());

        return builder.validateTransaction();
    }
}
