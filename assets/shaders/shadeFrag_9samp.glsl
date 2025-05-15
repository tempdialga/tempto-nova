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
uniform float u_elapsedTime; //Elapsed time in seconds (for debugging purposes)


varying vec3 v_a; //Location of point a, origin of shadow texture, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)
varying vec3 v_ab; //Vector from point a to b, corresponding to u on the texture
varying vec3 v_ac; //Vector
varying vec3 v_S; //Location of light source S, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)
varying float v_R; //Radius of the body casting the light,

void main()
{

    vec4 dmap = texture2D(u_dMapTex, v_dMapCoords);
    vec3 w_T = vec3(v_dMapCoords, 1/dmap.r-1);//Coordinates of target (screeen coords)
    vec3 T = vec3(w_T.xy*u_viewDims, w_T.z);//Coordinates of target
    vec3 S = vec3(v_S.xy*u_viewDims, v_S.z);
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

    float px_size_u = u_shadPxDims.x / v_shadWH.x;//Pixel size in terms of u, i.e. u=this is 1 px right of u=0
    float px_size_v = u_shadPxDims.y / v_shadWH.y;
    float r_px = v_R; //Radius of the light caster

    float base_r_u = r_px * px_size_u; //How far on u the light extends out by (separate because theoretically someone might squish a shadow texture, but the same for now because god why would you do that)
    float base_r_v = r_px * px_size_v;
    vec3 ab_nor = normalize(ab);
    vec3 ac_nor = normalize(ac);

    //Ignore the part of ST parallel to each texture vector,
    vec3 ST_along_ab = ab_nor * dot(lST, ab_nor);
    vec3 ST_along_ac = ac_nor * dot(lST, ac_nor);
    vec3 ST_nor = normalize(lST);

    float r_u = base_r_u               // Base pixel radius if it was directly facing the light rays going to the target
    *pow(1/length(cross(normalize(lST-ST_along_ac),ab_nor)), 1)  // Extend so that it reaches that radius at its angular offset
    *(1-t)
    ;                          // If the shadow's right behind the caster, the difference isn't that much, whereas if the caster is right up against the source, it makes all the difference


    float r_v = base_r_v
    *pow(1/length(cross(normalize(lST-ST_along_ab),ac_nor)), 1)
    *(1-t)
    ;

    //Extreme coordinates of the light region
    float u0 = u-r_u, u1 = u+r_u;
    float v0 = v-r_v, v1 = v+r_v;

    vec2 shadCoord = v_shadUV + (v_shadWH*vec2(u,1-v));
    vec2 shadDepCoord = v_shadDE + (v_shadWH*vec2(u,1-v));
    vec4 shadColor = texture2D(u_shadTex, shadCoord);
    vec4 shadDep = texture2D(u_shadTex, shadDepCoord);

    float t_mod = (3*(shadDep.r-0.5))/abs(lST.z);
    t_mod = 0;

    //If the intersection with the plane lies within the parallelogram created by a, b and c, and it's in front of the source, sample the shadow texture
    float t_fudge = 0.0;
    float coord_fudge = 0.001;
    if (t+t_mod > t_fudge && t+t_mod < 1-t_fudge &&
    u1 >= 0-coord_fudge && u0 <= 1+coord_fudge &&
    v1 >= 0-coord_fudge && v0 <= 1+coord_fudge) {

        float reg_px_width = 16;
        float reg_px_height = 16;

        vec2 shadCoord0m = v_shadUV + (v_shadWH*vec2(u0,1-v));//Shadow coords right in the middle
        vec4 shadCol0m = texture2D(u_shadTex, shadCoord0m);
        if (u0 < 0 ||
        v < 0 || v > 1) shadCol0m = vec4(0);
        vec2 shadCoord_m = v_shadUV + (v_shadWH*vec2(u,1-v));//Shadow coords right in the middle
        vec4 shadCol_m = texture2D(u_shadTex, shadCoord_m);
        if (u < 0 || u > 1 ||
        v < 0 || v > 1) shadCol_m = vec4(0);
        vec2 shadCoord1m = v_shadUV + (v_shadWH*vec2(u1,1-v));//Shadow coords right in the middle
        vec4 shadCol1m = texture2D(u_shadTex, shadCoord1m);
        if (u1 > 1 ||
        v < 0 || v > 1) shadCol1m = vec4(0);


        vec2 shadCoord00 = v_shadUV + (v_shadWH*vec2(u0,1-v0));
        vec4 shadCol00 = texture2D(u_shadTex, shadCoord00);
        if (u0 < 0 || v0 < 0) shadCol00 = vec4(0);
        vec2 shadCoordm0 = v_shadUV + (v_shadWH*vec2(u,1-v0));
        vec4 shadColm0 = texture2D(u_shadTex, shadCoordm0);
        if (u < 0 || u > 1 ||
        v0 < 0) shadColm0 = vec4(0);
        vec2 shadCoord10 = v_shadUV + (v_shadWH*vec2(u1,1-v0));
        vec4 shadCol10 = texture2D(u_shadTex, shadCoord10);
        if (u1 > 1 || v0 < 0) shadCol10 = vec4(0);

        vec2 shadCoord01 = v_shadUV + (v_shadWH*vec2(u0,1-v1));
        vec4 shadCol01 = texture2D(u_shadTex, shadCoord01);
        if (u0 < 0 || v1 > 1) shadCol01 = vec4(0);
        vec2 shadCoordm1 = v_shadUV + (v_shadWH*vec2(u,1-v1));
        vec4 shadColm1 = texture2D(u_shadTex, shadCoordm1);
        if (u < 0 || u > 1 ||
        v0 < 0) shadColm1 = vec4(0);
        vec2 shadCoord11 = v_shadUV + (v_shadWH*vec2(u1,1-v1));
        vec4 shadCol11 = texture2D(u_shadTex, shadCoord11);
        if (u1 > 1 || v1 > 1) shadCol11 = vec4(0);


        float shadFudge = 1.1f; //i.e. if there is already a shadow make it a little more opaque
        vec4 shadColor = shadFudge*(
        shadCol00+shadColm0+shadCol10+
        shadCol0m+shadCol_m+shadCol1m+
        shadCol01+shadColm1+shadCol11
        )/9;

        gl_FragColor = vec4(vec3(shadColor.a),0);
    } else {
        gl_FragColor = vec4(0,0,0,0);
    }

}