package game.utils;

import game.Chunk;
import org.joml.Vector3i;

public class GenerationEngine {

    private static final long SEED = 154555112;
    private static final float AMPLITUDE = 25.0f;
    private static final int BASE_HEIGHT = 10;
    private static final int SAMPLE_INTERVAL = 8;
    private static final float THRESHOLD = 0.1f;
    private static final PerlinCache perlinCache = new PerlinCache(SEED,1024*16);

    public static void generateChunkData(Chunk chunk) {
        float[][][] noiseSamples = new float[Chunk.SIZE / SAMPLE_INTERVAL + 3]
                [Chunk.SIZE / SAMPLE_INTERVAL + 3]
                [Chunk.SIZE / SAMPLE_INTERVAL + 3];

        for (int x = 0; x < noiseSamples.length; x++) {
            for (int z = 0; z < noiseSamples.length; z++) {
                for (int y = 0; y < noiseSamples.length; y++) {
                    int globalX = (chunk.getPosition().x * Chunk.SIZE) + (x - 1) * SAMPLE_INTERVAL;
                    int globalZ = (chunk.getPosition().z * Chunk.SIZE) + (z - 1) * SAMPLE_INTERVAL;

                    float terrainNoise = perlinCache.getNoise(globalX, globalZ) * AMPLITUDE;
                    noiseSamples[x][y][z] = terrainNoise;
                }
            }
        }

        for (int x = -Chunk.BORDER; x < Chunk.SIZE + Chunk.BORDER; x++) {
            for (int y = -Chunk.BORDER; y < Chunk.SIZE + Chunk.BORDER; y++) {
                for (int z = -Chunk.BORDER; z < Chunk.SIZE + Chunk.BORDER; z++) {
                    int globalX = (chunk.getPosition().x * Chunk.SIZE) + x;
                    int globalY = (chunk.getPosition().y * Chunk.SIZE) + y;
                    int globalZ = (chunk.getPosition().z * Chunk.SIZE) + z;

                    int x0 = (x + Chunk.BORDER) / SAMPLE_INTERVAL;
                    int x1 = x0 + 1;
                    int y0 = (y + Chunk.BORDER) / SAMPLE_INTERVAL;
                    int y1 = y0 + 1;
                    int z0 = (z + Chunk.BORDER) / SAMPLE_INTERVAL;
                    int z1 = z0 + 1;

                    float dx = ((x + Chunk.BORDER) % SAMPLE_INTERVAL) / (float) SAMPLE_INTERVAL;
                    float dy = ((y + Chunk.BORDER) % SAMPLE_INTERVAL) / (float) SAMPLE_INTERVAL;
                    float dz = ((z + Chunk.BORDER) % SAMPLE_INTERVAL) / (float) SAMPLE_INTERVAL;

                    float interpolatedNoise = trilinearInterpolate(
                            noiseSamples, x0, x1, y0, y1, z0, z1, dx, dy, dz);

                    int terrainHeight = (int) (BASE_HEIGHT + interpolatedNoise);

                    if (globalY < terrainHeight) {
                        short blockId;

                        if(false){
                            blockId = (short) BlockType.TEST.ordinal();
                        } else if (globalY == terrainHeight - 1) {
                            blockId = (short) BlockType.GRASS.ordinal(); // Surface block
                        } else if (globalY > terrainHeight - 8) {
                            blockId = (short) BlockType.DIRT.ordinal(); // Subsurface block
                        } else {
                            blockId = (short) BlockType.STONE.ordinal(); // Deep block
                        }
                        chunk.setBlock(x, y, z, blockId);
                    } else {
                        chunk.setBlock(x, y, z, (short) BlockType.AIR.ordinal()); // Air block
                    }
                }
            }
        }
    }


    private static float trilinearInterpolate(
            float[][][] samples, int x0, int x1, int y0, int y1, int z0, int z1,
            float dx, float dy, float dz) {

        float c000 = samples[x0][y0][z0];
        float c001 = samples[x0][y0][z1];
        float c010 = samples[x0][y1][z0];
        float c011 = samples[x0][y1][z1];
        float c100 = samples[x1][y0][z0];
        float c101 = samples[x1][y0][z1];
        float c110 = samples[x1][y1][z0];
        float c111 = samples[x1][y1][z1];

        float c00 = c000 * (1 - dz) + c001 * dz;
        float c01 = c010 * (1 - dz) + c011 * dz;
        float c10 = c100 * (1 - dz) + c101 * dz;
        float c11 = c110 * (1 - dz) + c111 * dz;

        float c0 = c00 * (1 - dy) + c01 * dy;
        float c1 = c10 * (1 - dy) + c11 * dy;

        return c0 * (1 - dx) + c1 * dx;
    }
}
