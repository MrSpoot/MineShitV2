package game;

import core.CubeMesh;
import core.Display;
import core.Mesh;
import core.interfaces.Renderable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Chunk implements Renderable {

    private Vector3f position;
    public static final int CHUNK_SIZE = 16;
    private boolean[] chunk;
    private Mesh mesh;
    private float[] vertices;  // Stocke les données de sommets
    private int[] indices;

    public Chunk(Vector3f position) {
        this.position = position;
        this.chunk = GenerationEngine.generateChunkData(position);
        generateMesh();
    }

    @Override
    public void render() {
        Display.shader.useProgram();
        Matrix4f model = new Matrix4f().identity()  // Commencer avec une matrice identité
                .translate(new Vector3f(0))  // Appliquer la translation
                .rotateX((float) Math.toRadians(0))  // Appliquer la rotation sur X
                .rotateY((float) Math.toRadians(0))  // Appliquer la rotation sur Y
                .rotateZ((float) Math.toRadians(0))  // Appliquer la rotation sur Z
                .scale(new Vector3f(1));  // Appliquer l'échelle

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
        indices = null;
    }

    public void generateMesh() {
        // Listes pour les données de sommets, couleurs et indices
        List<Float> verticesList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();

        // Indices de départ pour le mesh
        int index = 0;

        // Parcourir chaque bloc du chunk
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    // Si le bloc est vide, on passe
                    if (!chunk[x + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE]) {
                        continue;
                    }

                    // Vérifier les blocs adjacents pour déterminer les faces visibles
                    // Face avant
                    if (z == CHUNK_SIZE - 1 || !chunk[x + y * CHUNK_SIZE + (z + 1) * CHUNK_SIZE * CHUNK_SIZE]) {
                        addFace(verticesList, indicesList, x, y, z, "front", index);
                        index += 4;
                    }
                    // Face arrière
                    if (z == 0 || !chunk[x + y * CHUNK_SIZE + (z - 1) * CHUNK_SIZE * CHUNK_SIZE]) {
                        addFace(verticesList, indicesList, x, y, z, "back", index);
                        index += 4;
                    }
                    // Face gauche
                    if (x == 0 || !chunk[(x - 1) + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE]) {
                        addFace(verticesList, indicesList, x, y, z, "left", index);
                        index += 4;
                    }
                    // Face droite
                    if (x == CHUNK_SIZE - 1 || !chunk[(x + 1) + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE]) {
                        addFace(verticesList, indicesList, x, y, z, "right", index);
                        index += 4;
                    }
                    // Face supérieure
                    if (y == CHUNK_SIZE - 1 || !chunk[x + (y + 1) * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE]) {
                        addFace(verticesList, indicesList, x, y, z, "top", index);
                        index += 4;
                    }
                    // Face inférieure
                    if (y == 0 || !chunk[x + (y - 1) * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE]) {
                        addFace(verticesList, indicesList, x, y, z, "bottom", index);
                        index += 4;
                    }
                }
            }
        }

        // Convertir les listes en tableaux
        vertices = toFloatArray(verticesList);
        indices = toIntArray(indicesList);
    }

    public void createMesh() {
        if (mesh == null) {
            mesh = new Mesh(vertices, indices);  // Crée le Mesh avec les données
        }
    }

    // Méthode pour ajouter une face au mesh
    private void addFace(List<Float> verticesList, List<Integer> indicesList, int x, int y, int z, String face, int index) {
        // Positions des sommets pour chaque face
        float[][] positions = getFacePositions(x, y, z, face);

        // Ajouter les sommets de la face
        for (float[] pos : positions) {
            verticesList.add(pos[0]);
            verticesList.add(pos[1]);
            verticesList.add(pos[2]);
        }

        // Ajouter les indices pour former les deux triangles de la face
        indicesList.add(index);
        indicesList.add(index + 1);
        indicesList.add(index + 2);
        indicesList.add(index + 2);
        indicesList.add(index + 3);
        indicesList.add(index);
    }

    // Méthode pour obtenir les positions des sommets d'une face
    private float[][] getFacePositions(int x, int y, int z, String face) {
        switch (face) {
            case "front":
                return new float[][]{{x, y, z + 1}, {x, y + 1, z + 1}, {x + 1, y + 1, z + 1}, {x + 1, y, z + 1}};
            case "back":
                return new float[][]{{x, y, z}, {x + 1, y, z}, {x + 1, y + 1, z}, {x, y + 1, z}};
            case "left":
                return new float[][]{{x, y, z + 1}, {x, y, z}, {x, y + 1, z}, {x, y + 1, z + 1}};
            case "right":
                return new float[][]{{x + 1, y, z}, {x + 1, y, z + 1}, {x + 1, y + 1, z + 1}, {x + 1, y + 1, z}};
            case "top":
                return new float[][]{{x, y + 1, z}, {x + 1, y + 1, z}, {x + 1, y + 1, z + 1}, {x, y + 1, z + 1}};
            case "bottom":
                return new float[][]{{x, y, z}, {x, y, z + 1}, {x + 1, y, z + 1}, {x + 1, y, z}};
            default:
                return new float[0][0];
        }
    }


    // Méthodes utilitaires pour convertir les listes en tableaux
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
