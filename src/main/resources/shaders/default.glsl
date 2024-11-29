//@vs
#version 430 core

layout(location = 0) in uint aVertexData; // Données compressées

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProjection;

out vec3 Normal; // Normale pour le fragment shader
out vec3 FragPos; // Position pour le fragment shader

// Décompression des données de sommet
vec3 decodePosition(uint encodedVertex) {
    float x = float(encodedVertex & 63u);               // Extraire X (6 bits)
    float y = float((encodedVertex >> 6u) & 63u);      // Extraire Y (6 bits)
    float z = float((encodedVertex >> 12u) & 63u);     // Extraire Z (6 bits)
    return vec3(x, y, z);
}

vec3 decodeNormal(uint encodedVertex) {
    uint normal = (encodedVertex >> 18u) & 7u;         // Extraire la normale (3 bits)
    if (normal == 0u) return vec3(0.0, 0.0, 1.0);      // FRONT
    if (normal == 1u) return vec3(0.0, 0.0, -1.0);     // BACK
    if (normal == 2u) return vec3(1.0, 0.0, 0.0);      // RIGHT
    if (normal == 3u) return vec3(-1.0, 0.0, 0.0);     // LEFT
    if (normal == 4u) return vec3(0.0, 1.0, 0.0);      // TOP
    if (normal == 5u) return vec3(0.0, -1.0, 0.0);     // BOTTOM
    return vec3(0.0, 0.0, 0.0); // Normale par défaut (ne devrait pas arriver)
}

void main() {
    vec3 localPos = decodePosition(aVertexData); // Décode la position locale
    vec3 normal = decodeNormal(aVertexData);     // Décode la normale

    mat3 normalMatrix = transpose(inverse(mat3(uModel)));
    Normal = normalize(normalMatrix * normal);   // Transformation de la normale dans l'espace monde

    FragPos = localPos; // Position dans l'espace monde
    gl_Position = uProjection * uView * uModel * vec4(localPos, 1.0); // Position finale
}
//@endvs

//@fs
#version 430 core

in vec3 Normal; // Normale venant du vertex shader
in vec3 FragPos; // Position du fragment dans l'espace monde

out vec4 FragColor;

void main() {
    // Calcul de l'éclairage
    vec3 normal = normalize(Normal);
    vec3 lightDir = normalize(vec3(0.0, 1.0, 0.0)); // Direction de la lumière
    float lightIntensity = max(dot(normal, lightDir), 0.5);

    // Utiliser la position du fragment pour générer une couleur (dépend de ton style)
    //vec3 color = vec3(FragPos.x / 32.0, FragPos.y / 32.0, FragPos.z / 32.0) * lightIntensity;
    vec3 color = abs(normal);

    FragColor = vec4(color, 1.0);
}
//@endfs