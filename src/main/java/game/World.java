package game;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class World {

    private static final Map<Vector3f, Chunk> chunksToRender = new ConcurrentHashMap<>();
    private static final int RENDER_DISTANCE = 1;
    private static Vector3f lastPlayerChunkPosition = new Vector3f(Float.MAX_VALUE);

    // Création d'un ExecutorService pour gérer les threads
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);  // Par exemple, 4 threads
    private static final Map<Vector3f, Future<?>> chunkLoadingTasks = new ConcurrentHashMap<>();

    public static void render() {
        chunksToRender.values().forEach(chunk -> {
            chunk.createMesh();
            chunk.render();
        });
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
                    if (!chunksToRender.containsKey(chunkPosition) && !chunkLoadingTasks.containsKey(chunkPosition)) {
                        // Soumettre une tâche pour charger le chunk dans un autre thread
                        Future<?> future = executor.submit(() -> {
                            Chunk newChunk = new Chunk(chunkPosition);
                            synchronized (chunksToRender) {
                                chunksToRender.put(chunkPosition, newChunk);
                            }
                        });
                        chunkLoadingTasks.put(chunkPosition, future);
                    }
                }
            }
        }

        // Supprimer les chunks hors de la distance de rendu et annuler les tâches associées
        chunksToRender.entrySet().removeIf(entry -> {
            Vector3f pos = entry.getKey();
            boolean outOfRange = pos.x < chunkX - RENDER_DISTANCE || pos.x > chunkX + RENDER_DISTANCE ||
                    pos.y < chunkY - RENDER_DISTANCE || pos.y > chunkY + RENDER_DISTANCE ||
                    pos.z < chunkZ - RENDER_DISTANCE || pos.z > chunkZ + RENDER_DISTANCE;

            if (outOfRange) {
                entry.getValue().cleanup();
                chunkLoadingTasks.remove(pos);
            }
            return outOfRange;
        });

        // Supprimer les tâches terminées de la liste des tâches de chargement
        chunkLoadingTasks.entrySet().removeIf(entry -> entry.getValue().isDone());
    }

    public static void cleanup() {
        chunksToRender.clear();
        chunkLoadingTasks.clear();
        executor.shutdown();
    }

    private static void processChunkFace(List<Chunk> neighboringChunks){



    }

    public static List<Chunk> getNeighboringChunks(Chunk chunk) {
        List<Chunk> neighboringChunks = new ArrayList<>();

        Vector3f[] neighborOffsets = {
                new Vector3f(1, 0, 0),  // Droite
                new Vector3f(-1, 0, 0), // Gauche
                new Vector3f(0, 1, 0),  // Haut
                new Vector3f(0, -1, 0), // Bas
                new Vector3f(0, 0, 1),  // Avant
                new Vector3f(0, 0, -1)  // Arrière
        };

        for (Vector3f offset : neighborOffsets) {
            Vector3f neighborPosition = new Vector3f(chunk.getPosition()).add(offset);
            Chunk neighborChunk = chunksToRender.get(neighborPosition);

            if (neighborChunk != null) {
                neighboringChunks.add(neighborChunk);
            }
        }

        return neighboringChunks;
    }
}
