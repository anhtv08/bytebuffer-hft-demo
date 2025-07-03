package com.hft;

import com.hft.benchmark.SimpleBenchmark;
import com.hft.buffer.MarketDataBuffer;
import com.hft.buffer.OrderBuffer;
import com.hft.model.MarketData;
import com.hft.model.Order;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

/**
 * Main demonstration class for ByteBuffer usage in High-Frequency Trading
 */
@Slf4j
public class ByteBufferHFTDemo {
    
    public static void main(String[] args) {
        log.info("ByteBuffer High-Frequency Trading Demo");
        log.info("======================================");
        log.info("Demonstrating ultra-low latency techniques using ByteBuffer for HFT applications");
        log.info("");
        
        String demoType = args.length > 0 ? args[0] : "all";
        
        try {
            switch (demoType.toLowerCase()) {
                case "benchmark":
                    runBenchmarkDemo();
                    break;
                case "basic":
                    runBasicDemo();
                    break;
                case "all":
                default:
                    runAllDemos();
                    break;
            }
        } catch (Exception e) {
            log.error("Demo failed", e);
        }
        
        log.info("");
        log.info("ByteBuffer HFT Demo completed successfully!");
        log.info("Check the implementation in src/main/java/com/hft/ for detailed examples");
    }
    
    private static void runAllDemos() throws Exception {
        log.info("Running comprehensive ByteBuffer HFT demonstration...");
        log.info("");
        
        runBasicDemo();
        Thread.sleep(1000);
        
        log.info("Starting ByteBuffer vs Object Serialization Benchmark...");
        runBenchmarkDemo();
    }
    
    private static void runBasicDemo() {
        log.info("=== Basic ByteBuffer Operations ===");
        
        try {
            // Create sample market data
            MarketData marketData = MarketData.createQuote("AAPL", 150.25, 150.27, 1000, 1500, 12345L);
            log.info("Created market data: {}", marketData);
            
            // Serialize to ByteBuffer
            ByteBuffer buffer = ByteBuffer.allocate(MarketDataBuffer.MESSAGE_SIZE);
            long serializeStart = System.nanoTime();
            MarketDataBuffer.serialize(marketData, buffer);
            long serializeTime = System.nanoTime() - serializeStart;
            
            log.info("Serialized to ByteBuffer in {} nanoseconds", serializeTime);
            log.info("Buffer size: {} bytes", buffer.remaining());
            
            // Deserialize from ByteBuffer
            long deserializeStart = System.nanoTime();
            MarketData deserialized = MarketDataBuffer.deserialize(buffer);
            long deserializeTime = System.nanoTime() - deserializeStart;
            
            log.info("Deserialized from ByteBuffer in {} nanoseconds", deserializeTime);
            log.info("Deserialized market data: {}", deserialized);
            
            // Direct field access (ultra-fast)
            buffer.rewind();
            long directAccessStart = System.nanoTime();
            double midPrice = MarketDataBuffer.getMidPrice(buffer);
            String symbol = MarketDataBuffer.getSymbol(buffer);
            long timestamp = MarketDataBuffer.getTimestamp(buffer);
            long directAccessTime = System.nanoTime() - directAccessStart;
            
            log.info("Direct field access in {} nanoseconds", directAccessTime);
            log.info("Symbol: {}, Mid Price: {}, Timestamp: {}", symbol, String.format("%.4f", midPrice), timestamp);
            
            // Order demo
            log.info("\n--- Order ByteBuffer Demo ---");
            Order order = Order.createBuyOrder(1001L, "AAPL", 150.25, 1000, 'L', 5001L);
            log.info("Created order: {}", order);
            
            ByteBuffer orderBuffer = ByteBuffer.allocate(OrderBuffer.MESSAGE_SIZE);
            OrderBuffer.serialize(order, orderBuffer);
            Order deserializedOrder = OrderBuffer.deserialize(orderBuffer);
            log.info("Deserialized order: {}", deserializedOrder);
            
            // Direct order field access
            orderBuffer.rewind();
            long orderId = OrderBuffer.getOrderId(orderBuffer);
            double price = OrderBuffer.getPrice(orderBuffer);
            int quantity = OrderBuffer.getQuantity(orderBuffer);
            log.info("Direct access - Order ID: {}, Price: {}, Quantity: {}", orderId, String.format("%.4f", price), quantity);
            
        } catch (Exception e) {
            log.error("Basic demo failed", e);
        }
    }
    
    private static void runBenchmarkDemo() {
        try {
            new SimpleBenchmark().main(new String[0]);
        } catch (Exception e) {
            log.error("Benchmark demo failed", e);
        }
    }
}