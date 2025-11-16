#ifdef GL_ES 
#define LOWP lowp 
precision mediump float; 
#else 
#define LOWP  
#endif
varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform sampler2D u_depthMapTex;
uniform vec2 u_invScreenDims;
//uniform float u_lightDecodeFactor;
//uniform float u_sensitivity;
void main()
{
  vec4 depColor = v_color;
  vec2 coords = (gl_FragCoord.xy-vec2(0))*u_invScreenDims;
  vec4 screenDepthVec = texture2D(u_depthMapTex, coords);

  gl_FragColor = texture2D(u_texture, v_texCoords); //Get color
  if (depColor.r < screenDepthVec.r-(2.0/256.0)) { //Filter out if too deep
    gl_FragColor.a = 0;
  }
//  gl_FragColor = vec4(coords.xy, 0, 1);
}