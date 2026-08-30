package com.lucas.atomicadditions.multiblock;

import com.lucas.atomicadditions.chemical.AtomicGases;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
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

    private static final float SPHERE_RADIUS = 0.38F;
    private static final float SPHERE_X_OFFSET = 0.95F;
    private static final float SMOKE_RADIUS = 0.48F;
    private static final float ATMOSPHERE_RADIUS = 0.52F;

    private static final int SPHERE_SEGMENTS = 14;
    private static final int SPHERE_RINGS = 8;
    private static final int SMOKE_PARTICLES = 16;
    private static final int ATMOSPHERE_ARCS = 8;

    private static final float IDLE_BOB_AMPLITUDE = 0.06F;
    private static final float IDLE_BOB_SPEED = 0.06F;

    private static final double MAX_REFERENCE_ENERGY =
            80_000_000D;

    private long lastLogTick = -1;

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
         * Somente o Master renderiza a animação.
         *
         * É assim que evitamos desenhar o mesmo núcleo
         * dezenas de vezes, uma vez por casing.
         */
        if (!blockEntity.isMaster()) {
            return;
        }

        if (!multiblock.isFormed()) {
            return;
        }

        List<GasStack> gases =
                getActiveInputGases(multiblock);

        /*
         * Debug periódico.
         *
         * Um log por segundo aproximadamente.
         */
        logState(
                blockEntity,
                multiblock,
                gases
        );

        if (gases.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        /*
         * O renderer está preso ao BlockEntity Master.
         *
         * Usamos os limites reais do multiblock para colocar
         * o núcleo exatamente no centro da estrutura.
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

        float idleBob =
                (float) Math.sin(
                        gameTime * IDLE_BOB_SPEED
                ) * IDLE_BOB_AMPLITUDE;

        /*
         * Energia efetivamente usada no último tick.
         *
         * 0      = sem energia
         * 1      = 80 MFE/t
         */
        float energyFactor =
                (float) Math.min(
                        1D,
                        multiblock.lastReceivedEnergy
                                .doubleValue()
                                / MAX_REFERENCE_ENERGY
                );

        boolean active =
                multiblock.lastProcessed > 0;

        /*
         * Velocidade baseada na energia.
         */
        float rotationSpeed =
                active
                        ? 2.0F
                        + energyFactor * 28.0F
                        : 0.0F;

        float rotation =
                (float) (
                        gameTime
                                * rotationSpeed
                );

        VertexConsumer consumer =
                buffer.getBuffer(
                        MekanismRenderType.MEK_LIGHTNING
                );

        for (int index = 0;
             index < gases.size();
             index++) {

            GasStack gasStack =
                    gases.get(index);

            float x;

            if (gases.size() == 1) {
                x = 0;
            } else {
                x =
                        index == 0
                                ? -SPHERE_X_OFFSET
                                : SPHERE_X_OFFSET;
            }

            float y =
                    idleBob;

            /*
             * As duas esferas giram em sentidos opostos.
             */
            float localRotation =
                    index == 0
                            ? rotation
                            : -rotation * 0.82F;

            poseStack.pushPose();

            poseStack.translate(
                    x,
                    y,
                    0
            );

            if (active) {
                poseStack.mulPose(
                        Axis.YP.rotationDegrees(
                                localRotation
                        )
                );

                poseStack.mulPose(
                        Axis.XP.rotationDegrees(
                                localRotation * 0.47F
                        )
                );
            }

            /*
             * Esfera principal.
             */
            renderSphere(
                    poseStack,
                    consumer,
                    gasStack.getType(),
                    SPHERE_RADIUS,
                    0.92F
            );

            /*
             * Fumaça colorida.
             *
             * Continua presente mesmo com o AMR parado.
             */
            renderSmoke(
                    poseStack,
                    consumer,
                    gasStack.getType(),
                    gameTime,
                    index,
                    active
                            ? 0.75F
                            : 0.45F
            );

            if (active) {
                /*
                 * Atmosfera individual.
                 *
                 * Branca e com densidade proporcional à energia.
                 */
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
         * Raios entre o núcleo e as bobinas.
         */
        if (active) {
            renderCoilRays(
                    poseStack,
                    consumer,
                    multiblock,
                    energyFactor,
                    gameTime,
                    gases.size()
            );
        }

        poseStack.popPose();
    }

    private void logState(
            AtomicCasingBlockEntity blockEntity,
            AtomicMultiblockData multiblock,
            List<GasStack> gases
    ) {
        long gameTime =
                blockEntity.getLevel().getGameTime();

        if (gameTime == lastLogTick
                || gameTime % 20 != 0) {
            return;
        }

        lastLogTick = gameTime;

        String gas1 =
                multiblock.inputTank1
                        .getStack()
                        .isEmpty()
                        ? "EMPTY"
                        : multiblock.inputTank1
                        .getStack()
                        .getType()
                        .getRegistryName().toString();

        String gas2 =
                multiblock.inputTank2
                        .getStack()
                        .isEmpty()
                        ? "EMPTY"
                        : multiblock.inputTank2
                        .getStack()
                        .getType()
                        .getRegistryName().toString();

        LOGGER.info(
                "[AMR-RENDER] master={} | formed={} | renderLocation={} | gases={} | input1={} | input2={} | energy={} | lastProcessed={} | progress={}",
                blockEntity.getBlockPos(),
                multiblock.isFormed(),
                multiblock.renderLocation,
                gases.size(),
                gas1,
                gas2,
                multiblock.lastReceivedEnergy,
                multiblock.lastProcessed,
                multiblock.getScaledProgress()
        );
    }

    private List<GasStack> getActiveInputGases(
            AtomicMultiblockData multiblock
    ) {
        List<GasStack> gases =
                new ArrayList<>(2);

        GasStack first =
                multiblock.inputTank1.getStack();

        GasStack second =
                multiblock.inputTank2.getStack();

        if (!first.isEmpty()) {
            gases.add(first);
        }

        if (!second.isEmpty()
                && (
                gases.isEmpty()
                        || second.getType()
                        != gases.get(0).getType()
        )) {
            gases.add(second);
        }

        /*
         * O AMR utiliza somente estes quatro gases como entrada.
         */
        gases.removeIf(
                stack ->
                        stack.getType()
                                != AtomicGases.NIOBIUM.get()
                                && stack.getType()
                                != AtomicGases.GERMANIUM.get()
                                && stack.getType()
                                != AtomicGases.PALLADIUM.get()
                                && stack.getType()
                                != AtomicGases.COPPER.get()
        );

        return gases;
    }

    private void renderSphere(
            PoseStack poseStack,
            VertexConsumer consumer,
            Gas gas,
            float radius,
            float alpha
    ) {
        int color =
                gas.getColorRepresentation();

        float red =
                ((color >> 16) & 0xFF) / 255F;

        float green =
                ((color >> 8) & 0xFF) / 255F;

        float blue =
                (color & 0xFF) / 255F;

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

                consumer.vertex(
                                matrix,
                                Mth.cos(phi1)
                                        * ringRadius1,
                                y1,
                                Mth.sin(phi1)
                                        * ringRadius1
                        )
                        .color(
                                red,
                                green,
                                blue,
                                alpha
                        )
                        .endVertex();

                consumer.vertex(
                                matrix,
                                Mth.cos(phi1)
                                        * ringRadius2,
                                y2,
                                Mth.sin(phi1)
                                        * ringRadius2
                        )
                        .color(
                                red,
                                green,
                                blue,
                                alpha
                        )
                        .endVertex();

                consumer.vertex(
                                matrix,
                                Mth.cos(phi2)
                                        * ringRadius2,
                                y2,
                                Mth.sin(phi2)
                                        * ringRadius2
                        )
                        .color(
                                red,
                                green,
                                blue,
                                alpha
                        )
                        .endVertex();

                consumer.vertex(
                                matrix,
                                Mth.cos(phi2)
                                        * ringRadius1,
                                y1,
                                Mth.sin(phi2)
                                        * ringRadius1
                        )
                        .color(
                                red,
                                green,
                                blue,
                                alpha
                        )
                        .endVertex();
            }
        }
    }

    private void renderSmoke(
            PoseStack poseStack,
            VertexConsumer consumer,
            Gas gas,
            double gameTime,
            int sphereIndex,
            float alpha
    ) {
        int color =
                gas.getColorRepresentation();

        float red =
                ((color >> 16) & 0xFF) / 255F;

        float green =
                ((color >> 8) & 0xFF) / 255F;

        float blue =
                (color & 0xFF) / 255F;

        Matrix4f matrix =
                poseStack.last().pose();

        for (int i = 0;
             i < SMOKE_PARTICLES;
             i++) {

            double angle =
                    gameTime * 0.012
                            + i * Math.PI * 2
                            / SMOKE_PARTICLES
                            + sphereIndex * 1.73;

            double verticalWave =
                    Math.sin(
                            gameTime * 0.018
                                    + i * 1.7
                    ) * 0.10;

            float radius =
                    SMOKE_RADIUS
                            * (
                            0.85F
                                    + 0.12F
                                    * Mth.sin(
                                    (float) (
                                            gameTime * 0.04
                                                    + i
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
                    (float) (
                            verticalWave
                                    + (
                                    (i % 5)
                                            - 2
                            ) * 0.07
                    );

            float size =
                    0.09F
                            + (i % 3) * 0.025F;

            renderBillboard(
                    matrix,
                    consumer,
                    x,
                    y,
                    z,
                    size,
                    red,
                    green,
                    blue,
                    alpha * 0.22F
            );
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
         * Começa com poucos raios e aumenta até 8.
         */
        int arcCount =
                3
                        + Math.round(
                        energyFactor
                                * (
                                ATMOSPHERE_ARCS
                                        - 3
                        )
                );

        for (int i = 0;
             i < arcCount;
             i++) {

            double angle =
                    gameTime
                            * (
                            0.02
                                    + energyFactor
                                    * 0.10
                    )
                            + i * Math.PI * 2
                            / ATMOSPHERE_ARCS
                            + sphereIndex * 1.9;

            double nextAngle =
                    angle
                            + 0.18
                            + energyFactor * 0.16;

            float radius =
                    ATMOSPHERE_RADIUS
                            + energyFactor * 0.08F;

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

            float y =
                    (float) Math.sin(
                            angle * 3
                    ) * radius;

            drawSegment(
                    matrix,
                    consumer,
                    x1,
                    y,
                    z1,
                    x2,
                    y + 0.05F,
                    z2,
                    1F,
                    1F,
                    1F,
                    0.35F
                            + energyFactor * 0.45F,
                    0.018F
                            + energyFactor * 0.018F
            );
        }
    }

    private void renderCoilRays(
            PoseStack poseStack,
            VertexConsumer consumer,
            AtomicMultiblockData multiblock,
            float energyFactor,
            double gameTime,
            int gasCount
    ) {
        if (multiblock.coils.isEmpty()) {
            return;
        }

        Matrix4f matrix =
                poseStack.last().pose();

        int index = 0;

        for (BlockPos coil :
                multiblock.coils) {

            double targetX =
                    coil.getX()
                            + 0.5
                            - blockRenderAnchorX(
                            multiblock
                    );

            double targetY =
                    coil.getY()
                            + 0.5
                            - blockRenderAnchorY(
                            multiblock
                    );

            double targetZ =
                    coil.getZ()
                            + 0.5
                            - blockRenderAnchorZ(
                            multiblock
                    );

            /*
             * Distribui os raios entre as esferas existentes.
             *
             * Se só existir uma esfera, todos vão para ela.
             */
            int sphereIndex =
                    gasCount <= 1
                            ? 0
                            : index % gasCount;

            float sphereX =
                    gasCount <= 1
                            ? 0
                            : (
                            sphereIndex == 0
                                    ? -SPHERE_X_OFFSET
                                    : SPHERE_X_OFFSET
                    );

            float sphereY =
                    (float) Math.sin(
                            gameTime * IDLE_BOB_SPEED
                    ) * IDLE_BOB_AMPLITUDE;

            /*
             * Pequena variação para que os raios
             * não pareçam perfeitamente idênticos.
             */
            float pulse =
                    0.85F
                            + 0.15F * Mth.sin(
                            (float) (
                                    gameTime * 0.25
                                            + index * 1.7
                            )
                    );

            drawSegment(
                    matrix,
                    consumer,
                    sphereX,
                    sphereY,
                    0,
                    (float) targetX,
                    (float) targetY,
                    (float) targetZ,
                    1F,
                    1F,
                    1F,
                    (
                            0.20F
                                    + energyFactor * 0.80F
                    ) * pulse,
                    0.018F
                            + energyFactor * 0.045F
            );

            index++;
        }
    }

    private void renderBillboard(
            Matrix4f matrix,
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float size,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        consumer.vertex(
                        matrix,
                        x - size,
                        y - size,
                        z
                )
                .color(
                        red,
                        green,
                        blue,
                        alpha
                )
                .endVertex();

        consumer.vertex(
                        matrix,
                        x - size,
                        y + size,
                        z
                )
                .color(
                        red,
                        green,
                        blue,
                        alpha
                )
                .endVertex();

        consumer.vertex(
                        matrix,
                        x + size,
                        y + size,
                        z
                )
                .color(
                        red,
                        green,
                        blue,
                        alpha
                )
                .endVertex();

        consumer.vertex(
                        matrix,
                        x + size,
                        y - size,
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

        if (direction.lengthSqr() < 0.000001D) {
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

        if (perpendicular.lengthSqr() < 0.000001D) {
            perpendicular =
                    new Vec3(
                            1,
                            0,
                            0
                    );
        } else {
            perpendicular =
                    perpendicular
                            .normalize();
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

        consumer.vertex(
                        matrix,
                        x1 - px,
                        y1 - py,
                        z1 - pz
                )
                .color(
                        red,
                        green,
                        blue,
                        alpha
                )
                .endVertex();

        consumer.vertex(
                        matrix,
                        x1 + px,
                        y1 + py,
                        z1 + pz
                )
                .color(
                        red,
                        green,
                        blue,
                        alpha
                )
                .endVertex();

        consumer.vertex(
                        matrix,
                        x2 + px,
                        y2 + py,
                        z2 + pz
                )
                .color(
                        red,
                        green,
                        blue,
                        alpha
                )
                .endVertex();

        consumer.vertex(
                        matrix,
                        x2 - px,
                        y2 - py,
                        z2 - pz
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