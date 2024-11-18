package core;

import core.interfaces.Updatable;
import core.manager.Input;
import game.World;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static org.joml.Math.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Camera implements Updatable  {
    @Getter
    @Setter
    private Vector3f position;
    @Getter
    @Setter
    private Quaternionf rotation;
    private float fov;
    private float aspectRatio;
    private float nearPlane;
    private float farPlane;
    private float xSens = 25f;
    private float ySens = 25f;

    private int i = 0;

    public Camera(float fov, float aspectRatio, float nearPlane, float farPlane, Loop loop) {
        this.position = new Vector3f(0, 1, 5);
        this.rotation = new Quaternionf();
        this.fov = fov;
        this.aspectRatio = aspectRatio;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;

        loop.addComponent(this);
    }

    @Override
    public void update() {

        float mouseX = Input.getXAxisRaw() * Time.delta("update") * xSens;
        float mouseY = Input.getYAxisRaw() * Time.delta("update") * ySens;

        rotate(-mouseY, -mouseX, 0);

        Vector3f velocity = new Vector3f(0f);
        Vector3f forward = new Vector3f(0, 0, -1).rotate(rotation);
        Vector3f right = new Vector3f(forward).cross(new Vector3f(0, 1, 0)).normalize();

        float cameraSpeed = 5f * Time.delta("update");

        if(Input.isPressed("sprint")){
            cameraSpeed *= 5f;
        }
        if (Input.isPressed("forward")) {
            velocity.add(forward);
        }
        if (Input.isPressed("back")) {
            velocity.sub(forward);
        }
        if (Input.isPressed("right")) {
            velocity.add(right);
        }
        if (Input.isPressed("left")) {
            velocity.sub(right);
        }

        if(Input.isPressed("display_wireframe")){
            glPolygonMode(GL_FRONT, GL_LINE);
            glPolygonMode(GL_BACK, GL_LINE);
        }

        if(Input.isPressed("display_fill")){
            glPolygonMode(GL_FRONT, GL_FILL);
            glPolygonMode(GL_BACK, GL_FILL);
        }

        velocity.mul(cameraSpeed);

        position.add(velocity);

        //World.updateChunks(new Vector3f(0));
        World.updateChunks(position);
    }

    // Méthode pour limiter une valeur entre un minimum et un maximum
    private float clamp(float value, float min, float max) {
        return max(min, min(value, max));
    }

    public void rotate(float x, float y, float z) {
        rotation.rotateX((float) Math.toRadians(x));
        rotation.rotateLocalY((float) Math.toRadians(y));
        rotation.rotateZ((float) Math.toRadians(z));
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(position,getTarget(),new Vector3f(0f,1f,0f));
    }

    public Vector3f getTarget(){
        Vector3f forward = new Vector3f(0,0,-1);
        forward.rotate(rotation);

        return new Vector3f(position).add(forward);
    }

    public Matrix4f getProjectionMatrix() {
        Matrix4f projection = new Matrix4f().perspective(
                (float) Math.toRadians(fov),
                aspectRatio,
                nearPlane,
                farPlane
        );
        return projection;
    }
}
