package core;

import core.interfaces.Renderable;
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
    private final Shader shader = new Shader("src/main/resources/shaders/default.glsl");
    private final Mesh cube = CubeMesh.createCube();

    private Renderer(Display display, Loop loop) {
        this.display = display;
        this.loop = loop;
        this.camera = new Camera(75,display.aspectRatio(),0.1f,1000f);

        loop.addComponent(this);
    }

    @Override
    public void render(){
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        shader.useProgram();

        Matrix4f model = new Matrix4f().identity()  // Commencer avec une matrice identité
                .translate(new Vector3f(0))  // Appliquer la translation
                .rotateX((float) Math.toRadians(0))  // Appliquer la rotation sur X
                .rotateY((float) Math.toRadians(0))  // Appliquer la rotation sur Y
                .rotateZ((float) Math.toRadians(0))  // Appliquer la rotation sur Z
                .scale(new Vector3f(1));  // Appliquer l'échelle

        shader.setUniform("uView",camera.getViewMatrix());

        shader.setUniform("uProjection",camera.getProjectionMatrix());

        shader.setUniform("uModel",model);
        cube.render();

        model.setTranslation(2,0,0);

        shader.setUniform("uModel",model);
        cube.render();

        model.setTranslation(-2,0,0);

        shader.setUniform("uModel",model);
        cube.render();

        glfwSwapBuffers(this.display.getId());
        glfwPollEvents();
    }

    public static RendererBuilder builder() {
        return new RendererBuilder();
    }

    public static class RendererBuilder{

        private Display display;
        private Loop loop;

        public RendererBuilder display(Display display) {
            this.display = display;
            return this;
        }

        public RendererBuilder loop(Loop loop) {
            this.loop = loop;
            return this;
        }

        public Renderer build() {
            return new Renderer(display, loop);
        }
    }

}
