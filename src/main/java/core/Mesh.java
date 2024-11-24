package core;

import lombok.Getter;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

@Getter
public class Mesh {
    private final int vaoId;
    private final int positionsVboId;
    private final int normalsVboId;
    private final int textureCoordsVboId;
    private final int indicesVboId;
    private final int vertexCount;

    float[] vertices;
    float[] normals;
    float[] textureCoords;
    int[] indices;

    public Mesh(float[] vertices, float[] normals, float[] textureCoords, int[] indices) {

        this.vertices = vertices;
        this.normals = normals;
        this.textureCoords = textureCoords;
        this.indices = indices;

        vertexCount = indices.length;

        // Créez le VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Créez le VBO pour les positions
        positionsVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, positionsVboId);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0); // Activer l'attribut position

        // Créez le VBO pour les normales
        normalsVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, normalsVboId);
        glBufferData(GL_ARRAY_BUFFER, normals, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(1); // Activer l'attribut normale

        // Créez le VBO pour les coordonnées de texture
        textureCoordsVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, textureCoordsVboId);
        glBufferData(GL_ARRAY_BUFFER, textureCoords, GL_STATIC_DRAW);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(2); // Activer l'attribut coordonnées de texture

        // Créez le VBO pour les indices
        indicesVboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesVboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // Déconnectez le VAO et le VBO
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void render() {
        if (vertexCount > 0) {
            glBindVertexArray(vaoId);
            glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);
        }
    }

    public void cleanup() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);

        // Supprimez les VBO
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(positionsVboId);
        glDeleteBuffers(normalsVboId);
        glDeleteBuffers(textureCoordsVboId);
        glDeleteBuffers(indicesVboId);

        // Supprimez le VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }
}



