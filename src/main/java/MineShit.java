import core.*;
import core.manager.Input;

import static org.lwjgl.glfw.GLFW.*;

public class MineShit {

    public static void main(String[] args) {
            Loop loop = Loop.builder().render(100000).update(100000).build();
            Display window = Display.builder().title("v0.0.1").width(1280).height(720).loop(loop).build();
            Inputer inputer = Inputer.builder().display(window).loop(loop)
                    .touch(new InputTouch("exit",GLFW_KEY_ESCAPE))
                    .touch(new InputTouch("forward",GLFW_KEY_W))
                    .touch(new InputTouch("back",GLFW_KEY_S))
                    .touch(new InputTouch("left",GLFW_KEY_A))
                    .touch(new InputTouch("right",GLFW_KEY_D))
                    .touch(new InputTouch("sprint",GLFW_KEY_LEFT_SHIFT))
                    .touch(new InputTouch("display_wireframe",GLFW_KEY_P))
                    .touch(new InputTouch("display_fill",GLFW_KEY_O))
                    .touch(new InputTouch("rendermod_1",GLFW_KEY_L))
                    .touch(new InputTouch("rendermod_2",GLFW_KEY_K))
                    .build();

            Input.init(inputer);

            Camera camera = new Camera(90,window.aspectRatio(),0.1f,5000f, loop);

            Renderer renderer = Renderer.builder().display(window).loop(loop).camera(camera).build();

            loop.run();
    }

}
