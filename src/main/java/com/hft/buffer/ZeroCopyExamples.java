package com.hft.buffer;
import sun.misc.Unsafe;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
public class ZeroCopyExamples {

    private static final Unsafe UNSAFE = getUnsafe();

    // ===== TRADITIONAL COPY vs ZERO-COPY COMPARISON =====

    public static class CopyComparison {

        public static void demonstrateTraditionalVsZeroCopy() {
            System.out.println("=== Traditional Copy vs Zero-Copy Comparison ===\n");

            byte[] sourceData = new byte[1024 * 1024]; // 1MB data
            Arrays.fill(sourceData, (byte) 42);

            // Traditional copy approach
            long traditionalTime = measureTraditionalCopy(sourceData);

            // Zero-copy approach
            long zeroCopyTime = measureZeroCopy(sourceData);

            System.out.printf("Traditional copy: %d ms\n", traditionalTime / 1_000_000);
            System.out.printf("Zero-copy:        %d ms\n", zeroCopyTime / 1_000_000);
            System.out.printf("Zero-copy is %.1fx faster\n\n",
                    (double) traditionalTime / zeroCopyTime);
        }

        private static long measureTraditionalCopy(byte[] sourceData) {
            long start = System.nanoTime();

            // Multiple unnecessary copies
            byte[] copy1 = new byte[sourceData.length];
            System.arraycopy(sourceData, 0, copy1, 0, sourceData.length);

            byte[] copy2 = new byte[copy1.length];
            System.arraycopy(copy1, 0, copy2, 0, copy1.length);

            byte[] finalCopy = new byte[copy2.length];
            System.arraycopy(copy2, 0, finalCopy, 0, copy2.length);

            return System.nanoTime() - start;
        }

        private static long measureZeroCopy(byte[] sourceData) {
            long start = System.nanoTime();

            // Zero-copy: Just pass references
            byte[] reference1 = sourceData;      // No copy
            byte[] reference2 = reference1;      // No copy
            byte[] finalReference = reference2;  // No copy

            // Actually access the data to ensure no compiler optimization
            byte checksum = finalReference[0];

            return System.nanoTime() - start;
        }
    }

    // ===== BYTEBUFFER ZERO-COPY TECHNIQUES =====

    public static class ByteBufferZeroCopy {

        public static void demonstrateByteBufferZeroCopy() {
            System.out.println("=== ByteBuffer Zero-Copy Techniques ===\n");

            // Create source data
            ByteBuffer sourceBuffer = ByteBuffer.allocateDirect(1024);
            for (int i = 0; i < 256; i++) {
                sourceBuffer.putInt(i);
            }
            sourceBuffer.flip();

            // 1. Slice - Zero-copy view of subset
            demonstrateSlice(sourceBuffer);

            // 2. Duplicate - Zero-copy independent cursor
            demonstrateDuplicate(sourceBuffer);

            // 3. View buffers - Zero-copy type conversion
            demonstrateViewBuffers(sourceBuffer);

            // 4. Bulk transfer - Zero-copy buffer to buffer
            demonstrateBulkTransfer(sourceBuffer);
        }

        private static void demonstrateSlice(ByteBuffer source) {
            System.out.println("1. SLICE - Zero-Copy Subset:");

            source.position(100).limit(200);
            ByteBuffer slice = source.slice(); // Zero-copy!

            System.out.println("Original buffer: pos=" + source.position() +
                    ", lim=" + source.limit() + ", cap=" + source.capacity());
            System.out.println("Sliced buffer:   pos=" + slice.position() +
                    ", lim=" + slice.limit() + ", cap=" + slice.capacity());

            // Modify slice affects original
            slice.putInt(0, 0xDEADBEEF);
            source.position(100);
            System.out.printf("Modified through slice: 0x%08X\n", source.getInt());
            System.out.println("✅ Same memory, zero copy!\n");
        }

        private static void demonstrateDuplicate(ByteBuffer source) {
            System.out.println("2. DUPLICATE - Zero-Copy Independent Cursor:");

            source.clear();
            ByteBuffer duplicate = source.duplicate(); // Zero-copy!

            // Independent positions
            source.position(10);
            duplicate.position(20);

            System.out.println("Original position: " + source.position());
            System.out.println("Duplicate position: " + duplicate.position());

            // Same underlying data
            source.putInt(100, 0x12345678);
            System.out.printf("Written through original: 0x%08X\n", duplicate.getInt(100));
            System.out.println("✅ Same memory, independent cursors, zero copy!\n");
        }

        private static void demonstrateViewBuffers(ByteBuffer source) {
            System.out.println("3. VIEW BUFFERS - Zero-Copy Type Conversion:");

            source.clear();

            // Create typed views - all point to same memory!
            var intView = source.asIntBuffer();
            var longView = source.asLongBuffer();
            var doubleView = source.asDoubleBuffer();

            // Write as int
            intView.put(0, 0x12345678);
            intView.put(1, 0x9ABCDEF0);

            // Read as long (combines two ints)
            long combined = longView.get(0);
            System.out.printf("Written as 2 ints: 0x%08X 0x%08X\n",
                    intView.get(0), intView.get(1));
            System.out.printf("Read as 1 long:    0x%016X\n", combined);

            // Read individual bytes
            System.out.print("Read as bytes:     ");
            for (int i = 0; i < 8; i++) {
                System.out.printf("0x%02X ", source.get(i));
            }
            System.out.println("\n✅ Same memory, different interpretations, zero copy!\n");
        }

        private static void demonstrateBulkTransfer(ByteBuffer source) {
            System.out.println("4. BULK TRANSFER - Zero-Copy Buffer-to-Buffer:");

            source.clear();
            ByteBuffer destination = ByteBuffer.allocateDirect(1024);

            // Fill source with pattern
            for (int i = 0; i < 100; i++) {
                source.putInt(i * i);
            }
            source.flip();

            long start = System.nanoTime();

            // Bulk copy - optimized at OS level
            destination.put(source); // Zero-copy when possible!

            long time = System.nanoTime() - start;

            System.out.printf("Bulk transferred %d bytes in %d ns\n",
                    source.capacity(), time);
            System.out.printf("Throughput: %.2f GB/s\n",
                    source.capacity() * 1_000_000_000.0 / time / (1024*1024*1024));
            System.out.println("✅ OS-optimized bulk copy!\n");
        }
    }

    // ===== FILE I/O ZERO-COPY =====

    public static class FileIOZeroCopy {

        public static void demonstrateFileZeroCopy() throws IOException {
            System.out.println("=== File I/O Zero-Copy Techniques ===\n");

            // Create test file
            File testFile = File.createTempFile("zerocopy", ".dat");
            testFile.deleteOnExit();

            // Write test data
            try (FileOutputStream fos = new FileOutputStream(testFile)) {
                for (int i = 0; i < 1000; i++) {
                    fos.write(("Test data line " + i + "\n").getBytes());
                }
            }

            // 1. Memory-mapped files - Ultimate zero-copy
            demonstrateMemoryMapping(testFile);

            // 2. Channel transfer - Zero-copy file operations
            demonstrateChannelTransfer(testFile);

            // 3. DirectByteBuffer I/O - Reduced copy
            demonstrateDirectBufferIO(testFile);
        }

        private static void demonstrateMemoryMapping(File file) throws IOException {
            System.out.println("1. MEMORY-MAPPED FILES - Ultimate Zero-Copy:");

            try (RandomAccessFile raf = new RandomAccessFile(file, "r");
                 FileChannel channel = raf.getChannel()) {

                // Map entire file into memory - zero-copy!
                MappedByteBuffer mapped = channel.map(
                        FileChannel.MapMode.READ_ONLY, 0, channel.size());

                System.out.println("File size: " + channel.size() + " bytes");
                System.out.println("Mapped buffer capacity: " + mapped.capacity());

                // Direct memory access - no I/O system calls!
                long start = System.nanoTime();
                byte checksum = 0;
                for (int i = 0; i < mapped.capacity(); i++) {
                    checksum ^= mapped.get(i);
                }
                long time = System.nanoTime() - start;

                System.out.printf("Processed file in %d ns (checksum: 0x%02X)\n", time, checksum);
                System.out.printf("Throughput: %.2f GB/s\n",
                        mapped.capacity() * 1_000_000_000.0 / time / (1024*1024*1024));
                System.out.println("✅ Direct memory access, no system calls!\n");
            }
        }

        private static void demonstrateChannelTransfer(File sourceFile) throws IOException {
            System.out.println("2. CHANNEL TRANSFER - Zero-Copy File Operations:");

            File destFile = File.createTempFile("zerocopy_dest", ".dat");
            destFile.deleteOnExit();

            try (FileChannel source = new FileInputStream(sourceFile).getChannel();
                 FileChannel dest = new FileOutputStream(destFile).getChannel()) {

                long start = System.nanoTime();

                // Zero-copy transfer at OS level!
                long transferred = source.transferTo(0, source.size(), dest);

                long time = System.nanoTime() - start;

                System.out.printf("Transferred %d bytes in %d ns\n", transferred, time);
                System.out.printf("Throughput: %.2f GB/s\n",
                        transferred * 1_000_000_000.0 / time / (1024*1024*1024));
                System.out.println("✅ OS kernel handles copy, no user-space copying!\n");
            }
        }

        private static void demonstrateDirectBufferIO(File file) throws IOException {
            System.out.println("3. DIRECT BUFFER I/O - Reduced Copy:");

            ByteBuffer buffer = ByteBuffer.allocateDirect(8192);

            try (FileChannel channel = new FileInputStream(file).getChannel()) {

                long start = System.nanoTime();
                long totalBytes = 0;

                while (channel.read(buffer) > 0) {
                    totalBytes += buffer.position();
                    buffer.clear();
                }

                long time = System.nanoTime() - start;

                System.out.printf("Read %d bytes in %d ns\n", totalBytes, time);
                System.out.printf("Throughput: %.2f GB/s\n",
                        totalBytes * 1_000_000_000.0 / time / (1024*1024*1024));
                System.out.println("✅ Direct buffer reduces kernel-to-user copies!\n");
            }
        }
    }

    // ===== NETWORK ZERO-COPY =====

    public static class NetworkZeroCopy {

        public static void demonstrateNetworkZeroCopy() {
            System.out.println("=== Network Zero-Copy Concepts ===\n");

            System.out.println("1. TRADITIONAL NETWORK I/O (Multiple Copies):");
            System.out.println("   File → Kernel Buffer → User Buffer → Socket Buffer → Network");
            System.out.println("   💸 4 copies, high CPU usage");
            System.out.println();

            System.out.println("2. SENDFILE ZERO-COPY:");
            System.out.println("   File → Kernel Buffer → Network (via DMA)");
            System.out.println("   ✅ 0 user-space copies, low CPU usage");
            System.out.println();

            System.out.println("3. DIRECT BUFFER NETWORK I/O:");
            System.out.println("   Direct Buffer → Socket (no intermediate copy)");
            System.out.println("   ✅ Reduced copies, better performance");
            System.out.println();

            demonstrateSocketChannelZeroCopy();
        }

        private static void demonstrateSocketChannelZeroCopy() {
            System.out.println("SOCKET CHANNEL EXAMPLE:");
            System.out.println("```java");
            System.out.println("// Zero-copy network sending");
            System.out.println("ByteBuffer buffer = ByteBuffer.allocateDirect(1024);");
            System.out.println("buffer.put(marketData.serialize());");
            System.out.println("buffer.flip();");
            System.out.println();
            System.out.println("// Direct buffer to network - minimal copying");
            System.out.println("socketChannel.write(buffer);");
            System.out.println("```");
            System.out.println("✅ Direct buffer content sent without user-space copying!");
        }
    }

    // ===== TRADING SYSTEM ZERO-COPY EXAMPLE =====

    public static class TradingSystemZeroCopy {

        // Message format: [Header][Symbol][Price][Quantity][Timestamp]
        private static final int HEADER_SIZE = 4;
        private static final int SYMBOL_SIZE = 8;
        private static final int PRICE_SIZE = 8;
        private static final int QUANTITY_SIZE = 4;
        private static final int TIMESTAMP_SIZE = 8;
        private static final int MESSAGE_SIZE = HEADER_SIZE + SYMBOL_SIZE + PRICE_SIZE + QUANTITY_SIZE + TIMESTAMP_SIZE;

        public static void demonstrateTradingZeroCopy() {
            System.out.println("\n=== Trading System Zero-Copy Implementation ===\n");

            // Pre-allocate message buffer (reused across messages)
            ByteBuffer messageBuffer = ByteBuffer.allocateDirect(MESSAGE_SIZE);
            messageBuffer.order(java.nio.ByteOrder.nativeOrder());

            // Simulate processing 1 million market data messages
            int numMessages = 1_000_000;
            long start = System.nanoTime();

            for (int i = 0; i < numMessages; i++) {
                // Zero-copy message processing
                processMarketDataZeroCopy(messageBuffer, i);
            }

            long time = System.nanoTime() - start;

            System.out.printf("Processed %d messages in %.2f ms\n",
                    numMessages, time / 1_000_000.0);
            System.out.printf("Throughput: %.0f messages/second\n",
                    numMessages * 1_000_000_000.0 / time);
            System.out.printf("Latency per message: %.2f ns\n",
                    (double) time / numMessages);
            System.out.println();

            demonstrateTraditionalVsZeroCopyParsing();
        }

        private static void processMarketDataZeroCopy(ByteBuffer buffer, int messageId) {
            // Clear buffer (sets position=0, limit=capacity)
            buffer.clear();

            // Zero-copy message construction using absolute positioning
            buffer.putInt(0, messageId);                    // Header
            buffer.putLong(4, 0x4141504C00000000L);        // "AAPL" + padding
            buffer.putDouble(12, 150.25 + messageId * 0.01); // Price
            buffer.putInt(20, 1000 + messageId);           // Quantity
            buffer.putLong(24, System.nanoTime());         // Timestamp

            // Zero-copy message parsing using absolute positioning
            int header = buffer.getInt(0);
            long symbol = buffer.getLong(4);
            double price = buffer.getDouble(12);
            int quantity = buffer.getInt(20);
            long timestamp = buffer.getLong(24);

            // Process the data (in real system, this would be trading logic)
            if (price > 150.0 && quantity > 1000) {
                // Trigger some trading logic
            }
        }

        private static void demonstrateTraditionalVsZeroCopyParsing() {
            System.out.println("TRADITIONAL vs ZERO-COPY MESSAGE PARSING:\n");

            byte[] messageBytes = new byte[MESSAGE_SIZE];
            ByteBuffer zeroCopyBuffer = ByteBuffer.allocateDirect(MESSAGE_SIZE);

            int iterations = 1_000_000;

            // Traditional approach (lots of copying)
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                parseMessageTraditional(messageBytes, i);
            }
            long traditionalTime = System.nanoTime() - start;

            // Zero-copy approach
            start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                parseMessageZeroCopy(zeroCopyBuffer, i);
            }
            long zeroCopyTime = System.nanoTime() - start;

            System.out.printf("Traditional parsing: %.2f ms\n", traditionalTime / 1_000_000.0);
            System.out.printf("Zero-copy parsing:   %.2f ms\n", zeroCopyTime / 1_000_000.0);
            System.out.printf("Zero-copy is %.1fx faster\n",
                    (double) traditionalTime / zeroCopyTime);
        }

        private static void parseMessageTraditional(byte[] messageBytes, int messageId) {
            // Multiple array copies and object creation
            byte[] header = new byte[4];
            System.arraycopy(messageBytes, 0, header, 0, 4);

            byte[] symbolBytes = new byte[8];
            System.arraycopy(messageBytes, 4, symbolBytes, 0, 8);

            byte[] priceBytes = new byte[8];
            System.arraycopy(messageBytes, 12, priceBytes, 0, 8);

            // Convert bytes to primitives (more copying)
            ByteBuffer temp = ByteBuffer.wrap(priceBytes);
            double price = temp.getDouble();

            // Create objects (GC pressure)
            String symbol = new String(symbolBytes);

            // Process data...
        }

        private static void parseMessageZeroCopy(ByteBuffer buffer, int messageId) {
            // Direct access to buffer memory - no copying!
            buffer.clear();

            // Absolute positioning - no cursor management overhead
            int header = buffer.getInt(0);
            long symbolLong = buffer.getLong(4);  // Keep as long, no string creation
            double price = buffer.getDouble(12);
            int quantity = buffer.getInt(20);
            long timestamp = buffer.getLong(24);

            // Process data directly - no intermediate objects
        }
    }

    // ===== UNSAFE ZERO-COPY =====

    public static class UnsafeZeroCopy {

        public static void demonstrateUnsafeZeroCopy() {
            System.out.println("\n=== Unsafe Zero-Copy (Ultimate Performance) ===\n");

            // Allocate native memory
            long memory1 = UNSAFE.allocateMemory(1024);
            long memory2 = UNSAFE.allocateMemory(1024);

            try {
                // Fill first memory block
                for (int i = 0; i < 256; i++) {
                    UNSAFE.putInt(memory1 + i * 4, i * i);
                }

                long start = System.nanoTime();

                // Zero-copy: Just copy memory addresses/pointers
                long sourcePointer = memory1;
                long destPointer = memory2;

                // Bulk memory copy at native speed
                UNSAFE.copyMemory(sourcePointer, destPointer, 1024);

                long time = System.nanoTime() - start;

                System.out.printf("Unsafe memory copy: %d ns for 1024 bytes\n", time);
                System.out.printf("Throughput: %.2f GB/s\n",
                        1024 * 1_000_000_000.0 / time / (1024*1024*1024));

                // Verify copy
                boolean identical = true;
                for (int i = 0; i < 256; i++) {
                    if (UNSAFE.getInt(memory1 + i * 4) != UNSAFE.getInt(memory2 + i * 4)) {
                        identical = false;
                        break;
                    }
                }

                System.out.println("Copy verification: " + (identical ? "✅ SUCCESS" : "❌ FAILED"));
                System.out.println("✅ Raw memory operations, zero JVM overhead!");

            } finally {
                UNSAFE.freeMemory(memory1);
                UNSAFE.freeMemory(memory2);
            }
        }
    }

    private static Unsafe getUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Cannot access Unsafe", e);
        }
    }

    public static void main(String[] args) throws IOException {
        CopyComparison.demonstrateTraditionalVsZeroCopy();
        ByteBufferZeroCopy.demonstrateByteBufferZeroCopy();
        FileIOZeroCopy.demonstrateFileZeroCopy();
        NetworkZeroCopy.demonstrateNetworkZeroCopy();
        TradingSystemZeroCopy.demonstrateTradingZeroCopy();
        UnsafeZeroCopy.demonstrateUnsafeZeroCopy();

        System.out.println("\n=== Zero-Copy Benefits Summary ===");
        System.out.println("🚀 Reduced CPU usage (no copying overhead)");
        System.out.println("⚡ Lower memory bandwidth consumption");
        System.out.println("📈 Higher throughput capabilities");
        System.out.println("⏱️  Lower latency (fewer memory operations)");
        System.out.println("💾 Reduced memory fragmentation");
        System.out.println("🔄 Better cache utilization");
        System.out.println("🏆 Essential for high-frequency trading systems!");
    }
}
