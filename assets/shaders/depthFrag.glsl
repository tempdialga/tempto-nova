#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif
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

//    float base_d_px = 1/base.r;
    float base_d_px = v_depth;
//    float mod_d_px = 1/dMap.r-2;
    float mod_d_px = 3*(dMap.r-0.5)-0.01;//Fudge it a little

    vec4 final = vec4(
//    1/(base_d_px-mod_d_px),
    1/base_d_px,
    v_color.gb,
    tMap.a);
//    final.rgb=dMap.rgb;
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