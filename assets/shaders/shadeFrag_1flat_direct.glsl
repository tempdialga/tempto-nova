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


    vec3 S_mm = S;
    vec3 T_mm = T;
    vec3 uvt_mm = intersectionRayPlane(S_mm, T_mm, a, ab, ac);
    vec4 C_mm = vec4(1);
    if (!intersectionValid(uvt_mm, 0)) C_mm = vec4(0);


    //    shadColor *= 0.25;
//    vec4 shadColor = (
//    C_01 + C_m1 + C_11 +
//    C_0m + C_mm + C_1m +
//    C_00 + C_m0 + C_10
//    )*shadFudge/9.0;
    vec4 shadColor = C_mm;

    gl_FragColor = vec4(shadColor.aaaa);
    //    gl_FragColor = vec4(-(S-T).zzz/40.0, 0);
    //    gl_FragColor = vec4(vec3(-d_mm/depth_mod_max), 0);
    //    gl_FragColor = vec4(vec3(t_mod_mm*20), 0);
    //    gl_FragColor = vec4(vec3(uvt_mm.z+t_mod_mm)*2-1.5, 0);


    vec2 r_redundant = u_shadPxDims*0.25;
    //    float fu0 = u_shadPxDims.x*floor(u*u_shadTexWidth);
    //    float fv0 = u_shadPxDims.y*floor(v*u_shadTexHeight);
}



