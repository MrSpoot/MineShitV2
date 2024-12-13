package game;

import game.utils.FaceDirection;
import game.utils.GenerationEngine;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3i;
import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public static final int SIZE = 32;
    public static final int BORDER = 1;
    private static final int TOTAL_BLOCKS = (SIZE + 2 * BORDER) * (SIZE + 2 * BORDER) * (SIZE + 2 * BORDER);

    @Getter
    private boolean isUniform;
    @Getter @Setter
    private int bufferOffset;
    @Getter @Setter
    private int bufferSize;
    @Getter
    private short uniformBlockId;
    private List<Short> palette;
    private int bitsPerBlock;
    private long[] data;

    @Getter
    private final Vector3i position;
    @Getter
    private List<Integer> encodedData;

    public Chunk(Vector3i position) {
        this.position = position;
        this.bufferOffset = -1;
        this.isUniform = true;
        this.uniformBlockId = 0;
        initializePaletteAndData();
        generateData();

        setBlock(15,15,15,(short)4);

        generateMesh();
    }

    private void generateData() {
        GenerationEngine.generateChunkData(this);
    }

    private void generateMesh() {
        encodedData = new ArrayList<>();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    short block = getBlock(x, y, z);
                    if (block == 0) continue;

                    for (FaceDirection face : FaceDirection.values()) {
                        int neighborX = x + face.getOffsetX();
                        int neighborY = y + face.getOffsetY();
                        int neighborZ = z + face.getOffsetZ();

                        boolean isFaceVisible = isFaceExposed(neighborX, neighborY, neighborZ);
                        if (isFaceVisible) {
                            encodedData.add(encodeFaceData(x, y, z, (byte) block, face));
                        }
                    }
                }
            }
        }
    }

    private boolean isFaceExposed(int x, int y, int z) {
        if (x >= -BORDER && x < SIZE + BORDER && y >= -BORDER && y < SIZE + BORDER && z >= -BORDER && z < SIZE + BORDER) {
            return getBlock(x, y, z) == 0;
        }
        return true;
    }

    public short getBlock(int x, int y, int z) {
        int blockIndex = getBlockIndex(x + BORDER, y + BORDER, z + BORDER);
        if (isUniform) {
            return uniformBlockId;
        }
        int paletteIndex = readBlockData(blockIndex, data, bitsPerBlock);
        return palette.get(paletteIndex);
    }

    public void setBlock(int x, int y, int z, short blockId) {
        if (isUniform) {
            if (blockId == uniformBlockId) return;

            isUniform = false;
            initializePaletteAndData();
            fillUniformBlock();
        }

        int paletteIndex = palette.indexOf(blockId);
        if (paletteIndex == -1) {
            palette.add(blockId);
            paletteIndex = palette.size() - 1;
            ensureCapacity();
        }

        int blockIndex = getBlockIndex(x + BORDER, y + BORDER, z + BORDER);
        writeBlockData(blockIndex, paletteIndex, data, bitsPerBlock);
    }

    public void fillChunk(short blockId) {
        if (isUniform && uniformBlockId == blockId) {
            return;
        }
        isUniform = true;
        uniformBlockId = blockId;
        palette = null;
        data = null;
    }


    private void fillUniformBlock() {
        int uniformPaletteIndex = palette.indexOf(uniformBlockId);
        for (int i = 0; i < TOTAL_BLOCKS; i++) {
            writeBlockData(i, uniformPaletteIndex, data, bitsPerBlock);
        }
    }

    private void initializePaletteAndData() {
        palette = new ArrayList<>();
        bitsPerBlock = 4;
        int totalBits = TOTAL_BLOCKS * bitsPerBlock;
        int dataLength = (totalBits + 63) / 64;
        data = new long[dataLength];

        if (!palette.contains((short) 0)) {
            palette.add((short) 0);
        }

        if (!palette.contains(uniformBlockId)) {
            palette.add(uniformBlockId);
        }
    }

    private void ensureCapacity() {
        int requiredBits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
        if (requiredBits != bitsPerBlock) {
            bitsPerBlock = requiredBits;
            reallocateData();
        }
    }

    private void reallocateData() {
        int totalBits = TOTAL_BLOCKS * bitsPerBlock;
        int dataLength = (totalBits + 63) / 64;
        long[] newData = new long[dataLength];

        for (int i = 0; i < TOTAL_BLOCKS; i++) {
            int paletteIndex = readBlockData(i, data, bitsPerBlock);
            writeBlockData(i, paletteIndex, newData, bitsPerBlock);
        }

        data = newData;
    }

    private int getBlockIndex(int x, int y, int z) {
        return x + (z * (SIZE + 2 * BORDER)) + (y * (SIZE + 2 * BORDER) * (SIZE + 2 * BORDER));
    }

    private void writeBlockData(int blockIndex, int paletteIndex, long[] dataArray, int bitsPerBlock) {
        int bitIndex = blockIndex * bitsPerBlock;
        int arrayIndex = bitIndex / 64;
        int bitOffset = bitIndex % 64;

        long mask = ((1L << bitsPerBlock) - 1L) << bitOffset;
        dataArray[arrayIndex] = (dataArray[arrayIndex] & ~mask) | ((long) paletteIndex << bitOffset);

        if (64 - bitOffset < bitsPerBlock) {
            int remainingBits = bitsPerBlock - (64 - bitOffset);
            mask = (1L << remainingBits) - 1;
            dataArray[arrayIndex + 1] = (dataArray[arrayIndex + 1] & ~mask) | ((long) paletteIndex >> (bitsPerBlock - remainingBits));
        }
    }

    private int readBlockData(int blockIndex, long[] dataArray, int bitsPerBlock) {
        int bitIndex = blockIndex * bitsPerBlock;
        int arrayIndex = bitIndex / 64;
        int bitOffset = bitIndex % 64;

        long value = (dataArray[arrayIndex] >>> bitOffset) & ((1L << bitsPerBlock) - 1);

        if (64 - bitOffset < bitsPerBlock) {
            int remainingBits = bitsPerBlock - (64 - bitOffset);
            value |= (dataArray[arrayIndex + 1] & ((1L << remainingBits) - 1)) << (bitsPerBlock - remainingBits);
        }

        return (int) value;
    }

    private int encodeFaceData(int x, int y, int z, byte typeId, FaceDirection faceDir) {
        int encoded = 0;
        encoded |= (x & 0x1F);
        encoded |= (y & 0x1F) << 5;
        encoded |= (z & 0x1F) << 10;
        encoded |= faceDir.ordinal() << 15;
        encoded |= (typeId & 0xFF) << 18;
        return encoded;
    }

}
