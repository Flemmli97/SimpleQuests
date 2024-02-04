package io.github.flemmli97.simplequests.forge;

import io.github.flemmli97.simplequests.QuestCommand;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.datapack.ProgressionTrackerRegistry;
import io.github.flemmli97.simplequests.datapack.QuestBaseRegistry;
import io.github.flemmli97.simplequests.datapack.QuestEntryRegistry;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.forge.client.ForgeClientHandler;
import io.github.flemmli97.simplequests.network.PacketRegistrar;
import io.github.flemmli97.simplequests.player.PlayerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
        MinecraftForge.EVENT_BUS.addListener(SimpleQuestForge::addReload);
        MinecraftForge.EVENT_BUS.addListener(SimpleQuestForge::command);
        MinecraftForge.EVENT_BUS.addListener(SimpleQuestForge::kill);
        MinecraftForge.EVENT_BUS.addListener(SimpleQuestForge::interactSpecific);
        MinecraftForge.EVENT_BUS.addListener(SimpleQuestForge::interactBlock);
        MinecraftForge.EVENT_BUS.addListener(SimpleQuestForge::breakBlock);
        if (FMLEnvironment.dist == Dist.CLIENT)
            MinecraftForge.EVENT_BUS.addListener(ForgeClientHandler::login);
        QuestBaseRegistry.register();
        QuestEntryRegistry.register();
        ProgressionTrackerRegistry.register();
        ConfigHandler.init();
        SimpleQuests.FTB_RANKS = ModList.get().isLoaded("ftbranks");
    }

    public static void commonSetup(FMLCommonSetupEvent event) {
        PacketRegistrar.registerServerPackets(new PacketRegistrar.ServerPacketRegister() {
            @Override
            public <P> void registerMessage(int index, ResourceLocation id, Class<P> clss, BiConsumer<P, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, P> decoder, BiConsumer<P, ServerPlayer> handler) {
                DISPATCHER.registerMessage(index, clss, encoder, decoder, handlerServer(handler), Optional.of(NetworkDirection.PLAY_TO_SERVER));
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

    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> handlerServer(BiConsumer<T, ServerPlayer> handler) {
        return (p, ctx) -> {
            ctx.get().enqueueWork(() -> handler.accept(p, ctx.get().getSender()));
            ctx.get().setPacketHandled(true);
        };
    }
}
