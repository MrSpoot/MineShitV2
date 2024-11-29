package game;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL30C.*;

public class Mesh {

    private int vaoId;
    private int vboId;
    private int eboId;
    private int vertexCount;

    private int[] data;
    private int[] indices;

    private boolean isCompiled;

    public Mesh(int[] positions, int[] normals, int[] indices) {
        this.isCompiled = false;

        this.indices = indices;
        this.vertexCount = indices.length;

        int vertexCount = positions.length / 3;
        data = new int[vertexCount];

        for (int i = 0; i < vertexCount; i++) {
            int x = positions[i * 3];
            int y = positions[i * 3 + 1];
            int z = positions[i * 3 + 2];

            int normal = normals[i];

            // Encoder les données dans un entier
            int encodedVertex = 0;
            encodedVertex |= (x & 0b111111);         // Ajouter X (6 bits)
            encodedVertex |= (y & 0b111111) << 6;   // Ajouter Y (6 bits) décalés de 6 bits
            encodedVertex |= (z & 0b111111) << 12;  // Ajouter Z (6 bits) décalés de 12 bits
            encodedVertex |= (normal & 0b111) << 18; // Ajouter la normale (3 bits) décalés de 18 bits

            data[i] = encodedVertex;
        }
    }

    public void compile() {
        if(isCompiled) return;
        this.vaoId = glGenVertexArrays();
        glBindVertexArray(this.vaoId);

        this.vboId = glGenBuffers();
        IntBuffer vertexBuffer = null;

        try {
            vertexBuffer = MemoryUtil.memAllocInt(data.length);
            vertexBuffer.put(data).flip();

            //System.out.println("Buffer size is " + vertexBuffer.capacity() * Integer.BYTES+ " bytes");

            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

            // Positions (location = 0)
            glVertexAttribIPointer(0, 1, GL_UNSIGNED_INT, 0, 0);
            glEnableVertexAttribArray(0);

        } finally {
            if (vertexBuffer != null) {
                MemoryUtil.memFree(vertexBuffer);
            }
        }

        // EBO
        eboId = glGenBuffers();
        IntBuffer indicesBuffer = null;
        try {
            indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
        } finally {
            if (indicesBuffer != null) {
                MemoryUtil.memFree(indicesBuffer);
            }
        }
        glBindVertexArray(0);

        this.data = null;
        this.indices = null;

        this.isCompiled = true;
    }

    public void render() {
        if(isCompiled) {
            glBindVertexArray(vaoId);
            glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);
        }
    }

    public void cleanup(){
        if(isCompiled) {
            glDisableVertexAttribArray(0);
            glDisableVertexAttribArray(1);
            glDisableVertexAttribArray(2);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            glDeleteBuffers(vboId);
            glDeleteBuffers(eboId);
            glBindVertexArray(0);
            glDeleteVertexArrays(vaoId);
            isCompiled = false;
        }
    }

}
