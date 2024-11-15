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
    private Vector3f position;
    public static final int CHUNK_SIZE = 32;
    private boolean[] chunk;
    private Mesh mesh;
    private List<Float> vertices;  // Stocke les données de sommets
    private List<Float> textureCoords;  // Coordonnées de texture
    private List<Integer> indices;

    public Chunk(Vector3f position) {
        this.position = position;
        this.chunk = GenerationEngine.generateChunkData(position);
        generateMesh();
    }

    @Override
    public void render() {
        Display.shader.useProgram();
        Matrix4f model = new Matrix4f().identity()
                .translate(new Vector3f(0))
                .rotateX((float) Math.toRadians(0))
                .rotateY((float) Math.toRadians(0))
                .rotateZ((float) Math.toRadians(0))
                .scale(new Vector3f(1));

        model.setTranslation(position.x * CHUNK_SIZE, position.y * CHUNK_SIZE, position.z * CHUNK_SIZE);
        Display.shader.setUniform("uModel", model);
        mesh.render();
    }

    public void cleanup() {
        if (mesh != null) {
            mesh.cleanup();
            mesh = null;
        }
        chunk = null;
        vertices = null;
        textureCoords = null;
        indices = null;
    }

    public void generateMesh() {
        List<Float> verticesList = new ArrayList<>();
        List<Float> textureCoordsList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();

        int index = 0;

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    if (!chunk[x + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE]) {
                        continue;
                    }

                    float[] textureCoords = TextureAtlasManager.getTextureCoordinate(TextureAtlas.STONE);

                    if (z == CHUNK_SIZE - 1 || !chunk[x + y * CHUNK_SIZE + (z + 1) * CHUNK_SIZE * CHUNK_SIZE]) {
                        addFace(verticesList, textureCoordsList, indicesList, x, y, z, "front", index, textureCoords);
                        index += 4;
                    }
                    if (z == 0 || !chunk[x + y * CHUNK_SIZE + (z - 1) * CHUNK_SIZE * CHUNK_SIZE]) {
                        addFace(verticesList, textureCoordsList, indicesList, x, y, z, "back", index, textureCoords);
                        index += 4;
                    }
                    if (x == 0 || !chunk[(x - 1) + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE]) {
                        addFace(verticesList, textureCoordsList, indicesList, x, y, z, "left", index, textureCoords);
                        index += 4;
                    }
                    if (x == CHUNK_SIZE - 1 || !chunk[(x + 1) + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE]) {
                        addFace(verticesList, textureCoordsList, indicesList, x, y, z, "right", index, textureCoords);
                        index += 4;
                    }
                    if (y == CHUNK_SIZE - 1 || !chunk[x + (y + 1) * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE]) {
                        addFace(verticesList, textureCoordsList, indicesList, x, y, z, "top", index, textureCoords);
                        index += 4;
                    }
                    if (y == 0 || !chunk[x + (y - 1) * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE]) {
                        addFace(verticesList, textureCoordsList, indicesList, x, y, z, "bottom", index, textureCoords);
                        index += 4;
                    }
                }
            }
        }

        this.vertices = verticesList;
        this.textureCoords = textureCoordsList;
        this.indices = indicesList;
    }

    public void createMesh() {
        if (mesh == null) {
            mesh = new Mesh(toFloatArray(vertices), toFloatArray(textureCoords),toIntArray(indices));  // Crée le Mesh avec les données
        }
    }

    private void addFace(List<Float> verticesList, List<Float> textureCoordsList, List<Integer> indicesList, int x, int y, int z, String face, int index, float[] textureCoords) {
        float[][] positions = getFacePositions(x, y, z, face);

        for (float[] pos : positions) {
            verticesList.add(pos[0]);
            verticesList.add(pos[1]);
            verticesList.add(pos[2]);
        }

        for (float coord : textureCoords) {
            textureCoordsList.add(coord);
        }

        indicesList.add(index);
        indicesList.add(index + 1);
        indicesList.add(index + 2);
        indicesList.add(index + 2);
        indicesList.add(index + 3);
        indicesList.add(index);
    }

    private float[][] getFacePositions(int x, int y, int z, String face) {
        return switch (face) {
            case "front" -> new float[][]{{x, y, z + 1}, {x, y + 1, z + 1}, {x + 1, y + 1, z + 1}, {x + 1, y, z + 1}};
            case "back" -> new float[][]{{x, y, z}, {x + 1, y, z}, {x + 1, y + 1, z}, {x, y + 1, z}};
            case "left" -> new float[][]{{x, y, z + 1}, {x, y, z}, {x, y + 1, z}, {x, y + 1, z + 1}};
            case "right" -> new float[][]{{x + 1, y, z}, {x + 1, y, z + 1}, {x + 1, y + 1, z + 1}, {x + 1, y + 1, z}};
            case "top" -> new float[][]{{x, y + 1, z}, {x + 1, y + 1, z}, {x + 1, y + 1, z + 1}, {x, y + 1, z + 1}};
            case "bottom" -> new float[][]{{x, y, z}, {x, y, z + 1}, {x + 1, y, z + 1}, {x + 1, y, z}};
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
