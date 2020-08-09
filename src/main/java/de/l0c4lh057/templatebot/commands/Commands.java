package de.l0c4lh057.templatebot.commands;

import de.l0c4lh057.templatebot.utils.exceptions.BotException;
import de.l0c4lh057.templatebot.main.BotMain;
import de.l0c4lh057.templatebot.utils.BotUtils;
import discord4j.common.GitProperties;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.User;
import discord4j.core.util.EntityUtil;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.EmbedFieldData;
import discord4j.discordjson.json.MessageEditRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static de.l0c4lh057.templatebot.utils.BotUtils.getLanguageString;

public class Commands {
	
	private Commands(){}
	
	private static final Logger logger = LogManager.getLogger("Commands");
	
	/**
	 * The {@link Map} in which all {@link Command}s are stored. Every alias maps to the command.
	 */
	static final Map<String, Command> commands = new HashMap<>();
	
	/**
	 * All commands should get registered in here. If any command is registered in another class (e.g. a music bot class
	 * with which has its own function to register all commands) it should get called in this function.
	 */
	public static void registerCommands(){
		logger.info("Registering all commands");
		
		Command.builder()
				.setName("help")
				.setExecutor((event, language, prefix, args) -> {
					if(!args.isEmpty()){
						Command command = Commands.getCommand(args.get(0));
						if(command != null){
							String commandName = command.getName();
							List<String> commandTree = new ArrayList<>();
							commandTree.add(command.getName());
							for(int i = 1; i < args.size(); i++){
								Command cmd = command.getSubCommand(args.get(i));
								if(cmd == null) break;
								commandTree.add(cmd.getName());
								command = cmd;
							}
							return event.getMessage().getRestChannel().createMessage(EmbedData.builder()
									.title(getLanguageString(language, "help.command.title", commandName))
									.description(getLanguageString(language, "help." + String.join(".", commandTree) + ".detailed", prefix))
									.build()
							);
						}
					}
					Command.Category category = BotUtils.getHelpPage(language, args);
					AtomicInteger helpPage = new AtomicInteger(category.getHelpPage());
					return event.getMessage().getRestChannel().createMessage(BotUtils.getHelpEmbedData(language, prefix, category))
							.flatMap(messageData -> Mono.when(
									event.getMessage().getRestChannel().getRestMessage(Snowflake.of(messageData.id())).createReaction(EntityUtil.getEmojiString(BotUtils.EMOJI_ARROW_LEFT))
											.then(event.getMessage().getRestChannel().getRestMessage(Snowflake.of(messageData.id())).createReaction(EntityUtil.getEmojiString(BotUtils.EMOJI_ARROW_RIGHT))),
									event.getClient().on(ReactionAddEvent.class)
											.filter(ev -> ev.getMessageId().asString().equals(messageData.id()))
											.filter(ev -> ev.getUserId().equals(event.getMessage().getAuthor().map(User::getId).orElseThrow()))
											.filter(ev -> ev.getEmoji().equals(BotUtils.EMOJI_ARROW_LEFT) || ev.getEmoji().equals(BotUtils.EMOJI_ARROW_RIGHT))
											.timeout(Duration.ofMinutes(2), event.getMessage().getRestChannel().getRestMessage(Snowflake.of(messageData.id())).deleteAllReactions().then(Mono.empty()))
											.flatMap(ev -> {
												if(ev.getEmoji().equals(BotUtils.EMOJI_ARROW_LEFT)) helpPage.decrementAndGet();
												else if(ev.getEmoji().equals(BotUtils.EMOJI_ARROW_RIGHT)) helpPage.incrementAndGet();
												helpPage.set(BotUtils.clamp(1, helpPage.get(), Command.Category.values().length));
												Command.Category newCategory = Command.Category.getCategoryByHelpPage(helpPage.get());
												return Mono.when(
														event.getMessage().getRestChannel().getRestMessage(Snowflake.of(messageData.id())).deleteUserReaction(EntityUtil.getEmojiString(ev.getEmoji()), ev.getUserId()),
														event.getMessage().getRestChannel().getRestMessage(Snowflake.of(messageData.id())).edit(MessageEditRequest.builder()
																.embed(BotUtils.getHelpEmbedData(language, prefix, Objects.requireNonNull(newCategory)))
																.build()
														)
												);
											})
							));
				})
				.build().register();
		
		Command.collectionBuilder()
				.setName("prefix")
				.setCategory(Command.Category.GENERAL)
				.addSubCommand(
						Command.builder()
								.setName("get")
								.setExecutor((event, language, prefix, args) -> event.getMessage().getRestChannel()
										.createMessage(EmbedData.builder()
												.title(getLanguageString(language, "command.prefix.get.title"))
												.description(getLanguageString(language, "command.prefix.get.description", prefix))
												.color(BotUtils.COLOR_LIGHT_GREEN.getRGB())
												.build()
										)
								)
								.build()
				)
				.addSubCommand(
						Command.builder()
								.setName("set")
								.setExecutor((event, language, prefix, args) -> Mono.empty()) // TODO
								.build()
				)
				.setUnknownSubCommandHandler(
						Command.builder()
								.setExecutor((event, language, prefix, args) -> Mono.error(BotException.invalidArgument(null))) // TODO
								.build()
				)
				.build().register();
		
		Command.builder()
				.setName("info")
				.setUsableInDMs(true)
				.setCategory(Command.Category.GENERAL)
				.setExecutor((event, language, prefix, args) -> event.getMessage().getRestChannel().createMessage(EmbedData.builder()
						.title(getLanguageString(language, "command.info.title"))
						.description(getLanguageString(language, "command.info.general"))
						.addField(EmbedFieldData.builder()
								.name(getLanguageString(language, "command.info.version.title"))
								.value(getLanguageString(language, "command.info.version.description", BotMain.CURRENT_VERSION))
								.inline(false)
								.build()
						)
						.addField(EmbedFieldData.builder()
								.name(getLanguageString(language, "command.info.libraries.title"))
								.value(getLanguageString(language, "command.info.libraries.description",
										System.getProperty("java.version"),
										GitProperties.getProperties().getProperty(GitProperties.APPLICATION_VERSION))
								)
								.inline(false)
								.build()
						)
						.color(BotUtils.BOT_COLOR.getRGB())
						.build()
				))
				.build().register();
	}
	
	/**
	 * @param name The name of the command to find
	 * @return The {@link Command} with the specified name
	 */
	@Nullable
	public static Command getCommand(@NonNull String name){
		return commands.get(name.toLowerCase());
	}
	
	/**
	 * @return A {@link Stream} of all commands without any duplicates
	 */
	@NonNull
	public static Stream<Command> getCommands(){
		return commands.values().stream()
				.distinct();
	}
	
	/**
	 * @param category The {@link Command.Category} of which the {@link Command}s should get returned
	 * @return A {@link Stream} with all {@link Command}s in the specified {@link Command.Category} without duplicates
	 */
	@NonNull
	public static Stream<Command> getCommands(@NonNull Command.Category category){
		return commands.values().stream()
				.filter(cmd -> cmd.getCategory() == category)
				.distinct();
	}
	
}
