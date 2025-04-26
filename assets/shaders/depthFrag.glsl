#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif
varying LOWP vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_depCoords;
uniform sampler2D u_texture;
void main()
{
    vec4 base = v_color;
    vec4 tMap = texture2D(u_texture, v_texCoords);
    vec4 dMap = texture2D(u_texture, v_depCoords);

    float base_d_px = 1/base.r;
//    float mod_d_px = 1/dMap.r-2;
    float mod_d_px = 10*(dMap.r-0.5);

    vec4 final = vec4(
    1/(base_d_px-mod_d_px),
    v_color.gb,
    tMap.a);
//    final.rgb=dMap.rgb;
    //depth = floor(base.a)/(dMap.r+0.5 + )
    gl_FragColor = final;
//    gl_FragColor = vec4(1);
}