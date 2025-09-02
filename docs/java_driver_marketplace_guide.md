# Java Driver Marketplace Transaction Guide

## Overview

This guide documents the Java driver implementation for SmartChainDB marketplace transactions. The Java driver provides a complete set of methods for creating `ADVERTISEMENT`, `BUY_OFFER`, `SELL`, `REQUEST_RETURN`, and `ACCEPT_RETURN` transactions, following the same patterns and validation rules as the server-side Python implementation.

## Architecture

### Key Components

1. **Operations Enum**: Extended with new marketplace transaction types
2. **Transactions Class**: Contains factory methods for all transaction types
3. **BigchainDbTransactionBuilder**: Updated to support new operations
4. **MarketplaceTransactionExamples**: Comprehensive examples and demonstrations

### Transaction Flow

```
CREATE → ADVERTISEMENT → BUY_OFFER → SELL → REQUEST_RETURN → ACCEPT_RETURN
```

## Implementation Details

### 1. Operations Enum Extension

The `Operations` enum has been extended with new marketplace transaction types:

```java
public enum Operations {
    // Existing operations...
    CREATE("CREATE"),
    TRANSFER("TRANSFER"),
    // ... other existing operations
    
    // New marketplace operations
    ADVERTISEMENT("ADVERTISEMENT"),
    BUY_OFFER("BUY_OFFER"),
    SELL("SELL"),
    REQUEST_RETURN("REQUEST_RETURN"),
    ACCEPT_RETURN("ACCEPT_RETURN");
}
```

### 2. Transaction Construction Methods

#### ADVERTISEMENT Transaction

```java
public static String doAdvertisement(BigchainDBJavaDriver driver, String assetId, 
                                   MetaData metaData, KeyPair keys) throws Exception
```

**Purpose**: Creates an advertisement for listing an asset for sale.

**Parameters**:
- `driver`: BigchainDB driver instance
- `assetId`: ID of the asset to advertise
- `metaData`: Advertisement metadata (status, advertiser_public_key, price, etc.)
- `keys`: Keys to sign the transaction

**Required Metadata**:
- `status`: "OPEN", "LOCKED", or "CLOSED"
- `advertiser_public_key`: Public key of the advertiser
- `price`: Price of the asset
- `description`: Description of the asset

**Example**:
```java
MetaData metaData = new MetaData();
metaData.setMetaData("status", "OPEN");
metaData.setMetaData("advertiser_public_key", publicKey);
metaData.setMetaData("price", "1000.00");
metaData.setMetaData("description", "High-quality digital asset");

String advertisementId = Transactions.doAdvertisement(driver, assetId, metaData, keys);
```

#### BUY_OFFER Transaction

```java
public static String doBuyOffer(BigchainDBJavaDriver driver, String assetId, String advertisementId,
                               MetaData metaData, KeyPair keys, KeyPair escrowKeys) throws Exception
```

**Purpose**: Creates a buy offer with escrow mechanism.

**Parameters**:
- `driver`: BigchainDB driver instance
- `assetId`: ID of the asset being offered for
- `advertisementId`: ID of the advertisement being responded to
- `metaData`: Buy offer metadata
- `keys`: Buyer's keys
- `escrowKeys`: Escrow account keys

**Required Metadata**:
- `buyer_public_key`: Public key of the buyer
- `offer_amount`: Amount being offered
- `offer_currency`: Currency of the offer
- `offer_timestamp`: When the offer was created
- `offer_expiry`: When the offer expires
- `escrow_public_key`: Public key of the escrow account

**Example**:
```java
MetaData metaData = new MetaData();
metaData.setMetaData("buyer_public_key", buyerPublicKey);
metaData.setMetaData("offer_amount", "1000.00");
metaData.setMetaData("offer_currency", "USD");
metaData.setMetaData("offer_timestamp", LocalDateTime.now().format(ISO_FORMATTER));
metaData.setMetaData("offer_expiry", LocalDateTime.now().plusDays(7).format(ISO_FORMATTER));
metaData.setMetaData("escrow_public_key", escrowPublicKey);

String buyOfferId = Transactions.doBuyOffer(driver, assetId, advertisementId, metaData, buyerKeys, escrowKeys);
```

#### SELL Transaction

```java
public static String doSell(BigchainDBJavaDriver driver, String assetId, String buyOfferId,
                           MetaData metaData, KeyPair keys, KeyPair buyerKeys) throws Exception
```

**Purpose**: Executes the sale with atomic transfers (asset to buyer, payment to seller).

**Parameters**:
- `driver`: BigchainDB driver instance
- `assetId`: ID of the asset being sold
- `buyOfferId`: ID of the buy offer being accepted
- `metaData`: Sell metadata
- `keys`: Seller's keys
- `buyerKeys`: Buyer's keys

**Required Metadata**:
- `seller_public_key`: Public key of the seller
- `buyer_public_key`: Public key of the buyer
- `sale_amount`: Amount of the sale
- `sale_currency`: Currency of the sale
- `sale_timestamp`: When the sale was executed

**Example**:
```java
MetaData metaData = new MetaData();
metaData.setMetaData("seller_public_key", sellerPublicKey);
metaData.setMetaData("buyer_public_key", buyerPublicKey);
metaData.setMetaData("sale_amount", "1000.00");
metaData.setMetaData("sale_currency", "USD");
metaData.setMetaData("sale_timestamp", LocalDateTime.now().format(ISO_FORMATTER));

String sellId = Transactions.doSell(driver, assetId, buyOfferId, metaData, sellerKeys, buyerKeys);
```

#### REQUEST_RETURN Transaction

```java
public static String doRequestReturn(BigchainDBJavaDriver driver, String assetId, String sellTransactionId,
                                    MetaData metaData, KeyPair keys) throws Exception
```

**Purpose**: Requests a return/refund for a completed sale.

**Parameters**:
- `driver`: BigchainDB driver instance
- `assetId`: ID of the asset being returned
- `sellTransactionId`: ID of the sell transaction being disputed
- `metaData`: Return request metadata
- `keys`: Requester's keys (typically buyer)

**Required Metadata**:
- `requester_public_key`: Public key of the requester
- `return_reason`: Reason for the return
- `return_request_timestamp`: When the return was requested
- `return_policy_details`: Return policy information

**Example**:
```java
MetaData metaData = new MetaData();
metaData.setMetaData("requester_public_key", requesterPublicKey);
metaData.setMetaData("return_reason", "Item not as described");
metaData.setMetaData("return_request_timestamp", LocalDateTime.now().format(ISO_FORMATTER));

Map<String, Object> returnPolicyDetails = new HashMap<>();
returnPolicyDetails.put("return_window_days", 30);
returnPolicyDetails.put("return_status", "PENDING");
metaData.setMetaData("return_policy_details", returnPolicyDetails);

String requestReturnId = Transactions.doRequestReturn(driver, assetId, sellTransactionId, metaData, keys);
```

#### ACCEPT_RETURN Transaction

```java
public static String doAcceptReturn(BigchainDBJavaDriver driver, String assetId, String requestReturnId,
                                   MetaData metaData, KeyPair keys) throws Exception
```

**Purpose**: Accepts a return request and processes the refund.

**Parameters**:
- `driver`: BigchainDB driver instance
- `assetId`: ID of the asset being returned
- `requestReturnId`: ID of the return request being accepted
- `metaData`: Return acceptance metadata
- `keys`: Accepter's keys (typically seller)

**Required Metadata**:
- `accepter_public_key`: Public key of the accepter
- `return_acceptance_timestamp`: When the return was accepted
- `refund_details`: Refund information
- `return_processing_notes`: Notes about the return processing

**Example**:
```java
MetaData metaData = new MetaData();
metaData.setMetaData("accepter_public_key", accepterPublicKey);
metaData.setMetaData("return_acceptance_timestamp", LocalDateTime.now().format(ISO_FORMATTER));

Map<String, Object> refundDetails = new HashMap<>();
refundDetails.put("refund_amount", "1000.00");
refundDetails.put("refund_currency", "USD");
refundDetails.put("refund_method", "Escrow return");
metaData.setMetaData("refund_details", refundDetails);

metaData.setMetaData("return_processing_notes", "Return accepted, processing refund");

String acceptReturnId = Transactions.doAcceptReturn(driver, assetId, requestReturnId, metaData, keys);
```

## Complete Example

### MarketplaceTransactionExamples Class

The `MarketplaceTransactionExamples` class provides comprehensive examples for all transaction types:

```java
public class MarketplaceTransactionExamples {
    
    // Individual transaction examples
    public static String createAdvertisementExample(...)
    public static String createBuyOfferExample(...)
    public static String createSellExample(...)
    public static String createRequestReturnExample(...)
    public static String createAcceptReturnExample(...)
    
    // Complete flow demonstration
    public static void demonstrateMarketplaceFlow(BigchainDBJavaDriver driver)
    
    // Validation examples
    public static void demonstrateValidationExamples()
}
```

### Running the Examples

```java
public static void main(String[] args) {
    try {
        // Initialize driver
        BigchainDBJavaDriver.setConfig();
        BigchainDBJavaDriver driver = new BigchainDBJavaDriver();
        
        // Run marketplace flow demonstration
        MarketplaceTransactionExamples.demonstrateMarketplaceFlow(driver);
        
        // Demonstrate validation examples
        MarketplaceTransactionExamples.demonstrateValidationExamples();
        
    } catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
        e.printStackTrace();
    }
}
```

## Key Features

### 1. Escrow Mechanism
- BUY_OFFER creates escrow for buyer's payment
- Funds are locked until sale completion or refund
- Automatic refund capability for failed sales

### 2. Atomic Transfers
- SELL performs two transfers atomically:
  - Asset ownership to buyer
  - Payment from escrow to seller
- Both transfers happen simultaneously

### 3. Return Process
- REQUEST_RETURN initiates return process
- ACCEPT_RETURN processes refund
- Complete audit trail of return transactions

### 4. Validation
- Server-side validation ensures data integrity
- Client-side validation can be added for better UX
- Comprehensive error handling

## Integration with Existing System

### BigchainDbTransactionBuilder Updates

The `BigchainDbTransactionBuilder` has been updated to support the new operations:

```java
private void buildHelper() throws Exception {
    this.transaction = new Transaction();
    if (this.operation == Operations.CREATE
            || this.operation == Operations.TRANSFER
            // ... existing operations
            || this.operation == Operations.ADVERTISEMENT
            || this.operation == Operations.BUY_OFFER
            || this.operation == Operations.SELL
            || this.operation == Operations.REQUEST_RETURN
            || this.operation == Operations.ACCEPT_RETURN) {
        this.transaction.setOperation(this.operation.name());
    } else {
        throw new Exception("Invalid Operations value...");
    }
    // ... rest of the method
}
```

### Error Handling

All transaction methods include comprehensive error handling:

```java
try {
    // Transaction creation logic
    transaction = builder.sendTransaction(driver.handleServerResponse("OPERATION", metaData, null));
    System.out.println("(*) OPERATION Transaction sent.. - " + transaction.getId());
} catch (IOException e) {
    e.printStackTrace();
}
```

## Best Practices

### 1. Key Management
- Use proper key generation for all participants
- Keep private keys secure
- Use different keys for different roles (buyer, seller, escrow)

### 2. Metadata Validation
- Always validate metadata before creating transactions
- Use consistent timestamp formats
- Include all required fields

### 3. Error Handling
- Always handle exceptions properly
- Log transaction IDs for debugging
- Implement retry logic for network failures

### 4. Transaction Monitoring
- Monitor transaction status after submission
- Implement callbacks for transaction completion
- Handle transaction failures gracefully

## Security Considerations

### 1. Input Validation
- Validate all input parameters
- Sanitize metadata fields
- Check transaction references

### 2. Signature Verification
- Ensure all transactions are properly signed
- Verify key ownership
- Validate fulfillment conditions

### 3. Escrow Security
- Use secure escrow account management
- Implement proper access controls
- Monitor escrow account activities

## Performance Considerations

### 1. Transaction Size
- Keep metadata minimal
- Use efficient data structures
- Optimize asset references

### 2. Network Optimization
- Batch transactions when possible
- Use connection pooling
- Implement proper timeout handling

### 3. Caching
- Cache frequently accessed data
- Implement transaction result caching
- Use efficient data structures

## Troubleshooting

### Common Issues

1. **Invalid Operation Error**
   - Ensure the operation is supported in the builder
   - Check the Operations enum values

2. **Metadata Validation Errors**
   - Verify all required fields are present
   - Check data types and formats

3. **Signature Errors**
   - Ensure keys are properly generated
   - Verify key ownership

4. **Network Errors**
   - Check network connectivity
   - Verify server configuration
   - Implement proper retry logic

### Debugging Tips

1. **Enable Logging**
   - Use appropriate log levels
   - Log transaction IDs
   - Include error details

2. **Transaction Tracing**
   - Track transaction flow
   - Monitor transaction status
   - Implement transaction history

3. **Error Analysis**
   - Analyze error patterns
   - Implement proper error reporting
   - Use debugging tools

## Conclusion

The Java driver implementation provides a complete solution for marketplace transactions in SmartChainDB. It follows the same patterns and validation rules as the server-side implementation, ensuring consistency and reliability. The comprehensive examples and documentation make it easy to integrate marketplace functionality into Java applications.

The implementation supports the full marketplace transaction lifecycle, from advertisement creation to return processing, with proper escrow mechanisms, atomic transfers, and comprehensive error handling. This makes it suitable for production use in marketplace applications.
