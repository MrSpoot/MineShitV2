package game.texture;

public class TextureAtlasManager {

    private static final int TEXTURE_RESOLUTION = 128;

    public static float[] getTextureCoordinate(TextureAtlas texture) {
        final int atlasWidth = Texture.getWidth() / TEXTURE_RESOLUTION;
        final int atlasHeight = Texture.getHeight() / TEXTURE_RESOLUTION;

        final float xOffset = 1f / (float) atlasWidth;
        final float yOffset = 1f / (float) atlasHeight;

        float padding = 0.001f;

        float uMin = (xOffset * texture.getX()) + padding;
        float uMax = (xOffset * (texture.getX() + 1)) - padding;
        float vMin = (yOffset * texture.getY()) + padding;
        float vMax = (yOffset * (texture.getY() + 1)) - padding;

        return new float[]{
                uMax, vMax,
                uMin, vMax,
                uMin, vMin,
                uMax, vMin
        };
    }
}

