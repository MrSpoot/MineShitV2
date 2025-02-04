package core;

import core.interfaces.LoopAccessable;
import core.interfaces.Renderable;
import core.interfaces.Updatable;
import core.manager.Input;
import game.World;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

public class Loop {

    //With Getter
    private int RENDER_TICKS_PER_SECOND = 500;
    private int UPDATE_TICKS_PER_SECOND = 500;
    private double RENDER_TIME;
    private double UPDATE_TIME;

    private int frames = 0;
    private int fps = 0;

    //Without Getter
    private double lastSecondTime = 0.0;
    private long lastRenderTime = System.nanoTime();
    private long lastUpdateTime = System.nanoTime();
    private long lastFPSTime = System.nanoTime();
    private boolean shouldStop = false;

    List<LoopAccessable> components = new ArrayList<>();

    private Loop(int render, int update) {
        this.RENDER_TICKS_PER_SECOND = render;
        this.UPDATE_TICKS_PER_SECOND = update;
        this.RENDER_TIME = 1.0 / RENDER_TICKS_PER_SECOND;
        this.UPDATE_TIME = 1.0 / UPDATE_TICKS_PER_SECOND;
    }

    public void run(){
        while (!shouldStop) {
            long currentTime = System.nanoTime();
            double deltaTimeRender = (currentTime - lastRenderTime) / 1e9;
            double deltaTimeUpdate = (currentTime - lastUpdateTime) / 1e9;

            if (RENDER_TICKS_PER_SECOND <= 0 || deltaTimeRender >= RENDER_TIME) {
                //RENDER
                frames++;
                Time.update("render");
                components.stream().filter(c -> c instanceof Renderer).forEach(c -> ((Renderer) c).render());
                lastRenderTime = currentTime;
            }

            if (UPDATE_TICKS_PER_SECOND <= 0 || deltaTimeUpdate >= UPDATE_TIME) {
                //RENDER
                Time.update("update");
                components.stream().filter(c -> c instanceof Updatable).forEach(c -> ((Updatable) c).update());
                lastUpdateTime = currentTime;
                if(Input.isPressed("exit")){
                    shouldStop = true;
                }
            }

            if (currentTime - lastFPSTime >= 1000000000) { // 1 seconde écoulée
                fps = frames;
                frames = 0;
                lastFPSTime = currentTime;
                System.out.println("FPS: " + fps); // Affichage console
            }

            glfwPollEvents();

            if (currentTime - lastSecondTime >= 1e9) {
                lastSecondTime = currentTime;
            }

        }
        World.shutdown();
        glfwTerminate();
    }

    public void addComponent(LoopAccessable component){
        components.add(component);
    }

    public void stop(){
        this.shouldStop = true;
    }

    public static LoopBuilder builder(){
        return new LoopBuilder();
    }

    public static class LoopBuilder {
        private int render = 60;
        private int update = 20;

        public LoopBuilder render(int render){
            this.render = render;
            return this;
        }

        public LoopBuilder update(int update){
            this.update = update;
            return this;
        }

        public Loop build(){
            return new Loop(render, update);
        }
    }
}
