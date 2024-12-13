//@vs
#version 460 core

layout(location = 0) in vec3 aBaseVertex; // Vertices d'une face définie dans le shader
layout(location = 1) in uint aInstanceData; // Données compressées pour l'instance

uniform mat4 uView;
uniform mat4 uProjection;

layout(std430, binding = 0) buffer ChunkPositions {
    vec3 chunkPosition[];
};

out int TextureLayer;
out vec3 Normal;
out vec3 FragPos;
out int FaceIndex;

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

int decodeBlock(uint encodedInstance) {
    return int((encodedInstance >> 18u) & 0xFFu); // Bits 18-25 (8 bits)
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
        FaceIndex = FACE_TOP;
        basePos.y++;
    }else if(decodeFace(aInstanceData) == FACE_BOTTOM){
        FaceIndex = FACE_BOTTOM;
        basePos.xz = basePos.zx;
    }else if(decodeFace(aInstanceData) == FACE_LEFT){
        FaceIndex = FACE_LEFT;
        basePos.xy = basePos.yx;
    }else if(decodeFace(aInstanceData) == FACE_RIGHT){
        FaceIndex = FACE_RIGHT;
        basePos.zxy = basePos.xyz;
        basePos.x++;
    }else if(decodeFace(aInstanceData) == FACE_BACK){
        FaceIndex = FACE_BACK;
        basePos.zy = basePos.yz;
    }else if(decodeFace(aInstanceData) == FACE_FRONT){
        FaceIndex = FACE_FRONT;
        basePos.xzy = basePos.zyx;
        basePos.z++;
    }

    FragPos = basePos;

    vec3 offset = chunkPosition[drawIndex] * 32;
    basePos = basePos + instancePos;

    TextureLayer = decodeBlock(aInstanceData);
    Normal = decodeNormal(aInstanceData);

    gl_Position = uProjection * uView * vec4(basePos + offset, 1.0);
}
//@endvs

//@fs
#version 460 core

uniform sampler2DArray textureArray;

flat in int TextureLayer;
flat in int FaceIndex;
in vec3 Normal;
in vec3 FragPos;

out vec4 FragColor;

const int FACE_BACK = 0;
const int FACE_FRONT = 1;
const int FACE_LEFT = 2;
const int FACE_RIGHT = 3;
const int FACE_BOTTOM = 4;
const int FACE_TOP = 5;

void main() {
    vec3 color = abs(Normal);
    //vec3 color = vec3(FragPos.x, FragPos.y, FragPos.z);

    FragColor = vec4(color, 1.0);

    float faceWidth = 1.0 / 6.0;
    vec2 textureCoord = vec2(0);

    if(FaceIndex == FACE_LEFT){
        int faceIndex = 1;
        textureCoord = vec2(FragPos.z * faceWidth + faceWidth * faceIndex, FragPos.y);
    }else if(FaceIndex == FACE_RIGHT){
        int faceIndex = 2;
        textureCoord = vec2(FragPos.z * faceWidth,FragPos.y);
        textureCoord.x += faceWidth * faceIndex;
    }else if(FaceIndex == FACE_TOP){
        int faceIndex = 0;
        textureCoord = vec2(FragPos.x * faceWidth,FragPos.z);
        textureCoord.x += faceWidth * faceIndex;
    }else if(FaceIndex == FACE_BOTTOM){
        int faceIndex = 5;
        textureCoord = vec2(FragPos.x * faceWidth,FragPos.z);
        textureCoord.x += faceWidth * faceIndex;
    }else if(FaceIndex == FACE_FRONT){
        int faceIndex = 3;
        textureCoord = vec2(FragPos.x * faceWidth,FragPos.y);
        textureCoord.x += faceWidth * faceIndex;
    }else if(FaceIndex == FACE_BACK){
        int faceIndex = 4;
        textureCoord = vec2(FragPos.x * faceWidth,FragPos.y);
        textureCoord.x += faceWidth * faceIndex;
    }

    FragColor = texture(textureArray,vec3(textureCoord,TextureLayer));
    //FragColor = vec4(textureCoord,0,1);

}
//@endfs