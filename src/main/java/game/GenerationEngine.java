package game;

import org.joml.SimplexNoise;
import org.joml.Vector3f;

public class GenerationEngine {

    private static final long SEED = 154555112;
    private static final float FREQUENCY = 0.005f;
    private static final float AMPLITUDE = 50.0f;
    private static final int BASE_HEIGHT = 10;

    public static int[] generateChunkData(Vector3f chunkPosition) {
        int[] blocks = new int[Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE];

        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_SIZE; y++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    float globalX = chunkPosition.x * Chunk.CHUNK_SIZE + x;
                    float globalY = chunkPosition.y * Chunk.CHUNK_SIZE + y;
                    float globalZ = chunkPosition.z * Chunk.CHUNK_SIZE + z;

                    float noise = SimplexNoise.noise(globalX * FREQUENCY, globalZ * FREQUENCY) * SimplexNoise.noise(globalX * 0.01f, globalZ * 0.01f) * AMPLITUDE;
                    int terrainHeight = (int) (BASE_HEIGHT + noise);

                    if((globalY < terrainHeight)){
                        //blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = 8;
                        if (globalY == terrainHeight -1) {
                            blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = terrainHeight > 0 ? 1 : 4;
                        }else if(globalY < terrainHeight - 1 && globalY > terrainHeight - 8) {
                            blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = terrainHeight > 0 ? 3 : 4;
                        }
                        else{
                            blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = 2;
                        }
                    }else{
                        blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = 0;
                    }


                }
            }
        }

        return blocks;
    }
}
