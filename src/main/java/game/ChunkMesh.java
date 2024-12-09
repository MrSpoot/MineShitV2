package game;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import static org.lwjgl.opengl.GL15C.glGenBuffers;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL31C.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL33C.glVertexAttribDivisor;

public class ChunkMesh {

    private final Chunk chunk;

    @Getter
    private List<Integer> encodedData;

    public ChunkMesh(Chunk chunk) {
        this.chunk = chunk;
    }

    public void generate() {
        List<Integer> encodedData = new ArrayList<>();

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    short block = chunk.getBlock(x, y, z);

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
            short neighbor = chunk.getBlock(x, y, z);
            return neighbor == 0;
        }

        Chunk neighborChunk = chunk.getNeighbor(faceDir);

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