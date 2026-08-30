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

    private static final float SPHERE_RADIUS =
            0.38F;

    private static final float SPHERE_X_OFFSET =
            0.95F;

    private static final float SMOKE_RADIUS =
            0.48F;

    private static final float ATMOSPHERE_RADIUS =
            0.52F;

    private static final int SPHERE_SEGMENTS =
            14;

    private static final int SPHERE_RINGS =
            8;

    private static final int SMOKE_PARTICLES =
            16;

    private static final int ATMOSPHERE_ARCS =
            8;

    private static final float IDLE_BOB_AMPLITUDE =
            0.06F;

    private static final float IDLE_BOB_SPEED =
            0.06F;

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
         * Os dados visuais vêm do snapshot sincronizado
         * pelo servidor.
         */
        List<Integer> colors =
                getActiveInputColors(multiblock);

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
         * Leve oscilação vertical existente mesmo parado.
         */
        float idleBob =
                (float) Math.sin(
                        gameTime * IDLE_BOB_SPEED
                ) * IDLE_BOB_AMPLITUDE;

        /*
         * Intensidade da energia utilizada.
         *
         * 0     = sem energia
         * 1     = 80 MFE
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
         * Velocidade de rotação proporcional à energia.
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
             index < colors.size();
             index++) {

            int color =
                    colors.get(index);

            float x;

            if (colors.size() == 1) {

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
                    color,
                    SPHERE_RADIUS,
                    0.92F
            );

            /*
             * Fumaça colorida.
             */
            renderSmoke(
                    poseStack,
                    consumer,
                    color,
                    gameTime,
                    index,
                    active
                            ? 0.75F
                            : 0.45F
            );

            if (active) {

                /*
                 * Atmosfera individual de mini-raios.
                 *
                 * Sempre branca.
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
         * Raios entre as esferas e as bobinas.
         */
        if (active) {

            renderCoilRays(
                    poseStack,
                    consumer,
                    multiblock,
                    energyFactor,
                    gameTime,
                    colors.size()
            );
        }

        poseStack.popPose();
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
             * Evita desenhar duas esferas iguais caso,
             * por algum motivo, os dois lados contenham
             * o mesmo gás.
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

                addVertex(
                        consumer,
                        matrix,
                        Mth.cos(phi1)
                                * ringRadius1,
                        y1,
                        Mth.sin(phi1)
                                * ringRadius1,
                        red,
                        green,
                        blue,
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
                        red,
                        green,
                        blue,
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
                        red,
                        green,
                        blue,
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
                        red,
                        green,
                        blue,
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
                            + (i % 3)
                            * 0.025F;

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
         * Poucos raios com pouca energia.
         * Mais energia = atmosfera mais densa.
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
             * Distribui as bobinas entre as esferas.
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

            float pulse =
                    0.85F
                            + 0.15F
                            * Mth.sin(
                            (float) (
                                    gameTime * 0.25
                                            + index * 1.7
                            )
                    );

            /*
             * Raio branco.
             */
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
        addVertex(
                consumer,
                matrix,
                x - size,
                y - size,
                z,
                red,
                green,
                blue,
                alpha
        );

        addVertex(
                consumer,
                matrix,
                x - size,
                y + size,
                z,
                red,
                green,
                blue,
                alpha
        );

        addVertex(
                consumer,
                matrix,
                x + size,
                y + size,
                z,
                red,
                green,
                blue,
                alpha
        );

        addVertex(
                consumer,
                matrix,
                x + size,
                y - size,
                z,
                red,
                green,
                blue,
                alpha
        );
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