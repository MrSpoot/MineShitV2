package game;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class World {

    private static final Map<Vector3f, Chunk> chunksToRender = new HashMap<>();
    private static final int RENDER_DISTANCE = 4;
    private static Vector3f lastPlayerChunkPosition = new Vector3f(Float.MAX_VALUE);

    public static void render() {
        chunksToRender.values().forEach(Chunk::render);
    }

    public static void updateChunks(Vector3f playerPosition) {

        int chunkX = (int) playerPosition.x / Chunk.CHUNK_SIZE;
        int chunkY = (int) playerPosition.y / Chunk.CHUNK_SIZE;
        int chunkZ = (int) playerPosition.z / Chunk.CHUNK_SIZE;

        Vector3f _playerChunkPosition = new Vector3f(chunkX, chunkY, chunkZ);

        if (lastPlayerChunkPosition.equals(_playerChunkPosition)) {
            return;
        }

        lastPlayerChunkPosition = _playerChunkPosition;

        for (int x = chunkX - RENDER_DISTANCE; x <= chunkX + RENDER_DISTANCE; x++) {
            for (int y = chunkY - RENDER_DISTANCE; y <= chunkY + RENDER_DISTANCE; y++) {
                for (int z = chunkZ - RENDER_DISTANCE; z <= chunkZ + RENDER_DISTANCE; z++) {
                    Vector3f chunkPosition = new Vector3f(x, y, z);
                    if (!chunksToRender.containsKey(chunkPosition)) {
                        chunksToRender.put(chunkPosition, new Chunk(chunkPosition));
                    }
                }
            }
        }

        chunksToRender.entrySet().removeIf(entry ->
                entry.getKey().x < chunkX - RENDER_DISTANCE || entry.getKey().x > chunkX + RENDER_DISTANCE ||
                        entry.getKey().y < chunkY - RENDER_DISTANCE || entry.getKey().y > chunkY + RENDER_DISTANCE ||
                        entry.getKey().z < chunkZ - RENDER_DISTANCE || entry.getKey().z > chunkZ + RENDER_DISTANCE

        );
    }

}
