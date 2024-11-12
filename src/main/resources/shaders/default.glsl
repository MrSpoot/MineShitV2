//@vs
#version 430

attribute vec3 aPosition;  // Position du sommet
attribute vec3 aColor;     // Couleur du sommet

uniform mat4 uModel;       // Matrice de transformation du modèle
uniform mat4 uView;        // Matrice de vue
uniform mat4 uProjection;  // Matrice de projection

out vec3 vColor;       // Couleur interpolée envoyée au fragment shader

void main() {
    // Calculer la position finale du sommet
    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
    // Passer la couleur au fragment shader
    vColor = aColor;
}
//@endvs

//@fs
#version 430

in vec3 vColor;  // Couleur interpolée reçue du vertex shader

void main() {
    // Définir la couleur du fragment
    gl_FragColor = vec4(vColor, 1.0);
}
//@endfs