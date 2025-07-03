package com.hft.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trade execution message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {
    
    private long tradeId;
    private String symbol;
    private double price;
    private int quantity;
    private long timestamp;
    private long buyOrderId;
    private long sellOrderId;
    private char aggressor; // 'B' for buy aggressor, 'S' for sell aggressor
    private double notional;
    
    /**
     * Calculate notional value
     */
    public double calculateNotional() {
        return price * quantity;
    }
    
    /**
     * Create trade from order matching
     */
    public static Trade createTrade(long tradeId, String symbol, double price, int quantity,
                                   long buyOrderId, long sellOrderId, char aggressor) {
        return Trade.builder()
                .tradeId(tradeId)
                .symbol(symbol)
                .price(price)
                .quantity(quantity)
                .timestamp(System.nanoTime())
                .buyOrderId(buyOrderId)
                .sellOrderId(sellOrderId)
                .aggressor(aggressor)
                .notional(price * quantity)
                .build();
    }
    
    @Override
    public String toString() {
        return String.format("[TRADE] %s: %.4f x %d (id=%d, buy=%d, sell=%d, aggressor=%c, notional=%.2f)", 
                symbol, price, quantity, tradeId, buyOrderId, sellOrderId, aggressor, notional);
    }
}