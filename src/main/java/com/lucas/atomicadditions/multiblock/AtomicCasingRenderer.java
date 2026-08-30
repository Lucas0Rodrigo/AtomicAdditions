package com.lucas.atomicadditions.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import mekanism.client.render.MekanismRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class AtomicCasingRenderer
        implements BlockEntityRenderer<AtomicCasingBlockEntity> {

    /*
     * ============================================================
     * ESFERAS
     * ============================================================
     */

    private static final float SPHERE_RADIUS =
            0.285F;

    /*
     * Distância das esferas em relação ao centro
     * da órbita.
     */
    private static final float ORBIT_RADIUS =
            0.62F;

    /*
     * E1 e E2 ficam exatamente opostas.
     */
    private static final float ORBIT_PHASE_OFFSET =
            Mth.PI;

    /*
     * Oscilação vertical.
     */
    private static final float IDLE_BOB_AMPLITUDE =
            0.11F;

    private static final float IDLE_BOB_SPEED =
            0.055F;

    /*
     * Velocidade máxima.
     *
     * 1.22 = 100% da energia.
     */
    private static final float MAX_ORBIT_SPEED =
            1.22F;

    /*
     * Rotação própria.
     */
    private static final float MAX_SPIN_SPEED =
            14.0F;

    /*
     * ============================================================
     * NÚCLEO INTERNO
     * ============================================================
     */

    /*
     * Núcleo menor que a esfera principal.
     */
    private static final float CORE_RADIUS =
            SPHERE_RADIUS * 0.48F;

    /*
     * 60% opaco.
     */
    private static final float CORE_ALPHA =
            0.60F;

    /*
     * ============================================================
     * RASTRO
     * ============================================================
     */

    private static final int TRAIL_SEGMENTS =
            9;

    private static final float TRAIL_STEP_ANGLE =
            0.105F;

    private static final float TRAIL_RADIUS =
            0.030F;

    /*
     * ============================================================
     * ATMOSFERA ESFÉRICA
     * ============================================================
     */

    /*
     * Maior que a esfera principal.
     */
    private static final float ATMOSPHERE_RADIUS =
            0.46F;

    /*
     * Número máximo de pequenos raios sobre a esfera.
     */
    private static final int ATMOSPHERE_ARCS =
            26;

    /*
     * ============================================================
     * FLASHES ALEATÓRIOS
     * ============================================================
     */

    private static final int RANDOM_FLASHES =
            18;

    /*
     * ============================================================
     * ENERGIA
     * ============================================================
     */

    private static final double MAX_REFERENCE_ENERGY =
            80_000_000D;

    /*
     * ============================================================
     * GEOMETRIA
     * ============================================================
     */

    private static final int SPHERE_SEGMENTS =
            14;

    private static final int SPHERE_RINGS =
            8;

    /*
     * ============================================================
     * ESTADO DA ÓRBITA
     * ============================================================
     */

    private float orbitAngle =
            0F;

    private long lastOrbitTick =
            -1;

    public AtomicCasingRenderer(
            BlockEntityRendererProvider.Context context
    ) {
    }

    @Override
    public void render(
            AtomicCasingBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        AtomicMultiblockData multiblock =
                blockEntity.getMultiblock();

        if (multiblock == null
                || !blockEntity.isMaster()
                || !multiblock.isFormed()) {
            return;
        }

        /*
         * ========================================================
         * GASES
         * ========================================================
         */

        List<Integer> colors =
                getActiveInputColors(
                        multiblock
                );

        if (colors.isEmpty()) {
            return;
        }

        /*
         * ========================================================
         * CENTRO
         * ========================================================
         */

        poseStack.pushPose();

        double centerX =
                multiblock.getMinPos().getX()
                        + multiblock.width() / 2D
                        - blockEntity.getBlockPos().getX();

        double centerY =
                multiblock.getMinPos().getY()
                        + multiblock.height() / 2D
                        - blockEntity.getBlockPos().getY();

        double centerZ =
                multiblock.getMinPos().getZ()
                        + multiblock.length() / 2D
                        - blockEntity.getBlockPos().getZ();

        poseStack.translate(
                centerX,
                centerY,
                centerZ
        );

        double gameTime =
                blockEntity.getLevel().getGameTime()
                        + partialTick;

        /*
         * ========================================================
         * ENERGIA
         * ========================================================
         */

        float energyFactor =
                (float) Math.min(
                        1D,
                        multiblock.renderEnergy
                                / MAX_REFERENCE_ENERGY
                );

        boolean active =
                multiblock.renderProcessed > 0;

        /*
         * ========================================================
         * ÓRBITA
         * ========================================================
         */

        float orbitFactor =
                active
                        ? energyFactor
                        : 0F;

        updateOrbit(
                blockEntity,
                orbitFactor,
                partialTick
        );

        /*
         * Oscilação vertical.
         */
        float idleBob =
                (float) Math.sin(
                        gameTime * IDLE_BOB_SPEED
                ) * IDLE_BOB_AMPLITUDE;

        /*
         * Rotação própria.
         */
        float spin =
                active
                        ? (float) (
                        gameTime
                                * MAX_SPIN_SPEED
                                * energyFactor
                )
                        : 0F;

        /*
         * ========================================================
         * BUFFER
         * ========================================================
         */

        VertexConsumer consumer =
                buffer.getBuffer(
                        MekanismRenderType.MEK_LIGHTNING
                );

        /*
         * ========================================================
         * ESFERAS
         * ========================================================
         */

        for (int index = 0;
             index < colors.size();
             index++) {

            int color =
                    colors.get(index);

            /*
             * E1 = ângulo atual
             *
             * E2 = exatamente 180 graus afastada.
             */
            float localAngle =
                    orbitAngle;

            if (colors.size() > 1
                    && index == 1) {

                localAngle +=
                        ORBIT_PHASE_OFFSET;
            }

            /*
             * Quanto maior a energia,
             * menor fica levemente a órbita.
             */
            float effectiveOrbitRadius =
                    ORBIT_RADIUS
                            * (
                            1.0F
                                    - 0.25F
                                    * energyFactor
                    );

            float orbitX =
                    Mth.cos(localAngle)
                            * effectiveOrbitRadius;

            float orbitZ =
                    Mth.sin(localAngle)
                            * effectiveOrbitRadius;

            float orbitY =
                    idleBob;

            poseStack.pushPose();

            poseStack.translate(
                    orbitX,
                    orbitY,
                    orbitZ
            );

            /*
             * Rotação da esfera.
             */
            if (active
                    && energyFactor > 0F) {

                poseStack.mulPose(
                        Axis.YP.rotationDegrees(
                                spin
                        )
                );

                poseStack.mulPose(
                        Axis.XP.rotationDegrees(
                                spin * 0.47F
                        )
                );
            }

            /*
             * Esfera principal.
             */
            renderSphere(
                    poseStack,
                    consumer,
                    color,
                    SPHERE_RADIUS,
                    0.92F,
                    spin
            );

            /*
             * ====================================================
             * NÚCLEO INTERNO
             * ====================================================
             *
             * Alterna rapidamente entre:
             *
             * cor do elemento
             *        e
             * branco
             *
             * 60% de opacidade.
             */

            int coreColor =
                    isCoreWhite(
                            gameTime,
                            index
                    )
                            ? 0xFFFFFF
                            : color;

            renderSphere(
                    poseStack,
                    consumer,
                    coreColor,
                    CORE_RADIUS,
                    CORE_ALPHA,
                    0F
            );

            /*
             * ====================================================
             * RASTRO
             * ====================================================
             */

            if (active
                    && energyFactor > 0F) {

                renderTrail(
                        poseStack,
                        consumer,
                        color,
                        localAngle,
                        effectiveOrbitRadius,
                        energyFactor
                );
            }

            /*
             * ====================================================
             * ATMOSFERA
             * ====================================================
             */

            if (active
                    && energyFactor > 0F) {

                renderAtmosphere(
                        poseStack,
                        consumer,
                        energyFactor,
                        gameTime,
                        index
                );
            }

            poseStack.popPose();
        }

        /*
         * ========================================================
         * FLASHES ESPALHADOS PELO AMR
         * ========================================================
         */

        if (active
                && energyFactor > 0F) {

            renderRandomFlashes(
                    poseStack,
                    consumer,
                    energyFactor,
                    gameTime
            );
        }

        poseStack.popPose();
    }

    /*
     * ============================================================
     * ÓRBITA
     * ============================================================
     */

    private void updateOrbit(
            AtomicCasingBlockEntity blockEntity,
            float orbitFactor,
            float partialTick
    ) {
        long currentTick =
                blockEntity.getLevel()
                        .getGameTime();

        if (lastOrbitTick < 0) {

            lastOrbitTick =
                    currentTick;

            return;
        }

        long deltaTicks =
                currentTick
                        - lastOrbitTick;

        if (deltaTicks < 0) {

            lastOrbitTick =
                    currentTick;

            return;
        }

        deltaTicks =
                Math.min(
                        deltaTicks,
                        5
                );

        /*
         * O ângulo só avança quando existe processamento.
         *
         * A velocidade é diretamente proporcional
         * à energia utilizada.
         */
        if (orbitFactor > 0F) {

            orbitAngle +=
                    (float) deltaTicks
                            * MAX_ORBIT_SPEED
                            * orbitFactor;

            if (partialTick > 0F) {

                orbitAngle +=
                        partialTick
                                * MAX_ORBIT_SPEED
                                * orbitFactor
                                * 0.016F;
            }
        }

        orbitAngle =
                orbitAngle
                        % Mth.TWO_PI;

        if (orbitAngle < 0F) {

            orbitAngle +=
                    Mth.TWO_PI;
        }

        lastOrbitTick =
                currentTick;
    }

    /*
     * ============================================================
     * GASES
     * ============================================================
     */

    private List<Integer> getActiveInputColors(
            AtomicMultiblockData multiblock
    ) {
        List<Integer> colors =
                new ArrayList<>(2);

        if (multiblock.renderInput1Color >= 0) {

            colors.add(
                    multiblock.renderInput1Color
            );
        }

        if (multiblock.renderInput2Color >= 0) {

            /*
             * Não cria uma segunda esfera se os dois inputs
             * possuírem exatamente a mesma cor.
             */
            if (colors.isEmpty()
                    || multiblock.renderInput2Color
                    != colors.get(0)) {

                colors.add(
                        multiblock.renderInput2Color
                );
            }
        }

        return colors;
    }

    /*
     * ============================================================
     * NÚCLEO
     * ============================================================
     */

    private boolean isCoreWhite(
            double gameTime,
            int sphereIndex
    ) {
        /*
         * Alternância muito rápida.
         *
         * 12 mudanças por tick.
         *
         * A segunda esfera fica ligeiramente
         * defasada da primeira.
         */
        long phase =
                (long) Math.floor(
                        gameTime * 12.0
                                + sphereIndex * 3
                );

        return (phase & 1L) == 0L;
    }

    /*
     * ============================================================
     * RASTRO
     * ============================================================
     */

    private void renderTrail(
            PoseStack poseStack,
            VertexConsumer consumer,
            int color,
            float currentAngle,
            float orbitRadius,
            float energyFactor
    ) {
        Matrix4f matrix =
                poseStack.last().pose();

        Vec3 previous =
                getOrbitPosition(
                        currentAngle,
                        orbitRadius
                );

        /*
         * Quanto mais energia,
         * maior e mais forte o rastro.
         */
        float trailScale =
                0.45F
                        + energyFactor
                        * 0.75F;

        for (int i = 1;
             i <= TRAIL_SEGMENTS;
             i++) {

            float distance =
                    i
                            * TRAIL_STEP_ANGLE
                            * trailScale;

            float angle =
                    currentAngle
                            - distance;

            Vec3 point =
                    getOrbitPosition(
                            angle,
                            orbitRadius
                    );

            /*
             * A parte traseira desaparece gradualmente.
             */
            float fade =
                    1.0F
                            - (
                            i
                                    / (float) (
                                    TRAIL_SEGMENTS
                            )
                    );

            fade *=
                    energyFactor;

            float radius =
                    TRAIL_RADIUS
                            * fade
                            * (
                            0.35F
                                    + energyFactor
                                    * 0.65F
                    );

            float alpha =
                    fade
                            * (
                            0.25F
                                    + energyFactor
                                    * 0.75F
                    );

            drawLightningTube(
                    matrix,
                    consumer,
                    (float) previous.x,
                    (float) previous.y,
                    (float) previous.z,
                    (float) point.x,
                    (float) point.y,
                    (float) point.z,
                    colorRed(color),
                    colorGreen(color),
                    colorBlue(color),
                    alpha,
                    Math.max(
                            0.006F,
                            radius
                    )
            );

            previous =
                    point;
        }
    }

    private Vec3 getOrbitPosition(
            float angle,
            float radius
    ) {
        return new Vec3(
                Mth.cos(angle) * radius,
                0,
                Mth.sin(angle) * radius
        );
    }

    /*
     * ============================================================
     * ATMOSFERA ESFÉRICA
     * ============================================================
     *
     * A atmosfera é uma esfera maior que a principal.
     *
     * Não é um anel plano.
     *
     * Cada pequeno raio é colocado sobre a superfície
     * tridimensional da esfera.
     */

    private void renderAtmosphere(
            PoseStack poseStack,
            VertexConsumer consumer,
            float energyFactor,
            double gameTime,
            int sphereIndex
    ) {
        Matrix4f matrix =
                poseStack.last().pose();

        /*
         * Maior que a esfera principal e com pequena separação.
         */
        float radius =
                ATMOSPHERE_RADIUS
                        + energyFactor * 0.045F;

        /*
         * Alguns raios extras conforme a energia.
         */
        int arcCount =
                10
                        + Math.round(
                        energyFactor
                                * (
                                ATMOSPHERE_ARCS
                                        - 10
                        )
                );

        /*
         * Cada arco possui sua própria chance de piscar.
         */
        for (int i = 0;
             i < arcCount;
             i++) {

            /*
             * Identificador estável do arco.
             */
            double seed =
                    sphereIndex * 73.71
                            + i * 19.37;

            /*
             * Posição pseudoaleatória na esfera.
             */
            double latitude =
                    (
                            pseudoRandom(
                                    seed,
                                    0
                            ) * 2.0
                                    - 1.0
                    );

            double longitude =
                    pseudoRandom(
                            seed,
                            1
                    )
                            * Math.PI
                            * 2.0;

            /*
             * Pequena extensão angular.
             */
            double arcLength =
                    0.10
                            + pseudoRandom(
                            seed,
                            2
                    ) * (
                            0.12
                                    + energyFactor
                                    * 0.10
                    );

            /*
             * Pisca independente dos demais.
             */
            float flash =
                    lightningFlicker(
                            gameTime,
                            seed,
                            0.85F
                    );

            if (flash <= 0F) {
                continue;
            }

            Vec3 p1 =
                    spherePoint(
                            radius,
                            latitude,
                            longitude
                    );

            Vec3 p2 =
                    spherePoint(
                            radius,
                            latitude,
                            longitude
                                    + arcLength
                    );

            /*
             * Pequeno terceiro ponto para fazer o raio
             * parecer irregular.
             */
            double middleLongitude =
                    longitude
                            + arcLength
                            * 0.5;

            Vec3 middle =
                    spherePoint(
                            radius
                                    + 0.008F
                                    * (float) Math.sin(
                                    gameTime
                                            * 0.7
                                            + seed
                            ),
                            latitude,
                            middleLongitude
                    );

            /*
             * Raio fino, porém 3D.
             */
            float thickness =
                    0.013F
                            + energyFactor * 0.020F;

            float alpha =
                    flash
                            * (
                            0.35F
                                    + energyFactor
                                    * 0.60F
                    );

            drawLightningTube(
                    matrix,
                    consumer,
                    (float) p1.x,
                    (float) p1.y,
                    (float) p1.z,
                    (float) middle.x,
                    (float) middle.y,
                    (float) middle.z,
                    1F,
                    1F,
                    1F,
                    alpha,
                    thickness
            );

            drawLightningTube(
                    matrix,
                    consumer,
                    (float) middle.x,
                    (float) middle.y,
                    (float) middle.z,
                    (float) p2.x,
                    (float) p2.y,
                    (float) p2.z,
                    1F,
                    1F,
                    1F,
                    alpha,
                    thickness
            );
        }
    }

    private Vec3 spherePoint(
            float radius,
            double latitude,
            double longitude
    ) {
        double horizontal =
                Math.sqrt(
                        Math.max(
                                0D,
                                1D
                                        - latitude
                                        * latitude
                        )
                );

        return new Vec3(
                Math.cos(longitude)
                        * horizontal
                        * radius,
                latitude
                        * radius,
                Math.sin(longitude)
                        * horizontal
                        * radius
        );
    }

    /*
     * ============================================================
     * FLASHES ALEATÓRIOS
     * ============================================================
     *
     * Pequenos relâmpagos que aparecem em pontos diferentes
     * do interior do AMR e desaparecem rapidamente.
     */

    private void renderRandomFlashes(
            PoseStack poseStack,
            VertexConsumer consumer,
            float energyFactor,
            double gameTime
    ) {
        Matrix4f matrix =
                poseStack.last().pose();

        /*
         * Cada flash possui seu próprio ciclo.
         */
        for (int i = 0;
             i < RANDOM_FLASHES;
             i++) {

            double seed =
                    i * 47.193;

            /*
             * Frequência individual.
             */
            double frequency =
                    0.010
                            + pseudoRandom(
                            seed,
                            3
                    ) * 0.018;

            /*
             * Ciclo em que estamos.
             */
            long cycle =
                    (long) Math.floor(
                            gameTime
                                    * frequency
                    );

            /*
             * Momento do ciclo.
             */
            double cycleProgress =
                    gameTime
                            * frequency
                            - cycle;

            /*
             * O flash ocupa somente uma pequena parte
             * do ciclo.
             */
            if (cycleProgress > 0.10) {
                continue;
            }

            /*
             * Nem todo ciclo necessariamente gera flash.
             */
            if (pseudoRandom(
                    seed,
                    cycle
                            + 100
            ) > 0.58) {
                continue;
            }

            /*
             * Fade rápido.
             */
            float strength =
                    (float) (
                            1.0
                                    - cycleProgress
                                    / 0.10
                    );

            /*
             * Posição pseudoaleatória dentro do AMR.
             *
             * Mantemos uma margem para não atravessar
             * visualmente o casing.
             */
            float x =
                    -2.45F
                            + (float) pseudoRandom(
                            seed,
                            cycle
                                    + 1
                    ) * 4.90F;

            float y =
                    -2.45F
                            + (float) pseudoRandom(
                            seed,
                            cycle
                                    + 2
                    ) * 4.90F;

            float z =
                    -2.45F
                            + (float) pseudoRandom(
                            seed,
                            cycle
                                    + 3
                    ) * 4.90F;

            /*
             * Direção aleatória.
             */
            double angleA =
                    pseudoRandom(
                            seed,
                            cycle
                                    + 4
                    ) * Math.PI * 2;

            double angleB =
                    (
                            pseudoRandom(
                                    seed,
                                    cycle
                                            + 5
                            ) * 2.0
                                    - 1.0
                    ) * Math.PI * 0.5;

            Vec3 direction =
                    new Vec3(
                            Math.cos(angleA)
                                    * Math.cos(angleB),
                            Math.sin(angleB),
                            Math.sin(angleA)
                                    * Math.cos(angleB)
                    ).normalize();

            /*
             * Flash curto.
             */
            float length =
                    0.18F
                            + energyFactor
                            * 0.35F;

            Vec3 start =
                    new Vec3(
                            x,
                            y,
                            z
                    );

            Vec3 end =
                    start.add(
                            direction.scale(
                                    length
                            )
                    );

            float thickness =
                    0.015F
                            + energyFactor
                            * 0.025F;

            drawLightningTube(
                    matrix,
                    consumer,
                    (float) start.x,
                    (float) start.y,
                    (float) start.z,
                    (float) end.x,
                    (float) end.y,
                    (float) end.z,
                    1F,
                    1F,
                    1F,
                    strength
                            * (
                            0.55F
                                    + energyFactor
                                    * 0.45F
                    ),
                    thickness
            );
        }
    }

    /*
     * ============================================================
     * PSEUDO-RANDOM DETERMINÍSTICO
     * ============================================================
     *
     * Evita criar Random a cada frame.
     */

    private double pseudoRandom(
            double seed,
            double salt
    ) {
        double value =
                Math.sin(
                        seed * 12.9898
                                + salt * 78.233
                )
                        * 43758.5453;

        return value
                - Math.floor(value);
    }

    /*
     * ============================================================
     * FLICKER DOS RAIOS
     * ============================================================
     */

    private float lightningFlicker(
            double gameTime,
            double seed,
            float frequency
    ) {
        /*
         * Divide o tempo em pequenas janelas.
         */
        long bucket =
                (long) Math.floor(
                        gameTime
                                * frequency
                );

        double position =
                gameTime
                        * frequency
                        - bucket;

        /*
         * Chance de o raio estar ativo nessa janela.
         */
        double chance =
                pseudoRandom(
                        seed,
                        bucket
                                + 17
                );

        if (chance < 0.48) {
            return 0F;
        }

        /*
         * Flash extremamente rápido.
         */
        if (position > 0.26) {
            return 0F;
        }

        return (float) (
                1.0
                        - position
                        / 0.26
        );
    }

    /*
     * ============================================================
     * ESFERA
     * ============================================================
     */

    private void renderSphere(
            PoseStack poseStack,
            VertexConsumer consumer,
            int color,
            float radius,
            float alpha,
            float rotationDegrees
    ) {
        float red =
                colorRed(color);

        float green =
                colorGreen(color);

        float blue =
                colorBlue(color);

        float rotationRadians =
                rotationDegrees
                        * Mth.DEG_TO_RAD;

        Matrix4f matrix =
                poseStack.last().pose();

        for (int ring = 0;
             ring < SPHERE_RINGS;
             ring++) {

            float theta1 =
                    (float) Math.PI
                            * ring
                            / SPHERE_RINGS;

            float theta2 =
                    (float) Math.PI
                            * (ring + 1)
                            / SPHERE_RINGS;

            float y1 =
                    Mth.cos(theta1)
                            * radius;

            float y2 =
                    Mth.cos(theta2)
                            * radius;

            float ringRadius1 =
                    Mth.sin(theta1)
                            * radius;

            float ringRadius2 =
                    Mth.sin(theta2)
                            * radius;

            for (int segment = 0;
                 segment < SPHERE_SEGMENTS;
                 segment++) {

                float phi1 =
                        (float) (
                                2 * Math.PI
                                        * segment
                                        / SPHERE_SEGMENTS
                        );

                float phi2 =
                        (float) (
                                2 * Math.PI
                                        * (segment + 1)
                                        / SPHERE_SEGMENTS
                        );

                float phiCenter =
                        (phi1 + phi2)
                                * 0.5F;

                /*
                 * Destaque móvel.
                 */
                float highlight =
                        Math.max(
                                0F,
                                Mth.cos(
                                        phiCenter
                                                - rotationRadians
                                )
                        );

                float shade =
                        0.72F
                                + 0.28F
                                * highlight;

                float shadedRed =
                        red * shade;

                float shadedGreen =
                        green * shade;

                float shadedBlue =
                        blue * shade;

                addVertex(
                        consumer,
                        matrix,
                        Mth.cos(phi1)
                                * ringRadius1,
                        y1,
                        Mth.sin(phi1)
                                * ringRadius1,
                        shadedRed,
                        shadedGreen,
                        shadedBlue,
                        alpha
                );

                addVertex(
                        consumer,
                        matrix,
                        Mth.cos(phi1)
                                * ringRadius2,
                        y2,
                        Mth.sin(phi1)
                                * ringRadius2,
                        shadedRed,
                        shadedGreen,
                        shadedBlue,
                        alpha
                );

                addVertex(
                        consumer,
                        matrix,
                        Mth.cos(phi2)
                                * ringRadius2,
                        y2,
                        Mth.sin(phi2)
                                * ringRadius2,
                        shadedRed,
                        shadedGreen,
                        shadedBlue,
                        alpha
                );

                addVertex(
                        consumer,
                        matrix,
                        Mth.cos(phi2)
                                * ringRadius1,
                        y1,
                        Mth.sin(phi2)
                                * ringRadius1,
                        shadedRed,
                        shadedGreen,
                        shadedBlue,
                        alpha
                );
            }
        }
    }

    /*
     * ============================================================
     * TUBO DE LIGHTNING 3D
     * ============================================================
     *
     * O antigo drawSegment() era essencialmente uma superfície
     * 2D. Este método cria uma seção hexagonal, portanto o efeito
     * mantém volume e não desaparece dependendo do ângulo.
     */

    private void drawLightningTube(
            Matrix4f matrix,
            VertexConsumer consumer,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float red,
            float green,
            float blue,
            float alpha,
            float radius
    ) {
        Vec3 direction =
                new Vec3(
                        x2 - x1,
                        y2 - y1,
                        z2 - z1
                );

        if (direction.lengthSqr()
                < 0.000001D) {
            return;
        }

        direction =
                direction.normalize();

        Vec3 reference =
                Math.abs(direction.y) < 0.9
                        ? new Vec3(
                        0,
                        1,
                        0
                )
                        : new Vec3(
                        1,
                        0,
                        0
                );

        Vec3 side =
                direction.cross(
                        reference
                ).normalize();

        Vec3 side2 =
                direction.cross(
                        side
                ).normalize();

        /*
         * Hexágono é suficiente para dar volume
         * sem exagerar no custo.
         */
        final int sides =
                6;

        for (int i = 0;
             i < sides;
             i++) {

            double angle1 =
                    Math.PI * 2D
                            * i
                            / sides;

            double angle2 =
                    Math.PI * 2D
                            * (i + 1)
                            / sides;

            Vec3 offset1 =
                    side.scale(
                            Math.cos(angle1)
                                    * radius
                    ).add(
                            side2.scale(
                                    Math.sin(angle1)
                                            * radius
                            )
                    );

            Vec3 offset2 =
                    side.scale(
                            Math.cos(angle2)
                                    * radius
                    ).add(
                            side2.scale(
                                    Math.sin(angle2)
                                            * radius
                            )
                    );

            addVertex(
                    consumer,
                    matrix,
                    x1 + (float) offset1.x,
                    y1 + (float) offset1.y,
                    z1 + (float) offset1.z,
                    red,
                    green,
                    blue,
                    alpha
            );

            addVertex(
                    consumer,
                    matrix,
                    x1 + (float) offset2.x,
                    y1 + (float) offset2.y,
                    z1 + (float) offset2.z,
                    red,
                    green,
                    blue,
                    alpha
            );

            addVertex(
                    consumer,
                    matrix,
                    x2 + (float) offset2.x,
                    y2 + (float) offset2.y,
                    z2 + (float) offset2.z,
                    red,
                    green,
                    blue,
                    alpha
            );

            addVertex(
                    consumer,
                    matrix,
                    x2 + (float) offset1.x,
                    y2 + (float) offset1.y,
                    z2 + (float) offset1.z,
                    red,
                    green,
                    blue,
                    alpha
            );
        }
    }

    /*
     * ============================================================
     * VERTEX
     * ============================================================
     */

    private void addVertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        consumer.vertex(
                        matrix,
                        x,
                        y,
                        z
                )
                .color(
                        red,
                        green,
                        blue,
                        alpha
                )
                .endVertex();
    }

    /*
     * ============================================================
     * CORES
     * ============================================================
     */

    private float colorRed(
            int color
    ) {
        return (
                (color >> 16)
                        & 0xFF
        ) / 255F;
    }

    private float colorGreen(
            int color
    ) {
        return (
                (color >> 8)
                        & 0xFF
        ) / 255F;
    }

    private float colorBlue(
            int color
    ) {
        return (
                color
                        & 0xFF
        ) / 255F;
    }
}