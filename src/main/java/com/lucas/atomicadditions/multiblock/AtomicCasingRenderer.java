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
     * 75% do tamanho original:
     *
     * 0.38 × 0.75 = 0.285
     */
    private static final float SPHERE_RADIUS =
            0.285F;

    /*
     * Raio da órbita.
     */
    private static final float ORBIT_RADIUS =
            0.62F;

    /*
     * Distância angular entre as duas esferas.
     *
     * Ambas percorrem a mesma órbita e no mesmo sentido.
     */
    private static final float ORBIT_PHASE_OFFSET =
            Mth.PI;

    /*
     * Oscilação vertical.
     *
     * Aumentada em relação à versão anterior.
     */
    private static final float IDLE_BOB_AMPLITUDE =
            0.11F;

    private static final float IDLE_BOB_SPEED =
            0.055F;

    /*
     * Velocidade orbital máxima.
     *
     * Esse valor é multiplicado pelo percentual de energia.
     */
    private static final float MAX_ORBIT_SPEED =
            1.22F;

    /*
     * Pequena rotação própria somente para deixar
     * a esfera visualmente viva.
     *
     * Também depende da energia.
     */
    private static final float MAX_SPIN_SPEED =
            14.0F;

    /*
     * Fumaça maior e mais espalhada.
     */
    private static final float SMOKE_RADIUS =
            0.46F;

    private static final int SPHERE_SEGMENTS =
            14;

    private static final int SPHERE_RINGS =
            8;

    private static final int SMOKE_PARTICLES =
            24;

    /*
     * Atmosfera próxima da esfera.
     */
    private static final float ATMOSPHERE_RADIUS =
            0.34F;

    private static final int ATMOSPHERE_ARCS =
            8;

    /*
     * Pequenos flashes.
     */
    private static final int FLASH_ARCS =
            6;

    /*
     * Referência energética máxima do AMR.
     */
    private static final double MAX_REFERENCE_ENERGY =
            80_000_000D;

    /*
     * Estado da órbita.
     *
     * O ponto mais importante:
     *
     * NÃO usamos gameTime diretamente para determinar
     * a posição orbital.
     *
     * Esse valor só avança enquanto o AMR está ativo.
     */
    private float orbitAngle =
            0F;

    private long lastOrbitTick =
            -1;

    private long lastLogTick =
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

        logState(
                blockEntity,
                multiblock
        );

        /*
         * Os dados visuais vêm do snapshot sincronizado.
         */
        List<Integer> colors =
                getActiveInputColors(multiblock);

        /*
         * Mesmo que só exista UM elemento,
         * uma esfera deve aparecer.
         */
        if (colors.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        /*
         * Centro geométrico do multiblock.
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
         * Atualiza o ângulo orbital.
         *
         * A velocidade é proporcional ao uso de energia.
         *
         * Sem processamento:
         *     orbitFactor = 0
         *     → ângulo não avança
         *
         * Energia máxima:
         *     orbitFactor = 1
         *     → velocidade máxima
         */
        float energyFactor =
                (float) Math.min(
                        1D,
                        multiblock.renderEnergy
                                / MAX_REFERENCE_ENERGY
                );

        boolean active =
                multiblock.renderProcessed > 0;

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
         * Continua existindo mesmo parado.
         */
        float idleBob =
                (float) Math.sin(
                        gameTime * IDLE_BOB_SPEED
                ) * IDLE_BOB_AMPLITUDE;

        /*
         * Velocidade da rotação própria.
         *
         * Também acompanha a energia.
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

        VertexConsumer consumer =
                buffer.getBuffer(
                        MekanismRenderType.MEK_LIGHTNING
                );

        /*
         * Guarda as posições das esferas.
         *
         * Essas mesmas posições serão usadas pelos
         * raios das bobinas.
         */
        List<Vec3> spherePositions =
                new ArrayList<>(
                        colors.size()
                );

        for (int index = 0;
             index < colors.size();
             index++) {

            int color =
                    colors.get(index);

            /*
             * As duas usam a mesma órbita.
             *
             * A segunda possui apenas uma defasagem angular.
             */
            float localAngle =
                    orbitAngle;

            if (colors.size() > 1
                    && index == 1) {

                localAngle +=
                        ORBIT_PHASE_OFFSET;
            }

            /*
             * Translação orbital no plano horizontal.
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
             * Pequena rotação própria.
             *
             * Não controla a posição da esfera.
             * O movimento principal é a órbita.
             *
             * E, assim como a órbita, depende da energia.
             */
            if (active
                    && energyFactor > 0F) {

                poseStack.mulPose(
                        Axis.YP.rotationDegrees(
                                spin
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
             * Fumaça colorida.
             *
             * Continua existindo mesmo parada.
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

            if (active
                    && energyFactor > 0F) {

                /*
                 * Atmosfera branca.
                 */
                renderAtmosphere(
                        poseStack,
                        consumer,
                        energyFactor,
                        gameTime,
                        index
                );

                /*
                 * Pequenos flashes.
                 */
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
         * Raios entre bobinas e esferas.
         */
        if (active
                && energyFactor > 0F
                && !spherePositions.isEmpty()) {

            renderCoilRays(
                    poseStack,
                    consumer,
                    multiblock,
                    energyFactor,
                    gameTime,
                    spherePositions
            );
        }

        poseStack.popPose();
    }

    /*
     * Atualiza o ângulo orbital.
     *
     * O ângulo só avança enquanto existe processamento.
     *
     * Isso resolve o problema em que as esferas continuavam
     * se movendo depois que o processo parava.
     */
    private void updateOrbit(
            AtomicCasingBlockEntity blockEntity,
            float orbitFactor,
            float partialTick
    ) {
        long currentTick =
                blockEntity.getLevel()
                        .getGameTime();

        /*
         * Primeira execução.
         */
        if (lastOrbitTick < 0) {

            lastOrbitTick =
                    currentTick;

            return;
        }

        long deltaTicks =
                currentTick
                        - lastOrbitTick;

        /*
         * Segurança caso o mundo seja recarregado
         * ou o renderer seja reutilizado.
         */
        if (deltaTicks < 0) {

            lastOrbitTick =
                    currentTick;

            return;
        }

        /*
         * Limita o salto para evitar uma grande
         * aceleração visual depois de alguma pausa.
         */
        deltaTicks =
                Math.min(
                        deltaTicks,
                        5
                );

        if (orbitFactor > 0F) {

            /*
             * Avança proporcionalmente ao uso de energia.
             *
             * 0.0 → não avança
             * 0.5 → metade
             * 1.0 → máximo
             */
            float delta =
                    (float) deltaTicks
                            * MAX_ORBIT_SPEED
                            * orbitFactor;

            orbitAngle +=
                    delta;
        }

        /*
         * A fração do tick é usada apenas para
         * deixar a animação mais suave.
         */
        if (orbitFactor > 0F
                && partialTick > 0F) {

            orbitAngle +=
                    partialTick
                            * MAX_ORBIT_SPEED
                            * orbitFactor
                            * 0.016F;
        }

        /*
         * Mantém o ângulo dentro de 0..2π.
         */
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

    private void logState(
            AtomicCasingBlockEntity blockEntity,
            AtomicMultiblockData multiblock
    ) {
        long gameTime =
                blockEntity.getLevel()
                        .getGameTime();

        if (gameTime == lastLogTick
                || gameTime % 20 != 0) {
            return;
        }

        lastLogTick =
                gameTime;

        LOGGER.info(
                "[AMR-RENDER] master={} | formed={} | renderLocation={} | gases={} | input1={} | input2={} | energy={} | processed={} | progress={}",
                blockEntity.getBlockPos(),
                multiblock.isFormed(),
                multiblock.renderLocation,
                getActiveInputColors(
                        multiblock
                ).size(),
                getDisplayName(
                        multiblock.renderInput1Name
                ),
                getDisplayName(
                        multiblock.renderInput2Name
                ),
                multiblock.renderEnergy,
                multiblock.renderProcessed,
                multiblock.renderProgress
        );
    }

    private String getDisplayName(
            String name
    ) {
        return name == null
                || name.isEmpty()
                ? "EMPTY"
                : name;
    }

    private List<Integer> getActiveInputColors(
            AtomicMultiblockData multiblock
    ) {
        List<Integer> colors =
                new ArrayList<>(2);

        /*
         * -1 = vazio.
         */
        if (multiblock.renderInput1Color >= 0) {

            colors.add(
                    multiblock.renderInput1Color
            );
        }

        if (multiblock.renderInput2Color >= 0) {

            /*
             * Evita duplicar a esfera se os dois tanques
             * estiverem contendo exatamente o mesmo gás.
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
                 * Destaque móvel na superfície.
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

    private void renderSmoke(
            PoseStack poseStack,
            VertexConsumer consumer,
            int color,
            double gameTime,
            int sphereIndex,
            float alpha
    ) {
        /*
         * Fumaça maior e mais perceptível.
         */
        for (int i = 0;
             i < SMOKE_PARTICLES;
             i++) {

            double angle =
                    gameTime * 0.010
                            + i * Math.PI * 2
                            / SMOKE_PARTICLES
                            + sphereIndex * 1.73;

            float radius =
                    SMOKE_RADIUS
                            * (
                            0.78F
                                    + 0.22F
                                    * Mth.sin(
                                    (float) (
                                            gameTime * 0.03
                                                    + i * 1.17
                                    )
                            )
                    );

            float x =
                    (float) Math.cos(angle)
                            * radius;

            float z =
                    (float) Math.sin(angle)
                            * radius;

            /*
             * Movimento vertical suave.
             */
            float y =
                    -0.035F
                            + (i % 6)
                            * 0.050F
                            + (float) Math.sin(
                            gameTime * 0.016
                                    + i * 1.45
                    ) * 0.060F;

            /*
             * Partículas maiores.
             */
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
         * Mantém a atmosfera bastante próxima
         * da esfera.
         */
        float radius =
                ATMOSPHERE_RADIUS
                        + energyFactor * 0.035F;

        int arcCount =
                2
                        + Math.round(
                        energyFactor
                                * (
                                ATMOSPHERE_ARCS
                                        - 2
                        )
                );

        for (int i = 0;
             i < arcCount;
             i++) {

            double angle =
                    gameTime
                            * (
                            0.025
                                    + energyFactor
                                    * 0.09
                    )
                            + i * Math.PI * 2
                            / ATMOSPHERE_ARCS
                            + sphereIndex * 1.7;

            double nextAngle =
                    angle
                            + 0.14
                            + energyFactor * 0.13;

            float x1 =
                    (float) Math.cos(angle)
                            * radius;

            float z1 =
                    (float) Math.sin(angle)
                            * radius;

            float x2 =
                    (float) Math.cos(nextAngle)
                            * radius;

            float z2 =
                    (float) Math.sin(nextAngle)
                            * radius;

            float y1 =
                    (float) Math.sin(
                            angle * 2.7
                    ) * 0.10F;

            float y2 =
                    y1
                            + 0.035F;

            drawSegment(
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
                    0.40F
                            + energyFactor * 0.45F,
                    0.014F
                            + energyFactor * 0.016F
            );
        }
    }

    private void renderFlashes(
            PoseStack poseStack,
            VertexConsumer consumer,
            float energyFactor,
            double gameTime,
            int sphereIndex
    ) {
        Matrix4f matrix =
                poseStack.last().pose();

        /*
         * Flashes pequenos e rápidos.
         */
        for (int i = 0;
             i < FLASH_ARCS;
             i++) {

            double seed =
                    i * 2.71
                            + sphereIndex * 4.19;

            double phase =
                    gameTime * 0.18
                            + seed;

            float visibility =
                    Mth.sin(
                            (float) phase
                    );

            if (visibility < 0.35F) {
                continue;
            }

            float flash =
                    (visibility - 0.35F)
                            / 0.65F;

            double angle =
                    seed
                            + gameTime * 0.035;

            float radius =
                    SPHERE_RADIUS
                            + 0.075F
                            + energyFactor * 0.05F;

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

            drawSegment(
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
                    flash
                            * (
                            0.45F
                                    + energyFactor
                                    * 0.45F
                    ),
                    0.012F
                            + energyFactor * 0.010F
            );
        }
    }

    private void renderCoilRays(
            PoseStack poseStack,
            VertexConsumer consumer,
            AtomicMultiblockData multiblock,
            float energyFactor,
            double gameTime,
            List<Vec3> spherePositions
    ) {
        if (multiblock.coils.isEmpty()) {
            return;
        }

        Matrix4f matrix =
                poseStack.last().pose();

        int index = 0;

        for (BlockPos coil :
                multiblock.coils) {

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

            /*
             * Distribui as bobinas entre as esferas.
             *
             * Com apenas uma esfera:
             * todas apontam para ela.
             */
            int sphereIndex =
                    spherePositions.size() <= 1
                            ? 0
                            : index % spherePositions.size();

            Vec3 sphere =
                    spherePositions.get(
                            sphereIndex
                    );

            /*
             * Ponto inicial:
             * centro da bobina.
             */
            Vec3 start =
                    new Vec3(
                            coilX,
                            coilY,
                            coilZ
                    );

            /*
             * Ponto final:
             * superfície da esfera.
             */
            Vec3 target =
                    sphere;

            Vec3 direction =
                    target.subtract(start);

            double distance =
                    direction.length();

            if (distance < 0.001D) {
                index++;
                continue;
            }

            direction =
                    direction.normalize();

            /*
             * Retira o trecho que ficaria dentro da esfera.
             */
            double endDistance =
                    Math.max(
                            0.0D,
                            distance
                                    - SPHERE_RADIUS
                    );

            Vec3 end =
                    start.add(
                            direction.scale(
                                    endDistance
                            )
                    );

            /*
             * Número de segmentos do raio.
             *
             * Mais energia = raio mais detalhado.
             */
            int segments =
                    5
                            + Math.round(
                            energyFactor * 4
                    );

            /*
             * Vetor perpendicular principal.
             */
            Vec3 perpendicular =
                    new Vec3(
                            -direction.z,
                            0,
                            direction.x
                    );

            if (perpendicular.lengthSqr()
                    < 0.000001D) {

                perpendicular =
                        new Vec3(
                                1,
                                0,
                                0
                        );

            } else {

                perpendicular =
                        perpendicular.normalize();
            }

            /*
             * Segundo eixo perpendicular.
             */
            Vec3 perpendicular2 =
                    direction.cross(
                            perpendicular
                    ).normalize();

            /*
             * Ponto anterior começa na bobina.
             */
            Vec3 previous =
                    start;

            /*
             * Raio principal.
             */
            for (int segment = 1;
                 segment <= segments;
                 segment++) {

                float fraction =
                        segment
                                / (float) segments;

                Vec3 point =
                        start.lerp(
                                end,
                                fraction
                        );

                /*
                 * Não fazemos desvio no final para
                 * conectar suavemente à esfera.
                 */
                if (segment < segments) {

                    /*
                     * Padrão pseudoaleatório determinístico.
                     *
                     * Cada raio tem sua própria sequência,
                     * mas ela não muda de forma caótica
                     * a cada frame.
                     */
                    double seed =
                            index * 17.37
                                    + segment * 7.91;

                    double waveA =
                            Math.sin(
                                    gameTime * 0.45
                                            + seed
                            );

                    double waveB =
                            Math.cos(
                                    gameTime * 0.31
                                            + seed * 1.73
                            );

                    /*
                     * Quanto maior a energia,
                     * maior a irregularidade.
                     */
                    double jitter =
                            (
                                    0.035D
                                            + energyFactor
                                            * 0.075D
                            )
                                    * (
                                    0.35D
                                            + fraction * 0.65D
                            );

                    point =
                            point.add(
                                    perpendicular.scale(
                                            waveA * jitter
                                    )
                            );

                    point =
                            point.add(
                                    perpendicular2.scale(
                                            waveB * jitter
                                    )
                            );
                }

                /*
                 * Pulsação do raio.
                 */
                float pulse =
                        0.65F
                                + 0.35F
                                * Mth.sin(
                                (float) (
                                        gameTime
                                                * 0.55
                                                + index
                                                * 2.7
                                                + segment
                                                * 1.9
                                )
                        );

                /*
                 * Intensidade.
                 */
                float alpha =
                        (
                                0.35F
                                        + energyFactor
                                        * 0.65F
                        )
                                * pulse;

                /*
                 * Espessura.
                 */
                float thickness =
                        0.018F
                                + energyFactor * 0.030F;

                drawSegment(
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

            /*
             * Pequeno clarão exatamente na saída da bobina.
             */
            Vec3 coilFlashEnd =
                    start.add(
                            direction.scale(
                                    0.20D
                                            + energyFactor
                                            * 0.10D
                            )
                    );

            drawSegment(
                    matrix,
                    consumer,
                    (float) start.x,
                    (float) start.y,
                    (float) start.z,
                    (float) coilFlashEnd.x,
                    (float) coilFlashEnd.y,
                    (float) coilFlashEnd.z,
                    1F,
                    1F,
                    1F,
                    0.80F
                            + energyFactor * 0.20F,
                    0.035F
                            + energyFactor * 0.030F
            );

            index++;
        }
    }

    private void drawSegment(
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
            float thickness
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

        Vec3 perpendicular =
                new Vec3(
                        -direction.z,
                        0,
                        direction.x
                );

        if (perpendicular.lengthSqr()
                < 0.000001D) {

            perpendicular =
                    new Vec3(
                            1,
                            0,
                            0
                    );

        } else {

            perpendicular =
                    perpendicular.normalize();
        }

        perpendicular =
                perpendicular.scale(
                        thickness
                );

        float px =
                (float) perpendicular.x;

        float py =
                (float) perpendicular.y;

        float pz =
                (float) perpendicular.z;

        addVertex(
                consumer,
                matrix,
                x1 - px,
                y1 - py,
                z1 - pz,
                red,
                green,
                blue,
                alpha
        );

        addVertex(
                consumer,
                matrix,
                x1 + px,
                y1 + py,
                z1 + pz,
                red,
                green,
                blue,
                alpha
        );

        addVertex(
                consumer,
                matrix,
                x2 + px,
                y2 + py,
                z2 + pz,
                red,
                green,
                blue,
                alpha
        );

        addVertex(
                consumer,
                matrix,
                x2 - px,
                y2 - py,
                z2 - pz,
                red,
                green,
                blue,
                alpha
        );
    }

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