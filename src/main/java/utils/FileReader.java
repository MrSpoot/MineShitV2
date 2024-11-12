package utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.stb.STBImage.*;

public class FileReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReader.class);

    public static String readFile(String path){
        String content = "";
        try {
            // Lecture du contenu du fichier en utilisant la classe Files
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            content = new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public static Image readImage(String path, boolean flip) {
            stbi_set_flip_vertically_on_load(flip);
            IntBuffer x = BufferUtils.createIntBuffer(1);
            IntBuffer y = BufferUtils.createIntBuffer(1);
            IntBuffer channels = BufferUtils.createIntBuffer(1);
            ByteBuffer image = stbi_load(path, x, y, channels, STBI_rgb_alpha);
            if (image == null) {
                LOGGER.warn("Could not decode image file ["+ path +"]: ["+ STBImage.stbi_failure_reason() +"]");
                return null;
            }
            int width = x.get();
            int height = y.get();
            stbi_set_flip_vertically_on_load(false);
            return new Image(image,width,height);
    }

    public static ByteBuffer read(String resourcePath, int bufferSize) throws IOException {
        ByteBuffer buffer;

        try (InputStream source = FileReader.class.getResourceAsStream(resourcePath)) {
            if (source == null) {
                LOGGER.error("Resource not found");
            }
            try (ReadableByteChannel rbc = Channels.newChannel(source)) {
                buffer = BufferUtils.createByteBuffer(bufferSize);

                while (true) {
                    int bytes = rbc.read(buffer);
                    if (bytes == -1) {
                        break;
                    }
                    if (buffer.remaining() == 0) {
                        buffer = resizeBuffer(buffer, buffer.capacity() * 2);
                    }
                }
            }
        }

        buffer.flip();
        return buffer;
    }

    private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }

}
