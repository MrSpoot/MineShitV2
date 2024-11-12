package core;

import core.interfaces.LoopAccessable;
import core.interfaces.Renderable;
import core.interfaces.Updatable;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

public class Loop {

    //With Getter
    private int RENDER_TICKS_PER_SECOND = 500;
    private double RENDER_TIME = 1.0 / RENDER_TICKS_PER_SECOND;

    //Without Getter
    private double lastSecondTime = 0.0;
    private long lastRenderTime = System.nanoTime();
    private boolean shouldStop = false;

    List<LoopAccessable> components = new ArrayList<>();

    private Loop(int delta){
        this.RENDER_TICKS_PER_SECOND = delta;
    }

    public void run(){
        while (!shouldStop) {
            long currentTime = System.nanoTime();
            double deltaTimeRender = (currentTime - lastRenderTime) / 1e9;

            if (RENDER_TICKS_PER_SECOND <= 0 || deltaTimeRender >= RENDER_TIME) {
                //RENDER
                components.stream().filter(c -> c instanceof Renderable).forEach(c -> ((Renderable) c).render());
                lastRenderTime = currentTime;
            }

            glfwPollEvents();

            if (currentTime - lastSecondTime >= 1e9) {
                lastSecondTime = currentTime;
            }

        }
        glfwTerminate();
    }

    public void addComponent(LoopAccessable component){
        components.add(component);
    }

    public void stop(){
        this.shouldStop = true;
    }

    public void setFps(int fps){
        RENDER_TICKS_PER_SECOND = fps;
        RENDER_TIME = 1.0 / fps;
    }

    public static LoopBuilder builder(){
        return new LoopBuilder();
    }

    public static class LoopBuilder {
        private int delta;

        public LoopBuilder setDelta(int delta){
            this.delta = delta;
            return this;
        }

        public Loop build(){
            return new Loop(delta);
        }
    }
}
