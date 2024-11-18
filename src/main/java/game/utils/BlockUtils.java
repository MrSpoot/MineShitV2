package game.utils;

import game.BlockType;

public class BlockUtils {

    public static short create(BlockType type){
        int typeIndex = type.ordinal();
        int faceVisibility = 0b111111;
        int blockData = (typeIndex << 6) | faceVisibility;
        return (short) blockData;
    }

    public static boolean isFaceVisible(short data, Face face) {
        return (data & face.getFaceMask()) != 0;
    }

    public static int getType(short data) {
        return (data >> 6) & 0b1111111111;
    }

    public static short setBlockType(int type, short data) {
        data &= 0b111111;
        data |= (short) ((type & 0b1111111111) << 6);
        return data;
    }

    public static short setFaceVisibility(short data, Face face, boolean flag) {
        if (flag) {
            data |= (short) face.getFaceMask();
        } else {
            data &= (short) ~face.getFaceMask();
        }
        return data;
    }
}

