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
varying float v_R; //Radius of the light casting body
//uniform vec3 u_laS; //Vector from a to the light source (S - a)

vec3 intersectionRayPlane(in vec3 S, in vec3 T,
in vec3 a, in vec3 ab, in vec3 ac);

vec3 intersectionRayPlane(in vec3 S, in vec3 T,
in vec3 a, in vec3 ab, in vec3 ac) {
    vec3 laS = S - a; //Vector from a to light source S
    vec3 lST = T - S; //Vector from light source S to target T

    vec3 nA = cross(ab, ac);//This value is reused, and coincides with the normal area vector to the parallelogram
    float det = dot(-lST,nA);

    float det_recip = 1/det;
    float t = det_recip*dot(nA, laS); //I think this is from S to T, so 0 should mean at the light and 1 should mean at the surface
    float u = det_recip*dot(cross(ac, -lST), laS);
    float v = det_recip*dot(cross(-lST, ab), laS);
    return vec3(u, v, t);
}

float tOfIRP(in vec3 S, in vec3 T,
in vec3 a, in vec3 ab, in vec3 ac);

float tOfIRP(in vec3 S, in vec3 T,
in vec3 a, in vec3 ab, in vec3 ac) {
    vec3 laS = S - a; //Vector from a to light source S
    vec3 lST = T - S; //Vector from light source S to target T

    vec3 nA = cross(ab, ac);//This value is reused, and coincides with the normal area vector to the parallelogram
    float det = dot(-lST,nA);

    float det_recip = 1/det;
    float t = det_recip*dot(nA, laS);
    return t;
}

float zMod(in float rValue);
float zMod(in float rValue) {
    float depth_range = 2.0;
    float unrounded = -depth_range*(rValue-0.5);
//    unrounded = sign(rValue-0.5);
    //0.5 middle means no modification, higher means bringing forward (so negative z) and vice versa
    return unrounded;
    return clamp(unrounded, -depth_range*0.5, depth_range*0.5);
}

bool intersectionValid(in vec3 uvt, float t_mod);

bool intersectionValid(in vec3 uvt, float t_mod) {

    float u = uvt.x;
    float v = uvt.y;
    float t = uvt.z;

    float t_fudge = 0.0;
    float coord_fudge = 0.001;

    return (t+t_mod > t_fudge && t+t_mod < 1-t_fudge &&
    u >= 0-coord_fudge && u <= 1+coord_fudge &&
    v >= 0-coord_fudge && v <= 1+coord_fudge);
}


vec4 shadowFromS(in vec3 S, in vec3 T,
in vec3 a, in vec3 ab, in vec3 ac, float t_mod);

vec4 shadowFromS(in vec3 S, in vec3 T,
in vec3 a, in vec3 ab, in vec3 ac, float t_mod) {
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
    vec3 w_T = vec3(v_dMapCoords, (1-dmap.r)*256.0-1);//Coordinates of target (screeen coords)
    vec3 T = vec3(w_T.xy*u_viewDims, w_T.z);//Coordinates of target
    vec3 S = vec3(v_S.xy*u_viewDims, v_S.z);//Coordinates of light center

    //Approach 2: Physically model different light source points
    float w_r = v_R;//Radius in world coordinates
    float T_r = -0.33; //How much to extend out sampling around the target point itself

    vec3 a = vec3(v_a.xy*u_viewDims, v_a.z);
    vec3 ab = vec3(v_ab.xy*u_viewDims, v_ab.z);
    vec3 ac = vec3(v_ac.xy*u_viewDims, v_ac.z);

    float z_mod = 10*sin(3*u_elapsedTime);
//    S.z += z_mod;
    //    T.z += z_mod;
//    vec3 laS = S - a; //Vector from a to light source S
//    vec3 lST = T - S; //Vector from light source S to target T

    vec3 nA = cross(ab, ac);//This value is reused, and coincides with the normal area vector to the parallelogram
//    float det = dot(-lST,nA);
//
//    float det_recip = 1/det;
//    float t = det_recip*dot(nA, laS); //I think this is from S to T, so 0 should mean at the light and 1 should mean at the surface
//    float u = det_recip*dot(cross(ac, -lST), laS);
//    float v = det_recip*dot(cross(-lST, ab), laS);
//
//    vec2 shadCoordCenter = v_shadUV + (v_shadWH*vec2(u,1-v));
//    vec2 shadDepCoordCenter = v_shadDE + (v_shadWH*vec2(u,1-v));
//    vec4 shadColorCenter = texture2D(u_shadTex, shadCoordCenter);
//    vec4 shadDep = texture2D(u_shadTex, shadDepCoordCenter);



    //If the intersection with the plane lies within the parallelogram created by a, b and c, and it's in front of the source, sample the shadow texture
    float t_fudge = 0.00;
    float coord_fudge = 0.001;

    //Define center properties first so we can approximate depth of the shadow caster using just the center

//    caster_dep_center = 0;
//    caster_dep_center = 10;
//    float t_mod = (3*(shadDep.r-0.5))/abs(lST.z);
    float t_mod_base = t_fudge;



    float shadFudge = 1.01f; //i.e. if there is already a shadow make it a little more opaque

    float depth_mod_max = 2; //Since per pixel modifier is on a linear range instead of 1/z (so you can modify it), this is the maximum depth that can be added or subtracted
    
    

    vec3 S_00 = vec3(S.x-w_r, S.y-w_r, S.z);
    vec3 T_00 = vec3(T.x-T_r, T.y-T_r, T.z);
    vec3 uvt_00 = intersectionRayPlane(S_00, T_00, a, ab, ac);
    vec4 C_00 = texture2D(u_shadTex, v_shadUV + v_shadWH*vec2(uvt_00.x, 1-uvt_00.y));
    vec4 Dmap_00 = texture2D(u_shadTex, v_shadDE + v_shadWH*vec2(uvt_00.x, 1-uvt_00.y));
    float d_00 = zMod(Dmap_00.r);
    uvt_00.z = tOfIRP(S_00, T_00, a+vec3(0,0,d_00), ab, ac);
//    float t_mod_00 = t_mod_base-d_00/abs(S_00.z-T_00.z);
    if (!intersectionValid(uvt_00, 0)) C_00 = vec4(0);

    vec3 S_0m = vec3(S.x-w_r, S.y    , S.z);
    vec3 T_0m = vec3(T.x-T_r, T.y    , T.z);
    vec3 uvt_0m = intersectionRayPlane(S_0m, T_0m, a, ab, ac);
    vec4 C_0m = texture2D(u_shadTex, v_shadUV + v_shadWH*vec2(uvt_0m.x, 1-uvt_0m.y));
    vec4 Dmap_0m = texture2D(u_shadTex, v_shadDE + v_shadWH*vec2(uvt_0m.x, 1-uvt_0m.y));
    float d_0m = zMod(Dmap_0m.r);
    uvt_0m.z = tOfIRP(S_0m, T_0m, a+vec3(0,0,d_0m), ab, ac);
//    float t_mod_0m = t_mod_base-d_0m/abs(S_0m.z-T_0m.z);
    if (!intersectionValid(uvt_0m, 0)) C_0m = vec4(0);

    vec3 S_01 = vec3(S.x-w_r, S.y+w_r, S.z);
    vec3 T_01 = vec3(T.x-T_r, T.y+T_r, T.z);
    vec3 uvt_01 = intersectionRayPlane(S_01, T_01, a, ab, ac);
    vec4 C_01 = texture2D(u_shadTex, v_shadUV + v_shadWH*vec2(uvt_01.x, 1-uvt_01.y));
    vec4 Dmap_01 = texture2D(u_shadTex, v_shadDE + v_shadWH*vec2(uvt_01.x, 1-uvt_01.y));
    float d_01 = zMod(Dmap_01.r);
    uvt_01.z = tOfIRP(S_01, T_01, a+vec3(0,0,d_01), ab, ac);
//    float t_mod_01 = t_mod_base-d_01/abs(S_01.z-T_01.z);
    if (!intersectionValid(uvt_01, 0)) C_01 = vec4(0);


    vec3 S_m0 = vec3(S.x, S.y-w_r, S.z);
    vec3 T_m0 = vec3(T.x, T.y-T_r, T.z);
    vec3 uvt_m0 = intersectionRayPlane(S_m0, T_m0, a, ab, ac);
    vec4 C_m0 = texture2D(u_shadTex, v_shadUV + v_shadWH*vec2(uvt_m0.x, 1-uvt_m0.y));
    vec4 Dmap_m0 = texture2D(u_shadTex, v_shadDE + v_shadWH*vec2(uvt_m0.x, 1-uvt_m0.y));
    float d_m0 = zMod(Dmap_m0.r);
    uvt_m0.z = tOfIRP(S_m0, T_m0, a+vec3(0,0,d_m0), ab, ac);
//    float t_mod_m0 = t_mod_base-d_m0/abs(S_m0.z-T_m0.z);
    if (!intersectionValid(uvt_m0, 0)) C_m0 = vec4(0);

    vec3 S_mm = S;
    vec3 T_mm = T;
    vec3 uvt_mm = intersectionRayPlane(S_mm, T_mm, a, ab, ac);
    vec4 C_mm = texture2D(u_shadTex, v_shadUV + v_shadWH*vec2(uvt_mm.x, 1-uvt_mm.y));
    vec4 Dmap_mm = texture2D(u_shadTex, v_shadDE + v_shadWH*vec2(uvt_mm.x, 1-uvt_mm.y));
    float d_mm = zMod(Dmap_mm.r);
    uvt_mm.z = tOfIRP(S_mm, T_mm, a+vec3(0,0,d_mm), ab, ac);
//    float t_mod_mm = t_mod_base-d_mm/abs(S_mm.z-T_mm.z);
    if (!intersectionValid(uvt_mm, 0)) C_mm = vec4(0);

    vec3 S_m1 = vec3(S.x, S.y+w_r, S.z);
    vec3 T_m1 = vec3(T.x, T.y+T_r, T.z);
    vec3 uvt_m1 = intersectionRayPlane(S_m1, T_m1, a, ab, ac);
    vec4 C_m1 = texture2D(u_shadTex, v_shadUV + v_shadWH*vec2(uvt_m1.x, 1-uvt_m1.y));
    vec4 Dmap_m1 = texture2D(u_shadTex, v_shadDE + v_shadWH*vec2(uvt_m1.x, 1-uvt_m1.y));
    float d_m1 = zMod(Dmap_m1.r);
    uvt_m1.z = tOfIRP(S_m1, T_m1, a+vec3(0,0,d_m1), ab, ac);
//    float t_mod_m1 = t_mod_base-d_m1/abs(S_m1.z-T_m1.z);
    if (!intersectionValid(uvt_m1, 0)) C_m1 = vec4(0);


    vec3 S_10 = vec3(S.x+w_r, S.y-w_r, S.z);
    vec3 T_10 = vec3(T.x+T_r, T.y-T_r, T.z);
    vec3 uvt_10 = intersectionRayPlane(S_10, T_10, a, ab, ac);
    vec4 C_10 = texture2D(u_shadTex, v_shadUV + v_shadWH*vec2(uvt_10.x, 1-uvt_10.y));
    vec4 Dmap_10 = texture2D(u_shadTex, v_shadDE + v_shadWH*vec2(uvt_10.x, 1-uvt_10.y));
    float d_10 = zMod(Dmap_10.r);
    uvt_10.z = tOfIRP(S_10, T_10, a+vec3(0,0,d_10), ab, ac);
//    float t_mod_10 = t_mod_base-d_10/abs(S_10.z-T_10.z);
    if (!intersectionValid(uvt_10, 0)) C_10 = vec4(0);

    vec3 S_1m = vec3(S.x+w_r, S.y    , S.z);
    vec3 T_1m = vec3(T.x+T_r, T.y    , T.z);
    vec3 uvt_1m = intersectionRayPlane(S_1m, T_1m, a, ab, ac);
    vec4 C_1m = texture2D(u_shadTex, v_shadUV + v_shadWH*vec2(uvt_1m.x, 1-uvt_1m.y));
    vec4 Dmap_1m = texture2D(u_shadTex, v_shadDE + v_shadWH*vec2(uvt_1m.x, 1-uvt_1m.y));
    float d_1m = zMod(Dmap_1m.r);
    uvt_1m.z = tOfIRP(S_1m, T_1m, a+vec3(0,0,d_1m), ab, ac);
//    float t_mod_1m = t_mod_base-d_1m/abs(S_1m.z-T_1m.z);
    if (!intersectionValid(uvt_1m, 0)) C_1m = vec4(0);

    vec3 S_11 = vec3(S.x+w_r, S.y+w_r, S.z);
    vec3 T_11 = vec3(T.x+T_r, T.y+T_r, T.z);
    vec3 uvt_11 = intersectionRayPlane(S_11, T_11, a, ab, ac);
    vec4 C_11 = texture2D(u_shadTex, v_shadUV + v_shadWH*vec2(uvt_11.x, 1-uvt_11.y));
    vec4 Dmap_11 = texture2D(u_shadTex, v_shadDE + v_shadWH*vec2(uvt_11.x, 1-uvt_11.y));
    float d_11 = zMod(Dmap_11.r);
    uvt_11.z = tOfIRP(S_11, T_11, a+vec3(0,0,d_11), ab, ac);
//    float t_mod_11 = t_mod_base-d_11/abs(S_11.z-T_11.z);
    if (!intersectionValid(uvt_11, 0)) C_11 = vec4(0);


//    vec3 S_m0 = vec3(S.x, S.y-w_r, S.z);
//    vec4 C_m0 = shadowFromS(S_m0, T, a, ab, ac, t_mod);
//    vec3 S_mm = vec3(S.x, S.y , S.z);
//    vec4 C_mm = shadowFromS(S_mm, T, a, ab, ac, t_mod);
//    vec3 S_m1 = vec3(S.x, S.y+w_r, S.z);
//    vec4 C_m1 = shadowFromS(S_m1, T, a, ab, ac, t_mod);

//    vec3 S_10 = vec3(S.x+w_r, S.y-w_r, S.z);
//    vec4 C_10 = shadowFromS(S_10, T, a, ab, ac, t_mod);
//    vec3 S_1m = vec3(S.x+w_r, S.y , S.z);
//    vec4 C_1m = shadowFromS(S_1m, T, a, ab, ac, t_mod);
//    vec3 S_11 = vec3(S.x+w_r, S.y+w_r, S.z);
//    vec4 C_11 = shadowFromS(S_11, T, a, ab, ac, t_mod);


    //    shadColor *= 0.25;
    vec4 shadColor = (
    C_01 + C_m1 + C_11 +
    C_0m + C_mm + C_1m +
    C_00 + C_m0 + C_10
    )*shadFudge/9.0;

    gl_FragColor = vec4(shadColor.aaaa);
//    gl_FragColor = vec4(-(S-T).zzz/40.0, 0);
//    gl_FragColor = vec4(vec3(-d_mm/depth_mod_max), 0);
//    gl_FragColor = vec4(vec3(t_mod_mm*20), 0);
//    gl_FragColor = vec4(vec3(uvt_mm.z+t_mod_mm)*2-1.5, 0);


    vec2 r_redundant = u_shadPxDims*0.25;
//    float fu0 = u_shadPxDims.x*floor(u*u_shadTexWidth);
//    float fv0 = u_shadPxDims.y*floor(v*u_shadTexHeight);
}



