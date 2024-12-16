package game.utils;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL15C.*;

/**
 * Unified manager for handling OpenGL buffers with dynamic allocation, update, and removal support.
 */
public class BufferManager {
    /**
     * -- GETTER --
     *  Gets the OpenGL buffer ID.
     *
     * @return The OpenGL buffer ID.
     */
    @Getter
    private final int bufferId;
    private int capacity;
    private final TreeMap<Integer, Integer> freeOffsets; // Offset -> Size
    private final Map<Integer, Integer> idToOffset;      // ID -> Offset
    private final Map<Integer, Integer> idToSize;        // ID -> Size
    private final int BUFFER_TYPE;

    public BufferManager(int BufferType, int initialCapacity) {
        this.capacity = initialCapacity;
        this.bufferId = glGenBuffers();
        this.freeOffsets = new TreeMap<>();
        this.idToOffset = new ConcurrentHashMap<>();
        this.idToSize = new ConcurrentHashMap<>();
        this.BUFFER_TYPE = BufferType;

        // Initialize OpenGL buffer
        glBindBuffer(BUFFER_TYPE, bufferId);
        glBufferData(BUFFER_TYPE, capacity, GL_DYNAMIC_DRAW);
        glBindBuffer(BUFFER_TYPE, 0);

        // Initially, the whole buffer is free
        freeOffsets.put(0, capacity);
    }

    /**
     * Allocates space in the buffer and uploads the data.
     *
     * @param id   ID for tracking this allocation.
     * @param data The data to upload.
     * @throws RuntimeException If the buffer does not have enough space.
     */
    public synchronized void addData(int id, byte[] data) {
        int size = data.length;
        int offset = allocate(size);
        idToOffset.put(id, offset);
        idToSize.put(id, size);

        // Upload data to the OpenGL buffer
        glBindBuffer(BUFFER_TYPE, bufferId);
        glBufferSubData(BUFFER_TYPE, offset, ByteBuffer.wrap(data).flip());
        glBindBuffer(BUFFER_TYPE, 0);
    }

    /**
     * Updates data at the existing allocation.
     *
     * @param id   ID of the existing allocation.
     * @param data New data to upload.
     * @throws IllegalArgumentException If the ID does not exist.
     * @throws RuntimeException         If the new data size exceeds the allocated size.
     */
    public synchronized void updateData(int id, byte[] data) {
        Integer offset = idToOffset.get(id);
        Integer allocatedSize = idToSize.get(id);

        if (offset == null || allocatedSize == null) {
            throw new IllegalArgumentException("ID not found: " + id);
        }
        if (data.length > allocatedSize) {
            throw new RuntimeException("New data size exceeds allocated size.");
        }

        // Upload the new data
        glBindBuffer(BUFFER_TYPE, bufferId);
        glBufferSubData(BUFFER_TYPE, offset, ByteBuffer.wrap(data));
        glBindBuffer(BUFFER_TYPE, 0);
    }

    /**
     * Frees the space allocated for the given ID.
     *
     * @param id ID of the allocation to free.
     */
    public synchronized void removeData(int id) {
        Integer offset = idToOffset.remove(id);
        Integer size = idToSize.remove(id);

        if (offset != null && size != null) {
            freeOffsets.put(offset, size);
            mergeFreeOffsets();
        }
    }

    /**
     * Defragments the buffer by compacting data and eliminating unused space.
     */
    public synchronized void defragment() {
        ByteBuffer compactedBuffer = ByteBuffer.allocate(capacity);
        TreeMap<Integer, Integer> newOffsets = new TreeMap<>();
        int currentOffset = 0;

        // Compact the buffer by copying all active data
        glBindBuffer(BUFFER_TYPE, bufferId);
        for (Map.Entry<Integer, Integer> entry : idToOffset.entrySet()) {
            int id = entry.getKey();
            int oldOffset = entry.getValue();
            int size = idToSize.get(id);

            // Copy data from the old location to the new location
            ByteBuffer data = ByteBuffer.allocate(size);
            glGetBufferSubData(BUFFER_TYPE, oldOffset, data);
            compactedBuffer.position(currentOffset);
            compactedBuffer.put(data);

            // Update the new offset
            newOffsets.put(id, currentOffset);
            currentOffset += size;
        }

        // Replace the buffer content with the compacted data
        glBufferData(BUFFER_TYPE, capacity, GL_DYNAMIC_DRAW);
        compactedBuffer.flip();
        glBufferSubData(BUFFER_TYPE, 0, compactedBuffer);

        // Update offsets and free space
        idToOffset.clear();
        idToOffset.putAll(newOffsets);
        freeOffsets.clear();
        freeOffsets.put(currentOffset, capacity - currentOffset);

        glBindBuffer(BUFFER_TYPE, 0);
    }

    /**
     * Allocates a section of the buffer.
     *
     * @param size Size of the section to allocate.
     * @return Offset of the allocated section.
     * @throws RuntimeException If the buffer does not have enough space.
     */
    private int allocate(int size) {
        for (var entry : freeOffsets.entrySet()) {
            int offset = entry.getKey();
            int freeSize = entry.getValue();

            if (freeSize >= size) {
                // Allocate and adjust free space
                freeOffsets.remove(offset);
                if (freeSize > size) {
                    freeOffsets.put(offset + size, freeSize - size);
                }
                return offset;
            }
        }

        // If no space is found, expand the buffer
        expandBuffer(size);
        return allocate(size);
    }

    /**
     * Expands the buffer to accommodate additional data.
     *
     * @param additionalSize Minimum additional size required.
     */
    private void expandBuffer(int additionalSize) {
        int newCapacity = capacity + Math.max(capacity / 2, additionalSize);

        ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);

        // Copy existing data to the new buffer
        glBindBuffer(BUFFER_TYPE, bufferId);
        glGetBufferSubData(BUFFER_TYPE, 0, newBuffer);

        // Replace the old buffer with the new one
        glBufferData(BUFFER_TYPE, newCapacity, GL_DYNAMIC_DRAW);
        glBufferSubData(BUFFER_TYPE, 0, newBuffer);

        // Update the capacity and free space
        freeOffsets.put(capacity, newCapacity - capacity);
        capacity = newCapacity;

        glBindBuffer(BUFFER_TYPE, 0);
    }

    /**
     * Merges adjacent free blocks in the buffer.
     */
    private void mergeFreeOffsets() {
        var iterator = freeOffsets.entrySet().iterator();
        if (!iterator.hasNext()) {
            return;
        }

        var prev = iterator.next();
        while (iterator.hasNext()) {
            var current = iterator.next();
            if (prev.getKey() + prev.getValue() == current.getKey()) {
                // Merge adjacent blocks
                prev.setValue(prev.getValue() + current.getValue());
                iterator.remove();
            } else {
                prev = current;
            }
        }
    }

    /**
     * Deletes the OpenGL buffer.
     */
    public void delete() {
        glDeleteBuffers(bufferId);
    }

}
