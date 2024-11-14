package core;

import core.interfaces.Renderable;
import game.Chunk;
import game.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.opengl.GL11.*;

public class Renderer implements Renderable {

    private final Display display;
    private final Loop loop;
    private final Camera camera;
    private final List<Renderable> renderables;

    private Renderer(Display display, Loop loop, Camera camera, List<Renderable> renderables) {
        this.display = display;
        this.loop = loop;
        this.camera = camera;
        this.renderables = renderables;

        loop.addComponent(this);
    }

    @Override
    public void render(){
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        Display.shader.setUniform("uView",camera.getViewMatrix());
        Display.shader.setUniform("uProjection",camera.getProjectionMatrix());

        this.renderables.forEach(Renderable::render);

        World.render();

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
