attribute vec4 a_position;
attribute vec2 a_depCoord0;
attribute vec2 a_shadTexCoord0; //Coordinates on the shadow texture
attribute vec2 a_shadTexDims0; //Width on the shadow texture
attribute vec2 a_shadTexDepCoord0; //Coordinates on the shadow texture for the depth part of that shadow
attribute vec3 a_a;
attribute vec3 a_ab;
attribute vec3 a_ac;
attribute vec3 a_S;
attribute float a_lightBodyRadius; //Radius of the body casting the light
attribute vec2 a_positionChannel; //Where on the shadow map (column, row, column width, row width) this shadow is to be drawn

uniform mat4 u_projTrans;

varying vec2 v_shadUV;
varying vec2 v_shadWH;
varying vec2 v_shadDE;
varying vec2 v_dMapCoords;
varying vec3 v_a; //Location of point a, origin of shadow texture, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)
varying vec3 v_ab; //Vector from point a to b, corresponding to width on the shadow texture region
varying vec3 v_ac; //Vector from point a to c, corresponding to height on the shadow texture region
varying vec3 v_S; //Location of light source S, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)
varying float v_R; //Radius of the body casting the light (world coordinates)

uniform vec2 u_positionChannelDimensions; //Dimensions of a position channel, relative to the size of the screen

void main()
{
    gl_Position =  u_projTrans * a_position;
    v_dMapCoords = a_depCoord0;
    v_shadUV = a_shadTexCoord0;
    v_shadWH = a_shadTexDims0;
    v_shadDE = a_shadTexDepCoord0;

    v_a = a_a;
    v_ab = a_ab;
    v_ac = a_ac;
    v_S = a_S;
    v_R = a_lightBodyRadius;

    gl_Position.xy += vec2(1)+2*a_positionChannel;
    gl_Position.xy *= u_positionChannelDimensions;
    gl_Position.xy -= vec2(1);
//    gl_Position.xy += a_positionChannel.xy*a_positionChannel.zw;
}
