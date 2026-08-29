package com.lucas.atomicadditions;

import com.lucas.atomicadditions.multiblock.*;
import com.lucas.atomicadditions.chemical.*;
import com.lucas.atomicadditions.recipes.AtomicRecipes;
import com.mojang.logging.LogUtils;
import mekanism.common.lib.multiblock.MultiblockCache;
import mekanism.common.lib.multiblock.MultiblockManager;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.client.gui.screens.MenuScreens;
import org.slf4j.Logger;

@Mod(AtomicAdditions.MODID)
public class AtomicAdditions {

    public static final String MODID = "atomicadditions";

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(
                    ForgeRegistries.BLOCKS,
                    MODID
            );

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(
                    ForgeRegistries.BLOCK_ENTITY_TYPES,
                    MODID
            );

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(
                    ForgeRegistries.ITEMS,
                    MODID
            );

    public static final DeferredRegister<net.minecraft.world.item.CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    MODID
            );

    public static final MultiblockManager<AtomicMultiblockData> ATOMIC_MANAGER =
            new MultiblockManager<>(
                    "atomic",
                    MultiblockCache::new,
                    AtomicValidator::new
            );

    public static final RegistryObject<Block> CASING =
            BLOCKS.register(
                    "casing",
                    AtomicAdditions::createCasing
            );

    public static final RegistryObject<Block> CASING_PORT =
            BLOCKS.register(
                    "casing_port.json",
                    AtomicAdditions::createCasingPort
            );

    public static final RegistryObject<BlockEntityType<AtomicCasingBlockEntity>>
            ATOMIC_CASING_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "atomic_casing",
                    () -> BlockEntityType.Builder.of(
                            AtomicCasingBlockEntity::new,
                            CASING.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<AtomicPortBlockEntity>>
            ATOMIC_PORT_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "atomic_port",
                    () -> BlockEntityType.Builder.of(
                            AtomicPortBlockEntity::new,
                            CASING_PORT.get()
                    ).build(null)
            );

    public static final RegistryObject<Item> CASING_ITEM =
            ITEMS.register(
                    "casing",
                    () -> new BlockItem(
                            CASING.get(),
                            new Item.Properties()
                    )
            );

    public static final RegistryObject<Item> CASING_PORT_ITEM =
            ITEMS.register(
                    "casing_port.json",
                    () -> new BlockItem(
                            CASING_PORT.get(),
                            new Item.Properties()
                    )
            );

    private static AtomicCasingBlock<AtomicCasingBlockEntity> createCasing() {
        return new AtomicCasingBlock<>(
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL),
                new TileEntityTypeRegistryObject<>(
                        ATOMIC_CASING_BLOCK_ENTITY
                )
        );
    }

    private static AtomicPortBlock createCasingPort() {
        return new AtomicPortBlock(
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL),
                new TileEntityTypeRegistryObject<>(
                        ATOMIC_PORT_BLOCK_ENTITY
                )
        );
    }

    public AtomicAdditions(
            FMLJavaModLoadingContext context
    ) {
        IEventBus modEventBus =
                context.getModEventBus();

        modEventBus.addListener(
                this::commonSetup
        );

        modEventBus.addListener(
                this::clientSetup
        );

        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        AtomicContainerTypes.CONTAINER_TYPES.register(modEventBus);

        AtomicGases.GASES.register(modEventBus);

        MinecraftForge.EVENT_BUS.addListener(
                AtomicRecipes::addReloadListener
        );

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(
                this::addCreative
        );
    }

    private void commonSetup(
            final FMLCommonSetupEvent event
    ) {
        LOGGER.info(
                "Atomic Additions carregado com sucesso!"
        );
    }

    private void clientSetup(
            final FMLClientSetupEvent event
    ) {
        event.enqueueWork(() ->
                MenuScreens.register(
                        AtomicContainerTypes.ATOMIC.get(),
                        AtomicScreen::new
                )
        );
    }

    private void addCreative(
            BuildCreativeModeTabContentsEvent event
    ) {
        if (event.getTabKey() ==
                CreativeModeTabs.BUILDING_BLOCKS) {

            event.accept(
                    CASING_ITEM.get()
            );

            event.accept(
                    CASING_PORT_ITEM.get()
            );
        }
    }

    @SubscribeEvent
    public void onServerStarting(
            ServerStartingEvent event
    ) {
        LOGGER.info(
                "Atomic Additions: servidor iniciado!"
        );
    }

    @Mod.EventBusSubscriber(
            modid = MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT
    )
    public static class ClientModEvents {
    }
}