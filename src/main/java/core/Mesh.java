package core;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Mesh {
    private int vaoId;
    private int positionsVboId;
    private int normalsVboId;
    private int textureCoordsVboId;
    private int indicesVboId;
    private int vertexCount;

    public Mesh(float[] positions, float[] normals, float[] textureCoords, int[] indices) {
        vertexCount = indices.length;

        // Créez le VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Créez le VBO pour les positions
        positionsVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, positionsVboId);
        glBufferData(GL_ARRAY_BUFFER, positions, GL_STATIC_DRAW);
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
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
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



