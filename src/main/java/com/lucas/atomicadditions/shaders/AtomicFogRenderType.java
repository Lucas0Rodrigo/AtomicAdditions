package com.lucas.atomicadditions.shaders;

import com.lucas.atomicadditions.shaders.AtomicShaders;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderType;

public class AtomicFogRenderType extends RenderType {

    private AtomicFogRenderType(
            String name,
            VertexFormat format,
            VertexFormat.Mode mode,
            int bufferSize,
            boolean affectsCrumbling,
            boolean sortOnUpload,
            Runnable setupState,
            Runnable clearState
    ) {
        super(
                name,
                format,
                mode,
                bufferSize,
                affectsCrumbling,
                sortOnUpload,
                setupState,
                clearState
        );
    }

    public static final RenderType FOG =
            create(
                    "atomic_fog",
                    DefaultVertexFormat.POSITION_COLOR,
                    VertexFormat.Mode.QUADS,
                    256,
                    true,
                    true,
                    CompositeState.builder()
                            .setShaderState(
                                    AtomicShaders.FOG.shard
                            )
                            .setTransparencyState(
                                    TRANSLUCENT_TRANSPARENCY
                            )
                            .setCullState(
                                    NO_CULL
                            )
                            .setDepthTestState(
                                    LEQUAL_DEPTH_TEST
                            )
                            .setWriteMaskState(
                                    COLOR_WRITE
                            )
                            .createCompositeState(
                                    true
                            )
            );
}