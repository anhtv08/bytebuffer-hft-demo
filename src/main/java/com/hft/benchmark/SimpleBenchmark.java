package com.hft.benchmark;

import com.hft.buffer.MarketDataBuffer;
import com.hft.model.MarketData;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

/**
 * Simple benchmark comparing ByteBuffer vs Object serialization
 */
@Slf4j
public class SimpleBenchmark {
    
    private static final int ITERATIONS = 100_000;
    
    public static void main(String[] args) {
        log.info("Simple ByteBuffer vs Object Serialization Benchmark");
        log.info("==================================================");
        
        SimpleBenchmark benchmark = new SimpleBenchmark();
        try {
            benchmark.runBenchmark();
        } catch (Exception e) {
            log.error("Benchmark failed", e);
        }
    }
    
    public void runBenchmark() throws Exception {
        // Create test data
        MarketData testData = MarketData.createQuote("AAPL", 150.25, 150.27, 1000, 1500, 12345L);
        
        // Warm up JVM
        warmUp(testData);
        
        // ByteBuffer benchmark
        long byteBufferTime = benchmarkByteBuffer(testData);
        
        // Java serialization benchmark  
        long javaSerializationTime = benchmarkJavaSerialization(testData);
        
        // Results
        log.info("\nBenchmark Results:");
        log.info("ByteBuffer: {} ms ({} ops/sec)", 
                byteBufferTime / 1_000_000,
                (long) ((double) ITERATIONS * 2 * 1_000_000_000L / byteBufferTime));
        
        log.info("Java Serialization: {} ms ({} ops/sec)", 
                javaSerializationTime / 1_000_000,
                (long) ((double) ITERATIONS * 2 * 1_000_000_000L / javaSerializationTime));
        
        log.info("Performance ratio: {:.1f}x faster with ByteBuffer", 
                (double) javaSerializationTime / byteBufferTime);
        
        log.info("ByteBuffer message size: {} bytes", MarketDataBuffer.MESSAGE_SIZE);
    }
    
    private void warmUp(MarketData testData) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(MarketDataBuffer.MESSAGE_SIZE);
        for (int i = 0; i < 10_000; i++) {
            MarketDataBuffer.serialize(testData, buffer);
            MarketDataBuffer.deserialize(buffer);
        }
        
        // Java serialization warm up
        for (int i = 0; i < 1000; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(testData);
            oos.close();
            
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            MarketData deserializedData = (MarketData) ois.readObject();
            ois.close();
        }
    }
    
    private long benchmarkByteBuffer(MarketData testData) {
        ByteBuffer buffer = ByteBuffer.allocate(MarketDataBuffer.MESSAGE_SIZE);
        
        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            MarketDataBuffer.serialize(testData, buffer);
            MarketDataBuffer.deserialize(buffer);
        }
        return System.nanoTime() - startTime;
    }
    
    private long benchmarkJavaSerialization(MarketData testData) throws Exception {
        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(testData);
            oos.close();
            
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            MarketData deserializedData = (MarketData) ois.readObject();
            ois.close();
        }
        return System.nanoTime() - startTime;
    }
}