package com.lucas.atomicadditions.shaders;

import com.lucas.atomicadditions.AtomicAdditions;
import java.io.IOException;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = AtomicAdditions.MODID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class AtomicShaders {

    public static final ShaderTracker FOG =
            new ShaderTracker();

    private AtomicShaders() {
    }

    @SubscribeEvent
    public static void registerShaders(
            RegisterShadersEvent event
    ) throws IOException {

        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        AtomicAdditions.rl(
                                "atomic_fog"
                        ),
                        DefaultVertexFormat.POSITION_COLOR
                ),
                FOG::setInstance
        );
    }

    public static class ShaderTracker
            implements Supplier<ShaderInstance> {

        private ShaderInstance instance;

        public final ShaderStateShard shard =
                new ShaderStateShard(this);

        private void setInstance(
                ShaderInstance instance
        ) {
            this.instance =
                    instance;
        }

        @Override
        public ShaderInstance get() {
            return instance;
        }
    }
}