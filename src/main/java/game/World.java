package game;

import core.Display;
import game.utils.BufferManager;
import game.utils.TextureArray;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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

    private static TextureArray textureArray;
    private static final Map<Vector3i, Chunk> chunks = new ConcurrentHashMap<>();
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private static final List<Chunk> chunkToCompile = new ArrayList<>();

    private static int vaoId;
    private static int ssboId;
    private static int vboId;
    private static int indirectBufferId;
    private static BufferManager ssboBufferManager;
    private static BufferManager vboBufferManager;
    private static BufferManager indirectBufferManager;

    private static FloatBuffer chunkPositionBuffer;
    private static IntBuffer indirectBuffer;

    private static boolean buffersNeedUpdate = true;

    private static Vector3i lastPosition = new Vector3i(Integer.MAX_VALUE);

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

        ssboBufferManager = new BufferManager(vaoId,GL_SHADER_STORAGE_BUFFER,100_000);
        ssboId = ssboBufferManager.getBufferId();

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, ssboId);

        vboBufferManager = new BufferManager(vaoId,GL_ARRAY_BUFFER,1_000);
        vboId = vboBufferManager.getBufferId();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        glVertexAttribIPointer(1, 1, GL_UNSIGNED_INT, 0, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribDivisor(1, 1);

        indirectBufferManager = new BufferManager(vaoId,GL_DRAW_INDIRECT_BUFFER,100_000);
        indirectBufferId = indirectBufferManager.getBufferId();

        textureArray = new TextureArray();

        glBindVertexArray(0);
    }

    public static void generateChunksAroundPosition(Vector3f cameraPosition, int renderDistance) {
        int chunkX = (int) Math.floor(cameraPosition.x / Chunk.SIZE);
        int chunkY = (int) Math.floor(cameraPosition.y / Chunk.SIZE);
        int chunkZ = (int) Math.floor(cameraPosition.z / Chunk.SIZE);

        Vector3i center = new Vector3i(chunkX, chunkY, chunkZ);

        if (lastPosition.equals(center)) {
            return;
        }
        lastPosition = center;

        Set<Vector3i> newChunks = new HashSet<>();
        Set<Vector3i> existingChunks = new HashSet<>(chunks.keySet());

        for (int y = -renderDistance; y <= renderDistance; y++) {
            for (int x = -renderDistance; x <= renderDistance; x++) {
                for (int z = -renderDistance; z <= renderDistance; z++) {
                    Vector3i chunkPos = new Vector3i(center.x + x, center.y + y, center.z + z);
                    if (!chunks.containsKey(chunkPos)) {
                        newChunks.add(chunkPos);
                    }
                    existingChunks.remove(chunkPos);
                }
            }
        }

        for (Vector3i chunkPos : existingChunks) {
            executorService.execute(() -> {
                Chunk c = chunks.get(chunkPos);
                c.setState(2);
                chunks.put(chunkPos,c);
                buffersNeedUpdate = true;
            });
        }

        for (Vector3i chunkPos : newChunks) {
            executorService.execute(() -> {
                Chunk chunk = new Chunk(chunkPos);
                chunk.setState(1);
                chunks.put(chunkPos,chunk);
                buffersNeedUpdate = true;
            });
        }
    }

    public static void updateChunkDataBuffer(){
        for(Chunk chunk : chunks.values()){

            //REMOVE
            if(chunk.getState() == 2){
                chunks.remove(chunk.getPosition());
                vboBufferManager.removeData(chunk.getPosition().hashCode());
            }

            //ADD
            if(chunk.getState() == 1){
                if(!chunk.getEncodedData().isEmpty()){
                    vboBufferManager.addData(chunk.getPosition().hashCode(),toByteArray(chunk.getEncodedData()));
                }
            }

            //DIRTY
            if(chunk.getState() == 3){
                vboBufferManager.removeData(chunk.getPosition().hashCode());
                vboBufferManager.addData(chunk.getPosition().hashCode(),toByteArray(chunk.getEncodedData()));
            }

        }
    }

    public static void updateSmallBuffers() {
        chunkToCompile.clear();

        for(Map.Entry<Integer, Integer> entry : vboBufferManager.getOrderedOffsets()) {
             chunks.values().stream().filter(_c -> _c.getPosition().hashCode() == entry.getKey()).findFirst().ifPresent(chunkToCompile::add);
        }

        // Update chunk positions
        chunkPositionBuffer = MemoryUtil.memAllocFloat(chunkToCompile.size() * 4);
        indirectBuffer = MemoryUtil.memAllocInt(4 * chunkToCompile.size());

        for (Chunk chunk : chunkToCompile) {
            chunkPositionBuffer.put(chunk.getPosition().x);
            chunkPositionBuffer.put(chunk.getPosition().y);
            chunkPositionBuffer.put(chunk.getPosition().z);
            chunkPositionBuffer.put(0.0f);

            indirectBuffer.put(6); // Primitive count
            indirectBuffer.put(vboBufferManager.getIdSize(chunk.getPosition().hashCode()) / Integer.BYTES); // Instance count
            indirectBuffer.put(0); // First index
            indirectBuffer.put(vboBufferManager.getIdOffset(chunk.getPosition().hashCode()) / Integer.BYTES); // Base instance
        }

        chunkPositionBuffer.flip();
        indirectBuffer.flip();

        // Upload data to GPU
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssboId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, chunkPositionBuffer, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBufferId);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, indirectBuffer, GL_DYNAMIC_DRAW);

        MemoryUtil.memFree(chunkPositionBuffer);
        MemoryUtil.memFree(indirectBuffer);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
    }

    public static void render() {
        textureArray.bind();
        if (buffersNeedUpdate) {
            updateChunkDataBuffer();
            updateSmallBuffers();
            buffersNeedUpdate = false;
        }
        glBindVertexArray(vaoId);
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBufferId);
        glMultiDrawArraysIndirect(GL_TRIANGLES, 0, chunkToCompile.size(), 16);
        textureArray.unbind();
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private static byte[] toByteArray(List<Integer> list) {
        ByteBuffer buffer = ByteBuffer.allocate(list.size() * Integer.BYTES).order(ByteOrder.nativeOrder());
        for (int value : list) {
            buffer.putInt(value);
        }
        return buffer.array();
    }

    public static void shutdown() {
        executorService.shutdown();
    }

}