package game.utils;

import lombok.Getter;

@Getter
public enum BlockType {
    AIR("/texture/grass_full.png"),
    GRASS("/texture/grass_full.png"),
    DIRT("/texture/dirt_full.png"),
    STONE("/texture/stone_full.png"),
    TEST("/texture/test_full.png");

    private final String texturePath;

    BlockType(String texturePath) {
        this.texturePath = texturePath;
    }
}
