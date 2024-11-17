package game;

import core.Display;
import core.Mesh;
import core.interfaces.Renderable;
import game.texture.TextureAtlas;
import game.texture.TextureAtlasManager;
import lombok.Getter;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Chunk implements Renderable {

    @Getter
    private final Vector3f position;
    public static final int CHUNK_SIZE = 4;
    @Getter
    private final int[] chunkData;
    private Mesh mesh;
    private List<Float> vertices;
    private List<Float> textureCoords;
    private List<Integer> indices;

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
        List<Float> verticesList = new ArrayList<>();
        List<Float> textureCoordsList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();

        int index = 0;

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    if (chunkData[x + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE] == 0) continue;

                    BlockType blockType = BlockType.fromIndex(chunkData[x + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE]);

                    if (isFaceVisible(x, y, z, "front", neighboringChunks)) {
                        float[] textureCoords = TextureAtlasManager.getTextureCoordinate(blockType.getTextureForFace("front"));
                        addFace(verticesList, textureCoordsList, indicesList, x, y, z, "front", index, textureCoords);
                        index += 4;
                    }
                    if (isFaceVisible(x, y, z, "back", neighboringChunks)) {
                        float[] textureCoords = TextureAtlasManager.getTextureCoordinate(blockType.getTextureForFace("back"));
                        addFace(verticesList, textureCoordsList, indicesList, x, y, z, "back", index, textureCoords);
                        index += 4;
                    }
                    if (isFaceVisible(x, y, z, "left", neighboringChunks)) {
                        float[] textureCoords = TextureAtlasManager.getTextureCoordinate(blockType.getTextureForFace("left"));
                        addFace(verticesList, textureCoordsList, indicesList, x, y, z, "left", index, textureCoords);
                        index += 4;
                    }
                    if (isFaceVisible(x, y, z, "right", neighboringChunks)) {
                        float[] textureCoords = TextureAtlasManager.getTextureCoordinate(blockType.getTextureForFace("right"));
                        addFace(verticesList, textureCoordsList, indicesList, x, y, z, "right", index, textureCoords);
                        index += 4;
                    }
                    if (isFaceVisible(x, y, z, "top", neighboringChunks)) {
                        float[] textureCoords = TextureAtlasManager.getTextureCoordinate(blockType.getTextureForFace("top"));
                        addFace(verticesList, textureCoordsList, indicesList, x, y, z, "top", index, textureCoords);
                        index += 4;
                    }
                    if (isFaceVisible(x, y, z, "bottom", neighboringChunks)) {
                        float[] textureCoords = TextureAtlasManager.getTextureCoordinate(blockType.getTextureForFace("bottom"));
                        addFace(verticesList, textureCoordsList, indicesList, x, y, z, "bottom", index, textureCoords);
                        index += 4;
                    }
                }
            }
        }

        vertices = verticesList;
        textureCoords = textureCoordsList;
        indices = indicesList;
    }

    public void createMesh() {
        if (mesh == null && vertices != null && textureCoords != null && indices != null && !vertices.isEmpty() && !textureCoords.isEmpty() && !indices.isEmpty()) {
            mesh = new Mesh(toFloatArray(vertices),toFloatArray(textureCoords), toIntArray(indices));
        }
    }

    private boolean isFaceVisible(int x, int y, int z, String face, List<Chunk> neighboringChunks) {
        int adjacentX = x;
        int adjacentY = y;
        int adjacentZ = z;

        switch (face) {
            case "front":
                adjacentZ += 1;
                break;
            case "back":
                adjacentZ -= 1;
                break;
            case "left":
                adjacentX += 1;
                break;
            case "right":
                adjacentX -= 1;
                break;
            case "top":
                adjacentY += 1;
                break;
            case "bottom":
                adjacentY -= 1;
                break;
        }

        if (adjacentX >= 0 && adjacentX < CHUNK_SIZE &&
                adjacentY >= 0 && adjacentY < CHUNK_SIZE &&
                adjacentZ >= 0 && adjacentZ < CHUNK_SIZE) {
            return chunkData[adjacentX + adjacentY * CHUNK_SIZE + adjacentZ * CHUNK_SIZE * CHUNK_SIZE] == 0;
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

                if (neighbor.getChunkData()[neighborX + neighborY * CHUNK_SIZE + neighborZ * CHUNK_SIZE * CHUNK_SIZE] != 0) {
                    neighbor.removeFaceUsingVertices(neighborX, neighborY, neighborZ, getOppositeFace(face));
                    return false;
                }
            }
        }
        return true;
    }

    public void removeFaceUsingVertices(int x, int y, int z, String face) {
        if (vertices == null || indices == null) {
            return;
        }

        float[][] facePositions = getFacePositions(x, y, z, face);

        List<Integer> indicesToRemove = new ArrayList<>();
        for (int i = 0; i < indices.size(); i += 3) {
            boolean match = true;
            for (int j = 0; j < 3; j++) {
                int index = indices.get(i + j);
                float vx = vertices.get(index * 3);
                float vy = vertices.get(index * 3 + 1);
                float vz = vertices.get(index * 3 + 2);

                boolean vertexMatch = false;
                for (float[] pos : facePositions) {
                    if (vx == pos[0] && vy == pos[1] && vz == pos[2]) {
                        vertexMatch = true;
                        break;
                    }
                }
                if (!vertexMatch) {
                    match = false;
                    break;
                }
            }
            if (match) {
                indicesToRemove.add(i);
                indicesToRemove.add(i + 1);
                indicesToRemove.add(i + 2);
            }
        }

        indicesToRemove.sort((a, b) -> b - a);
        for (int index : indicesToRemove) {
            indices.remove(index);
        }
    }


    private String getOppositeFace(String face) {
        return switch (face) {
            case "front" -> "back";
            case "back" -> "front";
            case "left" -> "right";
            case "right" -> "left";
            case "top" -> "bottom";
            case "bottom" -> "top";
            default -> ""; // Face invalide
        };
    }

    public void removeSharedFacesWith(Chunk neighbor) {
        Vector3f neighborPosition = neighbor.getPosition();

        int offsetX = (int) (neighborPosition.x - this.position.x);
        int offsetY = (int) (neighborPosition.y - this.position.y);
        int offsetZ = (int) (neighborPosition.z - this.position.z);

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    if (chunkData[x + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE] == 0) continue;

                    if (offsetX == 1 && x == CHUNK_SIZE - 1) { // Voisin à droite
                        if (neighbor.getChunkData()[0 + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE] != 0) {
                            removeFaceUsingVertices(x, y, z, "right");
                            neighbor.removeFaceUsingVertices(0, y, z, "left");
                        }
                    } else if (offsetX == -1 && x == 0) { // Voisin à gauche
                        if (neighbor.getChunkData()[(CHUNK_SIZE - 1) + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE] != 0) {
                            removeFaceUsingVertices(x, y, z, "left");
                            neighbor.removeFaceUsingVertices(CHUNK_SIZE - 1, y, z, "right");
                        }
                    }
                    if (offsetY == 1 && y == CHUNK_SIZE - 1) { // Voisin au-dessus
                        if (neighbor.getChunkData()[x + 0 * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE] != 0) {
                            removeFaceUsingVertices(x, y, z, "top");
                            neighbor.removeFaceUsingVertices(x, 0, z, "bottom");
                        }
                    } else if (offsetY == -1 && y == 0) { // Voisin en dessous
                        if (neighbor.getChunkData()[x + (CHUNK_SIZE - 1) * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE] != 0) {
                            removeFaceUsingVertices(x, y, z, "bottom");
                            neighbor.removeFaceUsingVertices(x, CHUNK_SIZE - 1, z, "top");
                        }
                    }
                    if (offsetZ == 1 && z == CHUNK_SIZE - 1) { // Voisin devant
                        if (neighbor.getChunkData()[x + y * CHUNK_SIZE + 0 * CHUNK_SIZE * CHUNK_SIZE] != 0) {
                            removeFaceUsingVertices(x, y, z, "front");
                            neighbor.removeFaceUsingVertices(x, y, 0, "back");
                        }
                    } else if (offsetZ == -1 && z == 0) { // Voisin derrière
                        if (neighbor.getChunkData()[x + y * CHUNK_SIZE + (CHUNK_SIZE - 1) * CHUNK_SIZE * CHUNK_SIZE] != 0) {
                            removeFaceUsingVertices(x, y, z, "back");
                            neighbor.removeFaceUsingVertices(x, y, CHUNK_SIZE - 1, "front");
                        }
                    }
                }
            }
        }
    }

    private void addFace(List<Float> verticesList, List<Float> textureCoordsList, List<Integer> indicesList, int x, int y, int z, String face, int index, float[] textureCoords) {
        float[][] positions = getFacePositions(x, y, z, face);
        for (float[] pos : positions) {
            verticesList.add(pos[0]);
            verticesList.add(pos[1]);
            verticesList.add(pos[2]);
        }
        for (float coord : textureCoords) textureCoordsList.add(coord);
        indicesList.add(index);
        indicesList.add(index + 1);
        indicesList.add(index + 2);
        indicesList.add(index + 2);
        indicesList.add(index + 3);
        indicesList.add(index);
    }

    private float[][] getFacePositions(int x, int y, int z, String face) {
        return switch (face) {
            case "front" -> new float[][]{
                    {x, y, z + 1}, {x + 1, y, z + 1}, {x + 1, y + 1, z + 1}, {x, y + 1, z + 1}
            };
            case "back" -> new float[][]{
                    {x + 1, y, z}, {x, y, z}, {x, y + 1, z}, {x + 1, y + 1, z}
            };
            case "right" -> new float[][]{
                    {x, y, z}, {x, y, z + 1}, {x, y + 1, z + 1}, {x, y + 1, z}
            };
            case "left" -> new float[][]{
                    {x + 1, y, z + 1}, {x + 1, y, z}, {x + 1, y + 1, z}, {x + 1, y + 1, z + 1}
            };
            case "top" -> new float[][]{
                    {x, y + 1, z + 1}, {x + 1, y + 1, z + 1}, {x + 1, y + 1, z}, {x, y + 1, z}
            };
            case "bottom" -> new float[][]{
                    {x, y, z}, {x + 1, y, z}, {x + 1, y, z + 1}, {x, y, z + 1}
            };
            default -> new float[0][0];
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
