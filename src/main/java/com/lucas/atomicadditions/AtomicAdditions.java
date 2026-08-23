package com.lucas.atomicadditions;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.Logger;

@Mod(AtomicAdditions.MODID)
public class AtomicAdditions
{
    public static final String MODID = "atomicadditions";

    private static final Logger LOGGER = LogUtils.getLogger();

    // Registro dos blocos
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    // Registro dos Block Entities
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    // Registro dos itens
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    // Registro das abas criativas
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);


    // =========================================================
    // BLOCOS
    // =========================================================

    // Casing principal
    public static final RegistryObject<Block> CASING =
            BLOCKS.register("casing", () ->
                    new AtomicCasingBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)));

    // Casing Port
    public static final RegistryObject<Block> CASING_PORT =
            BLOCKS.register("casing_port", () ->
                    new AtomicPortBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)));

    // Block Entity do Casing principal
    public static final RegistryObject<BlockEntityType<AtomicCasingBlockEntity>> ATOMIC_CASING_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("atomic_casing",
                    () -> BlockEntityType.Builder.of(
                            AtomicCasingBlockEntity::new,
                            CASING.get()
                    ).build(null));


    // =========================================================
    // ITENS
    // =========================================================

    // Item correspondente ao Casing principal
    public static final RegistryObject<Item> CASING_ITEM =
            ITEMS.register("casing", () ->
                    new BlockItem(CASING.get(), new Item.Properties()));

    // Item correspondente ao Casing Port
    public static final RegistryObject<Item> CASING_PORT_ITEM =
            ITEMS.register("casing_port", () ->
                    new BlockItem(CASING_PORT.get(), new Item.Properties()));

    // Itens antigos — serão removidos depois
    public static final RegistryObject<Item> CASING_ENTRADA_ITEM =
            ITEMS.register("casing_entrada", () ->
                    new BlockItem(
                            BLOCKS.register("casing_entrada", () ->
                                    new Block(BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL))).get(),
                            new Item.Properties()));

    public static final RegistryObject<Item> CASING_SAIDA_ITEM =
            ITEMS.register("casing_saida", () ->
                    new Block(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                    ).asItem());


    // Item temporário
    public static final RegistryObject<Item> EXAMPLE_ITEM =
            ITEMS.register("example_item", () ->
                    new Item(new Item.Properties()));


    // =========================================================
    // ABA CRIATIVA
    // =========================================================

    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB =
            CREATIVE_MODE_TABS.register("example_tab", () ->
                    CreativeModeTab.builder()
                            .withTabsBefore(CreativeModeTabs.COMBAT)
                            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {

                                output.accept(CASING_ITEM.get());
                                output.accept(CASING_PORT_ITEM.get());

                            })
                            .build());


    // =========================================================
    // CONSTRUTOR
    // =========================================================

    public AtomicAdditions(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);
    }


    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Atomic Additions carregado com sucesso!");
    }


    // =========================================================
    // ABA BUILDING BLOCKS
    // =========================================================

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
        {
            event.accept(CASING_ITEM.get());
            event.accept(CASING_PORT_ITEM.get());
        }
    }


    // =========================================================
    // SERVIDOR
    // =========================================================

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Atomic Additions: servidor iniciado!");
    }


    // =========================================================
    // CLIENTE
    // =========================================================

    @Mod.EventBusSubscriber(
            modid = MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT
    )
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            LOGGER.info("Atomic Additions: cliente iniciado!");

            LOGGER.info("Jogador: {}",
                    Minecraft.getInstance().getUser().getName());
        }
    }
}