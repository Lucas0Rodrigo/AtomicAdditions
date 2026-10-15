package com.lucas.atomicadditions;

import com.lucas.atomicadditions.chemical.*;
import com.lucas.atomicadditions.item.ActivatedCrystalBakedModel;
import com.lucas.atomicadditions.item.ActivatedCrystalItem;
import com.lucas.atomicadditions.multiblock.*;
import com.lucas.atomicadditions.recipes.AtomicActivatedCrystalRecipeInjector;
import com.lucas.atomicadditions.recipes.AtomicRecipeSerializers;
import com.lucas.atomicadditions.recipes.AtomicRecipes;
import com.mojang.logging.LogUtils;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.lib.multiblock.MultiblockCache;
import mekanism.common.lib.multiblock.MultiblockManager;
import mekanism.common.registration.impl.SoundEventDeferredRegister;
import mekanism.common.registration.impl.SoundEventRegistryObject;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(AtomicAdditions.MODID)
public class AtomicAdditions {

    public static final String MODID = "atomicadditions";

    public static final Logger LOGGER =
            LogUtils.getLogger();

    public static final SoundEventDeferredRegister SOUND_EVENTS =
            new SoundEventDeferredRegister(MODID);

    public static final SoundEventRegistryObject<SoundEvent> AMR_SOUND =
            SOUND_EVENTS.register(
                    "tile.machine.amr"
            );

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(
                    Registries.BLOCK,
                    MODID
            );

    public static final TileEntityTypeDeferredRegister
            BLOCK_ENTITIES =
            new TileEntityTypeDeferredRegister(
                    MODID
            );

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(
                    Registries.ITEM,
                    MODID
            );

    public static final DeferredRegister<net.minecraft.world.item.CreativeModeTab>
            CREATIVE_MODE_TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    MODID
            );

    public static final MultiblockManager<AtomicMultiblockData>
            ATOMIC_MANAGER =
            new MultiblockManager<>(
                    "atomic",
                    MultiblockCache::new,
                    AtomicValidator::new
            );

    public static final DeferredHolder<Block, Block> CASING =
            BLOCKS.register(
                    "casing",
                    AtomicAdditions::createCasing
            );

    public static final DeferredHolder<Block, Block> CASING_PORT =
            BLOCKS.register(
                    "casing_port",
                    AtomicAdditions::createCasingPort
            );

    public static final TileEntityTypeRegistryObject<AtomicCasingBlockEntity>
            ATOMIC_CASING_BLOCK_ENTITY =
            BLOCK_ENTITIES.mekBuilder(
                    CASING,
                    AtomicCasingBlockEntity::new
            ).build();

    public static final TileEntityTypeRegistryObject<AtomicPortBlockEntity>
            ATOMIC_PORT_BLOCK_ENTITY =
            BLOCK_ENTITIES.mekBuilder(
                    CASING_PORT,
                    AtomicPortBlockEntity::new
            ).withSimple(
                    Capabilities.CONFIGURABLE
            ).build();

    public static final DeferredHolder<Item, Item> CASING_ITEM =
            ITEMS.register(
                    "casing",
                    () -> new BlockItem(
                            CASING.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredHolder<Item, Item> CASING_PORT_ITEM =
            ITEMS.register(
                    "casing_port",
                    () -> new BlockItem(
                            CASING_PORT.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredHolder<Item, Item> ACTIVATED_CRYSTAL =
            ITEMS.register(
                    "activated_crystal",
                    () -> new ActivatedCrystalItem(
                            new Item.Properties()
                    )
            );

    private static AtomicCasingBlock<AtomicCasingBlockEntity>
    createCasing() {
        return new AtomicCasingBlock<>(
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL),
                ATOMIC_CASING_BLOCK_ENTITY
        );
    }

    private static AtomicPortBlock
    createCasingPort() {
        return new AtomicPortBlock(
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL),
                ATOMIC_PORT_BLOCK_ENTITY
        );
    }

    public AtomicAdditions(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {
        modEventBus.addListener(
                this::commonSetup
        );

        modEventBus.addListener(
                this::clientSetup
        );

        SOUND_EVENTS.register(
                modEventBus
        );

        BLOCKS.register(
                modEventBus
        );

        BLOCK_ENTITIES.register(
                modEventBus
        );

        ITEMS.register(
                modEventBus
        );

        CREATIVE_MODE_TABS.register(
                modEventBus
        );

        AtomicRecipeSerializers.RECIPE_SERIALIZERS
                .register(
                        modEventBus
                );

        AtomicRecipeSerializers.INGREDIENT_TYPES
                .register(
                        modEventBus
                );

        AtomicContainerTypes.CONTAINER_TYPES
                .register(
                        modEventBus
                );

        AtomicGases.GASES.register(
                modEventBus
        );

        NeoForge.EVENT_BUS.addListener(
                AtomicRecipes::addReloadListener
        );

        NeoForge.EVENT_BUS.addListener(
                AtomicActivatedCrystalRecipeInjector::onDatapackSync
        );

        NeoForge.EVENT_BUS.register(
                this
        );

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
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(
                    ATOMIC_CASING_BLOCK_ENTITY.get(),
                    AtomicCasingRenderer::new
            );

            BlockEntityRenderers.register(
                    ATOMIC_PORT_BLOCK_ENTITY.get(),
                    AtomicCasingRenderer::new
            );
        });
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

    @EventBusSubscriber(
            modid = MODID,
            value = Dist.CLIENT
    )
    public static class ClientModEvents {

        @SubscribeEvent
        public static void registerMenuScreens(
                RegisterMenuScreensEvent event
        ) {
            event.register(
                    AtomicContainerTypes.ATOMIC.get(),
                    AtomicScreen::new
            );
        }

        @SubscribeEvent
        public static void registerAdditionalModels(
                ModelEvent.RegisterAdditional event
        ) {
            event.register(
                    ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(
                                    MODID,
                                    "item/activated_crystal_overlay"
                            )
                    )
            );
        }

        @SubscribeEvent
        public static void modifyBakingResult(
                ModelEvent.ModifyBakingResult event
        ) {
            event.getModels().computeIfPresent(
                    ModelResourceLocation.inventory(
                            ResourceLocation.fromNamespaceAndPath(
                                    MODID,
                                    "activated_crystal"
                            )
                    ),
                    (location, model) ->
                            new ActivatedCrystalBakedModel(
                                    model
                            )
            );
        }

        @SubscribeEvent
        public static void registerItemColors(
                RegisterColorHandlersEvent.Item event
        ) {
            event.register(
                    (stack, tintIndex) -> {

                        ResourceLocation sourceId =
                                ActivatedCrystalItem
                                        .getSourceCrystalId(
                                                stack
                                        );

                        if (sourceId == null
                                || sourceId.equals(
                                ResourceLocation.fromNamespaceAndPath(
                                        MODID,
                                        "activated_crystal"
                                )
                        )
                                || !BuiltInRegistries.ITEM.containsKey(
                                sourceId
                        )) {

                            return -1;
                        }

                        ItemStack sourceStack =
                                ActivatedCrystalItem
                                        .getSourceCrystalStack(
                                                stack
                                        );

                        if (sourceStack.isEmpty()) {
                            return -1;
                        }

                        return event.getItemColors()
                                .getColor(
                                        sourceStack,
                                        tintIndex
                                );
                    },
                    ACTIVATED_CRYSTAL.get()
            );
        }
    }
}