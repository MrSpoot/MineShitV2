package core;

import org.lwjgl.opengl.GL30;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.system.MemoryUtil;

public class Mesh {
    private int vaoId;
    private int vboId;
    private int eboId;
    private int textureVboId;
    private int vertexCount;

    public Mesh(float[] vertices,float[] textureCoords, int[] indices) {
        // Créer le VAO
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        // Charger les données de sommets dans le VBO
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();
        vboId = GL30.glGenBuffers();
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vboId);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, vertexBuffer, GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 0, 0);
        GL30.glEnableVertexAttribArray(0);
        MemoryUtil.memFree(vertexBuffer);

        // Charger les coordonnées de texture dans un autre VBO
        FloatBuffer textureBuffer = MemoryUtil.memAllocFloat(textureCoords.length);
        textureBuffer.put(textureCoords).flip();
        textureVboId = GL30.glGenBuffers();
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, textureVboId);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, textureBuffer, GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(1, 2, GL30.GL_FLOAT, false, 0, 0);
        GL30.glEnableVertexAttribArray(1);
        MemoryUtil.memFree(textureBuffer);

        // Charger les indices dans l'EBO
        IntBuffer indicesBuffer = MemoryUtil.memAllocInt(indices.length);
        indicesBuffer.put(indices).flip();
        eboId = GL30.glGenBuffers();
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, eboId);
        GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL30.GL_STATIC_DRAW);
        MemoryUtil.memFree(indicesBuffer);

        vertexCount = indices.length;

        // Désactiver le VAO
        GL30.glBindVertexArray(0);
    }

    // Méthode pour rendre le mesh
    public void render() {
        GL30.glBindVertexArray(vaoId);
        GL30.glDrawElements(GL30.GL_TRIANGLES, vertexCount, GL30.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }

    // Méthode pour libérer les ressources
    public void cleanup() {
        GL30.glDisableVertexAttribArray(0);
        GL30.glDisableVertexAttribArray(1);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
        GL30.glDeleteBuffers(vboId);
        GL30.glDeleteBuffers(textureVboId);

        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL30.glDeleteBuffers(eboId);

        GL30.glBindVertexArray(0);
        GL30.glDeleteVertexArrays(vaoId);
    }
}

