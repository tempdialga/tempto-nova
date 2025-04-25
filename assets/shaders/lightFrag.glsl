#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_depCoords;//Corresponds to the location on the depth map I think this is actually redundant
//varying vec3 v_lightCoords;//Location of the light source, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)
varying vec3 v_lightCoords; //Location of light source S, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)

uniform sampler2D u_texture; //The depth map
uniform vec2 u_viewDims; //Dimensions of the screen in world coordinates

void main()
{
    vec4 dMap = texture2D(u_texture, v_depCoords);
    float depth = 1/dMap.r;
    vec3 r = vec3((v_depCoords-v_lightCoords.xy)*u_viewDims,v_lightCoords.z-depth);
    gl_FragColor = vec4(vec2(100/dot(r,r)),0,1);
}