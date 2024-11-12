package core;

public class CubeMesh {
    public static Mesh createCube() {
        // Positions des sommets pour un cube
        float[] vertices = {
                // Face avant
                -0.5f, -0.5f,  0.5f,
                0.5f, -0.5f,  0.5f,
                0.5f,  0.5f,  0.5f,
                -0.5f,  0.5f,  0.5f,
                // Face arrière
                -0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f,  0.5f, -0.5f,
                -0.5f,  0.5f, -0.5f
        };

        // Indices pour former les triangles du cube
        int[] indices = {
                // Face avant
                0, 1, 2, 2, 3, 0,
                // Face arrière
                4, 5, 6, 6, 7, 4,
                // Face gauche
                0, 3, 7, 7, 4, 0,
                // Face droite
                1, 2, 6, 6, 5, 1,
                // Face supérieure
                3, 2, 6, 6, 7, 3,
                // Face inférieure
                0, 1, 5, 5, 4, 0
        };

        return new Mesh(vertices, indices);
    }
}

