package game;

import game.utils.BlockUtils;
import org.joml.SimplexNoise;
import org.joml.Vector3f;

public class GenerationEngine {

    private static final long SEED = 154555112;
    private static final float FREQUENCY = 0.005f;
    private static final float AMPLITUDE = 50.0f;
    private static final int BASE_HEIGHT = 10;

    public static short[] generateChunkData(Vector3f chunkPosition) {
        short[] blocks = new short[Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE];

        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_SIZE; y++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    float globalX = chunkPosition.x * Chunk.CHUNK_SIZE + x;
                    float globalY = chunkPosition.y * Chunk.CHUNK_SIZE + y;
                    float globalZ = chunkPosition.z * Chunk.CHUNK_SIZE + z;

                    float terrainNoise = SimplexNoise.noise(globalX * 0.01f, globalZ * 0.01f) * AMPLITUDE;
                    float mountainNoise = SimplexNoise.noise(globalX * 0.0005f, globalZ * 0.0005f) * (AMPLITUDE * 2);
                    float beachNoise = SimplexNoise.noise(globalX * 0.0002f, globalZ * 0.0002f) * (AMPLITUDE / 2);

                    float combinedNoise = terrainNoise + mountainNoise + beachNoise;
                    int terrainHeight = (int) (BASE_HEIGHT + combinedNoise);

                    if (globalY < terrainHeight) {
                        if (globalY == terrainHeight - 1) {
                            if (terrainHeight > BASE_HEIGHT + 10) {
                                blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = BlockUtils.create(BlockType.GRASS);
                            } else if (terrainHeight > BASE_HEIGHT - 5) {
                                blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = BlockUtils.create(BlockType.SAND);
                            } else {
                                blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = BlockUtils.create(BlockType.DIRT);
                            }
                        } else if (globalY < terrainHeight - 1 && globalY > terrainHeight - 8) {
                            blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = BlockUtils.create(BlockType.DIRT);
                        } else {
                            blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = BlockUtils.create(BlockType.STONE);
                        }
                    } else if (globalY < BASE_HEIGHT - 5 && SimplexNoise.noise(globalX * 0.1f, globalY * 0.1f, globalZ * 0.1f) > 0.6f) {
                        blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = BlockUtils.create(BlockType.AIR);
                    } else {
                        blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = BlockUtils.create(BlockType.AIR);
                    }
                }
            }
        }

        return blocks;
    }

}
