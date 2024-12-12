//@vs
#version 460 core

layout(location = 0) in vec3 aBaseVertex; // Vertices d'une face définie dans le shader
layout(location = 1) in uint aInstanceData; // Données compressées pour l'instance

uniform mat4 uView;
uniform mat4 uProjection;

layout(std430, binding = 0) buffer ChunkPositions {
    vec3 chunkPosition[];
};

out vec3 Normal;
out vec3 FragPos; // Position pour le fragment shader

const int FACE_BACK = 0;
const int FACE_FRONT = 1;
const int FACE_LEFT = 2;
const int FACE_RIGHT = 3;
const int FACE_BOTTOM = 4;
const int FACE_TOP = 5;

vec3 decodePosition(uint encodedInstance) {
    uint x = encodedInstance & 0x1Fu;           // Bits 0-4
    uint y = (encodedInstance >> 5u) & 0x1Fu;   // Bits 5-9
    uint z = (encodedInstance >> 10u) & 0x1Fu;  // Bits 10-14
    return vec3(x, y, z);
}

int decodeFace(uint encodedInstance) {
    return int((encodedInstance >> 15u) & 0x7u); // Bits 15-17 (3 bits)
}

vec3 decodeNormal(uint encodedInstance) {
    uint normal = decodeFace(encodedInstance);
    if (normal == 0u) return vec3(0.0, 0.0, 1.0);        // FRONT
    if (normal == 1u) return vec3(0.0, 0.0, -1.0);       // BACK
    if (normal == 2u) return vec3(1.0, 0.0, 0.0);        // RIGHT
    if (normal == 3u) return vec3(-1.0, 0.0, 0.0);       // LEFT
    if (normal == 4u) return vec3(0.0, 1.0, 0.0);        // TOP
    if (normal == 5u) return vec3(0.0, -1.0, 0.0);       // BOTTOM
    return vec3(0.0, 0.0, 0.0);
}

void main() {

    uint drawIndex = gl_DrawID;

    vec3 instancePos = decodePosition(aInstanceData);

    vec3 basePos = aBaseVertex;

    if(decodeFace(aInstanceData) == FACE_TOP) {
        basePos.y++;
    }else if(decodeFace(aInstanceData) == FACE_BOTTOM){
        basePos.xz = basePos.zx;
    }else if(decodeFace(aInstanceData) == FACE_LEFT){
        basePos.xy = basePos.yx;
    }else if(decodeFace(aInstanceData) == FACE_RIGHT){
        basePos.zxy = basePos.xyz;
        basePos.x++;
    }else if(decodeFace(aInstanceData) == FACE_BACK){
        basePos.zy = basePos.yz;
    }else if(decodeFace(aInstanceData) == FACE_FRONT){
        basePos.xzy = basePos.zyx;
        basePos.z++;
    }

    vec3 offset = chunkPosition[drawIndex] * 32;
    basePos = basePos + instancePos;

    Normal = decodeNormal(aInstanceData);;
    FragPos = basePos;

    gl_Position = uProjection * uView * vec4(basePos + offset, 1.0);
}
//@endvs

//@fs
#version 460 core

in vec3 Normal;
in vec3 FragPos;

out vec4 FragColor;

void main() {
    //vec3 color = abs(Normal);
    vec3 color = vec3(FragPos.x / 32.0, FragPos.y / 32.0, FragPos.z / 32.0);

    FragColor = vec4(color, 1.0);
}
//@endfs