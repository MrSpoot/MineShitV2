//@vs
#version 430 core

layout(location = 0) in vec3 aPosition;  // Position du sommet
layout(location = 1) in vec2 aTexCoord;  // Coordonnées de texture

uniform mat4 uModel;       // Matrice de transformation du modèle
uniform mat4 uView;        // Matrice de vue
uniform mat4 uProjection;  // Matrice de projection

out vec2 TexCoord;         // Coordonnées de texture envoyées au fragment shader

void main() {
    // Calculer la position finale du sommet
    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
    TexCoord = aTexCoord;  // Passer les coordonnées de texture au fragment shader
}
//@endvs

//@fs
#version 430 core

in vec2 TexCoord;          // Coordonnées de texture du vertex shader
out vec4 FragColor;

uniform sampler2D uTexture;  // Texture uniform

void main() {
    // Échantillonner la texture et l'utiliser comme couleur de fragment
    FragColor = texture(uTexture, TexCoord);
}
//@endfs