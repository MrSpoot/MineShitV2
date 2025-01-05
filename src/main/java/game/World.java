package game;

import core.Camera;
import core.Display;
import core.Shader;
import game.utils.BufferManager;
import game.utils.TextureArray;
import lombok.Setter;
import org.joml.Matrix4f;
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

import static org.lwjgl.glfw.GLFW.glfwGetTime;
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

    private static final ShadowMap shadowMap = new ShadowMap(8192 * 2,8192 * 2);
    private static final Shader shadowShader = new Shader("/shaders/shadow.glsl");
    private static final Light sunLight = new Light();

    private static TextureArray textureArray;
    private static final Map<Vector3i, Chunk> chunks = new ConcurrentHashMap<>();
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private static final List<Chunk> chunkToCompile = new ArrayList<>();

    private static final Queue<Chunk> updateQueue = new ConcurrentLinkedQueue<>();
    private static final int CHUNKS_PER_FRAME = 10; // Nombre de chunks à mettre à jour par frame
    private static boolean updateInProgress = false;
    private static Camera camera;

    private static int vaoId;
    private static int ssboId;
    private static int vboId;
    private static int indirectBufferId;
    private static BufferManager ssboBufferManager;
    private static BufferManager vboBufferManager;
    private static BufferManager indirectBufferManager;

    private static FloatBuffer chunkPositionBuffer;
    private static IntBuffer indirectBuffer;

    private static final Vector3f lightDirection = new Vector3f(0.0f, -1.0f, 0.0f).normalize(); // Direction du soleil
    private static final Vector3f lightPosition = new Vector3f(50.0f, 16.0f, 0.0f);
    private static final Matrix4f lightProjection = new Matrix4f().ortho(-500.0f, 500.0f, -500.0f, 500.0f, -500.0f, 100.0f);
    private static final Matrix4f lightView = new Matrix4f();
    private static final Matrix4f lightSpaceMatrix = new Matrix4f();

    @Setter
    private static boolean buffersNeedUpdate = true;

    private static int lastRenderDistance = -1;
    private static Vector3i lastPosition = new Vector3i(Integer.MAX_VALUE);
    private static Vector3f cameraPosition;

    private static final float[] baseVertexData = {
            1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f,

            1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f
    };

    public static void initialize(Camera _camera) {
        camera = _camera;
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Vertex data
        int baseVertexVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, baseVertexVboId);
        glBufferData(GL_ARRAY_BUFFER, baseVertexData, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);

        ssboBufferManager = new BufferManager(vaoId, GL_SHADER_STORAGE_BUFFER, 100_000, null);
        ssboId = ssboBufferManager.getBufferId();

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, ssboId);

        vboBufferManager = new BufferManager(vaoId, GL_ARRAY_BUFFER, 100_000_000, (b) -> {
            glVertexAttribIPointer(1, 1, GL_UNSIGNED_INT, 0, 0);
            glEnableVertexAttribArray(1);
            glVertexAttribDivisor(1, 1);
        });

        vboId = vboBufferManager.getBufferId();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        indirectBufferManager = new BufferManager(vaoId, GL_DRAW_INDIRECT_BUFFER, 100_000, null);
        indirectBufferId = indirectBufferManager.getBufferId();

        textureArray = new TextureArray();

        glBindVertexArray(0);
    }

    public static void generateChunksAroundPosition(Vector3f _cameraPosition, int renderDistance) {
        cameraPosition = _cameraPosition;
        int chunkX = (int) Math.floor(cameraPosition.x / Chunk.SIZE);
        int chunkY = (int) Math.floor(cameraPosition.y / Chunk.SIZE);
        int chunkZ = (int) Math.floor(cameraPosition.z / Chunk.SIZE);

        Vector3i center = new Vector3i(chunkX, chunkY, chunkZ);

        if (lastPosition.equals(center) && lastRenderDistance == renderDistance) {
            return;
        }
        lastPosition = center;
        lastRenderDistance = renderDistance;

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
                if (c != null) {
                    c.setState(2);
                    chunks.put(chunkPos, c);
                    buffersNeedUpdate = true;
                }
            });
        }

        for (Vector3i chunkPos : newChunks) {
            executorService.execute(() -> {
                Chunk chunk = new Chunk(chunkPos);
                chunk.setState(1);
                chunks.put(chunkPos, chunk);
                buffersNeedUpdate = true;
            });
        }
    }

    public static void updateChunkDataBuffer() {
        for (Chunk chunk : chunks.values()) {
            if (chunk.getState() == 2 || chunk.getState() == 1 || chunk.getState() == 3) {
                updateQueue.add(chunk);
            }
        }
        updateInProgress = true;
    }

    private static void processChunkUpdates() {
        int chunksProcessed = 0;

        while (!updateQueue.isEmpty() && chunksProcessed < CHUNKS_PER_FRAME) {
            Chunk chunk = updateQueue.poll();
            if (chunk != null) {
                int chunkHash = chunk.getPosition().hashCode();
                switch (chunk.getState()) {
                    case 2: // REMOVE
                        chunks.remove(chunk.getPosition());
                        vboBufferManager.removeData(chunkHash);
                        break;
                    case 1: // ADD
                        if (!chunk.getEncodedData().isEmpty()) {
                            vboBufferManager.addData(chunkHash, toByteArray(chunk.getEncodedData()));
                        }
                        break;
                    case 3: // DIRTY
                        vboBufferManager.removeData(chunkHash);
                        vboBufferManager.addData(chunkHash, toByteArray(chunk.getEncodedData()));
                        break;
                }
                chunk.setState(0); // Reset state after processing
                chunksProcessed++;
            }
        }

        // Update small buffers after processing chunks
        updateSmallBuffers();

        if (updateQueue.isEmpty()) {
            updateInProgress = false;
        }
    }

    public static void updateSmallBuffers() {
        chunkToCompile.clear();

        for (Map.Entry<Integer, Integer> entry : vboBufferManager.getOrderedOffsets()) {
            chunks.values().stream().filter(_c -> _c.getPosition().hashCode() == entry.getKey()).findFirst().ifPresent(chunkToCompile::add);
        }

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

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssboId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, chunkPositionBuffer, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBufferId);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, indirectBuffer, GL_DYNAMIC_DRAW);

        MemoryUtil.memFree(chunkPositionBuffer);
        MemoryUtil.memFree(indirectBuffer);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
    }

    private static void updateLightMatrices() {
        // La lumière regarde le centre de la scène depuis sa position
        lightView.identity().lookAt(lightPosition, new Vector3f(0, 0, 0), new Vector3f(0, 0, -1)); // Utilisez (0, 0, -1) si Y est "up".
        lightSpaceMatrix.set(lightProjection).mul(lightView);
    }


    public static void renderShadowMap() {


        shadowMap.bind();

        shadowShader.useProgram();
        shadowShader.setUniform("uLightSpaceMatrix", sunLight.getLightSpaceMatrix());
        shadowShader.setUniform("lightDir", sunLight.getLightDirection());

        glBindVertexArray(vaoId);
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBufferId);
        glMultiDrawArraysIndirect(GL_TRIANGLES, 0, chunkToCompile.size(), 16);

        glBindVertexArray(0);
        shadowShader.unbind();
        shadowMap.unbind(1280, 720);
    }


    public static void render() {
        sunLight.setLightDirection(new Vector3f((float) Math.sin(glfwGetTime() / 16), (float) -1, (float) Math.cos(glfwGetTime() / 16)));
        //sunLight.setLightDirection(new Vector3f(0.2f,-1,0.2f));

        glCullFace(GL_FRONT);
        renderShadowMap();
        glCullFace(GL_BACK);

        Display.shader.useProgram();

        Display.shader.setUniform("uLightSpaceMatrix", sunLight.getLightSpaceMatrix());
        Display.shader.setUniform("lightDir", sunLight.getLightDirection());
        Display.shader.setUniform("shadowMap", 1); // Texture unit 1
        shadowMap.bindTexture(1);

        textureArray.bind();
        if (buffersNeedUpdate) {
            updateChunkDataBuffer();
            buffersNeedUpdate = false;
        }
        if (updateInProgress) {
            processChunkUpdates();
        }

        glBindVertexArray(vaoId);
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBufferId);
        glMultiDrawArraysIndirect(GL_TRIANGLES, 0, chunkToCompile.size(), 16);
        textureArray.unbind();
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

