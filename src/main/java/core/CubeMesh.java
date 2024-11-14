package core;

public class CubeMesh {
    public static Mesh createCube() {
        float[] vertices = {
                // Face avant
                -0.5f, -0.5f,  0.5f,  // 0
                0.5f, -0.5f,  0.5f,  // 1
                0.5f,  0.5f,  0.5f,  // 2
                -0.5f,  0.5f,  0.5f,  // 3
                // Face arrière
                -0.5f, -0.5f, -0.5f,  // 4
                0.5f, -0.5f, -0.5f,  // 5
                0.5f,  0.5f, -0.5f,  // 6
                -0.5f,  0.5f, -0.5f   // 7
        };
        int[] indices = {
                // Face avant
                0, 3, 2, 2, 1, 0,
                // Face arrière
                4, 5, 6, 6, 7, 4,
                // Face gauche
                0, 4, 7, 7, 3, 0,
                // Face droite
                1, 2, 6, 6, 5, 1,
                // Face supérieure
                3, 7, 6, 6, 2, 3,
                // Face inférieure
                0, 1, 5, 5, 4, 0
        };


        return new Mesh(vertices, indices);
    }
}

