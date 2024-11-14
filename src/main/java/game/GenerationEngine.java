package game;

import org.joml.SimplexNoise;
import org.joml.Vector3f;

public class GenerationEngine {

    private static final long SEED = 154555112;
    private static final float FREQUENCY = 0.01f;
    private static final float AMPLITUDE = 5.0f;  // Amplitude ajustée pour contrôler la hauteur des collines
    private static final int BASE_HEIGHT = 0;  // Hauteur de base pour le terrain plat

    public static boolean[] generateChunkData(Vector3f chunkPosition) {
        boolean[] blocks = new boolean[Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE];

        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_SIZE; y++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    // Calculer la position globale
                    float globalX = chunkPosition.x * Chunk.CHUNK_SIZE + x;
                    float globalY = chunkPosition.y * Chunk.CHUNK_SIZE + y;
                    float globalZ = chunkPosition.z * Chunk.CHUNK_SIZE + z;

                    // Générer la valeur de bruit pour déterminer la hauteur du terrain
                    float noise = SimplexNoise.noise(globalX * FREQUENCY, globalZ * FREQUENCY) * AMPLITUDE;
                    int terrainHeight = (int) (BASE_HEIGHT + noise);  // Hauteur du terrain à cet endroit

                    // Remplir le bloc si la position y est en dessous de la hauteur du terrain
                    blocks[x + y * Chunk.CHUNK_SIZE + z * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE] = (globalY > terrainHeight);
                }
            }
        }

        return blocks;
    }
}
