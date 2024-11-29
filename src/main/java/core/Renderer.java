package core;

import core.interfaces.Renderable;
import game.Chunk;
import game.FaceDirection;
import org.joml.Matrix4f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.opengl.GL11.*;

public class Renderer implements Renderable {

    private final Display display;
    private final Loop loop;
    private final Camera camera;
    private final List<Renderable> renderables;

    private final Map<Vector3i,Chunk> chunks = new HashMap<>();
    private final int chunkRenderDistance = 16;

    private Renderer(Display display, Loop loop, Camera camera, List<Renderable> renderables) {
        this.display = display;
        this.loop = loop;
        this.camera = camera;
        this.renderables = renderables;

        generateChunks();

        loop.addComponent(this);
    }

    private void generateChunks() {
        for (int x = -chunkRenderDistance; x <= chunkRenderDistance; x++) {
            for (int z = -chunkRenderDistance; z <= chunkRenderDistance; z++) {
                for(int y = -chunkRenderDistance; y <= chunkRenderDistance; y++) {
                    Chunk chunk = new Chunk(new Vector3i(x, y, z));
                    connectChunk(chunk);
                    chunks.put(chunk.getPosition(),chunk);
                }
            }
        }

        for(Chunk chunk : chunks.values()) {
            chunk.generateMesh();
            chunk.compileMesh();
        }
    }

    private void connectChunk(Chunk newChunk) {
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

    private void disconnectChunk(Chunk chunk) {
        for (FaceDirection direction : FaceDirection.values()) {
            Chunk neighbor = chunk.getNeighbor(direction);
            if (neighbor != null) {
                neighbor.removeNeighbor(direction.getOpposite());
                chunk.removeNeighbor(direction);
            }
        }
    }

    @Override
    public void render(){
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        Display.shader.useProgram();

        Display.shader.setUniform("uView",camera.getViewMatrix());
        Display.shader.setUniform("uProjection",camera.getProjectionMatrix());

        this.renderables.forEach(Renderable::render);

        for (Chunk chunk : chunks.values()) {
            Matrix4f modelMatrix = new Matrix4f().translation(chunk.getPosition().x * Chunk.SIZE, chunk.getPosition().y * Chunk.SIZE, chunk.getPosition().z * Chunk.SIZE);
            Display.shader.setUniform("uModel", modelMatrix);
            chunk.render();
        }

        glfwSwapBuffers(this.display.getId());
    }

    public static RendererBuilder builder() {
        return new RendererBuilder();
    }

    public static class RendererBuilder{

        private Display display;
        private Loop loop;
        private Camera camera;
        private List<Renderable> renderables = new ArrayList<>();

        public RendererBuilder display(Display display) {
            this.display = display;
            return this;
        }

        public RendererBuilder loop(Loop loop) {
            this.loop = loop;
            return this;
        }

        public RendererBuilder camera(Camera camera) {
            this.camera = camera;
            return this;
        }

        public RendererBuilder renderable(Renderable renderables) {
            this.renderables.add(renderables);
            return this;
        }

        public Renderer build() {
            return new Renderer(display, loop, camera, renderables);
        }
    }

}
