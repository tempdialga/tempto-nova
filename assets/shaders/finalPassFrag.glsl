#ifdef GL_ES 
#define LOWP lowp 
precision mediump float; 
#else 
#define LOWP  
#endif
//layout(origin_lower_left) in vec4 gl_FragCoord;
varying LOWP vec4 v_color; 
varying vec2 v_texCoords;
//varying vec2 v_screenCoords;
uniform sampler2D u_texture;
uniform sampler2D u_lightMapTex;
uniform sampler2D u_depthMapTex;
uniform vec2 u_invScreenDims;
uniform float u_lightDecodeFactor;
uniform float u_sensitivity;
void main()
{
  vec4 depColor = v_color;
  vec2 coords = (gl_FragCoord.xy-vec2(0))*u_invScreenDims/**0.5*/;
  vec4 screenDepthVec = texture2D(u_depthMapTex, coords);

  vec4 lightLevel = texture2D(u_lightMapTex, coords)*u_lightDecodeFactor*u_sensitivity;

  lightLevel.a = 1;
  gl_FragColor = texture2D(u_texture, v_texCoords) * lightLevel;
//  float gamma = 1.5;
//  gl_FragColor.rgb = pow(gl_FragColor.rgb, vec3(1.0/gamma));
  if (depColor.r < screenDepthVec.r-(2.0/256.0)) {
    gl_FragColor.a = 0;
  }
//  gl_FragColor = vec4(coords.xy, 0, 1);
}