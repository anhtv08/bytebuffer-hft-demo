package com.hft.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Market data message representing real-time price information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketData implements Serializable {
    
    private String symbol;
    private double bidPrice;
    private double askPrice;
    private int bidSize;
    private int askSize;
    private double lastPrice;
    private int lastSize;
    private long timestamp;
    private long sequenceNumber;
    private char messageType; // 'Q' for quote, 'T' for trade
    
    /**
     * Calculate mid price
     */
    public double getMidPrice() {
        return (bidPrice + askPrice) / 2.0;
    }
    
    /**
     * Calculate spread
     */
    public double getSpread() {
        return askPrice - bidPrice;
    }
    
    /**
     * Calculate spread in basis points
     */
    public double getSpreadBps() {
        return (getSpread() / getMidPrice()) * 10000;
    }
    
    /**
     * Create market data with current timestamp
     */
    public static MarketData createQuote(String symbol, double bidPrice, double askPrice, 
                                        int bidSize, int askSize, long sequenceNumber) {
        return MarketData.builder()
                .symbol(symbol)
                .bidPrice(bidPrice)
                .askPrice(askPrice)
                .bidSize(bidSize)
                .askSize(askSize)
                .timestamp(System.nanoTime())
                .sequenceNumber(sequenceNumber)
                .messageType('Q')
                .build();
    }
    
    /**
     * Create trade data with current timestamp
     */
    public static MarketData createTrade(String symbol, double price, int size, long sequenceNumber) {
        return MarketData.builder()
                .symbol(symbol)
                .lastPrice(price)
                .lastSize(size)
                .timestamp(System.nanoTime())
                .sequenceNumber(sequenceNumber)
                .messageType('T')
                .build();
    }
    
    @Override
    public String toString() {
        if (messageType == 'Q') {
            return String.format("[%s] %s: %.4f x %d | %.4f x %d (seq=%d)", 
                    messageType, symbol, bidPrice, bidSize, askPrice, askSize, sequenceNumber);
        } else {
            return String.format("[%s] %s: %.4f x %d (seq=%d)", 
                    messageType, symbol, lastPrice, lastSize, sequenceNumber);
        }
    }
}