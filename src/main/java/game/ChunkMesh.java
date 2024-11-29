package game;

import java.util.ArrayList;
import java.util.List;

public class ChunkMesh {

    private Mesh mesh;
    private Chunk chunk;

    public ChunkMesh(Chunk chunk) {
        this.chunk = chunk;
    }

    private boolean dataIsModified = false;

    public void generate() {
        dataIsModified = true;
        List<Integer> positions = new ArrayList<>();
        List<Integer> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        int vertexCount = 0;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    short block = chunk.getBlock(x, y, z);
                    if (block > 0) {
                        if (shouldRenderFace(x, y, z - 1, FaceDirection.BACK)) {
                            addFace(positions, normals, indices, x, y, z, FaceDirection.BACK, vertexCount);
                            vertexCount += 4;
                        }
                        if (shouldRenderFace(x, y, z + 1, FaceDirection.FRONT)) {
                            addFace(positions, normals, indices, x, y, z, FaceDirection.FRONT, vertexCount);
                            vertexCount += 4;
                        }
                        if (shouldRenderFace(x - 1, y, z, FaceDirection.LEFT)) {
                            addFace(positions, normals, indices, x, y, z, FaceDirection.LEFT, vertexCount);
                            vertexCount += 4;
                        }
                        if (shouldRenderFace(x + 1, y, z, FaceDirection.RIGHT)) {
                            addFace(positions, normals, indices, x, y, z, FaceDirection.RIGHT, vertexCount);
                            vertexCount += 4;
                        }
                        if (shouldRenderFace(x, y + 1, z, FaceDirection.TOP)) {
                            addFace(positions, normals, indices, x, y, z, FaceDirection.TOP, vertexCount);
                            vertexCount += 4;
                        }
                        if (shouldRenderFace(x, y - 1, z, FaceDirection.BOTTOM)) {
                            addFace(positions, normals, indices, x, y, z, FaceDirection.BOTTOM, vertexCount);
                            vertexCount += 4;
                        }

                    }
                }
            }
        }

        int[] positionsArray = positions.stream().mapToInt(i -> i).toArray();
        int[] normalsArray = normals.stream().mapToInt(i -> i).toArray();
        int[] indicesArray = indices.stream().mapToInt(i -> i).toArray();

        this.mesh = new Mesh(positionsArray, normalsArray, indicesArray);
    }


    public void compile(){
        if(mesh != null || dataIsModified){
            assert mesh != null;
            mesh.compile();
        }
        dataIsModified = false;
    }

    public void render(){
        if(mesh != null){
            System.out.println("RENDERING");
            mesh.render();
        }
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

        return false;
    }


    private void addFace(List<Integer> positions, List<Integer> normals, List<Integer> indices,
                         int x, int y, int z, FaceDirection faceDir, int vertexStartIndex) {
        int[][] offsets = getFaceOffsets(faceDir);
        int normal = faceDir.ordinal();

        for (int[] offset : offsets) {
            positions.add(x + offset[0]);
            positions.add(y + offset[1]);
            positions.add(z + offset[2]);

            normals.add(normal);
        }

        // Ajouter les indices pour deux triangles
        indices.add(vertexStartIndex);
        indices.add(vertexStartIndex + 1);
        indices.add(vertexStartIndex + 2);
        indices.add(vertexStartIndex + 2);
        indices.add(vertexStartIndex + 3);
        indices.add(vertexStartIndex);
    }

    private int[][] getFaceOffsets(FaceDirection faceDir) {
        return switch (faceDir) {
            case FRONT ->  // Face avant
                    new int[][]{
                            {0, 1, 1}, // Haut gauche
                            {0, 0, 1}, // Bas gauche
                            {1, 0, 1}, // Bas droite
                            {1, 1, 1}  // Haut droite
                    };
            case BACK ->   // Face arrière
                    new int[][]{
                            {1, 1, 0}, // Haut gauche
                            {1, 0, 0}, // Bas gauche
                            {0, 0, 0}, // Bas droite
                            {0, 1, 0}  // Haut droite
                    };
            case LEFT ->   // Face gauche
                    new int[][]{
                            {0, 1, 0}, // Haut gauche
                            {0, 0, 0}, // Bas gauche
                            {0, 0, 1}, // Bas droite
                            {0, 1, 1}  // Haut droite
                    };
            case RIGHT ->  // Face droite
                    new int[][]{
                            {1, 1, 1}, // Haut gauche
                            {1, 0, 1}, // Bas gauche
                            {1, 0, 0}, // Bas droite
                            {1, 1, 0}  // Haut droite
                    };
            case BOTTOM ->    // Face supérieure
                    new int[][]{
                            {0, 0, 1}, // Avant gauche
                            {0, 0, 0}, // Arrière gauche
                            {1, 0, 0}, // Arrière droite
                            {1, 0, 1}  // Avant droite
                    };
            case TOP -> // Face inférieure
                    new int[][]{
                            {0, 1, 0}, // Avant gauche
                            {0, 1, 1}, // Arrière gauche
                            {1, 1, 1}, // Arrière droite
                            {1, 1, 0}  // Avant droite
                    };
            default -> throw new IllegalArgumentException("Direction de face inconnue : " + faceDir);
        };
    }

}