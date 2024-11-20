package game;

import core.Display;
import core.Mesh;
import core.interfaces.Renderable;
import game.texture.TextureAtlasManager;
import game.utils.BlockUtils;
import game.utils.Face;
import lombok.Getter;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Chunk implements Renderable {

    private final Lock lock = new ReentrantLock();

    @Getter
    private final Vector3f position;
    public static final int CHUNK_SIZE = 3;
    @Getter
    private final short[] chunkData;
    private Mesh mesh;
    private List<Float> vertices;
    private List<Float> textureCoords;
    private List<Integer> indices;
    private List<Float> normals;

    private boolean meshModified = false;

    public Chunk(Vector3f position) {
        this.position = position;
        this.chunkData = GenerationEngine.generateChunkData(position);
    }

    @Override
    public void render() {
        if (mesh == null) return;
        Display.shader.useProgram();
        Matrix4f model = new Matrix4f().identity().translate(position.x * CHUNK_SIZE, position.y * CHUNK_SIZE, position.z * CHUNK_SIZE);
        Display.shader.setUniform("uModel", model);
        mesh.render();
    }

    public void cleanup() {
        if (mesh != null) {
            mesh.cleanup();
            mesh = null;
        }
    }

    public void generateMesh(List<Chunk> neighboringChunks) {
        lock.lock();
        try {

            if (true) {
                generateMeshGreedy(neighboringChunks);
            } else {
                vertices = new ArrayList<>();
                textureCoords = new ArrayList<>();
                indices = new ArrayList<>();
                normals = new ArrayList<>();
                int index = 0;
                meshModified = true;

                for (int x = 0; x < CHUNK_SIZE; x++) {
                    for (int y = 0; y < CHUNK_SIZE; y++) {
                        for (int z = 0; z < CHUNK_SIZE; z++) {
                            short blockData = chunkData[x + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE];
                            int blockType = BlockUtils.getType(blockData);

                            if (blockType == 0) continue;

                            BlockType block = BlockType.fromIndex(blockType);

                            for (Face face : Face.values()) {
                                if (BlockUtils.isFaceVisible(blockData, face) && isFaceVisible(x, y, z, face, neighboringChunks)) {
                                    float[] textureCoordsArray = TextureAtlasManager.getTextureCoordinate(block.getTextureForFace(face));
                                    addFace(vertices, textureCoords, normals, indices, x, y, z, face, index, textureCoordsArray);
                                    index += 4;
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void generateMeshGreedy(List<Chunk> neighboringChunks) {
        lock.lock();
        try {
            vertices = new ArrayList<>();
            textureCoords = new ArrayList<>();
            indices = new ArrayList<>();
            normals = new ArrayList<>();
            meshModified = true;
            int index = 0;

            Face face = Face.TOP; // À changer pour tester d'autres faces si nécessaire

            boolean[][][] visited = new boolean[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];

            // Déterminer les axes principaux dynamiquement en fonction de la face
            int axisU, axisV, axisW;
            switch (face) {
                case TOP, BOTTOM -> {
                    axisU = 0; // X (largeur)
                    axisV = 2; // Z (hauteur)
                    axisW = 1; // Y (constant)
                }
                case FRONT, BACK -> {
                    axisU = 0; // X (largeur)
                    axisV = 1; // Y (hauteur)
                    axisW = 2; // Z (constant)
                }
                case LEFT, RIGHT -> {
                    axisU = 2; // Z (largeur)
                    axisV = 1; // Y (hauteur)
                    axisW = 0; // X (constant)
                }
                default -> throw new IllegalStateException("Unexpected face: " + face);
            }

            // Parcours du chunk
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int y = 0; y < CHUNK_SIZE; y++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        if (visited[x][y][z]) {
                            System.out.printf("Skipping block (%d, %d, %d), already visited.%n", x, y, z);
                            continue;
                        }

                        short blockData = chunkData[x + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE];
                        int blockType = BlockUtils.getType(blockData);

                        if (blockType == 0 || !BlockUtils.isFaceVisible(blockData, face) || !isFaceVisible(x, y, z, face, neighboringChunks)) {
                            continue;
                        }

                        System.out.printf("Starting calculations for block (%d, %d, %d).%n", x, y, z);

                        int width = 0, height = 0;

                        // Calculer la largeur (le long de axisU)
                        for (int u = 0; u < CHUNK_SIZE; u++) {
                            int ux = (axisU == 0) ? x + u : x;
                            int uy = (axisU == 1) ? y + u : y;
                            int uz = (axisU == 2) ? z + u : z;

                            if (ux >= CHUNK_SIZE || uy >= CHUNK_SIZE || uz >= CHUNK_SIZE || visited[ux][uy][uz]) {
                                break;
                            }

                            short _blockData = chunkData[ux + uy * CHUNK_SIZE + uz * CHUNK_SIZE * CHUNK_SIZE];
                            int _blockType = BlockUtils.getType(_blockData);

                            if (_blockType == blockType && BlockUtils.isFaceVisible(_blockData, face) && isFaceVisible(ux, uy, uz, face, neighboringChunks)) {
                                width++;
                            } else {
                                break;
                            }
                        }
                        System.out.printf("Calculated width=%d for block (%d, %d, %d).%n", width, x, y, z);

                        // Calculer la hauteur (le long de axisV)
                        for (int v = 0; v < CHUNK_SIZE; v++) {
                            boolean isRowValid = true;
                            for (int u = 0; u < width; u++) {
                                int ux = (axisU == 0) ? x + u : x;
                                int uy = (axisV == 1) ? y + v : y;
                                int uz = (axisW == 2) ? z + u : z;

                                if (ux >= CHUNK_SIZE || uy >= CHUNK_SIZE || uz >= CHUNK_SIZE || visited[ux][uy][uz]) {
                                    isRowValid = false;
                                    break;
                                }

                                short _blockData = chunkData[ux + uy * CHUNK_SIZE + uz * CHUNK_SIZE * CHUNK_SIZE];
                                int _blockType = BlockUtils.getType(_blockData);

                                if (_blockType != blockType || !BlockUtils.isFaceVisible(_blockData, face) || !isFaceVisible(ux, uy, uz, face, neighboringChunks)) {
                                    isRowValid = false;
                                    break;
                                }
                            }
                            if (isRowValid) {
                                height++;
                            } else {
                                break;
                            }
                        }
                        System.out.printf("Calculated height=%d for block (%d, %d, %d).%n", height, x, y, z);

                        // Marquer les blocs visités
                        for (int v = 0; v < height; v++) {
                            for (int u = 0; u < width; u++) {
                                int ux = (axisU == 0) ? x + u : x;
                                int uy = (axisV == 1) ? y + v : y;
                                int uz = (axisW == 2) ? z : z + u;

                                if (ux < CHUNK_SIZE && uy < CHUNK_SIZE && uz < CHUNK_SIZE) {
                                    visited[ux][uy][uz] = true;
                                    System.out.printf("Marking block (%d, %d, %d) as visited.%n", ux, uy, uz);
                                }
                            }
                        }

                        System.out.printf("Adding face for block (%d, %d, %d): width=%d, height=%d.%n", x, y, z, width, height);

                        // Ajouter la face fusionnée
                        float[] textureCoordsArray = TextureAtlasManager.getTextureCoordinate(BlockType.fromIndex(blockType).getTextureForFace(face));
                        addFace(vertices, textureCoords, normals, indices, x, y, z, face, index, textureCoordsArray, width, height);
                        index += 4;
                    }
                }
            }

        } finally {
            lock.unlock();
        }
    }


    private void addFace(List<Float> verticesList, List<Float> textureCoordsList, List<Float> normalsList,
                         List<Integer> indicesList, int x, int y, int z, Face face, int index,
                         float[] textureCoords, int width, int height) {
        float[][] positions = getFacePositions(x, y, z, face, width, height);
        float[] normal = getNormalForFace(face);

        for (int i = 0; i < 4; i++) {
            // Position
            verticesList.add(positions[i][0]);
            verticesList.add(positions[i][1]);
            verticesList.add(positions[i][2]);
            // Normale
            normalsList.add(normal[0]);
            normalsList.add(normal[1]);
            normalsList.add(normal[2]);
        }

        for (float coord : textureCoords) {
            textureCoordsList.add(coord);
        }

        // Indices
        indicesList.add(index);
        indicesList.add(index + 1);
        indicesList.add(index + 2);
        indicesList.add(index + 2);
        indicesList.add(index + 3);
        indicesList.add(index);
    }


    public void updateFaceVisibility(List<Chunk> neighboringChunks) {
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    short blockData = chunkData[x + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE];
                    int blockType = BlockUtils.getType(blockData);

                    if (blockType == 0) continue;

                    for (Face face : Face.values()) {
                        boolean visible = isFaceVisible(x, y, z, face, neighboringChunks);
                        BlockUtils.setFaceVisibility(blockData, face, visible);
                    }

                    chunkData[x + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE] = blockData;
                    meshModified = true;
                }
            }
        }
    }

    public void createMesh() {
        lock.lock();
        try {
            if ((mesh == null || meshModified) && vertices != null && textureCoords != null && indices != null && normals != null) {
                meshModified = false;
                mesh = new Mesh(toFloatArray(vertices), toFloatArray(normals), toFloatArray(textureCoords), toIntArray(indices));
            }
        } finally {
            lock.unlock();
        }

    }

    private boolean isFaceVisible(int x, int y, int z, Face face, List<Chunk> neighboringChunks) {
        int adjacentX = x;
        int adjacentY = y;
        int adjacentZ = z;

        switch (face) {
            case FRONT -> adjacentZ += 1;
            case BACK -> adjacentZ -= 1;
            case LEFT -> adjacentX += 1;
            case RIGHT -> adjacentX -= 1;
            case TOP -> adjacentY += 1;
            case BOTTOM -> adjacentY -= 1;
        }

        if (adjacentX >= 0 && adjacentX < CHUNK_SIZE &&
                adjacentY >= 0 && adjacentY < CHUNK_SIZE &&
                adjacentZ >= 0 && adjacentZ < CHUNK_SIZE) {
            short neighborData = chunkData[adjacentX + adjacentY * CHUNK_SIZE + adjacentZ * CHUNK_SIZE * CHUNK_SIZE];
            return BlockUtils.getType(neighborData) == 0;
        }

        Vector3f offset = new Vector3f();
        if (adjacentX < 0) offset.x = -1;
        if (adjacentX >= CHUNK_SIZE) offset.x = 1;
        if (adjacentY < 0) offset.y = -1;
        if (adjacentY >= CHUNK_SIZE) offset.y = 1;
        if (adjacentZ < 0) offset.z = -1;
        if (adjacentZ >= CHUNK_SIZE) offset.z = 1;

        for (Chunk neighbor : neighboringChunks) {
            if (neighbor.getPosition().equals(new Vector3f(position).add(offset))) {
                int neighborX = (adjacentX + CHUNK_SIZE) % CHUNK_SIZE;
                int neighborY = (adjacentY + CHUNK_SIZE) % CHUNK_SIZE;
                int neighborZ = (adjacentZ + CHUNK_SIZE) % CHUNK_SIZE;

                short neighborBlockData = neighbor.getChunkData()[neighborX + neighborY * CHUNK_SIZE + neighborZ * CHUNK_SIZE * CHUNK_SIZE];
                return BlockUtils.getType(neighborBlockData) == 0;
            }
        }
        return true;
    }

    private void addFace(List<Float> verticesList, List<Float> textureCoordsList, List<Float> normalsList, List<Integer> indicesList, int x, int y, int z, Face face, int index, float[] textureCoords) {
        float[][] positions = getFacePositions(x, y, z, face);
        float[] normal = getNormalForFace(face);

        for (int i = 0; i < 4; i++) {
            // Position
            verticesList.add(positions[i][0]);
            verticesList.add(positions[i][1]);
            verticesList.add(positions[i][2]);
            // Normale
            normalsList.add(normal[0]);
            normalsList.add(normal[1]);
            normalsList.add(normal[2]);
        }

        for (float coord : textureCoords) {
            textureCoordsList.add(coord);
        }

        // Indices
        indicesList.add(index);
        indicesList.add(index + 1);
        indicesList.add(index + 2);
        indicesList.add(index + 2);
        indicesList.add(index + 3);
        indicesList.add(index);
    }

    private float[][] getFacePositions(int x, int y, int z, Face face) {
        return switch (face) {
            case FRONT -> new float[][]{{x, y, z + 1}, {x + 1, y, z + 1}, {x + 1, y + 1, z + 1}, {x, y + 1, z + 1}};
            case BACK -> new float[][]{{x + 1, y, z}, {x, y, z}, {x, y + 1, z}, {x + 1, y + 1, z}};
            case RIGHT -> new float[][]{{x, y, z}, {x, y, z + 1}, {x, y + 1, z + 1}, {x, y + 1, z}};
            case LEFT -> new float[][]{{x + 1, y, z + 1}, {x + 1, y, z}, {x + 1, y + 1, z}, {x + 1, y + 1, z + 1}};
            case TOP -> new float[][]{{x, y + 1, z + 1}, {x + 1, y + 1, z + 1}, {x + 1, y + 1, z}, {x, y + 1, z}};
            case BOTTOM -> new float[][]{{x, y, z}, {x + 1, y, z}, {x + 1, y, z + 1}, {x, y, z + 1}};
        };
    }

    private float[][] getFacePositions(int x, int y, int z, Face face, int width, int height) {
        return switch (face) {
            case FRONT -> new float[][]{
                    {x, y, z + 1},
                    {x + width, y, z + 1},
                    {x + width, y + height, z + 1},
                    {x, y + height, z + 1}
            };
            case BACK -> new float[][]{
                    {x + width, y, z},
                    {x, y, z},
                    {x, y + height, z},
                    {x + width, y + height, z}
            };
            case RIGHT -> new float[][]{
                    {x + 1, y, z},
                    {x + 1, y, z + height},
                    {x + 1, y + width, z + height},
                    {x + 1, y + width, z}
            };
            case LEFT -> new float[][]{
                    {x + 1, y, z + height},
                    {x + 1, y, z},
                    {x + 1, y + width, z},
                    {x + 1, y + width, z + height}
            };
            case TOP -> new float[][]{
                    {x, y + 1, z + height},
                    {x + width, y + 1, z + height},
                    {x + width, y + 1, z},
                    {x, y + 1, z}
            };
            case BOTTOM -> new float[][]{
                    {x, y, z},
                    {x + width, y, z},
                    {x + width, y, z + height},
                    {x, y, z + height}
            };
        };
    }


    private float[] getNormalForFace(Face face) {
        return switch (face) {
            case FRONT -> new float[]{0, 0, 1};
            case BACK -> new float[]{0, 0, -1};
            case RIGHT -> new float[]{1, 0, 0};
            case LEFT -> new float[]{-1, 0, 0};
            case TOP -> new float[]{0, 1, 0};
            case BOTTOM -> new float[]{0, -1, 0};
        };
    }

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
