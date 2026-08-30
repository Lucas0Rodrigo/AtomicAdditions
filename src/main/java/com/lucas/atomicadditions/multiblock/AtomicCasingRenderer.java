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
     * Distância máxima das esferas em relação ao centro.
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
     * Velocidade orbital máxima.
     */
    private static final float MAX_ORBIT_SPEED =
            1.22F;

    /*
     * Rotação própria das esferas.
     */
    private static final float MAX_SPIN_SPEED =
            14.0F;

    /*
     * ============================================================
     * NÚCLEO INTERNO
     * ============================================================
     *
     * 80% do tamanho da esfera externa.
     */

    private static final float CORE_RADIUS =
            SPHERE_RADIUS * 0.80F;

    /*
     * 60% de opacidade.
     */
    private static final float CORE_ALPHA =
            0.60F;

    /*
     * ============================================================
     * RASTRO
     * ============================================================
     */

    /*
     * Quantidade de segmentos do rastro.
     */
    private static final int TRAIL_SEGMENTS =
            18;

    /*
     * Distância entre os segmentos.
     */
    private static final float TRAIL_STEP_ANGLE =
            0.075F;

    /*
     * Espessura base.
     */
    private static final float TRAIL_RADIUS =
            SPHERE_RADIUS * 0.80F;

    /*
     * ============================================================
     * ATMOSFERA
     * ============================================================
     *
     * É uma esfera maior envolvendo a principal.
     */

    private static final float ATMOSPHERE_RADIUS =
            0.47F;

    /*
     * Atmosfera mais densa.
     */
    private static final int ATMOSPHERE_ARCS =
            34;

    /*
     * ============================================================
     * FLASHES ALEATÓRIOS
     * ============================================================
     */

    private static final int RANDOM_FLASHES =
            22;

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
         * CENTRO DO MULTIBLOCK
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
         * Rotação própria da superfície.
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
         * POSIÇÕES DAS ESFERAS
         * ========================================================
         *
         * Usadas também pelo rastro.
         */

        List<Vec3> spherePositions =
                new ArrayList<>(
                        colors.size()
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
             * E2 = exatamente 180° afastada.
             */
            float localAngle =
                    orbitAngle;

            if (colors.size() > 1
                    && index == 1) {

                localAngle +=
                        ORBIT_PHASE_OFFSET;
            }

            /*
             * Em energia máxima a órbita fecha levemente.
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

            Vec3 spherePosition =
                    new Vec3(
                            orbitX,
                            orbitY,
                            orbitZ
                    );

            spherePositions.add(
                    spherePosition
            );

            poseStack.pushPose();

            poseStack.translate(
                    orbitX,
                    orbitY,
                    orbitZ
            );

            /*
             * Rotação própria.
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
             * ====================================================
             * ESFERA EXTERNA
             * ====================================================
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
             * 80% do tamanho da esfera externa.
             *
             * Alterna rapidamente entre a cor original
             * e branco.
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
         * RASTROS
         * ========================================================
         *
         * Importante:
         *
         * São desenhados no sistema de coordenadas do centro
         * do multiblock, e não dentro do PoseStack da esfera.
         *
         * Assim o rastro corresponde à trajetória real.
         */

        if (active
                && energyFactor > 0F) {

            for (int index = 0;
                 index < spherePositions.size();
                 index++) {

                int color =
                        colors.get(index);

                float localAngle =
                        orbitAngle;

                if (colors.size() > 1
                        && index == 1) {

                    localAngle +=
                            ORBIT_PHASE_OFFSET;
                }

                float effectiveOrbitRadius =
                        ORBIT_RADIUS
                                * (
                                1.0F
                                        - 0.25F
                                        * energyFactor
                        );

                renderTrail(
                        poseStack,
                        consumer,
                        color,
                        localAngle,
                        effectiveOrbitRadius,
                        energyFactor
                );
            }
        }

        /*
         * ========================================================
         * FLASHES ALEATÓRIOS
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
         * A órbita somente avança enquanto há energia
         * e processamento.
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
         * Alternância extremamente rápida.
         */
        long phase =
                (long) Math.floor(
                        gameTime * 18.0
                                + sphereIndex * 0.5
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

        /*
         * Começa na posição atual da esfera.
         */
        Vec3 previous =
                getOrbitPosition(
                        currentAngle,
                        orbitRadius
                );

        /*
         * Comprimento cresce bastante com a energia.
         */
        float trailScale =
                0.90F
                        + energyFactor
                        * 1.10F;

        for (int i = 1;
             i <= TRAIL_SEGMENTS;
             i++) {

            float distance =
                    i
                            * TRAIL_STEP_ANGLE
                            * trailScale;

            /*
             * O rastro fica atrás da direção do movimento.
             */
            float trailAngle =
                    currentAngle
                            - distance;

            Vec3 point =
                    getOrbitPosition(
                            trailAngle,
                            orbitRadius
                    );

            /*
             * A ponta próxima à esfera é forte.
             * A extremidade traseira desaparece.
             */
            float fade =
                    1.0F
                            - (
                            i
                                    / (float) (
                                    TRAIL_SEGMENTS
                            )
                    );

            /*
             * Dá uma pequena curvatura vertical ao rastro.
             */
            float vertical =
                    (float) Math.sin(
                            distance * 2.0F
                    )
                            * 0.025F
                            * energyFactor;

            point =
                    point.add(
                            0,
                            vertical,
                            0
                    );

            /*
             * Espessura.
             */
            float radius =
                    TRAIL_RADIUS
                            * (
                            0.65F
                                    + energyFactor
                                    * 0.35F
                    )
                            * (
                            0.25F
                                    + fade
                                    * 0.75F
                    );

            /*
             * Intensidade.
             */
            float alpha =
                    (
                            0.70F
                                    + energyFactor
                                    * 0.30F
                    )
                            * fade;

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
                            0.005F,
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
                Mth.cos(angle)
                        * radius,
                0,
                Mth.sin(angle)
                        * radius
        );
    }

    /*
     * ============================================================
     * ATMOSFERA ESFÉRICA
     * ============================================================
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
         * Esfera maior e ligeiramente afastada.
         */
        float radius =
                ATMOSPHERE_RADIUS
                        + energyFactor * 0.035F;

        /*
         * Muitos raios.
         */
        int arcCount =
                14
                        + Math.round(
                        energyFactor
                                * (
                                ATMOSPHERE_ARCS
                                        - 14
                        )
                );

        for (int i = 0;
             i < arcCount;
             i++) {

            double seed =
                    sphereIndex * 73.71
                            + i * 19.37;

            /*
             * Distribuição pseudoaleatória pela esfera.
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
             * ATMOSFERA MAIS LENTA.
             */
            double rotation =
                    gameTime
                            * (
                            0.008
                                    + energyFactor
                                    * 0.025
                    );

            longitude +=
                    rotation;

            /*
             * Cada raio possui comprimento diferente.
             */
            double arcLength =
                    0.16
                            + pseudoRandom(
                            seed,
                            2
                    ) * (
                            0.14
                                    + energyFactor
                                    * 0.10
                    );

            /*
             * Piscam independentemente.
             */
            float flicker =
                    lightningFlicker(
                            gameTime,
                            seed,
                            0.55F
                    );

            if (flicker <= 0F) {
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
             * Ponto intermediário irregular.
             */
            Vec3 middle =
                    spherePoint(
                            radius
                                    + 0.010F
                                    * (float) Math.sin(
                                    gameTime * 0.35
                                            + seed
                            ),
                            latitude,
                            longitude
                                    + arcLength
                                    * 0.50
                    );

            float thickness =
                    0.028F
                            + energyFactor * 0.022F;

            float alpha =
                    flicker
                            * (
                            0.70F
                                    + energyFactor
                                    * 0.30F
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
     */

    private void renderRandomFlashes(
            PoseStack poseStack,
            VertexConsumer consumer,
            float energyFactor,
            double gameTime
    ) {
        Matrix4f matrix =
                poseStack.last().pose();

        for (int i = 0;
             i < RANDOM_FLASHES;
             i++) {

            double seed =
                    i * 47.193;

            /*
             * Ciclo mais longo:
             * o flash permanece visível por mais tempo.
             */
            double frequency =
                    0.006
                            + pseudoRandom(
                            seed,
                            3
                    ) * 0.010;

            long cycle =
                    (long) Math.floor(
                            gameTime
                                    * frequency
                    );

            double progress =
                    gameTime
                            * frequency
                            - cycle;

            /*
             * Janela de flash maior.
             */
            if (progress > 0.22) {
                continue;
            }

            /*
             * Nem todo ciclo gera relâmpago.
             */
            if (pseudoRandom(
                    seed,
                    cycle + 100
            ) > 0.60) {
                continue;
            }

            /*
             * Fade.
             */
            float strength =
                    (float) (
                            1.0
                                    - progress
                                    / 0.22
                    );

            /*
             * Posição aleatória no interior.
             */
            float x =
                    -2.30F
                            + (float) pseudoRandom(
                            seed,
                            cycle + 1
                    ) * 4.60F;

            float y =
                    -2.30F
                            + (float) pseudoRandom(
                            seed,
                            cycle + 2
                    ) * 4.60F;

            float z =
                    -2.30F
                            + (float) pseudoRandom(
                            seed,
                            cycle + 3
                    ) * 4.60F;

            /*
             * Direção aleatória.
             */
            double angleA =
                    pseudoRandom(
                            seed,
                            cycle + 4
                    )
                            * Math.PI
                            * 2.0;

            double angleB =
                    (
                            pseudoRandom(
                                    seed,
                                    cycle + 5
                            ) * 2.0
                                    - 1.0
                    )
                            * Math.PI
                            * 0.5;

            Vec3 direction =
                    new Vec3(
                            Math.cos(angleA)
                                    * Math.cos(angleB),
                            Math.sin(angleB),
                            Math.sin(angleA)
                                    * Math.cos(angleB)
                    ).normalize();

            /*
             * Flashes maiores.
             */
            float length =
                    0.35F
                            + energyFactor
                            * 0.60F;

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

            /*
             * Pequena irregularidade no meio.
             */
            Vec3 middle =
                    start.add(
                            direction.scale(
                                    length * 0.50F
                            )
                    );

            /*
             * Segundo eixo perpendicular.
             */
            Vec3 side =
                    direction.cross(
                            Math.abs(
                                    direction.y
                            ) < 0.9
                                    ? new Vec3(
                                    0,
                                    1,
                                    0
                            )
                                    : new Vec3(
                                    1,
                                    0,
                                    0
                            )
                    ).normalize();

            middle =
                    middle.add(
                            side.scale(
                                    0.10F
                                            * Mth.sin(
                                            (float) (
                                                    gameTime
                                                            * 0.9
                                                            + seed
                                            )
                                    )
                            )
                    );

            float thickness =
                    0.048F
                            + energyFactor
                            * 0.038F;

            float alpha =
                    strength
                            * (
                            0.70F
                                    + energyFactor
                                    * 0.30F
                    );

            drawLightningTube(
                    matrix,
                    consumer,
                    (float) start.x,
                    (float) start.y,
                    (float) start.z,
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
                    (float) end.x,
                    (float) end.y,
                    (float) end.z,
                    1F,
                    1F,
                    1F,
                    alpha,
                    thickness
            );
        }
    }

    /*
     * ============================================================
     * PSEUDO-RANDOM
     * ============================================================
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
     * FLICKER
     * ============================================================
     */

    private float lightningFlicker(
            double gameTime,
            double seed,
            float frequency
    ) {
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
         * Chance de existir neste ciclo.
         */
        double chance =
                pseudoRandom(
                        seed,
                        bucket + 17
                );

        if (chance < 0.40) {
            return 0F;
        }

        /*
         * Flash curto.
         */
        if (position > 0.34) {
            return 0F;
        }

        return (float) (
                1.0
                        - position
                        / 0.34
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
                 * Destaque superficial móvel.
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
     * TUBO 3D
     * ============================================================
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

        /*
         * Cria uma base perpendicular à direção.
         */
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
         * Tubo hexagonal.
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
     * CORES DE COR
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