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

                    float noise = SimplexNoise.noise(globalX * FREQUENCY, globalZ * FREQUENCY) * SimplexNoise.noise(globalX * 0.01f, globalZ * 0.01f) * AMPLITUDE;
                    int terrainHeight = (int) (BASE_HEIGHT + noise);

                    if((globalY < terrainHeight)){
                        blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = BlockUtils.create(BlockType.TEST);
//                        if (globalY == terrainHeight -1) {
//                            blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = BlockUtils.create(terrainHeight > 0 ? BlockType.GRASS : BlockType.SAND);
//                        }else if(globalY < terrainHeight - 1 && globalY > terrainHeight - 8) {
//                            blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = BlockUtils.create(terrainHeight > 0 ? BlockType.DIRT : BlockType.SAND);
//                        }
//                        else{
//                            blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = BlockUtils.create(BlockType.STONE);
//                        }
                    }else{
                        blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = BlockUtils.create(BlockType.AIR);;
                    }


                }
            }
        }

        return blocks;
    }
}
