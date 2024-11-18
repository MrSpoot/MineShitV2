//@vs
#version 430 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;

uniform mat4 uModel;       // Matrice de transformation du modèle
uniform mat4 uView;        // Matrice de vue
uniform mat4 uProjection;  // Matrice de projection

out vec2 TexCoord;         // Coordonnées de texture envoyées au fragment shader
out vec3 Normal;

void main() {
    // Calculer la position finale du sommet
    gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0);
    TexCoord = aTexCoord;  // Passer les coordonnées de texture au fragment shader
    Normal = aNormal;
}
//@endvs

//@fs
#version 430 core

in vec2 TexCoord;          // Coordonnées de texture du vertex shader
in vec3 Normal;
out vec4 FragColor;

uniform sampler2D uTexture;  // Texture uniform

void main() {
    // Échantillonner la texture et l'utiliser comme couleur de fragment
    vec3 normal = normalize(Normal);
    vec3 lightDir = normalize(vec3(0.2, 1.0, 0.0));
    float lightIntensity = max(dot(normal, lightDir), 0.6);

    vec3 color = texture(uTexture, TexCoord).rgb;
    color *= lightIntensity;
    //color = pow(color, vec3(0.4545));

    FragColor = vec4(color,1);

    //FragColor = texture(uTexture, TexCoord);
    //FragColor = vec4(abs(Normal),1);
}
//@endfs