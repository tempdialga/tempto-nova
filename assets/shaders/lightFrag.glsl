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

    float alpha = 5;
    float specular = pow(max(0,-R.z), alpha);

    float diffuse = dot(TS_nor, N);

    float base_intensity = (250*5)/(r*r);
    vec3 base_color = vec3(1,1,0.5);

    vec3 final_color = base_intensity*base_color*((1-k)*diffuse + k*specular);

    gl_FragColor = vec4(final_color,1);
}