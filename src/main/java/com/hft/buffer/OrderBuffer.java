package com.hft.buffer;

import com.hft.model.Order;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * High-performance ByteBuffer serialization for Order messages
 */
public class OrderBuffer {
    
    public static final int MESSAGE_SIZE = 64; // bytes
    
    // Field offsets
    private static final int ORDER_ID_OFFSET = 0;
    private static final int SYMBOL_OFFSET = 8;
    private static final int SYMBOL_LENGTH = 8;
    private static final int SIDE_OFFSET = 16;
    private static final int PRICE_OFFSET = 17;
    private static final int QUANTITY_OFFSET = 25;
    private static final int FILLED_QUANTITY_OFFSET = 29;
    private static final int ORDER_TYPE_OFFSET = 33;
    private static final int TIME_IN_FORCE_OFFSET = 34;
    private static final int TIMESTAMP_OFFSET = 35;
    private static final int CLIENT_ORDER_ID_OFFSET = 43;
    private static final int STATUS_OFFSET = 51;
    
    /**
     * Serialize Order to ByteBuffer
     */
    public static void serialize(Order order, ByteBuffer buffer) {
        buffer.clear();
        
        // Order ID
        buffer.putLong(order.getOrderId());
        
        // Symbol (8 bytes, null-padded)
        byte[] symbolBytes = order.getSymbol().getBytes(StandardCharsets.US_ASCII);
        buffer.put(symbolBytes, 0, Math.min(symbolBytes.length, SYMBOL_LENGTH));
        for (int i = symbolBytes.length; i < SYMBOL_LENGTH; i++) {
            buffer.put((byte) 0);
        }
        
        // Order fields
        buffer.put((byte) order.getSide());
        buffer.putDouble(order.getPrice());
        buffer.putInt(order.getQuantity());
        buffer.putInt(order.getFilledQuantity());
        buffer.put((byte) order.getOrderType());
        buffer.put((byte) order.getTimeInForce());
        buffer.putLong(order.getTimestamp());
        buffer.putLong(order.getClientOrderId());
        buffer.put((byte) order.getStatus());
        
        // Padding
        while (buffer.position() < MESSAGE_SIZE) {
            buffer.put((byte) 0);
        }
        
        buffer.flip();
    }
    
    /**
     * Deserialize ByteBuffer to Order
     */
    public static Order deserialize(ByteBuffer buffer) {
        buffer.rewind();
        
        // Read order ID
        long orderId = buffer.getLong();
        
        // Read symbol
        byte[] symbolBytes = new byte[SYMBOL_LENGTH];
        buffer.get(symbolBytes);
        String symbol = new String(symbolBytes, StandardCharsets.US_ASCII).trim();
        
        // Read order fields
        char side = (char) buffer.get();
        double price = buffer.getDouble();
        int quantity = buffer.getInt();
        int filledQuantity = buffer.getInt();
        char orderType = (char) buffer.get();
        char timeInForce = (char) buffer.get();
        long timestamp = buffer.getLong();
        long clientOrderId = buffer.getLong();
        char status = (char) buffer.get();
        
        return Order.builder()
                .orderId(orderId)
                .symbol(symbol)
                .side(side)
                .price(price)
                .quantity(quantity)
                .filledQuantity(filledQuantity)
                .orderType(orderType)
                .timeInForce(timeInForce)
                .timestamp(timestamp)
                .clientOrderId(clientOrderId)
                .status(status)
                .build();
    }
    
    /**
     * Direct field access methods
     */
    
    public static long getOrderId(ByteBuffer buffer) {
        return buffer.getLong(ORDER_ID_OFFSET);
    }
    
    public static String getSymbol(ByteBuffer buffer) {
        byte[] symbolBytes = new byte[SYMBOL_LENGTH];
        buffer.position(SYMBOL_OFFSET);
        buffer.get(symbolBytes);
        return new String(symbolBytes, StandardCharsets.US_ASCII).trim();
    }
    
    public static char getSide(ByteBuffer buffer) {
        return (char) buffer.get(SIDE_OFFSET);
    }
    
    public static double getPrice(ByteBuffer buffer) {
        return buffer.getDouble(PRICE_OFFSET);
    }
    
    public static int getQuantity(ByteBuffer buffer) {
        return buffer.getInt(QUANTITY_OFFSET);
    }
    
    public static int getFilledQuantity(ByteBuffer buffer) {
        return buffer.getInt(FILLED_QUANTITY_OFFSET);
    }
    
    public static char getOrderType(ByteBuffer buffer) {
        return (char) buffer.get(ORDER_TYPE_OFFSET);
    }
    
    public static char getTimeInForce(ByteBuffer buffer) {
        return (char) buffer.get(TIME_IN_FORCE_OFFSET);
    }
    
    public static long getTimestamp(ByteBuffer buffer) {
        return buffer.getLong(TIMESTAMP_OFFSET);
    }
    
    public static long getClientOrderId(ByteBuffer buffer) {
        return buffer.getLong(CLIENT_ORDER_ID_OFFSET);
    }
    
    public static char getStatus(ByteBuffer buffer) {
        return (char) buffer.get(STATUS_OFFSET);
    }
    
    /**
     * Update order status directly in buffer
     */
    public static void updateStatus(ByteBuffer buffer, char newStatus) {
        buffer.put(STATUS_OFFSET, (byte) newStatus);
    }
    
    /**
     * Update filled quantity directly in buffer
     */
    public static void updateFilledQuantity(ByteBuffer buffer, int newFilledQuantity) {
        buffer.putInt(FILLED_QUANTITY_OFFSET, newFilledQuantity);
    }
    
    /**
     * Check if order is fully filled
     */
    public static boolean isFilled(ByteBuffer buffer) {
        return getFilledQuantity(buffer) >= getQuantity(buffer);
    }
    
    /**
     * Get remaining quantity
     */
    public static int getRemainingQuantity(ByteBuffer buffer) {
        return getQuantity(buffer) - getFilledQuantity(buffer);
    }
}