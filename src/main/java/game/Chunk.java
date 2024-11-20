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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Chunk implements Renderable {

    private final Lock lock = new ReentrantLock();

    @Getter
    private final Vector3f position;
    public static final int CHUNK_SIZE = 32;
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

    public int generateGreedyMeshForTopAndBot(List<Chunk> neighboringChunks, Face face, int index) {
        boolean[][][] visited = new boolean[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];
        int _index = index;
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {

                    short blockData = chunkData[x + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE];
                    int blockType = BlockUtils.getType(blockData);

                    if (blockType == 0) continue;

                    if(visited[x][y][z]) continue;

                    if (BlockUtils.isFaceVisible(blockData, face) && isFaceVisible(x, y, z, face, neighboringChunks)) {

                        int width = 0;
                        int height = 0;

                        for (int j = z; j < CHUNK_SIZE; j++) {
                            short _blockData = chunkData[x + y * CHUNK_SIZE + j * CHUNK_SIZE * CHUNK_SIZE];
                            int _blockType = BlockUtils.getType(_blockData);

                            if (_blockType == blockType && BlockUtils.isFaceVisible(_blockData, face) && isFaceVisible(x, y, j, face, neighboringChunks) && !visited[x][y][j]) {
                                width++;
                            } else {
                                break;
                            }
                        }

                        for (int i = x; i < CHUNK_SIZE; i++) {
                            boolean isRowValid = true;
                            for (int j = z; j < z + width; j++) {
                                short _blockData = chunkData[i + y * CHUNK_SIZE + j * CHUNK_SIZE * CHUNK_SIZE];
                                int _blockType = BlockUtils.getType(_blockData);

                                if (_blockType != blockType || !BlockUtils.isFaceVisible(_blockData, face) || !isFaceVisible(i, y, j, face, neighboringChunks) || visited[i][y][j]) {
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

                        for (int dx = 0; dx < height; dx++) {
                            for (int dz = 0; dz < width; dz++) {
                                visited[x + dx][y][z + dz] = true;
                            }
                        }

                        // Ajouter la face fusionnée
                        float[] textureCoordsArray = TextureAtlasManager.getTextureCoordinate(BlockType.fromIndex(blockType).getTextureForFace(face));
                        addFace(vertices, textureCoords, normals, indices, x, y, z, face, _index, textureCoordsArray, height, width);
                        _index += 4;
                    }
                }


            }
        }
        return _index;
    }

    public int generateGreedyMeshForFrontAndBack(List<Chunk> neighboringChunks, Face face, int index) {
        boolean[][][] visited = new boolean[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];
        int _index = index;

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {

                    short blockData = chunkData[x + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE];
                    int blockType = BlockUtils.getType(blockData);

                    if (blockType == 0 || visited[x][y][z]) continue;

                    if (BlockUtils.isFaceVisible(blockData, face) && isFaceVisible(x, y, z, face, neighboringChunks)) {

                        int width = 0;
                        int height = 0;

                        // Largeur (le long de l'axe X)
                        for (int dx = x; dx < CHUNK_SIZE; dx++) {
                            short _blockData = chunkData[dx + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE];
                            int _blockType = BlockUtils.getType(_blockData);

                            if (_blockType == blockType && BlockUtils.isFaceVisible(_blockData, face) && isFaceVisible(dx, y, z, face, neighboringChunks) && !visited[dx][y][z]) {
                                width++;
                            } else {
                                break;
                            }
                        }

                        // Hauteur (le long de l'axe Y)
                        for (int dy = y; dy < CHUNK_SIZE; dy++) {
                            boolean isRowValid = true;
                            for (int dx = x; dx < x + width; dx++) {
                                short _blockData = chunkData[dx + dy * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE];
                                int _blockType = BlockUtils.getType(_blockData);

                                if (_blockType != blockType || !BlockUtils.isFaceVisible(_blockData, face) || !isFaceVisible(dx, dy, z, face, neighboringChunks) ||visited[dx][dy][z]) {
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

                        for (int dy = 0; dy < height; dy++) {
                            for (int dx = 0; dx < width; dx++) {
                                visited[x + dx][y + dy][z] = true;
                            }
                        }

                        float[] textureCoordsArray = TextureAtlasManager.getTextureCoordinate(BlockType.fromIndex(blockType).getTextureForFace(face));
                        addFace(vertices, textureCoords, normals, indices, x, y, z, face, _index, textureCoordsArray,width, height  );
                        _index += 4;
                    }
                }
            }
        }
        return _index;
    }

    public int generateGreedyMeshForLeftAndRight(List<Chunk> neighboringChunks, Face face, int index) {
        boolean[][][] visited = new boolean[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];
        int _index = index;

        for (int z = 0; z < CHUNK_SIZE; z++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int x = 0; x < CHUNK_SIZE; x++) {

                    short blockData = chunkData[x + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE];
                    int blockType = BlockUtils.getType(blockData);

                    if (blockType == 0 || visited[x][y][z]) continue;

                    if (BlockUtils.isFaceVisible(blockData, face) && isFaceVisible(x, y, z, face, neighboringChunks)) {

                        int width = 0, height = 0;

                        // Largeur (le long de l'axe Z)
                        for (int dz = z; dz < CHUNK_SIZE; dz++) {
                            short _blockData = chunkData[x + y * CHUNK_SIZE + dz * CHUNK_SIZE * CHUNK_SIZE];
                            int _blockType = BlockUtils.getType(_blockData);

                            if (_blockType == blockType && BlockUtils.isFaceVisible(_blockData, face) && isFaceVisible(x, y, dz, face, neighboringChunks) && !visited[x][y][dz]) {
                                width++;
                            } else {
                                break;
                            }
                        }

                        // Hauteur (le long de l'axe Y)
                        for (int dy = y; dy < CHUNK_SIZE; dy++) {
                            boolean isRowValid = true;
                            for (int dz = z; dz < z + width; dz++) {
                                short _blockData = chunkData[x + dy * CHUNK_SIZE + dz * CHUNK_SIZE * CHUNK_SIZE];
                                int _blockType = BlockUtils.getType(_blockData);

                                if (_blockType != blockType || !BlockUtils.isFaceVisible(_blockData, face) || !isFaceVisible(x, dy, dz, face, neighboringChunks) || visited[x][dy][dz]) {
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

                        for (int dy = 0; dy < height; dy++) {
                            for (int dz = 0; dz < width; dz++) {
                                visited[x][y + dy][z + dz] = true;
                            }
                        }

                        float[] textureCoordsArray = TextureAtlasManager.getTextureCoordinate(BlockType.fromIndex(blockType).getTextureForFace(face));
                        addFace(vertices, textureCoords, normals, indices, x, y, z, face, _index, textureCoordsArray, height, width);
                        _index += 4;
                    }
                }
            }
        }
        return _index;
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



            index = generateGreedyMeshForTopAndBot(neighboringChunks, Face.TOP, index);
            index = generateGreedyMeshForTopAndBot(neighboringChunks, Face.BOTTOM, index);
            index = generateGreedyMeshForFrontAndBack(neighboringChunks, Face.BACK, index);
            index = generateGreedyMeshForFrontAndBack(neighboringChunks, Face.FRONT, index);
            index = generateGreedyMeshForLeftAndRight(neighboringChunks, Face.LEFT, index);
            index = generateGreedyMeshForLeftAndRight(neighboringChunks, Face.RIGHT, index);


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
            case FRONT -> new float[][] {
                    {x, y, z + 1},
                    {x + width, y, z + 1},
                    {x + width, y + height, z + 1},
                    {x, y + height, z + 1}
            };
            case BACK -> new float[][] {
                    {x + width, y, z},
                    {x, y, z},
                    {x, y + height, z},
                    {x + width, y + height, z}
            };
            case RIGHT -> new float[][]{
                    {x, y, z},
                    {x, y, z + height},
                    {x, y + width, z + height},
                    {x, y + width, z}
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
