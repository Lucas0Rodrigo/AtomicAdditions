package com.lucas.atomicadditions.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import mekanism.client.render.MekanismRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.slf4j.Logger;

public class AtomicCasingRenderer
        implements BlockEntityRenderer<AtomicCasingBlockEntity> {

    private static final Logger LOGGER =
            LogUtils.getLogger();

    /*
     * ============================================================
     * ESFERAS
     * ============================================================
     */

    private static final float SPHERE_RADIUS =
            0.285F;

    /*
     * Distância do centro da órbita.
     */
    private static final float ORBIT_RADIUS =
            0.62F;

    /*
     * Exatamente opostas.
     */
    private static final float ORBIT_PHASE_OFFSET =
            Mth.PI;

    /*
     * Oscilação quando paradas ou em movimento.
     */
    private static final float IDLE_BOB_AMPLITUDE =
            0.11F;

    private static final float IDLE_BOB_SPEED =
            0.055F;

    /*
     * Velocidade máxima da órbita.
     *
     * 1.22 = máxima energia.
     */
    private static final float MAX_ORBIT_SPEED =
            1.22F;

    /*
     * Pequena rotação própria da esfera.
     */
    private static final float MAX_SPIN_SPEED =
            14.0F;

    /*
     * ============================================================
     * FUMAÇA
     * ============================================================
     */

    private static final float SMOKE_RADIUS =
            0.46F;

    private static final int SMOKE_PARTICLES =
            24;

    /*
     * ============================================================
     * ATMOSFERA
     * ============================================================
     */

    private static final float ATMOSPHERE_RADIUS =
            0.34F;

    private static final int ATMOSPHERE_ARCS =
            8;

    /*
     * ============================================================
     * FLASH CENTRAL
     * ============================================================
     */

    private static final int CORE_FLASH_ARCS =
            9;

    private static final float CORE_FLASH_RADIUS =
            0.17F;

    /*
     * ============================================================
     * ENERGIA
     * ============================================================
     */

    private static final double MAX_REFERENCE_ENERGY =
            80_000_000D;

    /*
     * ============================================================
     * GEOMETRIA DA ESFERA
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

    /*
     * ============================================================
     * LOGGER TEMPORÁRIO
     * ============================================================
     */

    private long lastRayLogTick =
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

        if (multiblock == null) {
            return;
        }

        /*
         * Somente o Master renderiza o núcleo.
         */
        if (!blockEntity.isMaster()) {
            return;
        }

        if (!multiblock.isFormed()) {
            return;
        }

        /*
         * Pega os gases atualmente sincronizados.
         */
        List<Integer> colors =
                getActiveInputColors(
                        multiblock
                );

        /*
         * Nenhum gás = nada para renderizar.
         */
        if (colors.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        /*
         * ========================================================
         * CENTRO DO MULTIBLOCK
         * ========================================================
         */

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
         * LOGGER
         * ========================================================
         */

        logRayState(
                blockEntity,
                multiblock,
                active
        );

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
         *
         * Continua existindo mesmo quando o reator está parado.
         */
        float idleBob =
                (float) Math.sin(
                        gameTime * IDLE_BOB_SPEED
                ) * IDLE_BOB_AMPLITUDE;

        /*
         * ========================================================
         * ROTAÇÃO PRÓPRIA
         * ========================================================
         */

        float spin =
                active
                        ? (float) (
                        gameTime
                                * (
                                MAX_SPIN_SPEED
                                        * energyFactor
                        )
                )
                        : 0F;

        /*
         * ========================================================
         * BUFFER DOS EFEITOS
         * ========================================================
         */

        VertexConsumer consumer =
                buffer.getBuffer(
                        MekanismRenderType.MEK_LIGHTNING
                );

        /*
         * Guarda as posições das esferas.
         *
         * É importante para o restante dos efeitos,
         * embora os raios das bobinas agora apontem para
         * o centro e não para elas.
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
             * E1
             *
             * E2 = E1 + PI
             *
             * Isso garante 180 graus de separação.
             */
            float localAngle =
                    orbitAngle;

            if (colors.size() > 1
                    && index == 1) {

                localAngle +=
                        ORBIT_PHASE_OFFSET;
            }

            /*
             * Quanto mais energia:
             *
             * - órbita mais rápida
             * - órbita um pouco menor
             */
            float effectiveOrbitRadius =
                    ORBIT_RADIUS
                            * (
                            1.0F
                                    - 0.25F
                                    * energyFactor
                    );

            /*
             * Translação orbital.
             */
            float orbitX =
                    Mth.cos(localAngle)
                            * effectiveOrbitRadius;

            float orbitZ =
                    Mth.sin(localAngle)
                            * effectiveOrbitRadius;

            /*
             * Movimento vertical.
             */
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
             *
             * Também dependente da energia.
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
             * Fumaça.
             */
            renderSmoke(
                    poseStack,
                    consumer,
                    color,
                    gameTime,
                    index,
                    active
                            ? 1.0F
                            : 0.72F
            );

            /*
             * Atmosfera e flashes somente ligada.
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

                renderFlashes(
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
         * RAIOS DAS BOBINAS
         * ========================================================
         *
         * Todos terminam no centro da órbita.
         */
        if (active
                && energyFactor > 0F) {

            renderCoilRays(
                    poseStack,
                    consumer,
                    multiblock,
                    energyFactor,
                    gameTime
            );
        }

        /*
         * ========================================================
         * FLASH CENTRAL
         * ========================================================
         */

        if (active
                && energyFactor > 0F) {

            renderCoreFlash(
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
         * O ângulo somente avança se:
         *
         * active == true
         *
         * e
         *
         * energy > 0
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
     * LOGGER
     * ============================================================
     */

    private void logRayState(
            AtomicCasingBlockEntity blockEntity,
            AtomicMultiblockData multiblock,
            boolean active
    ) {
        long gameTime =
                blockEntity.getLevel()
                        .getGameTime();

        if (gameTime == lastRayLogTick
                || gameTime % 20 != 0) {
            return;
        }

        lastRayLogTick =
                gameTime;

        LOGGER.info(
                "[AMR-RAYS] active={} | energy={} | processed={} | coils={} | spheres={}",
                active,
                multiblock.renderEnergy,
                multiblock.renderProcessed,
                multiblock.coils.size(),
                getActiveInputColors(
                        multiblock
                ).size()
        );

        int index = 0;

        for (BlockPos coil :
                multiblock.coils) {

            LOGGER.info(
                    "[AMR-RAYS] coil[{}]={}",
                    index,
                    coil
            );

            index++;

            if (index >= 8) {

                LOGGER.info(
                        "[AMR-RAYS] ... demais bobinas omitidas"
                );

                break;
            }
        }
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
             * Não duplica se por algum motivo os dois
             * tanques tiverem exatamente o mesmo gás.
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
                ((color >> 16) & 0xFF)
                        / 255F;

        float green =
                ((color >> 8) & 0xFF)
                        / 255F;

        float blue =
                (color & 0xFF)
                        / 255F;

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
     * FUMAÇA
     * ============================================================
     */

    private void renderSmoke(
            PoseStack poseStack,
            VertexConsumer consumer,
            int color,
            double gameTime,
            int sphereIndex,
            float alpha
    ) {
        for (int i = 0;
             i < SMOKE_PARTICLES;
             i++) {

            double seed =
                    sphereIndex * 17.31
                            + i * 4.73;

            double angle =
                    seed
                            + gameTime * 0.010
                            + Math.sin(
                            gameTime * 0.004
                                    + seed
                    ) * 0.40;

            float radius =
                    SMOKE_RADIUS
                            * (
                            0.78F
                                    + 0.22F
                                    * Mth.sin(
                                    (float) (
                                            gameTime * 0.03
                                                    + seed
                                    )
                            )
                    );

            float x =
                    (float) Math.cos(angle)
                            * radius;

            float z =
                    (float) Math.sin(angle)
                            * radius;

            float y =
                    -0.035F
                            + (i % 6)
                            * 0.050F
                            + (float) Math.sin(
                            gameTime * 0.016
                                    + i * 1.45
                    ) * 0.060F;

            float size =
                    0.075F
                            + (i % 4)
                            * 0.022F;

            float pulse =
                    0.70F
                            + 0.30F
                            * Mth.sin(
                            (float) (
                                    gameTime * 0.025
                                            + i * 2.1
                            )
                    );

            poseStack.pushPose();

            poseStack.translate(
                    x,
                    y,
                    z
            );

            renderSphere(
                    poseStack,
                    consumer,
                    color,
                    size,
                    alpha
                            * 0.20F
                            * pulse,
                    0F
            );

            poseStack.popPose();
        }
    }

    /*
     * ============================================================
     * ATMOSFERA
     * ============================================================
     *
     * Raios brancos próximos da esfera.
     *
     * Agora usam drawLightningTube(), então não dependem
     * de uma única face 2D.
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

        float radius =
                SPHERE_RADIUS
                        + 0.045F
                        + energyFactor * 0.035F;

        /*
         * Atmosfera mais densa.
         */
        int arcCount =
                5
                        + Math.round(
                        energyFactor * 7
                );

        for (int i = 0;
             i < arcCount;
             i++) {

            double seed =
                    sphereIndex * 5.31
                            + i * 2.17;

            double angle =
                    seed
                            + gameTime
                            * (
                            0.03
                                    + energyFactor
                                    * 0.10
                    );

            double angle2 =
                    angle
                            + 0.13
                            + energyFactor * 0.12;

            float x1 =
                    (float) Math.cos(angle)
                            * radius;

            float z1 =
                    (float) Math.sin(angle)
                            * radius;

            float x2 =
                    (float) Math.cos(angle2)
                            * radius;

            float z2 =
                    (float) Math.sin(angle2)
                            * radius;

            float y1 =
                    (float) Math.sin(
                            angle * 3.0
                    ) * 0.09F;

            float y2 =
                    y1
                            + 0.025F;

            float alpha =
                    0.55F
                            + energyFactor * 0.40F;

            float thickness =
                    0.020F
                            + energyFactor * 0.018F;

            drawLightningTube(
                    matrix,
                    consumer,
                    x1,
                    y1,
                    z1,
                    x2,
                    y2,
                    z2,
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
     * FLASHES DA ATMOSFERA
     * ============================================================
     */

    private void renderFlashes(
            PoseStack poseStack,
            VertexConsumer consumer,
            float energyFactor,
            double gameTime,
            int sphereIndex
    ) {
        Matrix4f matrix =
                poseStack.last().pose();

        for (int i = 0;
             i < 6;
             i++) {

            double seed =
                    sphereIndex * 4.19
                            + i * 2.71;

            double phase =
                    gameTime * 0.18
                            + seed;

            float visibility =
                    Mth.sin(
                            (float) phase
                    );

            if (visibility < 0.30F) {
                continue;
            }

            float strength =
                    (visibility - 0.30F)
                            / 0.70F;

            double angle =
                    seed
                            + gameTime * 0.035;

            float radius =
                    SPHERE_RADIUS
                            + 0.075F
                            + energyFactor * 0.045F;

            float x1 =
                    (float) Math.cos(angle)
                            * radius;

            float z1 =
                    (float) Math.sin(angle)
                            * radius;

            float x2 =
                    (float) Math.cos(
                            angle + 0.08
                    ) * (
                            radius
                                    + 0.055F
                                    + energyFactor
                                    * 0.035F
                    );

            float z2 =
                    (float) Math.sin(
                            angle + 0.08
                    ) * (
                            radius
                                    + 0.055F
                                    + energyFactor
                                    * 0.035F
                    );

            float y =
                    (float) Math.sin(
                            phase * 1.7
                    ) * 0.18F;

            drawLightningTube(
                    matrix,
                    consumer,
                    x1,
                    y,
                    z1,
                    x2,
                    y + 0.025F,
                    z2,
                    1F,
                    1F,
                    1F,
                    strength
                            * (
                            0.65F
                                    + energyFactor * 0.35F
                    ),
                    0.018F
                            + energyFactor * 0.012F
            );
        }
    }

    /*
     * ============================================================
     * RAIOS DAS BOBINAS
     * ============================================================
     *
     * TODOS vão:
     *
     * BOBINA → CENTRO
     *
     * e não:
     *
     * BOBINA → ESFERA
     */

    private void renderCoilRays(
            PoseStack poseStack,
            VertexConsumer consumer,
            AtomicMultiblockData multiblock,
            float energyFactor,
            double gameTime
    ) {
        if (multiblock.coils.isEmpty()) {
            return;
        }

        Matrix4f matrix =
                poseStack.last().pose();

        /*
         * Centro real da órbita.
         */
        final Vec3 center =
                Vec3.ZERO;

        int index = 0;

        for (BlockPos coil :
                multiblock.coils) {

            /*
             * Posição da bobina em relação ao centro
             * já traduzido pelo PoseStack.
             */
            double coilX =
                    coil.getX()
                            + 0.5
                            - blockRenderAnchorX(
                            multiblock
                    );

            double coilY =
                    coil.getY()
                            + 0.5
                            - blockRenderAnchorY(
                            multiblock
                    );

            double coilZ =
                    coil.getZ()
                            + 0.5
                            - blockRenderAnchorZ(
                            multiblock
                    );

            Vec3 start =
                    new Vec3(
                            coilX,
                            coilY,
                            coilZ
                    );

            Vec3 direction =
                    center.subtract(
                            start
                    );

            double distance =
                    direction.length();

            if (distance < 0.001D) {
                index++;
                continue;
            }

            direction =
                    direction.normalize();

            /*
             * Cada raio possui um número variável de
             * segmentos.
             */
            int segments =
                    7
                            + Math.round(
                            energyFactor * 5
                    );

            /*
             * Eixos perpendiculares.
             */
            Vec3 side =
                    direction.cross(
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
                            )
                    ).normalize();

            Vec3 side2 =
                    direction.cross(
                            side
                    ).normalize();

            Vec3 previous =
                    start;

            for (int segment = 1;
                 segment <= segments;
                 segment++) {

                float fraction =
                        segment
                                / (float) segments;

                Vec3 point =
                        start.lerp(
                                center,
                                fraction
                        );

                /*
                 * Exatamente no centro no último segmento.
                 */
                if (segment < segments) {

                    double seed =
                            index * 13.73
                                    + segment * 7.91;

                    /*
                     * Movimento rápido e irregular.
                     */
                    double wave1 =
                            Math.sin(
                                    gameTime * 0.78
                                            + seed
                            );

                    double wave2 =
                            Math.cos(
                                    gameTime * 0.59
                                            + seed * 1.71
                            );

                    double wave3 =
                            Math.sin(
                                    gameTime * 1.13
                                            + seed * 0.73
                            );

                    /*
                     * Jitter máximo no meio do raio,
                     * quase zero nas pontas.
                     */
                    double envelope =
                            Math.sin(
                                    fraction
                                            * Math.PI
                            );

                    double jitter =
                            (
                                    0.025D
                                            + energyFactor
                                            * 0.075D
                            )
                                    * envelope;

                    point =
                            point.add(
                                    side.scale(
                                            wave1 * jitter
                                    )
                            );

                    point =
                            point.add(
                                    side2.scale(
                                            wave2 * jitter
                                    )
                            );

                    point =
                            point.add(
                                    direction.scale(
                                            wave3
                                                    * jitter
                                                    * 0.30D
                                    )
                            );
                } else {
                    point =
                            center;
                }

                /*
                 * Pequena pulsação.
                 */
                float pulse =
                        0.80F
                                + 0.20F
                                * Mth.sin(
                                (float) (
                                        gameTime * 0.85
                                                + index * 2.37
                                                + segment * 1.17
                                )
                        );

                /*
                 * Quase completamente opaco.
                 */
                float alpha =
                        (
                                0.88F
                                        + energyFactor
                                        * 0.12F
                        )
                                * pulse;

                /*
                 * Raio volumétrico.
                 */
                float thickness =
                        0.034F
                                + energyFactor
                                * 0.050F;

                drawLightningTube(
                        matrix,
                        consumer,
                        (float) previous.x,
                        (float) previous.y,
                        (float) previous.z,
                        (float) point.x,
                        (float) point.y,
                        (float) point.z,
                        1F,
                        1F,
                        1F,
                        alpha,
                        thickness
                );

                previous =
                        point;
            }

            index++;
        }
    }

    /*
     * ============================================================
     * FLASH CENTRAL
     * ============================================================
     *
     * Pequena explosão de luz no ponto em que os raios
     * das bobinas convergem.
     */

    private void renderCoreFlash(
            PoseStack poseStack,
            VertexConsumer consumer,
            float energyFactor,
            double gameTime
    ) {
        Matrix4f matrix =
                poseStack.last().pose();

        float pulse =
                0.45F
                        + 0.55F
                        * Mth.sin(
                        (float) (
                                gameTime * 1.25
                        )
                );

        float radius =
                CORE_FLASH_RADIUS
                        + energyFactor * 0.11F;

        /*
         * Núcleo central.
         */
        float coreSize =
                0.045F
                        + energyFactor * 0.055F;

        renderSphere(
                poseStack,
                consumer,
                0xFFFFFF,
                coreSize,
                0.85F
                        * pulse,
                0F
        );

        /*
         * Pequenos raios irradiando do centro.
         */
        int rays =
                6
                        + Math.round(
                        energyFactor
                                * (
                                CORE_FLASH_ARCS
                                        - 6
                        )
                );

        for (int i = 0;
             i < rays;
             i++) {

            double angle =
                    i * Math.PI * 2
                            / rays
                            + gameTime * 0.12;

            double seed =
                    i * 3.91;

            float length =
                    radius
                            * (
                            0.85F
                                    + 0.35F
                                    * Mth.sin(
                                    (float) (
                                            gameTime * 1.5
                                                    + seed
                                    )
                            )
                    );

            float x =
                    (float) Math.cos(angle)
                            * length;

            float z =
                    (float) Math.sin(angle)
                            * length;

            float y =
                    (float) Math.sin(
                            angle * 2.0
                                    + gameTime
                    )
                            * radius
                            * 0.45F;

            drawLightningTube(
                    matrix,
                    consumer,
                    0F,
                    0F,
                    0F,
                    x,
                    y,
                    z,
                    1F,
                    1F,
                    1F,
                    pulse
                            * (
                            0.65F
                                    + energyFactor
                                    * 0.35F
                    ),
                    0.028F
                            + energyFactor * 0.018F
            );
        }
    }

    /*
     * ============================================================
     * RAIO 3D
     * ============================================================
     *
     * Diferente do antigo drawSegment():
     *
     * - possui volume;
     * - tem seção hexagonal;
     * - continua fino;
     * - aparece de qualquer ângulo;
     * - não é uma única superfície 2D.
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
         * Primeiro eixo perpendicular.
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

        /*
         * Segundo eixo perpendicular.
         */
        Vec3 side2 =
                direction.cross(
                        side
                ).normalize();

        /*
         * Seção hexagonal.
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
     * CENTRO DE REFERÊNCIA DO MULTIBLOCK
     * ============================================================
     */

    private double blockRenderAnchorX(
            AtomicMultiblockData multiblock
    ) {
        return multiblock.getMinPos().getX()
                + multiblock.width() / 2D;
    }

    private double blockRenderAnchorY(
            AtomicMultiblockData multiblock
    ) {
        return multiblock.getMinPos().getY()
                + multiblock.height() / 2D;
    }

    private double blockRenderAnchorZ(
            AtomicMultiblockData multiblock
    ) {
        return multiblock.getMinPos().getZ()
                + multiblock.length() / 2D;
    }
}