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

        logRayState(
                blockEntity,
                multiblock,
                active
        );

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
         * Atmosfera próxima da esfera.
         */
        float radius =
                SPHERE_RADIUS
                        + 0.045F
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

            /*
             * Cada pequeno raio possui seu próprio movimento.
             */
            double seed =
                    sphereIndex * 3.71
                            + i * 2.47;

            double angle =
                    seed
                            + gameTime
                            * (
                            0.025
                                    + energyFactor
                                    * 0.10
                    );

            double nextAngle =
                    angle
                            + 0.12
                            + energyFactor * 0.12;

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
                    ) * 0.09F;

            float y2 =
                    y1
                            + 0.025F
                            + energyFactor * 0.025F;

            /*
             * Branco puro.
             */
            float alpha =
                    0.35F
                            + energyFactor * 0.50F;

            float thickness =
                    0.012F
                            + energyFactor * 0.016F;

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
                    alpha,
                    thickness
            );
        }
    }

    private void renderCoreFlash(
            PoseStack poseStack,
            VertexConsumer consumer,
            float energyFactor,
            double gameTime
    ) {
        Matrix4f matrix =
                poseStack.last().pose();

        float pulse =
                0.5F
                        + 0.5F
                        * Mth.sin(
                        (float) (
                                gameTime * 0.95
                        )
                );

        float radius =
                0.10F
                        + energyFactor * 0.08F;

        int rays =
                4
                        + Math.round(
                        energyFactor * 3
                );

        for (int i = 0;
             i < rays;
             i++) {

            double angle =
                    i * Math.PI * 2
                            / rays
                            + gameTime * 0.16;

            float length =
                    radius
                            * (
                            1.0F
                                    + 0.35F
                                    * Mth.sin(
                                    (float) (
                                            gameTime * 1.7
                                                    + i * 2.3
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
                            gameTime * 1.3
                                    + i * 1.9
                    ) * radius * 0.5F;

            drawSegment(
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
                            0.45F
                                    + energyFactor * 0.45F
                    ),
                    0.020F
                            + energyFactor * 0.012F
            );
        }
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

    private long lastRayLogTick =
            -1;

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
                getActiveInputColors(multiblock).size()
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
        float red =
                ((color >> 16) & 0xFF)
                        / 255F;

        float green =
                ((color >> 8) & 0xFF)
                        / 255F;

        float blue =
                (color & 0xFF)
                        / 255F;

        /*
         * Quantidade de pequenas nuvens.
         */
        final int CLOUDS =
                20;

        for (int i = 0;
             i < CLOUDS;
             i++) {

            /*
             * Cada nuvem possui seu próprio movimento.
             */
            double seed =
                    sphereIndex * 17.31
                            + i * 4.73;

            double drift =
                    gameTime * (
                            0.008
                                    + (i % 3)
                                    * 0.0015
                    );

            /*
             * Movimento circular bem lento.
             */
            double angle =
                    seed
                            + drift
                            + Math.sin(
                            gameTime * 0.004
                                    + seed
                    ) * 0.40;

            /*
             * Distância da esfera.
             */
            float radius =
                    0.27F
                            + 0.20F
                            * Mth.sin(
                            (float) (
                                    gameTime * 0.015
                                            + seed
                            )
                    );

            /*
             * Posição horizontal.
             */
            float x =
                    (float) Math.cos(angle)
                            * radius;

            float z =
                    (float) Math.sin(angle)
                            * radius;

            /*
             * A fumaça sobe lentamente.
             */
            float rise =
                    (float) (
                            i * 0.018
                                    + Math.sin(
                                    gameTime * 0.012
                                            + seed
                            ) * 0.06
                    );

            float y =
                    -0.10F
                            + rise;

            /*
             * Cada nuvem é alongada em uma direção diferente.
             */
            float sizeX =
                    0.075F
                            + (i % 4) * 0.022F;

            float sizeY =
                    0.11F
                            + (i % 3) * 0.025F;

            /*
             * Dissipação suave.
             */
            float fade =
                    0.55F
                            + 0.45F
                            * Mth.sin(
                            (float) (
                                    gameTime * 0.018
                                            + seed
                            )
                    );

            float cloudAlpha =
                    alpha
                            * 0.075F
                            * fade;

            poseStack.pushPose();

            poseStack.translate(
                    x,
                    y,
                    z
            );

            /*
             * Pequenas rotações para impedir que todas
             * as nuvens tenham a mesma aparência.
             */
            poseStack.mulPose(
                    com.mojang.math.Axis.YP.rotation(
                            (float) (
                                    seed
                            )
                    )
            );

            poseStack.mulPose(
                    com.mojang.math.Axis.ZP.rotation(
                            0.35F
                                    * Mth.sin(
                                    (float) (
                                            gameTime * 0.01
                                                    + seed
                                    )
                            )
                    )
            );

            /*
             * Nuvem alongada.
             *
             * Usamos vários elementos sobrepostos em vez
             * de uma esfera transparente única.
             */
            renderSmokeBlob(
                    poseStack,
                    consumer,
                    sizeX,
                    sizeY,
                    red,
                    green,
                    blue,
                    cloudAlpha
            );

            poseStack.popPose();
        }
    }

    private void renderSmokeBlob(
            PoseStack poseStack,
            VertexConsumer consumer,
            float width,
            float height,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Matrix4f matrix =
                poseStack.last().pose();

        /*
         * Três camadas sobrepostas criam uma nuvem
         * irregular em vez de um círculo.
         */
        renderSmokeQuad(
                matrix,
                consumer,
                -width * 0.75F,
                0F,
                width,
                height,
                red,
                green,
                blue,
                alpha
        );

        renderSmokeQuad(
                matrix,
                consumer,
                0F,
                height * 0.18F,
                width * 0.85F,
                height * 0.85F,
                red,
                green,
                blue,
                alpha * 0.85F
        );

        renderSmokeQuad(
                matrix,
                consumer,
                width * 0.50F,
                height * 0.05F,
                width * 0.70F,
                height * 0.70F,
                red,
                green,
                blue,
                alpha * 0.65F
        );
    }

    private void renderSmokeQuad(
            Matrix4f matrix,
            VertexConsumer consumer,
            float x,
            float y,
            float width,
            float height,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        addVertex(
                consumer,
                matrix,
                x - width,
                y - height,
                0F,
                red,
                green,
                blue,
                alpha
        );

        addVertex(
                consumer,
                matrix,
                x - width * 0.55F,
                y + height,
                0F,
                red,
                green,
                blue,
                alpha
        );

        addVertex(
                consumer,
                matrix,
                x + width * 0.70F,
                y + height * 0.80F,
                0F,
                red,
                green,
                blue,
                alpha
        );

        addVertex(
                consumer,
                matrix,
                x + width,
                y - height * 0.70F,
                0F,
                red,
                green,
                blue,
                alpha
        );
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

        /*
         * O destino NÃO é mais a esfera.
         *
         * Todos os raios convergem para o centro do AMR,
         * exatamente no ponto em torno do qual as esferas orbitam.
         */
        Vec3 center =
                new Vec3(
                        0,
                        0,
                        0
                );

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

            Vec3 start =
                    new Vec3(
                            coilX,
                            coilY,
                            coilZ
                    );

            /*
             * Direção da bobina para o centro.
             */
            Vec3 direction =
                    center.subtract(start);

            double distance =
                    direction.length();

            if (distance < 0.001D) {
                index++;
                continue;
            }

            direction =
                    direction.normalize();

            /*
             * Quantidade de "quebras" do raio.
             *
             * Mais energia = mais segmentos.
             */
            int segments =
                    6
                            + Math.round(
                            energyFactor * 5
                    );

            /*
             * Dois eixos perpendiculares para criar
             * desvios tridimensionais.
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

            Vec3 perpendicular2 =
                    direction.cross(
                            perpendicular
                    ).normalize();

            Vec3 previous =
                    start;

            /*
             * Comprimento do último trecho.
             *
             * Termina no próprio centro.
             */
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
                 * O último ponto é exatamente o centro.
                 */
                if (segment == segments) {
                    point =
                            center;
                } else {

                    /*
                     * Semente determinística para que cada
                     * raio tenha um padrão próprio.
                     */
                    double seed =
                            index * 13.71
                                    + segment * 6.37;

                    /*
                     * Movimento contínuo e irregular.
                     */
                    double waveA =
                            Math.sin(
                                    gameTime * 0.65
                                            + seed
                            );

                    double waveB =
                            Math.cos(
                                    gameTime * 0.47
                                            + seed * 1.83
                            );

                    double waveC =
                            Math.sin(
                                    gameTime * 0.91
                                            + seed * 0.71
                            );

                    /*
                     * Desvio maior no meio do raio
                     * e menor próximo das pontas.
                     */
                    double envelope =
                            Math.sin(
                                    fraction * Math.PI
                            );

                    double jitter =
                            (
                                    0.035D
                                            + energyFactor
                                            * 0.085D
                            )
                                    * envelope;

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

                    point =
                            point.add(
                                    direction.scale(
                                            waveC
                                                    * jitter
                                                    * 0.35D
                                    )
                            );
                }

                /*
                 * Pulso de luminosidade.
                 */
                float pulse =
                        0.75F
                                + 0.25F
                                * Mth.sin(
                                (float) (
                                        gameTime
                                                * 0.75
                                                + index
                                                * 2.41
                                                + segment
                                                * 1.37
                                )
                        );

                /*
                 * Raios mais opacos.
                 */
                float alpha =
                        (
                                0.78F
                                        + energyFactor
                                        * 0.22F
                        )
                                * pulse;

                /*
                 * Mais grossos que antes.
                 */
                float thickness =
                        0.022F
                                + energyFactor
                                * 0.040F;

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