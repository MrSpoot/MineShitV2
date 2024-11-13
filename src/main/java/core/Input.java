package core;

import org.spoot.ryukazev2.manager.InputManager;
import org.spoot.ryukazev2.utils.ServiceLocator;

public class Input {

    private static InputManager inputManager;

    public static void init(){
        inputManager = ServiceLocator.getService(InputManager.class);
    }

    public static boolean isPressed(String touch){
        return inputManager.isPressed(touch);
    }

    public static float getXAxisRaw(){
        return inputManager.getXAxisRaw();
    };

    public static float getYAxisRaw(){
        return inputManager.getYAxisRaw();
    };

}
