package game.texture;

import lombok.Getter;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

public class Texture {

    @Getter
    private static int id;
    @Getter
    private static int width, height;

    public static void create(String atlasPath, int filter) {
        BufferedImage image;
        try {
            image = ImageIO.read(TextureAtlasManager.class.getResource(atlasPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int[] pixels = null;

        width = image.getWidth();
        height = image.getHeight();
        pixels = new int[width*height];
        image.getRGB(0,0,width,height,pixels,0,width);

        int[] data = new int[width*height];
        for(int i = 0; i < data.length; i++){

            int a = (pixels[i] & 0xff000000) >> 24;
            int r = (pixels[i] & 0xff0000) >> 16;
            int g = (pixels[i] & 0xff00) >> 8;
            int b = (pixels[i] & 0xff);

            data[i] = a << 24 | b << 16 | g << 8 | r;
        }

        IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
        buffer.put(data).flip();

        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }
}
