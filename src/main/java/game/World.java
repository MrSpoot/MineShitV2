package game;

import core.Display;
import core.Renderer;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.*;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL33C.glVertexAttribDivisor;
import static org.lwjgl.opengl.GL40C.GL_DRAW_INDIRECT_BUFFER;
import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL43C.glMultiDrawArraysIndirect;

public class World {

    private static final Logger LOGGER = LoggerFactory.getLogger(World.class);

    private static final Map<Vector3i, Chunk> chunks = new ConcurrentHashMap<>();
    private static final int chunkRenderDistance = 12;

    private static int vaoId;
    private static int indirectBufferId;

    private static float[] baseVertexData = {
            1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f,

            1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f
    };

    public static void generateChunks() {
        for (int x = -chunkRenderDistance; x <= chunkRenderDistance; x++) {
            for (int z = -chunkRenderDistance; z <= chunkRenderDistance; z++) {
                for (int y = -chunkRenderDistance; y <= chunkRenderDistance; y++) {
                    LOGGER.info("Toujours vivant alloa");
                    Vector3i chunkPos = new Vector3i(x, y, z);
                    if (!chunks.containsKey(chunkPos)) {
                        Chunk chunk = new Chunk(chunkPos);
                        connectChunk(chunk);
                        chunks.put(chunk.getPosition(), chunk);
                    }
                }
            }
        }

        int i = 0;
        for(Chunk chunk : chunks.values()) {
            LOGGER.info("Progression -> "+ (i * 100 / chunks.values().size()));
            chunk.generateMesh();
            i++;
        }
    }

    public static void compile() {
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        int baseVertexVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, baseVertexVboId);

        glBufferData(GL_ARRAY_BUFFER, baseVertexData, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);

        List<Integer> globalData = new ArrayList<>();
        IntBuffer indirectBuffer = MemoryUtil.memAllocInt(4 * chunks.size());
        FloatBuffer chunkPositionBuffer = MemoryUtil.memAllocFloat(4 * chunks.size());
        int offset = 0;

        List<Chunk> chunkToCompile = new ArrayList<>();

        for(Chunk chunk : chunks.values()) {
            List<Integer> chunkData = chunk.getChunkMesh().getEncodedData();
            if(chunkData != null && !chunkData.isEmpty()){
                chunkToCompile.add(chunk);
            }
        }

        for (Chunk chunk : chunkToCompile) {
            List<Integer> chunkData = chunk.getChunkMesh().getEncodedData();

            if (chunkData != null && !chunkData.isEmpty()) {
                globalData.addAll(chunkData);

                chunkPositionBuffer.put(chunk.getPosition().x);
                chunkPositionBuffer.put(chunk.getPosition().y);
                chunkPositionBuffer.put(chunk.getPosition().z);
                chunkPositionBuffer.put(0.0f);

                indirectBuffer.put(6);
                indirectBuffer.put(chunkData.size());
                indirectBuffer.put(0);
                indirectBuffer.put(offset);
                offset += chunkData.size();
            }
        }

        indirectBuffer.flip();
        chunkPositionBuffer.flip();

        int ssboId = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssboId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, chunkPositionBuffer, GL_STATIC_DRAW);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, toIntArray(globalData), GL_STATIC_DRAW);

        glVertexAttribIPointer(1, 1, GL_UNSIGNED_INT, 0, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribDivisor(1, 1);

        indirectBufferId = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBufferId);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, indirectBuffer, GL_STATIC_DRAW);

        MemoryUtil.memFree(chunkPositionBuffer);
        MemoryUtil.memFree(indirectBuffer);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, ssboId);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        glBindVertexArray(0);
    }

    public static void addChunk(Chunk chunk) {
        chunks.put(chunk.getPosition(), chunk);
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
        glBindVertexArray(vaoId);
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBufferId);
        glMultiDrawArraysIndirect(GL_TRIANGLES, 0, chunks.values().stream().filter(c -> c.getChunkMesh().getEncodedData() != null && !c.getChunkMesh().getEncodedData().isEmpty()).toList().size(), 16);
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
