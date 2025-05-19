#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif
#define M_PI 3.1415926535897932384626433832795
varying LOWP vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_depCoords;
varying float v_depth;
uniform sampler2D u_texture;
void main()
{
    vec4 base = v_color;
    vec4 tMap = texture2D(u_texture, v_texCoords);

    vec4 dMap = texture2D(u_texture, v_depCoords);


    vec3 N_b = vec3(base.gb*2-1, sqrt(1 - base.g*base.g - base.b*base.b)); //Normal vector of the base plane, positive z for now
//    vec3 N_mod = vec3(dMap.gb*2-1, sqrt(1 - dMap.g*dMap.g - dMap.b*dMap.b)-1); //Normal vector of the modifying texture, relative to a default vector of (0,0,1), also positive z for now


//    vec3 N_b = vec3(0,0,1); //Normal vector of the base plane, positive z for now
    vec3 N_mod = vec3(dMap.gb*2-1, sqrt(1 - dMap.g*dMap.g - dMap.b*dMap.b)-1); //Normal vector of the modifying texture, relative to a default vector of (0,0,1), also positive z for now

    //Simple addition, accurate at small angles, and at large angles we don't want full accuracy anyways because we don't want things facing away from the screen technically
    vec3 N_comb = (N_b + N_mod);
//    N_comb = N_b;

//    //Break into a counterclockwise rotation by theta around the y axis (i.e. so z goes into x), and then a counterclockwise rotation by phi around the z axis (so x curls into y)
//    float mod_theta = acos(N_mod.z);
//    float mod_phi = atan(N_mod.y/N_mod.x);
//    if (N_mod.x < 0) mod_phi += M_PI;
//
//    //Rotate by theta around y axis
//    float ct = cos(mod_theta);
//    float st = sin(mod_theta);
//    vec3 N_comb = vec3(N_b.x*ct+N_b.z*st, N_b.y, N_b.z*ct-N_b.x*st);
//    //Rotate by phi around z axis
//    float cp = cos(mod_phi);
//    float sp = sin(mod_phi);
//    N_comb = vec3(N_comb.x*cp-N_comb.y*sp, N_comb.y*cp+N_comb.x*sp, N_comb.z);
//    //Clamp to positive z range
//    N_comb.z = clamp(N_comb.z, 0, 1);
//    N_comb = normalize(N_comb);

//    vec3 N_comb = vec3(
//        N_b.x*N_mod.z + N_b.z*N_mod.x,
//        N_b.y*N_mod.z + N_b.z*N_mod.y,
//        clamp(N_b.z*N_mod.z - N_b.x*N_mod.x - N_b.y*N_mod.y,0,1)
//    );
//    N_comb = normalize(N_comb);

//    float base_d_px = 1/base.r;
    float base_d_px = v_depth;
    float depth_mod_max = -3.0; //Since per pixel modifier is on a linear range instead of 1/z (so you can modify it), this is the maximum depth that can be added or subtracted
                                //Positive modifying means going forward, which means a lower z coordinate because z goes away from the screen

    //    float mod_d_px = 1/dMap.r-2;
    float mod_d_px = depth_mod_max*(dMap.r-0.5)/*-0.01*/;//Fudge it a little

    vec4 final = vec4(
    1/(base_d_px+mod_d_px),
//    1/base_d_px,
    N_comb.xy*0.5+0.5,
    dMap.a
//    1
    );
    //depth = floor(base.a)/(dMap.r+0.5 + )
    gl_FragColor = final;
    if (tMap.a > 0.01) {
        gl_FragDepth = final.r;
    } else {
        gl_FragDepth = 0;
    }
//    if (final.r < gl_FragDepth) {
//        gl_FragColor = vec4(1);
//        discard;
//    } else {
//
//    }
//    gl_FragColor = vec4(1);
}