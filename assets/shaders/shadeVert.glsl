attribute vec4 a_position;
attribute vec2 a_depCoord0;
attribute vec2 a_shadTexCoord0; //Coordinates on the shadow texture

attribute vec3 a_a;
attribute vec3 a_ab;
attribute vec3 a_ac;
attribute vec3 a_S;

uniform mat4 u_projTrans;

varying vec2 v_shadTexCoords;
varying vec2 v_dMapCoords;
varying vec3 v_a; //Location of point a, origin of shadow texture, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)
varying vec3 v_ab; //Vector from point a to b, corresponding to u on the
varying vec3 v_ac; //Vector
varying vec3 v_S; //Location of light source S, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)

void main()
{
    gl_Position =  u_projTrans * a_position;
    v_shadTexCoords = a_shadTexCoord0;
    v_dMapCoords = a_depCoord0;
    v_a = a_a;
    v_ab = a_ab;
    v_ac = a_ac;
    v_S = a_S;
}
