import core.*;

public class MineShit {

    public static void main(String[] args) {
        Loop loop = Loop.builder().setDelta(60).build();
        Display window = Display.builder().title("v0.0.1").width(1280).height(720).loop(loop).build();
        Renderer renderer = Renderer.builder().display(window).loop(loop).build();
        loop.run();
    }

}
