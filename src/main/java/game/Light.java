package game;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Light {

    private final Matrix4f lightProjection;
    private final Matrix4f lightView;
    private final Matrix4f lightSpaceMatrix;

    private final Vector3f lightDirection;
    private final Vector3f lightPosition;

    /**
     * Crée une lumière de type soleil avec une direction par défaut.
     */
    public Light() {
        this.lightProjection = new Matrix4f().ortho(-1000.0f, 1000.0f, -1000.0f, 1000.0f, 0.1f, 1000.0f);
        this.lightView = new Matrix4f();
        this.lightSpaceMatrix = new Matrix4f();

        this.lightDirection = new Vector3f(0.0f, -1.0f, 0.0f); // Direction initiale (au zénith)
        this.lightPosition = new Vector3f(); // Calculée en fonction de la direction
        updateLight();
    }

    /**
     * Définit directement la direction de la lumière.
     *
     * @param direction Direction de la lumière (doit être un vecteur normalisé).
     */
    public void setLightDirection(Vector3f direction) {
        this.lightDirection.set(direction).normalize(); // Normalise pour garantir des calculs corrects
        updateLight();
    }

    /**
     * Calcule la position fictive de la lumière et les matrices en fonction de la direction.
     */
    private void updateLight() {
        // Position fictive de la lumière (utile pour la matrice de vue)
        lightPosition.set(lightDirection).mul(-100.0f); // "Recule" la lumière selon sa direction

        // Matrice de vue : la lumière regarde le centre de la scène
        lightView.identity().lookAt(lightPosition, new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, 0.0f, -1.0f));

        // Matrice espace lumière
        lightSpaceMatrix.set(lightProjection).mul(lightView);
    }

    /**
     * Retourne la direction actuelle de la lumière.
     *
     * @return Direction de la lumière.
     */
    public Vector3f getLightDirection() {
        return new Vector3f(lightDirection);
    }

    /**
     * Retourne la matrice de projection de la lumière.
     *
     * @return Matrice de projection.
     */
    public Matrix4f getLightProjection() {
        return new Matrix4f(lightProjection);
    }

    /**
     * Retourne la matrice de vue de la lumière.
     *
     * @return Matrice de vue.
     */
    public Matrix4f getLightView() {
        return new Matrix4f(lightView);
    }

    /**
     * Retourne la matrice espace lumière (projection * vue).
     *
     * @return Matrice espace lumière.
     */
    public Matrix4f getLightSpaceMatrix() {
        return new Matrix4f(lightSpaceMatrix);
    }
}
