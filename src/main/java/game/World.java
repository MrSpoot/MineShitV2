package game;

import core.Display;
import org.joml.Matrix4f;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.*;

public class World {

    private static final Map<Vector3i, Chunk> chunks = new ConcurrentHashMap<>();
    private static final Queue<Chunk> chunksToCompile = new ConcurrentLinkedQueue<>();
    private static final int chunkRenderDistance = 8;
    private static final ExecutorService executor = Executors.newFixedThreadPool(8);

    public static void generateChunks() {
        for (int x = -chunkRenderDistance; x <= chunkRenderDistance; x++) {
            for (int z = -chunkRenderDistance; z <= chunkRenderDistance; z++) {
                for (int y = -chunkRenderDistance; y <= chunkRenderDistance; y++) {
                    Vector3i chunkPos = new Vector3i(x, y, z);
                    executor.submit(() -> {
                        if (!chunks.containsKey(chunkPos)) {
                            Chunk chunk = new Chunk(chunkPos);
                            connectChunk(chunk);
                            chunks.put(chunk.getPosition(), chunk);

                            chunk.generateMesh();
                            chunksToCompile.add(chunk);
                        }
                    });
                }
            }
        }
    }

    public static void update() {
        Chunk chunk;
        while ((chunk = chunksToCompile.poll()) != null) {
            chunk.compileMesh();
        }
    }

    private static void connectChunk(Chunk newChunk) {
        Vector3i position = newChunk.getPosition();

        for (FaceDirection direction : FaceDirection.values()) {
            Vector3i neighborPos = position.add(direction.getOffset());
            Chunk neighbor = chunks.get(neighborPos);
            if (neighbor != null) {
                newChunk.addNeighbor(direction, neighbor);
                neighbor.addNeighbor(direction.getOpposite(), newChunk);
            }
        }
    }

    private static void disconnectChunk(Chunk chunk) {
        for (FaceDirection direction : FaceDirection.values()) {
            Chunk neighbor = chunk.getNeighbor(direction);
            if (neighbor != null) {
                neighbor.removeNeighbor(direction.getOpposite());
                chunk.removeNeighbor(direction);
            }
        }
    }

    public static void render() {
        for (Chunk chunk : chunks.values()) {
            Matrix4f modelMatrix = new Matrix4f().translation(
                    chunk.getPosition().x * Chunk.SIZE,
                    chunk.getPosition().y * Chunk.SIZE,
                    chunk.getPosition().z * Chunk.SIZE
            );
            Display.shader.setUniform("uModel", modelMatrix);
            chunk.render();
        }
    }

    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
