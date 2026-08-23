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
import org.slf4j.Logger;
import net.minecraft.world.level.block.entity.BlockEntityType;

//teste de commit, foi chat?
// O valor aqui deve corresponder ao modId definido no arquivo mods.toml
@Mod(AtomicAdditions.MODID)
public class AtomicAdditions
{
    // ID principal do mod
    public static final String MODID = "atomicadditions";

    // Sistema de logs
    private static final Logger LOGGER = LogUtils.getLogger();

    // Registro dos blocos
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    // Registro dos itens
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    // Registro das abas criativas
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Casing principal
    public static final RegistryObject<Block> CASING =
            BLOCKS.register("casing", () ->
                    new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)));

    public static final RegistryObject<BlockEntityType<AtomicCasingBlockEntity>> ATOMIC_CASING_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("atomic_casing",
                    () -> BlockEntityType.Builder.of(
                            AtomicCasingBlockEntity::new,
                            CASING.get()
                    ).build(null));

    // Casing de entrada
    public static final RegistryObject<Block> CASING_ENTRADA =
            BLOCKS.register("casing_entrada", () ->
                    new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)));

    // Casing de saída
    public static final RegistryObject<Block> CASING_SAIDA =
            BLOCKS.register("casing_saida", () ->
                    new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)));

    // Item correspondente ao Casing principal
    public static final RegistryObject<Item> CASING_ITEM =
            ITEMS.register("casing", () ->
                    new BlockItem(CASING.get(), new Item.Properties()));

    // Item correspondente ao Casing de entrada
    public static final RegistryObject<Item> CASING_ENTRADA_ITEM =
            ITEMS.register("casing_entrada", () ->
                    new BlockItem(CASING_ENTRADA.get(), new Item.Properties()));

    // Item correspondente ao Casing de saída
    public static final RegistryObject<Item> CASING_SAIDA_ITEM =
            ITEMS.register("casing_saida", () ->
                    new BlockItem(CASING_SAIDA.get(), new Item.Properties()));

    // Item temporário utilizado para testar a aba criativa
    public static final RegistryObject<Item> EXAMPLE_ITEM =
            ITEMS.register("example_item", () ->
                    new Item(new Item.Properties()));

    // Aba criativa temporária para testes
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB =
            CREATIVE_MODE_TABS.register("example_tab", () ->
                    CreativeModeTab.builder()
                            .withTabsBefore(CreativeModeTabs.COMBAT)
                            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(CASING_ITEM.get());
                                output.accept(CASING_ENTRADA_ITEM.get());
                                output.accept(CASING_SAIDA_ITEM.get());
                            })
                            .build());

    public AtomicAdditions(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Registra a configuração comum do mod
        modEventBus.addListener(this::commonSetup);

        // Registra blocos, itens e abas criativas
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);

        // Registra os eventos do Forge
        MinecraftForge.EVENT_BUS.register(this);

        // Adiciona os blocos às abas criativas
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Código executado durante a inicialização comum
        LOGGER.info("Atomic Additions carregado com sucesso!");
    }

    // Adiciona os três Casings à aba de blocos de construção
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
        {
            event.accept(CASING_ITEM.get());
            event.accept(CASING_ENTRADA_ITEM.get());
            event.accept(CASING_SAIDA_ITEM.get());
        }
    }

    // Evento executado quando o servidor começa
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Atomic Additions: servidor iniciado!");
    }

    // Eventos específicos do cliente
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