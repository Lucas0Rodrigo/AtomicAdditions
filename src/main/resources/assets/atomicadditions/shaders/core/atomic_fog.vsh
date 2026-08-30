#version 150

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 LocalPosition;
out vec4 VertexColor;

void main() {
    LocalPosition = Position;
    VertexColor = Color;

    gl_Position =
        ProjMat
        * ModelViewMat
        * vec4(Position, 1.0);
}