#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_dMapCoords;//Corresponds to the location on the depth map
varying vec2 v_shadUV;//origin coordinate on shadow texture
varying vec2 v_shadWH;//width and height of of the region of the shadow texture
uniform sampler2D u_dMapTex;//Corresponds to the depth map
uniform sampler2D u_shadTex;//Corresponds to the shadow texture

uniform vec2 u_shadCoordOrigin;
uniform vec2 u_shadCoordUV;

varying vec3 v_a; //Location of point a, origin of shadow texture, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)
varying vec3 v_ab; //Vector from point a to b, corresponding to u on the
varying vec3 v_ac; //Vector
varying vec3 v_S; //Location of light source S, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)
//uniform vec3 u_laS; //Vector from a to the light source (S - a)

void main()
{

    vec4 dmap = texture2D(u_dMapTex, v_dMapCoords);
    vec3 T = vec3(v_dMapCoords, 1/dmap.r-1);//Coordinates of target
    vec3 laS = v_S - v_a; //Vector from a to light source S
    vec3 lST = T - v_S; //Vector from light source S to target T

    vec3 nA = cross(v_ab, v_ac);//This value is reused, and coincides with the normal area vector to the parallelogram
    float det = dot(-lST,nA);

    float det_recip = 1/det;
    float t = det_recip*dot(nA, laS);
    float u = det_recip*dot(cross(v_ac, -lST), laS);
    float v = det_recip*dot(cross(-lST, v_ab), laS);

    //If the intersection with the plane lies within the parallelogram created by a, b and c, and it's in front of the source, sample the shadow texture
    float t_fudge = 0.0;
    float coord_fudge = 0.001;
    if (t > t_fudge && t < 1-t_fudge &&
        u >= 0-coord_fudge && u <= 1+coord_fudge &&
        v >= 0-coord_fudge && v <= 1+coord_fudge) {
        vec2 shadCoord = v_shadUV + (v_shadWH*vec2(u,1-v));
//        vec2 shadCoord = vec2(u,v);
        vec4 shadColor = texture2D(u_shadTex, shadCoord);
//        vec4 shadColor = texture2D(u_shadTex, vec2(u,v));
        gl_FragColor = vec4(vec3(1-shadColor.a), 1 );
//        gl_FragColor = vec4(vec3(1-shadColor.a),1);//Start by making it 0 to see if it works
    } else {
        gl_FragColor = vec4(1,1,1,1);
    }
//    gl_FragColor = texture2D(u_dMapTex, gl_FragCoord.xy);
//    float depth = (T.z-v_S.z);


    //gl_FragColor = vec4((T.xy-v_S.xy),1/depth,1);
//    gl_FragColor = vec4(0.5,0,0.5,1);

}