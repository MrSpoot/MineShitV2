package game;

import org.joml.Vector3f;
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
    private static final List<Chunk> chunkToCompile = new ArrayList<>();

    private static int vaoId;
    private static int ssboId;
    private static int vboId;
    private static int indirectBufferId;

    private static FloatBuffer chunkPositionBuffer;
    private static IntBuffer indirectBuffer;
    private static List<Integer> globalData;

    private static final float[] baseVertexData = {
            1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f,

            1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f
    };

    public static void initialize() {
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Vertex data
        int baseVertexVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, baseVertexVboId);
        glBufferData(GL_ARRAY_BUFFER, baseVertexData, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);

        // SSBO for chunk positions
        ssboId = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssboId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, 0, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, ssboId);

        // VBO for chunk data
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, 0, GL_DYNAMIC_DRAW);

        glVertexAttribIPointer(1, 1, GL_UNSIGNED_INT, 0, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribDivisor(1, 1);

        // Indirect buffer
        indirectBufferId = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBufferId);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, 0, GL_DYNAMIC_DRAW);

        glBindVertexArray(0);

        chunks.put(new Vector3i(0),new Chunk(new Vector3i(0)));
        chunks.put(new Vector3i(1,0,0),new Chunk(new Vector3i(1,0,0)));
        chunks.put(new Vector3i(0,0,1),new Chunk(new Vector3i(0,0,1)));
        chunks.put(new Vector3i(1,0,1),new Chunk(new Vector3i(1,0,1)));
        updateBuffers();
    }

    public static void updateBuffers() {
        chunkToCompile.clear();
        globalData = new ArrayList<>();

        for (Chunk chunk : chunks.values()) {
            List<Integer> chunkData = chunk.getChunkMesh().getEncodedData();
            if (chunkData != null && !chunkData.isEmpty()) {
                chunkToCompile.add(chunk);
                globalData.addAll(chunkData);
            }
        }

        // Update chunk positions
        chunkPositionBuffer = MemoryUtil.memAllocFloat(chunkToCompile.size() * 4);
        indirectBuffer = MemoryUtil.memAllocInt(4 * chunkToCompile.size());

        int offset = 0;
        for (Chunk chunk : chunkToCompile) {
            chunkPositionBuffer.put(chunk.getPosition().x);
            chunkPositionBuffer.put(chunk.getPosition().y);
            chunkPositionBuffer.put(chunk.getPosition().z);
            chunkPositionBuffer.put(0.0f);

            List<Integer> chunkData = chunk.getChunkMesh().getEncodedData();
            indirectBuffer.put(6); // Primitive count
            indirectBuffer.put(chunkData.size()); // Instance count
            indirectBuffer.put(0); // First index
            indirectBuffer.put(offset); // Base instance
            offset += chunkData.size();
        }

        chunkPositionBuffer.flip();
        indirectBuffer.flip();

        // Upload data to GPU
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssboId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, chunkPositionBuffer, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, toIntArray(globalData), GL_DYNAMIC_DRAW);

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBufferId);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, indirectBuffer, GL_DYNAMIC_DRAW);

        MemoryUtil.memFree(chunkPositionBuffer);
        MemoryUtil.memFree(indirectBuffer);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
    }

    public static void render() {
        glBindVertexArray(vaoId);
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBufferId);
        glMultiDrawArraysIndirect(GL_TRIANGLES, 0, chunkToCompile.size(), 16);
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}