package core;

import core.interfaces.Updatable;
import lombok.Data;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.glfw.GLFW.*;

public class Inputer implements Updatable {

    private final Loop loop;
    private final Display display;
    private final Map<String, InputTouch> touchList;
    private final Map<String, InputTouch> pressedList;
    @Getter
    private final MouseInfo mouseInfo;
    private boolean mouseIsInitiated = false;

    public Inputer(Display display, Map<String, InputTouch> touchList, Loop loop) {
        this.display = display;
        this.touchList = touchList;
        this.pressedList = new ConcurrentHashMap<>();
        this.mouseInfo = new MouseInfo();
        this.loop = loop;

        this.loop.addComponent(this);

        glfwSetCursorPosCallback(display.getId(),(w, x, y) -> processMouseInput((float)x,(float)y));
    }

    @Override
    public void update() {
        touchList.values().forEach((input) -> {
            if (glfwGetKey(display.getId(), input.getTouch()) == GLFW_PRESS) {
                if (!isPressed(input.getName())) {
                    pressedList.put(input.getName(), input);
                }
            } else {
                if (isPressed(input.getName())) {
                    pressedList.remove(input.getName());
                }
            }
        });
    }

    public void processMouseInput(float x, float y){
        if(!mouseIsInitiated){
            mouseInfo.setLastX(x);
            mouseInfo.setLastY(y);
            mouseIsInitiated = true;
        }
        mouseInfo.setXAxis(x - mouseInfo.getLastX());
        mouseInfo.setYAxis(y - mouseInfo.getLastY());

        mouseInfo.setLastX(x);
        mouseInfo.setLastY(y);
    }

    public boolean isPressed(String touchName){
        return this.pressedList.containsKey(touchName);
    }

    @Data
    public class MouseInfo {
        private float lastX = 0f;
        private float lastY = 0f;
        private float xAxis = 0f;
        private float yAxis = 0f;

        public float getXAxisRaw(){
            float x = this.xAxis;
            this.xAxis = 0;
            return x;
        }
        public float getYAxisRaw(){
            float y = this.yAxis;
            this.yAxis = 0;
            return y;
        }
    }

    public static InputerBuilder builder() {
        return new InputerBuilder();
    }

    public static class InputerBuilder {
        private Display display;
        private Map<String, InputTouch> touchList = new ConcurrentHashMap<>();
        private Loop loop;

        private MouseInfo mouseInfo;

        public InputerBuilder display(Display display) {
            this.display = display;
            return this;
        }

        public InputerBuilder touch(InputTouch touch) {
            touchList.put(touch.getName(), touch);
            return this;
        }

        public InputerBuilder loop(Loop loop) {
            this.loop = loop;
            return this;
        }

        public Inputer build() {
            return new Inputer(display, touchList, loop);
        }
    }

}
