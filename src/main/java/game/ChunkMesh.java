package game;

import lombok.Getter;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.opengl.GL15C.glGenBuffers;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL31C.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL33C.glVertexAttribDivisor;

public class ChunkMesh {

    private int vaoId;
    private int vboId;
    private final Chunk chunk;

    @Getter
    private List<Integer> encodedData;
    private int[] data;

    public ChunkMesh(Chunk chunk) {
        this.chunk = chunk;
    }

    public void generate() {
        List<Integer> encodedData = new ArrayList<>();

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    short block = chunk.getBlock(x, y, z);

                    if (block == 0) continue;

                    for (FaceDirection faceDir : FaceDirection.values()) {
                        int neighborX = x + faceDir.getOffsetX();
                        int neighborY = y + faceDir.getOffsetY();
                        int neighborZ = z + faceDir.getOffsetZ();

                        if (shouldRenderFace(neighborX, neighborY, neighborZ, faceDir)) {
                            encodedData.add(encodeFaceData(x, y, z, faceDir));
                        }
                    }
                }
            }
        }
        this.encodedData = encodedData;
       data = encodedData.stream().mapToInt(i -> i).toArray();
    }

    public void compile() {
        // Génère un VAO
        this.vaoId = glGenVertexArrays();
        glBindVertexArray(this.vaoId);

        // VBO pour les sommets de base (aBaseVertex)
        int baseVertexVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, baseVertexVboId);

        // Exemple de données pour aBaseVertex (6 sommets pour une face carrée)
        float[] baseVertexData = {
                1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f,

                1.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f
        };
        glBufferData(GL_ARRAY_BUFFER, baseVertexData, GL_STATIC_DRAW);

        // Associe aBaseVertex (location = 0) au VBO
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        // VBO pour les données d'instance (aInstanceData)
        this.vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, this.vboId);

        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);

        // Associe aInstanceData (location = 1) au VBO
        glVertexAttribIPointer(1, 1, GL_UNSIGNED_INT, 0, 0);
        glEnableVertexAttribArray(1);

        // Définir le divisor pour aInstanceData (une donnée par instance)
        glVertexAttribDivisor(1, 1);

        // Désactive le VAO
        glBindVertexArray(0);
    }


    public void render() {
        if (this.data.length == 0) return;

        glBindVertexArray(this.vaoId);

        glDrawArraysInstanced(GL_TRIANGLES, 0, 6, this.data.length);

        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(this.vboId);
        glDeleteVertexArrays(this.vaoId);
    }

    private boolean shouldRenderFace(int x, int y, int z, FaceDirection faceDir) {
        if (x >= 0 && x < Chunk.SIZE && y >= 0 && y < Chunk.SIZE && z >= 0 && z < Chunk.SIZE) {
            short neighbor = chunk.getBlock(x, y, z);
            return neighbor == 0;
        }

        Chunk neighborChunk = chunk.getNeighbor(faceDir);

        if (neighborChunk != null) {
            int neighborX = (x + Chunk.SIZE) % Chunk.SIZE;
            int neighborY = (y + Chunk.SIZE) % Chunk.SIZE;
            int neighborZ = (z + Chunk.SIZE) % Chunk.SIZE;

            short neighbor = neighborChunk.getBlock(neighborX, neighborY, neighborZ);
            return neighbor == 0;
        }

        return true;
    }

    private int encodeFaceData(int x, int y, int z, FaceDirection faceDir) {
        int encoded = 0;

        encoded |= (x & 0b11111); // Bits 0-4 pour X
        encoded |= (y & 0b11111) << 5; // Bits 5-9 pour Y
        encoded |= (z & 0b11111) << 10; // Bits 10-14 pour Z

        encoded |= faceDir.ordinal() << 15;

        return encoded;
    }

}