package com.hft.buffer;

import com.hft.model.MarketData;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * High-performance ByteBuffer serialization for MarketData
 * Optimized for minimal garbage collection and maximum throughput
 */
public class MarketDataBuffer {
    
    // Fixed message size for predictable performance
    public static final int MESSAGE_SIZE = 64; // bytes
    
    // Field offsets for direct access
    private static final int SYMBOL_OFFSET = 0;
    private static final int SYMBOL_LENGTH = 8; // max 8 chars
    private static final int BID_PRICE_OFFSET = 8;
    private static final int ASK_PRICE_OFFSET = 16;
    private static final int BID_SIZE_OFFSET = 24;
    private static final int ASK_SIZE_OFFSET = 28;
    private static final int LAST_PRICE_OFFSET = 32;
    private static final int LAST_SIZE_OFFSET = 40;
    private static final int TIMESTAMP_OFFSET = 44;
    private static final int SEQUENCE_OFFSET = 52;
    private static final int MESSAGE_TYPE_OFFSET = 60;
    
    /**
     * Serialize MarketData to ByteBuffer
     */
    public static void serialize(MarketData marketData, ByteBuffer buffer) {
        buffer.clear();
        
        // Symbol (8 bytes, null-padded)
        byte[] symbolBytes = marketData.getSymbol().getBytes(StandardCharsets.US_ASCII);
        buffer.put(symbolBytes, 0, Math.min(symbolBytes.length, SYMBOL_LENGTH));
        // Pad with zeros if symbol is shorter than 8 bytes
        for (int i = symbolBytes.length; i < SYMBOL_LENGTH; i++) {
            buffer.put((byte) 0);
        }
        
        // Prices and sizes
        buffer.putDouble(marketData.getBidPrice());
        buffer.putDouble(marketData.getAskPrice());
        buffer.putInt(marketData.getBidSize());
        buffer.putInt(marketData.getAskSize());
        buffer.putDouble(marketData.getLastPrice());
        buffer.putInt(marketData.getLastSize());
        
        // Timestamps and sequence
        buffer.putLong(marketData.getTimestamp());
        buffer.putLong(marketData.getSequenceNumber());
        
        // Message type
        buffer.put((byte) marketData.getMessageType());
        
        // Padding to reach fixed size
        while (buffer.position() < MESSAGE_SIZE) {
            buffer.put((byte) 0);
        }
        
        buffer.flip();
    }
    
    /**
     * Deserialize ByteBuffer to MarketData
     */
    public static MarketData deserialize(ByteBuffer buffer) {
        buffer.rewind();
        
        // Read symbol
        byte[] symbolBytes = new byte[SYMBOL_LENGTH];
        buffer.get(symbolBytes);
        String symbol = new String(symbolBytes, StandardCharsets.US_ASCII).trim();
        
        // Read prices and sizes
        double bidPrice = buffer.getDouble();
        double askPrice = buffer.getDouble();
        int bidSize = buffer.getInt();
        int askSize = buffer.getInt();
        double lastPrice = buffer.getDouble();
        int lastSize = buffer.getInt();
        
        // Read timestamps and sequence
        long timestamp = buffer.getLong();
        long sequenceNumber = buffer.getLong();
        
        // Read message type
        char messageType = (char) buffer.get();
        
        return MarketData.builder()
                .symbol(symbol)
                .bidPrice(bidPrice)
                .askPrice(askPrice)
                .bidSize(bidSize)
                .askSize(askSize)
                .lastPrice(lastPrice)
                .lastSize(lastSize)
                .timestamp(timestamp)
                .sequenceNumber(sequenceNumber)
                .messageType(messageType)
                .build();
    }
    
    /**
     * Direct field access methods for ultra-low latency
     */
    
    public static String getSymbol(ByteBuffer buffer) {
        byte[] symbolBytes = new byte[SYMBOL_LENGTH];
        buffer.position(SYMBOL_OFFSET);
        buffer.get(symbolBytes);
        return new String(symbolBytes, StandardCharsets.US_ASCII).trim();
    }
    
    public static double getBidPrice(ByteBuffer buffer) {
        return buffer.getDouble(BID_PRICE_OFFSET);
    }
    
    public static double getAskPrice(ByteBuffer buffer) {
        return buffer.getDouble(ASK_PRICE_OFFSET);
    }
    
    public static int getBidSize(ByteBuffer buffer) {
        return buffer.getInt(BID_SIZE_OFFSET);
    }
    
    public static int getAskSize(ByteBuffer buffer) {
        return buffer.getInt(ASK_SIZE_OFFSET);
    }
    
    public static double getLastPrice(ByteBuffer buffer) {
        return buffer.getDouble(LAST_PRICE_OFFSET);
    }
    
    public static int getLastSize(ByteBuffer buffer) {
        return buffer.getInt(LAST_SIZE_OFFSET);
    }
    
    public static long getTimestamp(ByteBuffer buffer) {
        return buffer.getLong(TIMESTAMP_OFFSET);
    }
    
    public static long getSequenceNumber(ByteBuffer buffer) {
        return buffer.getLong(SEQUENCE_OFFSET);
    }
    
    public static char getMessageType(ByteBuffer buffer) {
        return (char) buffer.get(MESSAGE_TYPE_OFFSET);
    }
    
    /**
     * Calculate mid price directly from buffer without object creation
     */
    public static double getMidPrice(ByteBuffer buffer) {
        return (getBidPrice(buffer) + getAskPrice(buffer)) / 2.0;
    }
    
    /**
     * Calculate spread directly from buffer
     */
    public static double getSpread(ByteBuffer buffer) {
        return getAskPrice(buffer) - getBidPrice(buffer);
    }
    
    /**
     * Batch serialize multiple MarketData objects
     */
    public static void serializeBatch(MarketData[] marketDataArray, ByteBuffer buffer) {
        buffer.clear();
        for (MarketData marketData : marketDataArray) {
            ByteBuffer temp = ByteBuffer.allocate(MESSAGE_SIZE);
            serialize(marketData, temp);
            buffer.put(temp);
        }
        buffer.flip();
    }
    
    /**
     * Batch deserialize multiple MarketData objects
     */
    public static MarketData[] deserializeBatch(ByteBuffer buffer, int count) {
        MarketData[] result = new MarketData[count];
        buffer.rewind();
        
        for (int i = 0; i < count; i++) {
            ByteBuffer slice = buffer.slice();
            slice.limit(MESSAGE_SIZE);
            result[i] = deserialize(slice);
            buffer.position(buffer.position() + MESSAGE_SIZE);
        }
        
        return result;
    }
}