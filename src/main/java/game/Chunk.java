package game;

import lombok.Getter;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chunk {
    public static final int SIZE = 32;
    private static final int TOTAL_BLOCKS = SIZE * SIZE * SIZE;

    private boolean isUniform;
    private short uniformBlockId;

    private List<Short> palette;
    private int bitsPerBlock;
    private long[] data;

    @Getter
    private final Vector3i position;
    @Getter
    private final Map<FaceDirection, Chunk> neighbors;

    @Getter
    private List<Integer> encodedData;

    public Chunk(Vector3i position) {
        this.isUniform = true;
        this.uniformBlockId = 0;
        this.position = position;
        this.neighbors = new HashMap<>();
        GenerationEngine.generateChunkData(this);
        generate();
    }

    public void addNeighbor(FaceDirection direction, Chunk neighbor) {
        this.neighbors.put(direction, neighbor);
    }

    public void removeNeighbor(FaceDirection direction) {
        this.neighbors.remove(direction);
    }

    public Chunk getNeighbor(FaceDirection direction) {
        return this.neighbors.get(direction);
    }

    public void fillChunk(short blockId) {
        if (isUniform && uniformBlockId == blockId) {
            return;
        }

        if (isUniform) {
            uniformBlockId = blockId;
        } else {
            isUniform = true;
            uniformBlockId = blockId;
            palette = null;
            data = null;
        }
    }

    public void setBlock(int x, int y, int z, short blockId) {
        if (isUniform) {
            if (blockId == uniformBlockId) {
                return;
            } else {
                isUniform = false;
                initializePaletteAndData();

                int previousPaletteIndex = palette.indexOf(uniformBlockId);
                if (previousPaletteIndex == -1) {
                    palette.add(uniformBlockId);
                    previousPaletteIndex = palette.size() - 1;
                }

                for (int i = 0; i < TOTAL_BLOCKS; i++) {
                    writeBlockData(i, previousPaletteIndex, data, bitsPerBlock);
                }
            }
        }

        int paletteIndex = palette.indexOf(blockId);
        if (paletteIndex == -1) {
            palette.add(blockId);
            paletteIndex = palette.size() - 1;
            ensureCapacity();
        }

        int blockIndex = getBlockIndex(x, y, z);
        writeBlockData(blockIndex, paletteIndex, data, bitsPerBlock);
    }

    public short getBlock(int x, int y, int z) {
        if (isUniform) {
            return uniformBlockId;
        }
        int blockIndex = getBlockIndex(x, y, z);
        int paletteIndex = readBlockData(blockIndex, data, bitsPerBlock);
        return palette.get(paletteIndex);
    }

    public void removeBlock(int x, int y, int z) {
        setBlock(x, y, z, (short) 0);

        if (!isUniform) {
            short firstBlockId = -1;
            boolean uniform = true;

            for (int i = 0; i < TOTAL_BLOCKS; i++) {
                int paletteIndex = readBlockData(i, data, bitsPerBlock);
                short blockId = palette.get(paletteIndex);

                if (firstBlockId == -1) {
                    firstBlockId = blockId;
                } else if (blockId != firstBlockId) {
                    uniform = false;
                    break;
                }
            }

            if (uniform) {
                isUniform = true;
                uniformBlockId = firstBlockId;
                palette = null;
                data = null;
            }
        }
    }

    public boolean isUniform() {
        return isUniform;
    }

    public short getUniformBlockId() {
        return uniformBlockId;
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

        if (uniformBlockId != 0 && !palette.contains(uniformBlockId)) {
            palette.add(uniformBlockId);
        }
    }

    private void ensureCapacity() {
        int paletteSize = palette.size();
        int requiredBits = Math.max(4, 32 - Integer.numberOfLeadingZeros(paletteSize - 1));

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
        return x + (z * SIZE) + (y * SIZE * SIZE);
    }

    private void writeBlockData(int blockIndex, int paletteIndex, long[] dataArray, int bitsPerBlock) {
        int bitIndex = blockIndex * bitsPerBlock;
        int arrayIndex = bitIndex / 64;
        int bitOffset = bitIndex % 64;

        long mask = ((1L << bitsPerBlock) - 1L) << bitOffset;
        dataArray[arrayIndex] = (dataArray[arrayIndex] & ~mask) | ((long) paletteIndex << bitOffset);

        int bitsInCurrentLong = 64 - bitOffset;
        if (bitsInCurrentLong < bitsPerBlock) {
            int bitsInNextLong = bitsPerBlock - bitsInCurrentLong;
            mask = (1L << bitsInNextLong) - 1L;
            dataArray[arrayIndex + 1] = (dataArray[arrayIndex + 1] & ~mask) | ((long) paletteIndex >> bitsInCurrentLong);
        }
    }

    private int readBlockData(int blockIndex, long[] dataArray, int bitsPerBlock) {
        int bitIndex = blockIndex * bitsPerBlock;
        int arrayIndex = bitIndex / 64;
        int bitOffset = bitIndex % 64;

        long value = (dataArray[arrayIndex] >>> bitOffset) & ((1L << bitsPerBlock) - 1L);

        int bitsInCurrentLong = 64 - bitOffset;
        if (bitsInCurrentLong < bitsPerBlock) {
            int bitsInNextLong = bitsPerBlock - bitsInCurrentLong;
            long nextValue = dataArray[arrayIndex + 1] & ((1L << bitsInNextLong) - 1L);
            value |= nextValue << bitsInCurrentLong;
        }

        return (int) value;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Chunk Info:").append("\n");
        sb.append("Uniform: ").append(isUniform).append("\n");

        if (isUniform) {
            sb.append("Uniform Block ID: ").append(uniformBlockId).append("\n");
        } else {
            sb.append("Palette Size: ").append(palette.size()).append("\n");
            sb.append("Bits Per Block: ").append(bitsPerBlock).append("\n");
        }
        return sb.toString();
    }

    public void generate() {
        List<Integer> encodedData = new ArrayList<>();

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    short block = getBlock(x, y, z);

                    if (block == 0) continue;

                    for (FaceDirection faceDir : FaceDirection.values()) {
                        int neighborX = x + faceDir.getOffsetX();
                        int neighborY = y + faceDir.getOffsetY();
                        int neighborZ = z + faceDir.getOffsetZ();

                        if (shouldRenderFace(neighborX, neighborY, neighborZ, faceDir)) {
                            encodedData.add(encodeFaceData(x, y, z, faceDir));
                        }
                    }
                }
            }
        }
        this.encodedData = encodedData;
    }

    private boolean shouldRenderFace(int x, int y, int z, FaceDirection faceDir) {
        if (x >= 0 && x < Chunk.SIZE && y >= 0 && y < Chunk.SIZE && z >= 0 && z < Chunk.SIZE) {
            short neighbor = getBlock(x, y, z);
            return neighbor == 0;
        }

        Chunk neighborChunk = getNeighbor(faceDir);

        if (neighborChunk != null) {
            int neighborX = (x + Chunk.SIZE) % Chunk.SIZE;
            int neighborY = (y + Chunk.SIZE) % Chunk.SIZE;
            int neighborZ = (z + Chunk.SIZE) % Chunk.SIZE;

            short neighbor = neighborChunk.getBlock(neighborX, neighborY, neighborZ);
            return neighbor == 0;
        }

        return true;
    }

    private int encodeFaceData(int x, int y, int z, FaceDirection faceDir) {
        int encoded = 0;

        encoded |= (x & 0b11111); // Bits 0-4 pour X
        encoded |= (y & 0b11111) << 5; // Bits 5-9 pour Y
        encoded |= (z & 0b11111) << 10; // Bits 10-14 pour Z

        encoded |= faceDir.ordinal() << 15;

        return encoded;
    }
}
