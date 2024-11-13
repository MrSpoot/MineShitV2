package core.manager;

import core.Inputer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Input {
    private static final Logger LOGGER = LoggerFactory.getLogger(Input.class);

    private static Inputer inputer;

    public static void init(Inputer _inputer) {
        inputer = _inputer;
    }

    public static boolean isPressed(String name){
        return inputer.isPressed(name);
    }

    public static float getXAxisRaw(){
        return inputer.getMouseInfo().getXAxis();
    };

    public static float getYAxisRaw(){
        return inputer.getMouseInfo().getYAxis();
    };


}
