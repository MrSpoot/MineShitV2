package game;

import core.Mesh;
import org.joml.Vector3f;
import java.util.*;
import java.util.concurrent.*;

public class World {

    private static final Map<Vector3f, Chunk> chunksToRender = new ConcurrentHashMap<>();
    private static final int RENDER_DISTANCE = 8;
    private static Vector3f lastPlayerChunkPosition = new Vector3f(Float.MAX_VALUE);

    private static final ExecutorService executor = Executors.newFixedThreadPool(1);
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

        List<Vector3f> chunksToGenerate = getChunksToGenerate(chunkX, chunkY, chunkZ);

        chunksToGenerate.sort(Comparator.comparingDouble(chunkPos -> chunkPos.distance(playerPosition)));

        for (Vector3f chunkPosition : chunksToGenerate) {
            Future<?> future = executor.submit(() -> {
                Chunk newChunk = new Chunk(chunkPosition,getLodWithDistance(lastPlayerChunkPosition,chunkPosition));
                List<Chunk> neighboringChunks = getNeighboringChunks(newChunk);

                newChunk.updateFaceVisibility(neighboringChunks);

                if (!isChunkCompletelyHidden(newChunk, neighboringChunks)) {
                    newChunk.generateMesh(neighboringChunks);
                    synchronized (chunksToRender) {
                        chunksToRender.put(chunkPosition, newChunk);
                    }
                    for (Chunk neighbor : neighboringChunks) {
                        neighbor.updateFaceVisibility(getNeighboringChunks(neighbor));
                        neighbor.generateMesh(getNeighboringChunks(neighbor));
                    }
                } else {
                    newChunk.cleanup();
                }
            });
            chunkLoadingTasks.put(chunkPosition, future);
        }

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

        chunkLoadingTasks.entrySet().removeIf(entry -> entry.getValue().isDone());
    }

    private static List<Vector3f> getChunksToGenerate(int chunkX, int chunkY, int chunkZ) {
        List<Vector3f> chunksToGenerate = new ArrayList<>();

        for (int x = chunkX - RENDER_DISTANCE; x <= chunkX + RENDER_DISTANCE; x++) {
            for (int y = chunkY - RENDER_DISTANCE; y <= chunkY + RENDER_DISTANCE; y++) {
                for (int z = chunkZ - RENDER_DISTANCE; z <= chunkZ + RENDER_DISTANCE; z++) {
                    Vector3f chunkPosition = new Vector3f(x, y, z);

                    if (!chunksToRender.containsKey(chunkPosition) && !chunkLoadingTasks.containsKey(chunkPosition)) {
                        chunksToGenerate.add(chunkPosition);
                    }
                }
            }
        }

        return chunksToGenerate;
    }


    public static List<Chunk> getNeighboringChunks(Chunk chunk) {
        List<Chunk> neighbors = new ArrayList<>();
        Vector3f[] neighborOffsets = {
                new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0),
                new Vector3f(0, 1, 0), new Vector3f(0, -1, 0),
                new Vector3f(0, 0, 1), new Vector3f(0, 0, -1)
        };
        for (Vector3f offset : neighborOffsets) {
            Vector3f neighborPos = new Vector3f(chunk.getPosition()).add(offset);
            if (chunksToRender.containsKey(neighborPos)) {
                neighbors.add(chunksToRender.get(neighborPos));
            }
        }
        return neighbors;
    }

    private static boolean isChunkCompletelyHidden(Chunk chunk, List<Chunk> neighboringChunks) {
        Vector3f[] neighborOffsets = {
                new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0),
                new Vector3f(0, 1, 0), new Vector3f(0, -1, 0),
                new Vector3f(0, 0, 1), new Vector3f(0, 0, -1)
        };

        for (Vector3f offset : neighborOffsets) {
            Vector3f neighborPosition = new Vector3f(chunk.getPosition()).add(offset);
            Chunk neighborChunk = neighboringChunks.stream()
                    .filter(n -> n.getPosition().equals(neighborPosition))
                    .findFirst()
                    .orElse(null);

            if (neighborChunk == null || !isChunkFull(neighborChunk)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isChunkFull(Chunk chunk) {
        for (int block : chunk.getChunkData()) {
            if (block == 0) {
                return false;
            }
        }
        return true;
    }

    public static void cleanup() {
        chunksToRender.values().forEach(Chunk::cleanup);
        chunksToRender.clear();
        chunkLoadingTasks.clear();
        executor.shutdown();
    }

    private static int getLodWithDistance(Vector3f playerPosition, Vector3f chunkPosition) {
        float distance = playerPosition.distance(chunkPosition);

        if (distance < 12) {
            return 0;
        } else if (distance < 16) {
            return 1;
        } else if (distance < 32) {
            return 2;
        } else {
            return 3;
        }

    }
}
