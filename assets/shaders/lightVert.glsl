attribute vec4 a_position;
attribute vec2 a_depCoord0;
attribute vec3 a_lightCoord0;
attribute float a_shadowChannel;
attribute vec4 a_lightColor;
attribute vec2 a_positionChannel;
attribute float a_lightSpread;
uniform mat4 u_projTrans;
varying vec2 v_depCoords;
varying vec3 v_lightCoords;
flat varying float v_colChannel;
flat varying vec2 v_posChannel;
flat varying float v_spread;
varying vec4 v_color;

void main()
{
//    v_depCoords = a_depCoord0;
    gl_Position =  u_projTrans * a_position;
    v_depCoords = 0.5*(gl_Position.xy+vec2(1));
//    v_depCoords = gl_Position.xy;
    v_lightCoords = a_lightCoord0;
    v_colChannel = a_shadowChannel;
    v_posChannel = a_positionChannel;
    v_color = a_lightColor;
    v_spread = a_lightSpread;
}
