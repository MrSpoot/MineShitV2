package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

public class Time {

    private static final Map<String, TimeInfo> timeInfoList = new HashMap<>();

    static void update(String name){

        if(timeInfoList.containsKey(name)){
            timeInfoList.get(name).update();
        }else{
            TimeInfo timeInfo = new TimeInfo();
            timeInfoList.put(name, timeInfo);
            timeInfo.update();
        }
    }

    static float delta(String name){
        if(timeInfoList.containsKey(name)){
            return timeInfoList.get(name).deltaTime;
        }else{
            return 0;
        }
    }

    private static class TimeInfo {
        public float deltaTime = 0f;
        private float lastTime = 0.0f;

        void update(){
            float currentFrame = (float)glfwGetTime();
            deltaTime = currentFrame - lastTime;
            lastTime = currentFrame;
        }
    }
}
