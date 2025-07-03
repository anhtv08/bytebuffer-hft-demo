package com.hft.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Order message for HFT operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Serializable {
    
    private long orderId;
    private String symbol;
    private char side; // 'B' for buy, 'S' for sell
    private double price;
    private int quantity;
    private int filledQuantity;
    private char orderType; // 'M' for market, 'L' for limit
    private char timeInForce; // 'D' for day, 'I' for IOC, 'F' for FOK
    private long timestamp;
    private long clientOrderId;
    private char status; // 'N' for new, 'F' for filled, 'C' for cancelled
    
    /**
     * Get remaining quantity
     */
    public int getRemainingQuantity() {
        return quantity - filledQuantity;
    }
    
    /**
     * Check if order is fully filled
     */
    public boolean isFilled() {
        return filledQuantity >= quantity;
    }
    
    /**
     * Check if order is buy order
     */
    public boolean isBuy() {
        return side == 'B';
    }
    
    /**
     * Check if order is sell order
     */
    public boolean isSell() {
        return side == 'S';
    }
    
    /**
     * Create new buy order
     */
    public static Order createBuyOrder(long orderId, String symbol, double price, int quantity, 
                                      char orderType, long clientOrderId) {
        return Order.builder()
                .orderId(orderId)
                .symbol(symbol)
                .side('B')
                .price(price)
                .quantity(quantity)
                .filledQuantity(0)
                .orderType(orderType)
                .timeInForce('D')
                .timestamp(System.nanoTime())
                .clientOrderId(clientOrderId)
                .status('N')
                .build();
    }
    
    /**
     * Create new sell order
     */
    public static Order createSellOrder(long orderId, String symbol, double price, int quantity, 
                                       char orderType, long clientOrderId) {
        return Order.builder()
                .orderId(orderId)
                .symbol(symbol)
                .side('S')
                .price(price)
                .quantity(quantity)
                .filledQuantity(0)
                .orderType(orderType)
                .timeInForce('D')
                .timestamp(System.nanoTime())
                .clientOrderId(clientOrderId)
                .status('N')
                .build();
    }
    
    /**
     * Fill order partially or completely
     */
    public void fill(int fillQuantity) {
        this.filledQuantity = Math.min(this.filledQuantity + fillQuantity, this.quantity);
        if (isFilled()) {
            this.status = 'F';
        }
    }
    
    /**
     * Cancel order
     */
    public void cancel() {
        this.status = 'C';
    }
    
    @Override
    public String toString() {
        return String.format("[%c] %s %s: %.4f x %d/%d (id=%d, client=%d, status=%c)", 
                side, symbol, orderType, price, filledQuantity, quantity, orderId, clientOrderId, status);
    }
}