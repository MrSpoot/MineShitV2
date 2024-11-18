package game;

import game.texture.TextureAtlas;
import game.utils.Face;

public enum BlockType {
    AIR(TextureAtlas.AIR, TextureAtlas.AIR, TextureAtlas.AIR, TextureAtlas.AIR, TextureAtlas.AIR, TextureAtlas.AIR),
    GRASS(TextureAtlas.GRASS_TOP, TextureAtlas.GRASS_BOTTOM, TextureAtlas.GRASS_SIDE, TextureAtlas.GRASS_SIDE, TextureAtlas.GRASS_SIDE, TextureAtlas.GRASS_SIDE),
    STONE(TextureAtlas.STONE, TextureAtlas.STONE, TextureAtlas.STONE, TextureAtlas.STONE, TextureAtlas.STONE, TextureAtlas.STONE),
    DIRT(TextureAtlas.DIRT, TextureAtlas.DIRT, TextureAtlas.DIRT, TextureAtlas.DIRT, TextureAtlas.DIRT, TextureAtlas.DIRT),
    SAND(TextureAtlas.SAND, TextureAtlas.SAND, TextureAtlas.SAND, TextureAtlas.SAND, TextureAtlas.SAND, TextureAtlas.SAND),
    LOG(TextureAtlas.LOG_TOP, TextureAtlas.LOG_TOP, TextureAtlas.LOG_SIDE, TextureAtlas.LOG_SIDE, TextureAtlas.LOG_SIDE, TextureAtlas.LOG_SIDE),
    BEDROCK(TextureAtlas.BEDROCK, TextureAtlas.BEDROCK, TextureAtlas.BEDROCK, TextureAtlas.BEDROCK, TextureAtlas.BEDROCK, TextureAtlas.BEDROCK),
    OAK_LEAVES(TextureAtlas.OAK_LEAVES, TextureAtlas.OAK_LEAVES, TextureAtlas.OAK_LEAVES, TextureAtlas.OAK_LEAVES, TextureAtlas.OAK_LEAVES, TextureAtlas.OAK_LEAVES),
    TEST(TextureAtlas.DIRT, TextureAtlas.BEDROCK, TextureAtlas.LOG_TOP, TextureAtlas.STONE, TextureAtlas.OAK_LEAVES, TextureAtlas.SAND);

    private final TextureAtlas topTexture;
    private final TextureAtlas bottomTexture;
    private final TextureAtlas leftTexture;
    private final TextureAtlas rightTexture;
    private final TextureAtlas frontTexture;
    private final TextureAtlas backTexture;

    BlockType(TextureAtlas topTexture, TextureAtlas bottomTexture, TextureAtlas leftTexture,
              TextureAtlas rightTexture, TextureAtlas frontTexture, TextureAtlas backTexture) {
        this.topTexture = topTexture;
        this.bottomTexture = bottomTexture;
        this.leftTexture = leftTexture;
        this.rightTexture = rightTexture;
        this.frontTexture = frontTexture;
        this.backTexture = backTexture;
    }

    public static BlockType fromIndex(int index) {
        BlockType[] values = BlockType.values();
        if (index < 0 || index >= values.length) {
            return AIR;
        }
        return values[index];
    }

    public TextureAtlas getTextureForFace(Face face) {
        return switch (face) {
            case TOP -> topTexture;
            case BOTTOM -> bottomTexture;
            case LEFT -> leftTexture;
            case RIGHT -> rightTexture;
            case FRONT -> frontTexture;
            case BACK -> backTexture;
        };
    }
}

