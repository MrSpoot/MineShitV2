package game;

import org.joml.SimplexNoise;

public class GenerationEngine {

    private static final long SEED = 154555112;
    private static final float AMPLITUDE = 35.0f;
    private static final int BASE_HEIGHT = 10;

    public static short[] generateChunkData(Chunk chunk, int lod) {
        int chunkSize = Chunk.SIZE;
        short[] blocks = new short[chunkSize * chunkSize * chunkSize];

        for (int x = 0; x < chunkSize; x++) {
            for (int y = 0; y < chunkSize; y++) {
                for (int z = 0; z < chunkSize; z++) {
                    float globalX = (chunk.getPosition().x << 5) + x;
                    float globalY = (chunk.getPosition().y << 5) + y;
                    float globalZ = (chunk.getPosition().z << 5) + z;

                    float terrainNoise = SimplexNoise.noise(globalX * 0.01f, globalZ * 0.01f) * AMPLITUDE;
                    float mountainNoise = SimplexNoise.noise(globalX * 0.0005f, globalZ * 0.0005f) * (AMPLITUDE * 2);
                    float beachNoise = SimplexNoise.noise(globalX * 0.0002f, globalZ * 0.0002f) * (AMPLITUDE / 2);

                    float combinedNoise = terrainNoise + mountainNoise + beachNoise;
                    int terrainHeight = (int) (BASE_HEIGHT + combinedNoise);

                    if (globalY < terrainHeight) {
                        if (globalY == terrainHeight - 1) {
                            if (terrainHeight > BASE_HEIGHT + 10) {
                                chunk.setBlock(x,y,z,(short) 1);
                            } else if (terrainHeight > BASE_HEIGHT - 5) {
                                chunk.setBlock(x,y,z,(short) 3);
                            } else {
                                chunk.setBlock(x,y,z,(short) 2);
                            }
                        } else if (globalY < terrainHeight - 1 && globalY > terrainHeight - 8) {
                            chunk.setBlock(x,y,z,(short) 2);
                        } else {
                            chunk.setBlock(x,y,z,(short) 4);
                        }
                    } else if (globalY < BASE_HEIGHT - 5 && SimplexNoise.noise(globalX * 0.1f, globalY * 0.1f, globalZ * 0.1f) > 0.6f) {
                        chunk.setBlock(x,y,z,(short) 0);
                    } else {
                        chunk.setBlock(x,y,z,(short) 0);
                    }
                }
            }
        }

        return blocks;
    }

}
