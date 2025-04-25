attribute vec4 a_position;
attribute vec2 a_depCoord0;
attribute vec3 a_lightCoord0;
uniform mat4 u_projTrans;
varying vec2 v_depCoords;
varying vec3 v_lightCoords;

void main()
{
//    v_depCoords = a_depCoord0;
    gl_Position =  u_projTrans * a_position;
    v_depCoords = 0.5*(gl_Position.xy+vec2(1));
//    v_depCoords = gl_Position.xy;
    v_lightCoords = a_lightCoord0;
}
