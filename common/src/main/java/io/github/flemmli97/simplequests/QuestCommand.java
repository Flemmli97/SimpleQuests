package io.github.flemmli97.simplequests;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.gui.CurrentQuestGui;
import io.github.flemmli97.simplequests.gui.QuestCategoryGui;
import io.github.flemmli97.simplequests.gui.QuestGui;
import io.github.flemmli97.simplequests.player.PlayerData;
import io.github.flemmli97.simplequests.player.QuestProgress;
import io.github.flemmli97.simplequests.quest.QuestCategory;
import io.github.flemmli97.simplequests.quest.types.CompositeQuest;
import io.github.flemmli97.simplequests.quest.types.QuestBase;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class QuestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("simplequests")
                .then(Commands.literal("quests").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.QUESTS)).executes(QuestCommand::show)
                        .then(Commands.argument("category", ResourceLocationArgument.id())
                                .requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.SHOW_CATEGORY, true)).suggests(QuestCommand::questCategories).executes(QuestCommand::showCategory)))
                .then(Commands.literal("accept").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.ACCEPT))
                        .then(Commands.argument("quest", ResourceLocationArgument.id())
                                .suggests(QuestCommand::quests).executes(QuestCommand::accept))
                        .then(Commands.literal("select").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.SELECT)).then(Commands.argument("quest", ResourceLocationArgument.id())
                                .suggests(QuestCommand::questsComposite).then(Commands.argument("select", ResourceLocationArgument.id())).executes(QuestCommand::acceptComposite))))
                .then(Commands.literal("submit").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.SUBMIT)).executes(QuestCommand::submit)
                        .then(Commands.argument("type", StringArgumentType.string()).requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.SUBMIT_TYPE, true))
                                .executes(QuestCommand::submitType)))
                .then(Commands.literal("current").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.CURRENT))
                        .executes(QuestCommand::current))
                .then(Commands.literal("reset").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.RESET))
                        .then(Commands.argument("quest", ResourceLocationArgument.id()).suggests(QuestCommand::activequests).executes(QuestCommand::reset)))
                .then(Commands.literal("resetAll").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.RESET_ALL, true))
                        .then(Commands.argument("target", EntityArgument.players()).executes(QuestCommand::resetAll)))
                .then(Commands.literal("resetCooldown").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.RESET_COOLDOWN, true))
                        .then(Commands.argument("target", EntityArgument.players()).executes(QuestCommand::resetCooldown)))
                .then(Commands.literal("reload").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.RELOAD, true)).executes(QuestCommand::reload))
                .then(Commands.literal("unlock").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.UNLOCK, true))
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.argument("quest", ResourceLocationArgument.id())
                                        .suggests(QuestCommand::lockedQuests).executes(QuestCommand::unlock)))));
    }

    private static int show(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (QuestsManager.instance().categories().size() == 1)
            QuestGui.openGui(player, QuestsManager.instance().categories().get(0), false, 0);
        QuestCategoryGui.openGui(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int showCategory(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "category");
        QuestCategory category = QuestsManager.instance().getQuestCategory(id);
        if (category == null) {
            ctx.getSource().sendFailure(Component.translatable(ConfigHandler.LANG.get(player, "simplequests.quest.category.noexist"), id));
            return 0;
        }
        QuestGui.openGui(player, category, false, 0);
        return Command.SINGLE_SUCCESS;
    }

    private static int accept(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "quest");
        QuestBase base = QuestsManager.instance().getAllQuests().get(id);
        if (base == null || (!SimpleQuests.getHandler().hasPerm(ctx.getSource(), QuestCommandPerms.ACCEPTADMIN, true) && !base.category.canBeSelected)) {
            ctx.getSource().sendSuccess(Component.translatable(ConfigHandler.LANG.get(player, "simplequests.quest.noexist"), id), false);
            return 0;
        }
        if (base instanceof CompositeQuest) {
            ctx.getSource().sendSuccess(Component.translatable(ConfigHandler.LANG.get(player, "simplequests.quest.is_selection"), id), false);
            return 0;
        }
        if (PlayerData.get(player).acceptQuest(base, 0))
            return Command.SINGLE_SUCCESS;
        return 0;
    }

    private static int acceptComposite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "quest");
        QuestBase base = QuestsManager.instance().getAllQuests().get(id);
        if (base == null || (!SimpleQuests.getHandler().hasPerm(ctx.getSource(), QuestCommandPerms.ACCEPTADMIN, true) && !base.category.canBeSelected)) {
            ctx.getSource().sendSuccess(Component.translatable(ConfigHandler.LANG.get(player, "simplequests.quest.noexist"), id), false);
            return 0;
        }
        if (!(base instanceof CompositeQuest composite)) {
            ctx.getSource().sendSuccess(Component.translatable(ConfigHandler.LANG.get(player, "simplequests.quest.composite.noexist"), id), false);
            return 0;
        }
        ResourceLocation select = ResourceLocationArgument.getId(ctx, "select");
        int i = -1;
        for (int idx = 0; idx < composite.getCompositeQuests().size(); idx++) {
            if (composite.getCompositeQuests().get(idx).equals(select)) {
                i = idx;
                break;
            }
        }
        QuestBase quest = composite.resolveToQuest(player, i);
        if (quest == null) {
            ctx.getSource().sendSuccess(Component.translatable(ConfigHandler.LANG.get(player, "simplequests.quest.composite.resolve.none"), composite, select), false);
            return 0;
        }
        if (PlayerData.get(player).acceptQuest(composite, i))
            return Command.SINGLE_SUCCESS;
        return 0;
    }

    private static int current(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CurrentQuestGui.openGui(player);
        return 0;
    }

    private static int submit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!PlayerData.get(player).submit("", true).isEmpty())
            return Command.SINGLE_SUCCESS;
        return 0;
    }

    private static int submitType(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String type = StringArgumentType.getString(ctx, "type");
        if (!PlayerData.get(player).submit(type, true).isEmpty())
            return Command.SINGLE_SUCCESS;
        return 0;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        ConfigHandler.reloadConfigs();
        ctx.getSource().sendSuccess(Component.translatable(ConfigHandler.LANG.get(null, "simplequests.reload")), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResourceLocation res = ResourceLocationArgument.getId(ctx, "quest");
        PlayerData.get(player).reset(res, false);
        return Command.SINGLE_SUCCESS;
    }

    private static int resetCooldown(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int i = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "target")) {
            PlayerData.get(player).resetCooldown();
            ctx.getSource().sendSuccess(Component.translatable(ConfigHandler.LANG.get(null, "simplequests.reset.cooldown"), player.getName()).withStyle(ChatFormatting.DARK_RED), true);
            i++;
        }
        return i;
    }

    private static int resetAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int i = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "target")) {
            PlayerData.get(player).resetAll();
            ctx.getSource().sendSuccess(Component.translatable(ConfigHandler.LANG.get(null, "simplequests.reset.all"), player.getName()).withStyle(ChatFormatting.DARK_RED), true);
            i++;
        }
        return i;
    }

    private static int unlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation res = ResourceLocationArgument.getId(ctx, "quest");
        if (!QuestsManager.instance().getAllQuests().containsKey(res)) {
            ctx.getSource().sendSuccess(Component.translatable(ConfigHandler.LANG.get(null, "simplequests.unlock.fail"), res), true);
            return 0;
        }
        int i = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "target")) {
            PlayerData.get(player)
                    .unlockQuest(res);
            ctx.getSource().sendSuccess(Component.translatable(ConfigHandler.LANG.get(null, "simplequests.unlock"), player.getName(), res), true);
            i++;
        }
        return i;
    }

    public static CompletableFuture<Suggestions> quests(CommandContext<CommandSourceStack> context, SuggestionsBuilder build) throws CommandSyntaxException {
        return SharedSuggestionProvider.suggest(acceptableQuests(context.getSource().getPlayerOrException(), false), build);
    }

    public static CompletableFuture<Suggestions> questsComposite(CommandContext<CommandSourceStack> context, SuggestionsBuilder build) throws CommandSyntaxException {
        return SharedSuggestionProvider.suggest(acceptableQuests(context.getSource().getPlayerOrException(), true), build);
    }

    public static CompletableFuture<Suggestions> activequests(CommandContext<CommandSourceStack> context, SuggestionsBuilder build) throws CommandSyntaxException {
        return SharedSuggestionProvider.suggest(PlayerData.get(context.getSource().getPlayerOrException())
                .getCurrentQuest().stream().map(prog -> prog.getQuest().id.toString())
                .toList(), build);
    }

    private static List<String> acceptableQuests(ServerPlayer player, boolean composite) {
        return QuestsManager.instance().getAllQuests()
                .entrySet().stream()
                .filter(e -> e.getValue().category.canBeSelected && (e.getValue() instanceof CompositeQuest == composite) && PlayerData.get(player).canAcceptQuest(e.getValue()) == PlayerData.AcceptType.ACCEPT)
                .map(e -> e.getKey().toString()).collect(Collectors.toList());
    }

    public static CompletableFuture<Suggestions> lockedQuests(CommandContext<CommandSourceStack> context, SuggestionsBuilder build) {
        return SharedSuggestionProvider.suggest(QuestsManager.instance().getAllQuests()
                .entrySet().stream()
                .filter(e -> e.getValue().needsUnlock)
                .map(e -> e.getKey().toString()).collect(Collectors.toList()), build);
    }

    public static CompletableFuture<Suggestions> questCategories(CommandContext<CommandSourceStack> context, SuggestionsBuilder build) {
        return SharedSuggestionProvider.suggest(QuestsManager.instance().getCategories()
                .keySet().stream()
                .map(ResourceLocation::toString).collect(Collectors.toList()), build);
    }
}
