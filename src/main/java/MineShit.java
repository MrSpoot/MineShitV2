import core.*;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;

public class MineShit {

    public static void main(String[] args) {
        Loop loop = Loop.builder().render(120).build();
        Display window = Display.builder().title("v0.0.1").width(1280).height(720).loop(loop).build();
        Inputer inputer = Inputer.builder().display(window).loop(loop).touch(new InputTouch("ESCAPE",GLFW_KEY_ESCAPE)).build();

        Camera camera = new Camera(90,window.aspectRatio(),0.1f,1000f);

        Renderer renderer = Renderer.builder().display(window).loop(loop).camera(camera).build();

        loop.run();
    }

}
