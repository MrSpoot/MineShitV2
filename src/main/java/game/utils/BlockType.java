package game.utils;

import lombok.Getter;

@Getter
public enum BlockType {
    AIR("/texture/grass.png"),
    GRASS("/texture/grass.png"),
    DIRT("/texture/dirt.png"),
    STONE("/texture/stone.png");

    private final String texturePath;

    BlockType(String texturePath) {
        this.texturePath = texturePath;
    }
}
