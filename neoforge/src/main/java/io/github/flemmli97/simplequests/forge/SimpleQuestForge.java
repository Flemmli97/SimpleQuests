package io.github.flemmli97.simplequests.forge;

import io.github.flemmli97.simplequests.QuestCommand;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.datapack.QuestBaseRegistry;
import io.github.flemmli97.simplequests.datapack.QuestEntryRegistry;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.forge.client.ForgeClientHandler;
import io.github.flemmli97.simplequests.network.PacketRegistrar;
import io.github.flemmli97.simplequests.player.PlayerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.simple.MessageFunctions;
import net.neoforged.neoforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;

@Mod(value = SimpleQuests.MODID)
public class SimpleQuestForge {

    public static final SimpleChannel DISPATCHER =
            NetworkRegistry.ChannelBuilder.named(new ResourceLocation(SimpleQuests.MODID, "packets"))
                    .clientAcceptedVersions(a -> true)
                    .serverAcceptedVersions(a -> true)
                    .networkProtocolVersion(() -> "v1.0").simpleChannel();

    public SimpleQuestForge() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "*", (s1, s2) -> true));
        SimpleQuests.updateLoaderImpl(new LoaderImpl());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(SimpleQuestForge::commonSetup);
        NeoForge.EVENT_BUS.addListener(SimpleQuestForge::addReload);
        NeoForge.EVENT_BUS.addListener(SimpleQuestForge::command);
        NeoForge.EVENT_BUS.addListener(SimpleQuestForge::kill);
        NeoForge.EVENT_BUS.addListener(SimpleQuestForge::interactSpecific);
        NeoForge.EVENT_BUS.addListener(SimpleQuestForge::interactBlock);
        NeoForge.EVENT_BUS.addListener(SimpleQuestForge::breakBlock);
        if (FMLEnvironment.dist == Dist.CLIENT)
            NeoForge.EVENT_BUS.addListener(ForgeClientHandler::login);
        QuestBaseRegistry.register();
        QuestEntryRegistry.register();
        ConfigHandler.init();
        SimpleQuests.FTB_RANKS = ModList.get().isLoaded("ftbranks");
    }

    public static void commonSetup(FMLCommonSetupEvent event) {
        PacketRegistrar.registerServerPackets(new PacketRegistrar.ServerPacketRegister() {
            @Override
            public <P> void registerMessage(int index, ResourceLocation id, Class<P> clss, BiConsumer<P, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, P> decoder, BiConsumer<P, ServerPlayer> handler) {
                DISPATCHER.registerMessage(index, clss, encoder::accept, decoder::apply, handlerServer(handler));
            }
        }, 0);
    }

    public static void addReload(AddReloadListenerEvent event) {
        event.addListener(QuestsManager.instance());
    }

    public static void command(RegisterCommandsEvent event) {
        QuestCommand.register(event.getDispatcher());
    }

    public static void kill(LivingDeathEvent event) {
        if (event.getEntity().getKillCredit() instanceof ServerPlayer player) {
            PlayerData.get(player).onKill(event.getEntity());
        }
    }

    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
            SimpleQuests.onInteractEntity(serverPlayer, event.getTarget(), event.getHand());
    }

    public static void interactBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getUseBlock() != Event.Result.DENY)
            PlayerData.get(serverPlayer).onBlockInteract(event.getPos(), true);
    }

    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer)
            PlayerData.get(serverPlayer).onBlockInteract(event.getPos(), false);
    }

    private static <T> MessageFunctions.MessageConsumer<T> handlerServer(BiConsumer<T, ServerPlayer> handler) {
        return (p, ctx) -> {
            ctx.enqueueWork(() -> handler.accept(p, ctx.getSender()));
            ctx.setPacketHandled(true);
        };
    }
}
