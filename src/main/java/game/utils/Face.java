package game.utils;

import lombok.Getter;

@Getter
public enum Face {

    TOP(0b100000),
    BOTTOM(0b100000),
    LEFT(0b100000),
    RIGHT(0b100000),
    FRONT(0b100000),
    BACK(0b100000);

    private final int faceMask;

    Face(int faceMask) {
        this.faceMask = faceMask;
    }

}
