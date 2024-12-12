package game.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class DynamicByteBuffer {
    private ByteBuffer buffer;

    public DynamicByteBuffer(int initialCapacity) {
        buffer = ByteBuffer.allocateDirect(initialCapacity).order(ByteOrder.nativeOrder());
    }

    public void ensureCapacity(int capacity) {
        if (buffer.capacity() < capacity) {
            ByteBuffer newBuffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
            buffer.flip();
            newBuffer.put(buffer);
            buffer = newBuffer;
        }
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}

