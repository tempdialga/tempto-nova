attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
attribute vec2 a_depCoord0;
uniform mat4 u_projTrans;
varying vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_depCoords;
varying float v_depth;

void main()
{
    v_color = a_color;
    v_depth = 1/a_color.r;
    v_color.a = v_color.a * (255.0/254.0);
    v_texCoords = a_texCoord0;
    v_depCoords = a_depCoord0;
    gl_Position =  u_projTrans * a_position;
    gl_Position.z = v_color.r;
}
