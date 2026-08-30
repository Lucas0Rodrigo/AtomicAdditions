#version 150

in vec3 LocalPosition;
in vec4 VertexColor;

uniform float GameTime;

out vec4 fragColor;

/*
 * ------------------------------------------------------------
 * HASH
 * ------------------------------------------------------------
 */

float hash(vec3 p) {

    return fract(
            sin(
                    dot(
                            p,
                            vec3(
                                    127.1,
                                    311.7,
                                    74.7
                            )
                    )
            )
            * 43758.5453
    );
}

/*
 * ------------------------------------------------------------
 * VALUE NOISE
 * ------------------------------------------------------------
 */

float noise(vec3 p) {

    vec3 i =
    floor(p);

    vec3 f =
    fract(p);

    f =
    f * f
    * (
    3.0
    - 2.0 * f
    );

    float n000 =
    hash(i);

    float n100 =
    hash(
            i
            + vec3(
                    1.0,
                    0.0,
                    0.0
            )
    );

    float n010 =
    hash(
            i
            + vec3(
                    0.0,
                    1.0,
                    0.0
            )
    );

    float n110 =
    hash(
            i
            + vec3(
                    1.0,
                    1.0,
                    0.0
            )
    );

    float n001 =
    hash(
            i
            + vec3(
                    0.0,
                    0.0,
                    1.0
            )
    );

    float n101 =
    hash(
            i
            + vec3(
                    1.0,
                    0.0,
                    1.0
            )
    );

    float n011 =
    hash(
            i
            + vec3(
                    0.0,
                    1.0,
                    1.0
            )
    );

    float n111 =
    hash(
            i
            + vec3(
                    1.0,
                    1.0,
                    1.0
            )
    );

    float x00 =
    mix(
            n000,
            n100,
            f.x
    );

    float x10 =
    mix(
            n010,
            n110,
            f.x
    );

    float x01 =
    mix(
            n001,
            n101,
            f.x
    );

    float x11 =
    mix(
            n011,
            n111,
            f.x
    );

    float y0 =
    mix(
            x00,
            x10,
            f.y
    );

    float y1 =
    mix(
            x01,
            x11,
            f.y
    );

    return mix(
            y0,
            y1,
            f.z
    );
}

/*
 * ------------------------------------------------------------
 * FBM
 * ------------------------------------------------------------
 */

float fbm(vec3 p) {

    float value =
    0.0;

    float amplitude =
    0.5;

    for (int i = 0;
        i < 4;
    i++) {

        value +=
        noise(p)
        * amplitude;

        p *= 2.0;

        amplitude *= 0.5;
    }

    return value;
}

/*
 * ------------------------------------------------------------
 * MAIN
 * ------------------------------------------------------------
 */

void main() {

    /*
     * Movimento lento da névoa.
     */
    float time =
    GameTime * 0.015;

    vec3 p =
    LocalPosition * 3.2;

    /*
     * Faz a névoa "ferver".
     */
    p += vec3(
            time * 0.40,
            time * 0.20,
            -time * 0.30
    );

    /*
     * Ruído principal.
     */
    float cloud =
    fbm(p);

    /*
     * Segundo padrão de ruído para quebrar
     * as bordas.
     */
    float detail =
    fbm(
            p * 1.8
            + vec3(
                    5.2,
                    1.7,
                    3.8
            )
    );

    cloud =
    cloud * 0.75
    + detail * 0.25;

    /*
     * Contraste da nuvem.
     */
    cloud =
    smoothstep(
            0.40,
            0.72,
            cloud
    );

    /*
     * Pequena variação baseada na posição.
     */
    float edge =
    1.0
    - length(
            LocalPosition
    );

    edge =
    clamp(
            edge,
            0.0,
            1.0
    );

    /*
     * Quanto mais perto do centro da geometria,
     * mais forte.
     */
    float density =
    cloud
    * (
    0.35
    + edge
    * 0.65
    );

    /*
     * Cor levemente azulada/esbranquiçada.
     */
    vec3 fogColor =
    vec3(
            0.72,
            0.86,
            1.00
    );

    /*
     * Alpha final.
     */
    float alpha =
    density
    * VertexColor.a;

    /*
     * Suaviza a saída.
     */
    alpha =
    clamp(
            alpha,
            0.0,
            0.38
    );

    if (alpha < 0.01) {
        discard;
    }

    fragColor =
    vec4(
            fogColor,
            alpha
    );
}