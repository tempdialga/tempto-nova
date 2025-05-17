#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_dMapCoords;//Corresponds to the location on the depth map
varying vec2 v_shadUV;//origin coordinate on shadow texture
varying vec2 v_shadWH;//width and height of of the region of the shadow texture
varying vec2 v_shadDE;//origin coordinate on the shadow texture but which correspond to the depth texture instead of the normal


uniform vec2 u_viewDims; //Dimensions of the screen in world coordinates
uniform sampler2D u_dMapTex;//Corresponds to the depth map
uniform sampler2D u_shadTex;//Corresponds to the shadow texture
uniform vec2 u_shadPxDims; //how wide/tall each pixel on u_shadTex is
uniform int u_shadTexWidth; //How many pixels wide u_shadTex is (recip of u_shadPxDims.x)
uniform int u_shadTexHeight; //How mnay puxels high u_shadTex is
uniform float u_elapsedTime; //Elapsed time in seconds (for debugging purposes)


varying vec3 v_a; //Location of point a, origin of shadow texture, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)
varying vec3 v_ab; //Vector from point a to b, corresponding to u on the texture
varying vec3 v_ac; //Vector
varying vec3 v_S; //Location of light source S, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)
//uniform vec3 u_laS; //Vector from a to the light source (S - a)


vec4 shadowFromS(in vec3 S, in vec3 T,
in vec3 a, in vec3 ab, in vec3 ac,
in float t_mod);

vec4 shadowFromS(in vec3 S, in vec3 T,
in vec3 a, in vec3 ab, in vec3 ac, in float t_mod) {
    vec3 laS = S - a; //Vector from a to light source S
    vec3 lST = T - S; //Vector from light source S to target T

    vec3 nA = cross(ab, ac);//This value is reused, and coincides with the normal area vector to the parallelogram
    float det = dot(-lST,nA);

    float det_recip = 1/det;
    float t = det_recip*dot(nA, laS); //I think this is from S to T, so 0 should mean at the light and 1 should mean at the surface
    float u = det_recip*dot(cross(ac, -lST), laS);
    float v = det_recip*dot(cross(-lST, ab), laS);

    float t_fudge = 0.0;
    float coord_fudge = 0.001;
    float shadFudge = 1.01;

    vec2 shadCoord = v_shadUV + v_shadWH*vec2(u, 1-v);
    vec4 shadCol = shadFudge*texture2D(u_shadTex, shadCoord);
    //Repeat original sampling process for each point
    if (t+t_mod > t_fudge && t+t_mod < 1-t_fudge &&
    u >= 0-coord_fudge && u <= 1+coord_fudge &&
    v >= 0-coord_fudge && v <= 1+coord_fudge) {

        return shadCol;
    } else {
        return vec4(0);
    }
}

void main()
{

    vec4 dmap = texture2D(u_dMapTex, v_dMapCoords);
    vec3 w_T = vec3(v_dMapCoords, 1/dmap.r-1);//Coordinates of target (screeen coords)
    vec3 T = vec3(w_T.xy*u_viewDims, w_T.z);//Coordinates of target
    vec3 S = vec3(v_S.xy*u_viewDims, v_S.z);

    //Approach 2: Physically model different light source points
    float w_r = 1.7*0.5;//Radius in world coordinates

    vec3 a = vec3(v_a.xy*u_viewDims, v_a.z);
    vec3 ab = vec3(v_ab.xy*u_viewDims, v_ab.z);
    vec3 ac = vec3(v_ac.xy*u_viewDims, v_ac.z);

    float z_mod = 10*sin(3*u_elapsedTime);
    //    T.z += z_mod;
    vec3 laS = S - a; //Vector from a to light source S
    vec3 lST = T - S; //Vector from light source S to target T

    vec3 nA = cross(ab, ac);//This value is reused, and coincides with the normal area vector to the parallelogram
    float det = dot(-lST,nA);

    float det_recip = 1/det;
    float t = det_recip*dot(nA, laS); //I think this is from S to T, so 0 should mean at the light and 1 should mean at the surface
    float u = det_recip*dot(cross(ac, -lST), laS);
    float v = det_recip*dot(cross(-lST, ab), laS);

    vec2 shadCoordCenter = v_shadUV + (v_shadWH*vec2(u,1-v));
    vec2 shadDepCoordCenter = v_shadDE + (v_shadWH*vec2(u,1-v));
    vec4 shadColorCenter = texture2D(u_shadTex, shadCoordCenter);
    vec4 shadDep = texture2D(u_shadTex, shadDepCoordCenter);

    float t_mod = (3*(shadDep.r-0.5))/abs(lST.z);
    t_mod = 0;

    //If the intersection with the plane lies within the parallelogram created by a, b and c, and it's in front of the source, sample the shadow texture
    float t_fudge = 0.0;
    float coord_fudge = 0.001;


    float shadFudge = 1.1f; //i.e. if there is already a shadow make it a little more opaque

    //    if (t+t_mod > t_fudge && t+t_mod < 1-t_fudge &&
    //    u >= 0-coord_fudge && u <= 1+coord_fudge &&
    //    v >= 0-coord_fudge && v <= 1+coord_fudge) {
    //        vec2 shadCoord = v_shadUV + v_shadWH*vec2(u, 1-v);
    //        shadColor.rgb += shadFudge*texture2D(u_shadTex, shadCoord).aaa;
    //    }



    vec3 S_00 = vec3(S.x-w_r, S.y-w_r, S.z);
    vec4 C_00 = shadowFromS(S_00, T, a, ab, ac, t_mod);
    vec3 S_0m = vec3(S.x-w_r, S.y    , S.z);
    vec4 C_0m = shadowFromS(S_0m, T, a, ab, ac, t_mod);
    vec3 S_01 = vec3(S.x-w_r, S.y+w_r, S.z);
    vec4 C_01 = shadowFromS(S_01, T, a, ab, ac, t_mod);

    vec3 S_m0 = vec3(S.x, S.y-w_r, S.z);
    vec4 C_m0 = shadowFromS(S_m0, T, a, ab, ac, t_mod);
    vec3 S_mm = vec3(S.x, S.y , S.z);
    vec4 C_mm = shadowFromS(S_mm, T, a, ab, ac, t_mod);
    vec3 S_m1 = vec3(S.x, S.y+w_r, S.z);
    vec4 C_m1 = shadowFromS(S_m1, T, a, ab, ac, t_mod);

    vec3 S_10 = vec3(S.x+w_r, S.y-w_r, S.z);
    vec4 C_10 = shadowFromS(S_10, T, a, ab, ac, t_mod);
    vec3 S_1m = vec3(S.x+w_r, S.y , S.z);
    vec4 C_1m = shadowFromS(S_1m, T, a, ab, ac, t_mod);
    vec3 S_11 = vec3(S.x+w_r, S.y+w_r, S.z);
    vec4 C_11 = shadowFromS(S_11, T, a, ab, ac, t_mod);


    //    shadColor *= 0.25;
    vec4 shadColor = (
    C_01 + C_m1 + C_11 +
    C_0m + C_mm + C_1m +
    C_00 + C_m0 + C_10
    )*shadFudge/9.0;

    gl_FragColor = vec4(shadColor.aaa, 0);

    vec2 r_redundant = u_shadPxDims*0.25;
    float fu0 = u_shadPxDims.x*floor(u*u_shadTexWidth);
    float fv0 = u_shadPxDims.y*floor(v*u_shadTexHeight);
}



