package game.texture;

public class TextureAtlasManager {

    private static final int TEXTURE_RESOLUTION = 128;

    public static float[] getTextureCoordinate(TextureAtlas texture){

        final int atlasWidth = Texture.getWidth() / TEXTURE_RESOLUTION;
        final int atlasHeight = Texture.getHeight() / TEXTURE_RESOLUTION;

        final float xOffset = 1f / (float)atlasWidth;
        final float yOffset = 1f / (float)atlasHeight;

        return new float[]{
                1 * xOffset + (xOffset * texture.getX()),1 * yOffset + (yOffset * texture.getY()),
                0 * xOffset + (xOffset * texture.getX()),1 * yOffset + (yOffset * texture.getY()),
                0 * xOffset + (xOffset * texture.getX()),0 * yOffset + (yOffset * texture.getY()),
                1 * xOffset + (xOffset * texture.getX()),0 * yOffset + (yOffset * texture.getY()),};
    }

}
