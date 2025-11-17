#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

varying vec2 v_depCoords;//Corresponds to the location on the depth map I think this is actually redundant
varying vec3 v_lightCoords; //Location of light source S, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)
flat varying float v_colChannel; //Which color channel of the shadowmap this light uses (red, greeen, blue, or alpha)
flat varying vec2 v_posChannel; //Which position on the shadowmap this uses (column, row, column width, column height)
varying LOWP vec4 v_color; //Color of the light source
flat varying float v_spread; //How far in pixels the light can spread from the source

uniform sampler2D u_baseColTex; //The base color of every pixel on the screen if they were all at plain white 1.0 lighting
uniform float u_exposure; // Exposure/sensitivity

void main()
{
    vec3 light_color = v_color.rgb*v_color.a;

    vec4 surfColor = texture2D(u_baseColTex, v_depCoords);

    gl_FragColor = vec4(surfColor.xyz*light_color*u_exposure,1);
}
