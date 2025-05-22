#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

varying vec2 v_depCoords;//Corresponds to the location on the depth map I think this is actually redundant
//varying vec3 v_lightCoords;//Location of the light source, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)
varying vec3 v_lightCoords; //Location of light source S, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)
flat varying float v_colChannel; //Which color channel of the shadowmap this light uses (red, greeen, blue, or alpha)
flat varying vec2 v_posChannel; //Which position on the shadowmap this uses (column, row, column width, column height)
varying LOWP vec4 v_color; //Color of the light source

uniform sampler2D u_dMapTex; //The depth map
uniform sampler2D u_shadMapTex; //The map of existing shadows, not to be confused with the texture used to draw shadows in the first place
uniform vec2 u_viewDims; //Dimensions of the screen in world coordinates
uniform vec2 u_positionChannelDimensions; //Dimensions of each channel on the shadow map

void main()
{
    vec4 dMap = texture2D(u_dMapTex, v_depCoords);
    float depth = 1/dMap.r;
    float n_x = dMap.g*2-1;
    float n_y = dMap.b*2-1;
    float n_z = -sqrt(1 - n_x*n_x - n_y*n_y); //Always assume normal vector faces towards the camera
    float k = dMap.a*8; //8 channels
    int k_type = int(floor(k));
    k -= k_type;

    vec3 N = vec3(n_x, n_y, n_z);
    vec3 TS = vec3((v_lightCoords.xy-v_depCoords)*u_viewDims, v_lightCoords.z-depth); //From the target to the light source
    float r = length(TS);
    vec3 TS_nor = TS/r;
    vec3 R = 2*dot(TS_nor, N)*N - TS_nor; //Reflection direction

    float alpha = 2;
    float specular = pow(max(0,-R.z), alpha);

    float diffuse = dot(TS_nor, N);
    float perfect_rough_diffuse = 0.5;

    float base_intensity = (250*5)/(r*r);
//    base_intensity = 1-exp(-2*base_intensity);
    vec3 base_color = v_color.rgb*v_color.a;

    vec3 final_color = /*(1-exp(-2**/base_intensity*base_color/*))*/*((1-k)*perfect_rough_diffuse + k*specular);

    vec2 shadMapCoords = v_depCoords;
    shadMapCoords.xy += /*vec2(1)+*/v_posChannel;
    shadMapCoords.xy *= u_positionChannelDimensions;
//    shadMapCoords.xy -= vec2(1);
    vec4 shadMask = texture2D(u_shadMapTex, shadMapCoords);
    int ch_idx = int(floor(v_colChannel));
//    ch_idx = 0;
    vec4 channelMask = vec4(float(ch_idx == 0), float(ch_idx == 1), float(ch_idx == 2), float(ch_idx == 3));
//    channelMask = vec4(0.25);
    float shadValue = dot(shadMask, channelMask);


//    shadValue = ch_idx == 0 ? shadMask.r :
//                ch_idx == 1 ? shadMask.g :
//                ch_idx == 2 ? shadMask.b : shadMask.a;

    gl_FragColor = vec4(final_color*shadValue,1);
//    gl_FragColor = vec4(channelMask)*0.5;
}