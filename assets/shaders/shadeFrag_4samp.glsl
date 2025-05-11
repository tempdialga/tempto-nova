#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_dMapCoords;//Corresponds to the location on the depth map
varying vec2 v_shadUV;//origin coordinate on shadow texture
varying vec2 v_shadWH;//width and height of of the region of the shadow texture
varying vec2 v_shadDE;//origin coordinate on the shadow texture but which correspond to the depth texture instead of the normal


uniform sampler2D u_dMapTex;//Corresponds to the depth map
uniform sampler2D u_shadTex;//Corresponds to the shadow texture
uniform vec2 u_shadPxDims; //how wide/tall each pixel on u_shadTex is
uniform int u_shadTexWidth; //How many pixels wide u_shadTex is (recip of u_shadPxDims.x)
uniform int u_shadTexHeight; //How mnay puxels high u_shadTex is
uniform float u_elapsedTime; //Elapsed time in seconds (for debugging purposes)


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

    float r = (0.5+0.5*sin(3*u_elapsedTime))*0.7/32.0; //Impromptu radius of less than half a pixel on a 16x16 texture
    //Extreme coordinates of the light region
    float u0 = u-r, u1 = u+r;
    float v0 = v-r, v1 = v+r;
    //Cutoff factors, how much either extreme goes off the edge'
    //    float tu0 = 1-(max(u0,0)-u0)/(r*2), tu1 = 1-(u1-min(u1,1))/(r*2);
    //    float tv0 = (max(v0,0)-v0)/(r*2), tv1 = (v1-min(v1,1))/(r*2);

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

        float cu0 = max(u0,0);
        float cu1 = min(u1,1);
        float cv0 = max(v0,0);
        float cv1 = min(v1,1);

        //        u = clamp(u,0,1);
        //        float cu0 = max(u0,0);
        //        tu0 = 1-(cu0-u0)/(u1-u0);
        //        float cu1 = min(u1,1);
        //        tu0 = 1-(u1-cu1)/(u1-u0);
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

        //        vec4 shadColor = (
        //            shadCol00*tu0*tv0  +shadCol10*tu1*tv0 +
        //            shadCol01*tu0*tv1  +shadCol11*tu1*tv1
        //        );

        float shadFudge = 1.1f; //i.e. if there is already a shadow make it a little more opaque
        vec4 shadColor = shadFudge*(
        shadCol00/*+shadColm0*/+shadCol10+
//        shadCol0m+shadCol_m+shadCol1m+
        shadCol01/*+shadColm1*/+shadCol11
        )/4;
        //        vec4 shadColor = vec4((u_-u0)/du);



        //        float area_px = 1;
        vec2 r_redundant = u_shadPxDims*0.25;
        //        float u0 = shadCoord.x-r.x;
        float fu0 = u_shadPxDims.x*floor(u0*u_shadTexWidth);
        //        float tu0 = 0.5;
        //        float u1 = shadCoord.x+r.x;
        //        float tu1 = 1-tu0; //True for now when the shadow is exactly 1px, need to interpret this for more cases later
        //
        //        float v0 = shadCoord.y-r.y;
        float fv0 = u_shadPxDims.y*floor(v0*u_shadTexHeight);
        //        float tv0 = 0.5;
        //        float v1 = shadCoord.y+r.y;
        //        float tv1 = 1-tv0;
        //
        //        vec4 shadColor = (
        //            tu0*tv0*texture2D(u_shadTex, vec2(u0, v0))+
        //            tu1*tv0*texture2D(u_shadTex, vec2(u1, v0))+
        //            tu1*tv1*texture2D(u_shadTex, vec2(u1, v1))+
        //            tu0*tv1*texture2D(u_shadTex, vec2(u0, v1))
        //        )/(area_px);

        //        vec4 shadColor = (
        //        tu0*texture2D(u_shadTex, vec2(u0, v0))+
        //        tu1*texture2D(u_shadTex, vec2(u1, v0))
        //        )/(area_px);
        //        shadColor = (
        //            texture2D(u_shadTex, vec2(u0, v0))+
        //            texture2D(u_shadTex, vec2(u1, v0))+
        //            texture2D(u_shadTex, vec2(u1, v1))+
        //            texture2D(u_shadTex, vec2(u0, v1))
        //        )/4;


        //        vec4 shadColor = texture2D(u_shadTex, shadCoord);
        //        vec4 nextShadColor = texture2D(u_shadTex, shadCoord+u_shadPxDims);
        //        shadColor = 0.5*(shadColor+nextShadColor);

        gl_FragColor = vec4(vec3(shadColor.a), 0);
        //        gl_FragColor = vec4(vec3(1-shadColor.a),1);//Start by making it 0 to see if it works
    } else {
        gl_FragColor = vec4(1,1,1,1);
        gl_FragColor = vec4(0,0,0,0);
    }
    //    gl_FragColor = texture2D(u_dMapTex, gl_FragCoord.xy);
    //    float depth = (T.z-v_S.z);


    //gl_FragColor = vec4((T.xy-v_S.xy),1/depth,1);
    //    gl_FragColor = vec4(0.5,0,0.5,1);

}