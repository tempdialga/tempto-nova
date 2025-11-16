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

uniform sampler2D u_dMapTex; //The depth map
uniform sampler2D u_shadMapTex; //The map of existing shadows, not to be confused with the texture used to draw shadows in the first place
uniform sampler2D u_baseColTex; //The base color of every pixel on the screen if they were all at plain white 1.0 lighting
uniform vec2 u_viewDims; //Dimensions of the screen in world coordinates
uniform vec2 u_positionChannelDimensions; //Dimensions of each channel on the shadow map
uniform float u_exposure; // Exposure/sensitivity

void main()
{
    vec4 dMap = texture2D(u_dMapTex, v_depCoords);
    float depth = (1-dMap.r)*256.0-1;
    float n_x = dMap.g*2-1;
    float n_y = dMap.b*2-1;
    float n_z = -sqrt(1 - n_x*n_x - n_y*n_y); //Always assume normal vector faces towards the camera
    float k = dMap.a*8; //8 channels
    int k_type = int(floor(k));
    k -= k_type;

    vec3 N = vec3(n_x, n_y, n_z);
    vec3 TS = vec3((v_lightCoords.xy-v_depCoords)*u_viewDims, v_lightCoords.z-depth); //From the target to the light source
    float r = length(TS);
    if (r > v_spread) discard;
    vec3 TS_nor = TS/r;
    vec3 R = 2*dot(TS_nor, N)*N - TS_nor; //Reflection direction

    float alpha = 2;
    float specular = pow(max(0,-R.z), alpha);

    float diffuse = dot(TS_nor, N);
    float perfect_rough_diffuse = 0.5*max(0, sign(diffuse));



    float min_intensity = 0.2;//Intensity at the edge of the light bounds
    float falloff_power = 1.25;
    float max_intensity = min_intensity *(pow(v_spread, falloff_power)); //Intensity at the center such that at the given spread, the intensity reaches min_intensity

    float intensity = max_intensity/(pow(r, falloff_power));

    vec3 light_color = v_color.rgb*v_color.a;


    vec3 light_color_eff = /*(1-exp(-2**/intensity*light_color/*))*/*((1-k)*diffuse + k*specular);

    vec2 shadMapCoords = v_depCoords;
    shadMapCoords.xy += /*vec2(1)+*/v_posChannel;
    shadMapCoords.xy *= u_positionChannelDimensions;
    vec4 shadMask = texture2D(u_shadMapTex, shadMapCoords);
    int ch_idx = int(floor(v_colChannel));
    vec4 channelMask = vec4(float(ch_idx == 0), float(ch_idx == 1), float(ch_idx == 2), float(ch_idx == 3));
    float shadValue = dot(shadMask, channelMask);
    vec3 shadHue = vec3(1,0.6,0.48);
    vec3 shadTint = (1-shadValue)*shadHue+vec3(shadValue);


    vec4 surfColor = texture2D(u_baseColTex, v_depCoords);
//    surfColor = vec4(v_depCoords, 1, 1);

    gl_FragColor = vec4(surfColor.xyz*light_color_eff*shadTint*shadValue*u_exposure,1);
//    gl_FragColor = surfColor;
    //    gl_FragColor = vec4(channelMask)*0.5;
}