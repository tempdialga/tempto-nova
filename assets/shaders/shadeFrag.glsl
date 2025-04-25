#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_dMapCoords;//Corresponds to the location on the depth map
varying vec2 v_shadCoords;
uniform sampler2D u_dMapTex;//Corresponds to the depth map
uniform sampler2D u_shadTex;//Corresponds to the shadow texture

uniform vec2 u_shadCoordOrigin;//origin coordinate on shadow texture
uniform vec2 u_shadCoordUV;//width and height of of the region of the shadow texture (This may be split into two vectors if for some reason)

varying vec3 v_a; //Location of point a, origin of shadow texture, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)
varying vec3 v_ab; //Vector from point a to b, corresponding to u on the
varying vec3 v_ac; //Vector
varying vec3 v_S; //Location of light source S, in depth map coordinates (x = screen[0-1], y = screen[0-1], z is pixels away from camera)
//uniform vec3 u_laS; //Vector from a to the light source (S - a)

void main()
{

    vec3 T = vec3(v_dMapCoords, 1/(texture2D(u_dMapTex, v_dMapCoords).r));//Coordinates of target
    vec3 laS = v_S - v_a;
    vec3 lST = T - laS - v_a; //(T - (S - a)) - a = (T-S+a-a) = Vector from light source S to target T

    vec3 nA = cross(v_ab, v_ac);//This value is reused, and coincides with the normal area vector to the parallelogram
    float det = -dot(lST,nA);

    float det_recip = 1/det;
    float t = det_recip*dot(nA, laS);
    float u = det_recip*dot(cross(v_ac, -lST), laS);
    float v = det_recip*dot(cross(-lST, v_ab), laS);

    //If the intersection with the plane lies within the parallelogram created by a, b and c, and it's in front of the source, sample the shadow texture
    if (t >= 0 && t <= 1 && u >= 0 && u <= 1 && v >= 0 && v <= 1) {
//        gl_fragColor = texture2D(u_shadTex, u_shadCoordOrigin + (u_shadCoordUV*vec2(u,v)));
        gl_FragColor = vec4(0);//Start by making it 0 to see if it works
    } else {
        gl_FragColor = vec4(1);
    }

    gl_FragColor = vec4(1,1,1,0.5);
}