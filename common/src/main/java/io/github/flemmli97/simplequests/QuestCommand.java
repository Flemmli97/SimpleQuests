package io.github.flemmli97.simplequests;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.gui.QuestGui;
import io.github.flemmli97.simplequests.player.PlayerData;
import io.github.flemmli97.simplequests.player.QuestProgress;
import io.github.flemmli97.simplequests.quest.Quest;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class QuestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("simplequests")
                .then(Commands.literal("show").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.show)).executes(QuestCommand::show))
                .then(Commands.literal("accept").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.accept))
                        .then(Commands.argument("quest", ResourceLocationArgument.id())
                                .suggests(QuestCommand::quests).executes(QuestCommand::accept)))
                .then(Commands.literal("submit").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.submit)).executes(QuestCommand::submit))
                .then(Commands.literal("reload").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.reload, true)).executes(QuestCommand::reload))
                .then(Commands.literal("current").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.current)).executes(QuestCommand::current))
                .then(Commands.literal("reset").requires(src -> SimpleQuests.getHandler().hasPerm(src, QuestCommandPerms.reset)).executes(QuestCommand::reset)));
    }

    private static int show(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        QuestGui.openGui(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int accept(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "quest");
        Quest quest = QuestsManager.instance().getQuests().get(id);
        if (quest == null) {
            ctx.getSource().sendSuccess(new TextComponent(String.format("simplequests.quest.noexist", id)), false);
            return 0;
        }
        if (PlayerData.get(player).acceptQuest(quest))
            return Command.SINGLE_SUCCESS;
        return 0;
    }

    private static int current(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        QuestProgress prog = PlayerData.get(player).getCurrentQuest();
        if (prog != null) {
            ctx.getSource().sendSuccess(new TextComponent(String.format(ConfigHandler.lang.get("simplequests.current"), prog.getQuest().questTaskString)).withStyle(ChatFormatting.GOLD), false);
            List<String> finished = prog.finishedTasks();
            prog.getQuest().entries.entrySet().stream()
                    .filter(e -> !finished.contains(e.getKey()))
                    .forEach(e -> ctx.getSource().sendSuccess(e.getValue().translation(ctx.getSource().getServer()).withStyle(ChatFormatting.RED), false));
        } else {
            ctx.getSource().sendSuccess(new TextComponent(ConfigHandler.lang.get("simplequests.current.no")).withStyle(ChatFormatting.DARK_RED), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int submit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerData.get(player).submit();
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        ConfigHandler.reloadConfigs();
        ctx.getSource().sendSuccess(new TextComponent(ConfigHandler.lang.get("simplequests.reload")), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerData.get(player).reset();
        return Command.SINGLE_SUCCESS;
    }

    public static CompletableFuture<Suggestions> quests(CommandContext<CommandSourceStack> context, SuggestionsBuilder build) throws CommandSyntaxException {
        return SharedSuggestionProvider.suggest(acceptableQuests(context.getSource().getPlayerOrException()), build);
    }

    private static List<String> acceptableQuests(ServerPlayer player) {
        return QuestsManager.instance().getQuests()
                .entrySet().stream()
                .filter(e -> PlayerData.get(player).canAcceptQuest(e.getValue()) == PlayerData.AcceptType.ACCEPT)
                .map(e -> e.getKey().toString()).collect(Collectors.toList());
    }
}
