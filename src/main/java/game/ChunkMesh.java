package game;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import static org.lwjgl.opengl.GL15C.glGenBuffers;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL31C.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL33C.glVertexAttribDivisor;

public class ChunkMesh {

    private final Chunk chunk;

    @Getter
    private List<Integer> encodedData;

    public ChunkMesh(Chunk chunk) {
        this.chunk = chunk;
    }



}