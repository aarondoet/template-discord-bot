package de.l0c4lh057.templatebot.commands;

import de.l0c4lh057.templatebot.commands.exceptions.InvalidArgumentException;
import de.l0c4lh057.templatebot.main.BotMain;
import de.l0c4lh057.templatebot.utils.BotUtils;
import static de.l0c4lh057.templatebot.utils.BotUtils.getLanguageString;

import de.l0c4lh057.templatebot.utils.ratelimits.RatelimitType;
import discord4j.common.GitProperties;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.EmbedFieldData;
import io.github.bucket4j.Bandwidth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Commands {
	
	private static final Logger logger = LogManager.getLogger("Commands");
	
	/**
	 * The {@link Map} in which all {@link Command}s are stored. Every alias maps to the command.
	 */
	protected static final Map<String, Command> commands = new HashMap<>();
	
	/**
	 * All commands should get registered in here. If any command is registered in another class (e.g. a music bot class
	 * with which has its own function to register all commands) it should get called in this function.
	 */
	public static void registerCommands(){
		logger.info("Registering all commands");
		
		Command.builder()
				.setName("help")
				.setRatelimit(RatelimitType.CHANNEL, Bandwidth.simple(3, Duration.ofSeconds(10)), Bandwidth.simple(7, Duration.ofHours(1)))
				.setExecutor((event, language, prefix, args) -> event.getMessage().getChannel()
						.flatMap(channel -> {
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
									return channel.createEmbed(ecs -> ecs
											.setTitle(BotUtils.getLanguageString(language, "help.command.title", commandName))
											.setDescription(BotUtils.getLanguageString(language, "help." + String.join(".", commandTree) + ".detailed", prefix))
									);
								}
							}
							Command.Category category = BotUtils.getHelpPage(language, args);
							AtomicInteger helpPage = new AtomicInteger(category.getHelpPage());
							return channel.createEmbed(BotUtils.getHelpSpec(language, prefix, category))
									.flatMap(message -> Mono.when(
											message.addReaction(BotUtils.EMOJI_ARROW_LEFT).then(message.addReaction(BotUtils.EMOJI_ARROW_RIGHT)),
											event.getClient().on(ReactionAddEvent.class)
													.filter(ev -> ev.getMessageId().equals(message.getId()))
													.filter(ev -> ev.getUserId().equals(event.getMessage().getAuthor().map(User::getId).orElseThrow()))
													.filter(ev -> ev.getEmoji().equals(BotUtils.EMOJI_ARROW_LEFT) || ev.getEmoji().equals(BotUtils.EMOJI_ARROW_RIGHT))
													.timeout(Duration.ofMinutes(2), message.removeAllReactions().then(Mono.empty()))
													.flatMap(ev -> {
														if(ev.getEmoji().equals(BotUtils.EMOJI_ARROW_LEFT)) helpPage.decrementAndGet();
														else if(ev.getEmoji().equals(BotUtils.EMOJI_ARROW_RIGHT)) helpPage.decrementAndGet();
														helpPage.set(BotUtils.clamp(1, helpPage.get(), Command.Category.values().length));
														Command.Category newCategory = Command.Category.getCategoryByHelpPage(helpPage.get());
														return Mono.when(
																message.removeReaction(ev.getEmoji(), ev.getUserId()),
																message.edit(mes -> mes.setEmbed(BotUtils.getHelpSpec(language, prefix, newCategory)))
														);
													})
									)
									);
						})
						.then()
				)
				.build().register();
		
		Command.collectionBuilder()
				.setName("prefix")
				.setCategory(Command.Category.GENERAL)
				.addSubCommand(
						Command.builder()
								.setName("get")
								.setExecutor((event, language, prefix, args) -> event.getMessage().getChannel()
										.flatMap(channel -> channel.createEmbed(ecs -> ecs
												.setTitle(getLanguageString(language, "command.prefix.get.title"))
												.setDescription(getLanguageString(language, "command.prefix.get.description", prefix))
												.setColor(BotUtils.COLOR_LIGHT_GREEN)
										))
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
								.setExecutor((event, language, prefix, args) -> Mono.error(new InvalidArgumentException(null))) // TODO
								.build()
				)
				.build().register();
		
		Command.builder()
				.setName("info")
				.setUsableInDMs(true)
				.setCategory(Command.Category.GENERAL)
				.setExecutor((event, language, prefix, args) -> event.getMessage().getRestChannel().createMessage(EmbedData.builder()
						.title(BotUtils.getLanguageString(language, "command.info.title"))
						.description(BotUtils.getLanguageString(language, "command.info.general"))
						.addField(EmbedFieldData.builder()
								.name(BotUtils.getLanguageString(language, "command.info.version.title"))
								.value(BotUtils.getLanguageString(language, "command.info.version.description", BotMain.CURRENT_VERSION))
								.inline(false)
								.build()
						)
						.addField(EmbedFieldData.builder()
								.name(BotUtils.getLanguageString(language, "command.info.libraries.title"))
								.value(BotUtils.getLanguageString(language, "command.info.libraries.description",
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
	 *
	 * @param name
	 * @return
	 */
	@Nullable public static Command getCommand(@NotNull String name){
		return commands.get(name.toLowerCase());
	}
	
	/**
	 *
	 * @return
	 */
	public static Stream<Command> getCommands(){
		return commands.values().stream()
				.distinct();
	}
	
	/**
	 *
	 * @param category
	 * @return
	 */
	public static Stream<Command> getCommands(@NotNull Command.Category category){
		return commands.values().stream()
				.filter(cmd -> cmd.getCategory() == category)
				.distinct();
	}
	
}
