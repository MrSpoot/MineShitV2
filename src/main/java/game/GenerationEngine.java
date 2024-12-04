package game;

import org.joml.SimplexNoise;

public class GenerationEngine {

    private static final long SEED = 154555112;
    private static final float AMPLITUDE = 35.0f;
    private static final int BASE_HEIGHT = 10;
    private static final int SAMPLE_INTERVAL = 8; // Distance entre les points d'échantillonnage

    private static final FastNoiseLite noise = new FastNoiseLite();

    static {
        noise.SetSeed((int)SEED);
        noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
    }

    public static short[] generateChunkData(Chunk chunk) {
        int chunkSize = Chunk.SIZE;
        short[] blocks = new short[chunkSize * chunkSize * chunkSize];

        // Grille d'échantillonnage pour le bruit
        float[][][] noiseSamples = new float[chunkSize / SAMPLE_INTERVAL + 1]
                [chunkSize / SAMPLE_INTERVAL + 1]
                [chunkSize / SAMPLE_INTERVAL + 1];

        // Échantillonner les valeurs de bruit sur une grille espacée
        for (int x = 0; x <= chunkSize / SAMPLE_INTERVAL; x++) {
            for (int z = 0; z <= chunkSize / SAMPLE_INTERVAL; z++) {
                for (int y = 0; y <= chunkSize / SAMPLE_INTERVAL; y++) {
                    float globalX = (chunk.getPosition().x << 5) + x * SAMPLE_INTERVAL;
                    float globalY = (chunk.getPosition().y << 5) + y * SAMPLE_INTERVAL;
                    float globalZ = (chunk.getPosition().z << 5) + z * SAMPLE_INTERVAL;

                    float terrainNoise = noise.GetNoise(globalX, globalZ) * AMPLITUDE;

                    noiseSamples[x][y][z] = terrainNoise;
                }
            }
        }

        // Remplir les voxels avec interpolation
        for (int x = 0; x < chunkSize; x++) {
            for (int y = 0; y < chunkSize; y++) {
                for (int z = 0; z < chunkSize; z++) {
                    // Indices pour l'interpolation
                    int x0 = x / SAMPLE_INTERVAL;
                    int x1 = x0 + 1;
                    int y0 = y / SAMPLE_INTERVAL;
                    int y1 = y0 + 1;
                    int z0 = z / SAMPLE_INTERVAL;
                    int z1 = z0 + 1;

                    // Interpolation fractionnelle
                    float dx = (x % SAMPLE_INTERVAL) / (float) SAMPLE_INTERVAL;
                    float dy = (y % SAMPLE_INTERVAL) / (float) SAMPLE_INTERVAL;
                    float dz = (z % SAMPLE_INTERVAL) / (float) SAMPLE_INTERVAL;

                    // Trilinear interpolation
                    float interpolatedNoise = trilinearInterpolate(
                            noiseSamples, x0, x1, y0, y1, z0, z1, dx, dy, dz);

                    float globalY = (chunk.getPosition().y << 5) + y;
                    int terrainHeight = (int) (BASE_HEIGHT + interpolatedNoise);

                    // Déterminer le type de voxel en fonction de la hauteur
                    if (globalY < terrainHeight) {
                        if (globalY == terrainHeight - 1) {
                            chunk.setBlock(x, y, z, (short) 1);
                        } else if (globalY < terrainHeight - 1 && globalY > terrainHeight - 8) {
                            chunk.setBlock(x, y, z, (short) 2);
                        } else {
                            chunk.setBlock(x, y, z, (short) 4);
                        }
                    } else {
                        chunk.setBlock(x, y, z, (short) 0);
                    }
                }
            }
        }

        return blocks;
    }

    private static float trilinearInterpolate(
            float[][][] samples, int x0, int x1, int y0, int y1, int z0, int z1,
            float dx, float dy, float dz) {

        // Interpolation sur les 8 points autour du voxel
        float c000 = samples[x0][y0][z0];
        float c001 = samples[x0][y0][z1];
        float c010 = samples[x0][y1][z0];
        float c011 = samples[x0][y1][z1];
        float c100 = samples[x1][y0][z0];
        float c101 = samples[x1][y0][z1];
        float c110 = samples[x1][y1][z0];
        float c111 = samples[x1][y1][z1];

        // Interpolation le long de l'axe Z
        float c00 = c000 * (1 - dz) + c001 * dz;
        float c01 = c010 * (1 - dz) + c011 * dz;
        float c10 = c100 * (1 - dz) + c101 * dz;
        float c11 = c110 * (1 - dz) + c111 * dz;

        // Interpolation le long de l'axe Y
        float c0 = c00 * (1 - dy) + c01 * dy;
        float c1 = c10 * (1 - dy) + c11 * dy;

        // Interpolation le long de l'axe X
        return c0 * (1 - dx) + c1 * dx;
    }
}


