//@vs
#version 430 core

layout(location = 0) in vec3 aPosition;  // Position du sommet

uniform mat4 uModel;       // Matrice de transformation du modèle
uniform mat4 uView;        // Matrice de vue
uniform mat4 uProjection;  // Matrice de projection // Couleur envoyée au fragment shader

out vec3 aColor;

void main() {
    // Calculer la position finale du sommet
    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
    aColor = aPosition;
}
//@endvs

//@fs
#version 430 core

in vec3 aColor;
out vec4 FragColor;

void main() {
    FragColor = vec4(aColor / 16,1);
}
//@endfs