package core;

import core.interfaces.Renderable;
import game.utils.TextureArray;
import lombok.Getter;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

@Getter
public class Display implements Renderable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Display.class);

    private final long id;
    private final String title;
    private final int width;
    private final int height;
    private final Loop loop;
    public static Shader shader;

    private Display(long id, String title, int width, int height, Loop loop, Shader shader) {
        this.id = id;
        this.title = title;
        this.width = width;
        this.height = height;
        this.loop = loop;
        Display.shader = shader;

        loop.addComponent(this);
    }

    public float aspectRatio() {
        return width / (float) height;
    }

    @Override
    public void render(){
        if(shouldClose()){
            this.loop.stop();
        }
    }

    public static class DisplayBuilder {
        private long id;
        private String title;
        private int width;
        private int height;
        private Loop loop;

        public DisplayBuilder title(String title) {
            this.title = title;
            return this;
        }

        public DisplayBuilder width(int width) {
            this.width = width;
            return this;
        }

        public DisplayBuilder height(int height) {
            this.height = height;
            return this;
        }

        public DisplayBuilder loop(Loop loop) {
            this.loop = loop;
            return this;
        }

        public Display build() {
            if (!glfwInit()) {
                throw new IllegalStateException("Unable to initialize GLFW");
            }

            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

            glfwWindowHint(GLFW_SAMPLES, 4);

            if (this.width == 0 && this.height == 0) {
                glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);
                GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
                assert vidMode != null;
                width = vidMode.width();
                height = vidMode.height();
            }

            id = glfwCreateWindow(width, height, "", NULL, NULL);
            if (id == NULL) {
                throw new RuntimeException("Failed to create the GLFW window");
            }

            glfwSetFramebufferSizeCallback(id, this::resize);

            glfwSetErrorCallback((int errorCode, long msgPtr) ->
                    LOGGER.error("Error code [{}], msg [{}]", errorCode, MemoryUtil.memUTF8(msgPtr))
            );
            glfwMakeContextCurrent(id);
            glfwSetInputMode(id,GLFW_CURSOR,GLFW_CURSOR_DISABLED);
            GL.createCapabilities();

            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);

            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);

            glfwSwapInterval(0);
            glViewport(0,0,width,height);
            glfwShowWindow(id);
            glfwSetWindowTitle(id,this.title);

            Shader shader = new Shader("/shaders/default.glsl");

            return new Display(id, title, width, height,loop,shader);
        }

        private void resize(long windowHandle, int width, int height){
            glViewport(0,0,width,height);
        }
    }

    public static DisplayBuilder builder() {
        return new DisplayBuilder();
    }

    public boolean shouldClose(){
        return glfwWindowShouldClose(this.id);
    }
}
