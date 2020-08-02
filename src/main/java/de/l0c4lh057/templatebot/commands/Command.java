package de.l0c4lh057.templatebot.commands;

import de.l0c4lh057.templatebot.commands.exceptions.InvalidArgumentException;
import de.l0c4lh057.templatebot.commands.exceptions.MissingPermissionsException;
import de.l0c4lh057.templatebot.commands.exceptions.NotExecutableException;
import de.l0c4lh057.templatebot.commands.exceptions.RatelimitedException;
import de.l0c4lh057.templatebot.utils.BotUtils;
import static de.l0c4lh057.templatebot.utils.BotUtils.getLanguageString;

import de.l0c4lh057.templatebot.utils.ratelimits.NoRatelimit;
import de.l0c4lh057.templatebot.utils.ratelimits.Ratelimit;
import de.l0c4lh057.templatebot.utils.ratelimits.RatelimitFactory;
import de.l0c4lh057.templatebot.utils.ratelimits.RatelimitType;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.rest.util.PermissionSet;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.local.LocalBucketBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.*;

public class Command {
	
	private static final Logger logger = LogManager.getLogger("Command");
	
	private final String name;
	private final String[] aliases;
	private final Category category;
	private final CommandExecutor executor;
	private final boolean usableInGuilds;
	private final boolean usableInDMs;
	private final Map<String, Command> subCommands;
	private final int helpPagePosition;
	private final boolean nsfw;
	private final Ratelimit ratelimit;
	private final boolean requiresBotOwner;
	private final boolean requiresGuildOwner;
	private final Permission requiredPermissions;
	
	private Command(CommandBuilder builder){
		this.name = builder.name;
		this.aliases = builder.aliases;
		this.category = builder.category;
		this.executor = builder.executor;
		this.usableInGuilds = builder.usableInGuilds;
		this.usableInDMs = builder.usableInDMs;
		this.subCommands = builder.subCommands;
		this.helpPagePosition = builder.helpPagePosition;
		this.nsfw = builder.nsfw;
		this.ratelimit = builder.ratelimit;
		this.requiresBotOwner = builder.requiresBotOwner;
		this.requiresGuildOwner = builder.requiresGuildOwner;
		this.requiredPermissions = builder.requiredPermissions;
	}
	
	/**
	 * @return The name of the command
	 */
	public String getName(){ return name; }
	
	/**
	 * @return All the aliases of the command
	 */
	public String[] getAliases(){ return aliases; }
	
	/**
	 * @return The {@link Category} the command is in
	 */
	public Category getCategory(){ return category; }
	
	/**
	 * @return The {@link CommandExecutor} of this command
	 */
	public CommandExecutor getExecutor(){ return executor; }
	
	/**
	 * @return Whether the command can be executed in guilds
	 */
	public boolean isUsableInGuilds(){ return usableInGuilds; }
	
	/**
	 * @return Whether this command can be executed in DMs
	 */
	public boolean isUsableInDMs(){ return usableInDMs; }
	
	/**
	 * @param subCommand The name of the sub command
	 * @return The {@link Command} with the passed value as name or alias or null if it does not exist
	 */
	public Command getSubCommand(String subCommand){ return subCommands.get(subCommand.toLowerCase()); }
	
	/**
	 * @return The position of the command on the help page
	 */
	public int getHelpPagePosition(){ return helpPagePosition; }
	
	/**
	 * @return Whether this command can only be used in NSFW channels and DMs
	 */
	public boolean isNsfw(){ return nsfw; }
	
	/**
	 * This function checks if the user/guild is rate limited.
	 * <p>
	 * In case of a rate limit this function will just return true,
	 * otherwise it will automatically increase the usage count by one.
	 *
	 * @param guildId   The {@link Snowflake} ID of the guild the command got executed in or null if executed in DMs
	 * @param channelId The {@link Snowflake} ID of the channel the command got executed in
	 * @param userId    The {@link Snowflake} ID of the user who executed the command
	 * @return Whether the guild/member/user/channel is rate limited
	 */
	public boolean isRatelimited(@Nullable Snowflake guildId, @NotNull Snowflake channelId, @NotNull Snowflake userId){
		return ratelimit.isRatelimited(guildId, channelId, userId);
	}
	
	/**
	 * Adds this command with name and aliases as key to {@link Commands#commands}.
	 */
	public void register(){
		if(Commands.getCommand(name) != null || Arrays.stream(aliases).anyMatch(alias -> Commands.getCommand(alias) != null)){
			logger.warn("Command {} is already registered", name);
		}else{
			Commands.commands.put(name.toLowerCase(), this);
			for (String alias : aliases) {
				Commands.commands.put(alias.toLowerCase(), this);
			}
		}
	}
	
	/**
	 * Executes this command. If an error occurs it will automatically get caught and processed.
	 *
	 * @param event    The raw {@link MessageCreateEvent} that cause the call of this command
	 * @param language The language that should be used in responses
	 * @param prefix   The bot prefix
	 * @param args     The list of all arguments passed to this command
	 * @return An empty {@link Mono}.
	 */
	public Mono<Void> execute(@NotNull MessageCreateEvent event, @NotNull String language, @NotNull String prefix, @NotNull ArgumentList args){
		return execute(event, language, prefix, args, true);
	}
	
	/**
	 * Executes this command. If an error occurs and {@code handleExceptions} is set to true
	 * it will automatically get caught and processed.
	 *
	 * @param event    The raw {@link MessageCreateEvent} that caused the call of this command
	 * @param language The language that should be used in responses
	 * @param prefix   The bot prefix
	 * @param args     The list of all arguments passed to this command
	 * @return An empty {@link Mono}. It will contain an error if command execution failed and {@code handleExceptions}
	 * is set to false.
	 */
	private Mono<Void> execute(MessageCreateEvent event, String language, String prefix, ArgumentList args, boolean handleExceptions){
		Snowflake authorId = event.getMessage().getAuthor().map(User::getId).orElseThrow();
		Mono<?> executionMono;
		if(requiresBotOwner && !BotUtils.botOwners.contains(authorId)){
			executionMono = Mono.error(new NotExecutableException(null));
		}else if(event.getGuildId().isPresent() && !usableInGuilds){
			executionMono = Mono.error(new NotExecutableException(null));
		}else if(event.getGuildId().isEmpty() && !usableInDMs){
			executionMono = Mono.error(new NotExecutableException(null));
		}else if(isRatelimited(event.getGuildId().orElse(null), event.getMessage().getChannelId(), authorId)){
			executionMono = Mono.error(new RatelimitedException("exception.ratelimited.description"));
		}else{
			executionMono = PermissionManager.checkExecutability(event.getGuildId().orElse(null), authorId, event.getMessage().getChannelId(), requiredPermissions, requiresGuildOwner, nsfw)
					.then(executor.execute(event, language, prefix, args));
		}
		if(handleExceptions) return handleExceptions(executionMono, event, language, name);
		else return executionMono.then();
	}
	
	/**
	 * This function simply catches all the exceptions that could happen on when executing the command.
	 *
	 * @param executionMono The result {@link Mono} got by command execution
	 * @param event         The raw {@link MessageCreateEvent} that caused the call of this command
	 * @param language      The language that should be used in responses
	 * @param commandName   The name of the command
	 * @return An empty {@link Mono}
	 */
	private static Mono<Void> handleExceptions(Mono<?> executionMono, MessageCreateEvent event, String language, String commandName){
		return executionMono.then()
				.onErrorResume(MissingPermissionsException.class, err -> event.getMessage().getChannel()
						.flatMap(channel -> channel.createEmbed(ecs -> ecs
								.setTitle(getLanguageString(language, "exception.missingpermissions.title"))
								.setDescription(err.getErrorMessage(language))
								.setColor(BotUtils.COLOR_LIGHT_RED)
						)).then()
						.onErrorResume(ex -> Mono.empty())
				)
				.onErrorResume(InvalidArgumentException.class, err -> event.getMessage().getChannel()
						.flatMap(channel -> channel.createEmbed(ecs -> ecs
								.setTitle(getLanguageString(language, "exception.invalidargument.title"))
								.setDescription(err.getErrorMessage(language))
								.setColor(BotUtils.COLOR_LIGHT_RED)
						)).then()
						.onErrorResume(ex -> Mono.empty())
				)
				.onErrorResume(RatelimitedException.class, err -> event.getMessage().getChannel()
						.flatMap(channel -> channel.createEmbed(ecs -> ecs
								.setTitle(getLanguageString(language, "exception.ratelimited.title"))
								.setDescription(err.getErrorMessage(language))
								.setColor(BotUtils.COLOR_LIGHT_RED)
						)).then()
						.onErrorResume(ex -> Mono.empty())
				)
				.onErrorResume(err -> {
					logger.error("Unexpected exception when executing command " + commandName, err);
					return event.getMessage().getChannel()
							.flatMap(channel -> channel.createEmbed(ecs -> ecs
									.setTitle(getLanguageString(language, "exception.unknown.title"))
									.setDescription(getLanguageString(language, "exception.unknown.description"))
									.setColor(BotUtils.COLOR_DARK_RED)
							)).then()
							.onErrorResume(ex -> Mono.empty());
				});
	}
	
	/**
	 * @return A new {@link CommandBuilder} instance
	 */
	public static CommandBuilder builder(){
		return new CommandBuilder();
	}
	
	/**
	 * A command collection is a {@link Command} that has a list of sub commands. The first passed argument is used to
	 * get the sub command which will then be executed with all initial arguments except the first one which is just the
	 * command name itself.
	 *
	 * @return A new {@link CommandCollectionBuilder} instance
	 */
	public static CommandCollectionBuilder collectionBuilder(){
		return new CommandCollectionBuilder();
	}
	
	@Override
	public boolean equals(Object o){
		return (o instanceof Command) && ((Command)o).name.equals(name);
	}
	
	@Override
	public int hashCode(){
		return name.hashCode();
	}
	
	public static class CommandBuilder {
		private String name = "";
		private String[] aliases = {};
		private CommandExecutor executor = null;
		private Category category = Category.GENERAL;
		private boolean usableInGuilds = true;
		private boolean usableInDMs = false;
		private Map<String, Command> subCommands = Collections.emptyMap();
		private int helpPagePosition = 0;
		private boolean nsfw = false;
		private Ratelimit ratelimit = NoRatelimit.getInstance();
		private boolean requiresBotOwner = false;
		private boolean requiresGuildOwner = false;
		private Permission requiredPermissions = null;
		
		private CommandBuilder(){}
		
		/**
		 * @param name    The name this command should have
		 * @param aliases The list of aliases this command has
		 * @return This {@link CommandBuilder} instance to allow chaining
		 */
		public CommandBuilder setName(@NotNull String name, @NotNull String... aliases){
			this.name = name;
			this.aliases = aliases;
			return this;
		}
		
		/**
		 * It is guaranteed that {@link Message#getAuthor()} of {@link MessageCreateEvent#getMessage()} will never be empty.
		 *
		 * @param executor The action that should get performed when executing the command
		 * @return This {@link CommandBuilder} instance to allow chaining
		 */
		public CommandBuilder setExecutor(@NotNull CommandExecutor executor){
			this.executor = executor;
			return this;
		}
		
		/**
		 * @param category The {@link Category} this command should be listed in
		 * @return This {@link CommandBuilder} instance to allow chaining
		 */
		public CommandBuilder setCategory(@NotNull Category category){
			this.category = category;
			return this;
		}
		
		/**
		 * @param subCommands A map of command names and aliases to the respective sub commands
		 * @return This {@link CommandBuilder} instance to allow chaining
		 */
		private CommandBuilder setSubCommands(Map<String, Command> subCommands){
			this.subCommands = subCommands;
			return this;
		}
		
		/**
		 * By default all {@link Command}s on the help page are sorted by their name.
		 * If a custom order is desired this value can be set. All commands have a default of {@code 0} and after sorting
		 * by name they are sorted by this value.
		 *
		 * @param helpPagePosition The position of this command on the help page
		 * @return This {@link CommandBuilder} instance to allow chaining
		 */
		public CommandBuilder setHelpPagePosition(int helpPagePosition){
			this.helpPagePosition = helpPagePosition;
			return this;
		}
		
		/**
		 * @param nsfw Whether this command should only be executable in NSFW channels and DMs
		 * @return This {@link CommandBuilder} instance to allow chaining
		 */
		public CommandBuilder setNsfw(boolean nsfw){
			this.nsfw = nsfw;
			return this;
		}
		
		/**
		 * @param usableInDMs Whether this command can be executed in DMs
		 * @return This {@link CommandBuilder} instance to allow chaining
		 */
		public CommandBuilder setUsableInDMs(boolean usableInDMs){
			this.usableInDMs = usableInDMs;
			return this;
		}
		
		/**
		 * @param usableInGuilds Whether this command can be executed in guilds
		 * @return This {@link CommandBuilder} instance to allow chaining
		 */
		public CommandBuilder setUsableInGuilds(boolean usableInGuilds){
			this.usableInGuilds = usableInGuilds;
			return this;
		}
		
		/**
		 * @param requiresBotOwner
		 * @return
		 */
		public CommandBuilder setRequiresBotOwner(boolean requiresBotOwner){
			this.requiresBotOwner = requiresBotOwner;
			return this;
		}
		
		/**
		 * @param requiresGuildOwner
		 * @return
		 */
		public CommandBuilder setRequiresGuildOwner(boolean requiresGuildOwner){
			this.requiresGuildOwner = requiresGuildOwner;
			return this;
		}
		
		/**
		 * @param permissionName
		 * @param defaultPermissions
		 * @return
		 */
		public CommandBuilder setRequiredPermissions(@NotNull String permissionName, @NotNull discord4j.rest.util.Permission... defaultPermissions){
			return setRequiredPermissions(new Permission(permissionName, defaultPermissions));
		}
		
		private CommandBuilder setRequiredPermissions(Permission permissions){
			this.requiredPermissions = permissions;
			return this;
		}
		
		/**
		 *
		 * @param type
		 * @param bandwidths
		 * @return
		 */
		public CommandBuilder setRatelimit(RatelimitType type, Bandwidth... bandwidths){
			this.ratelimit = RatelimitFactory.getRatelimit(type, List.of(bandwidths));
			return this;
		}
		
		private CommandBuilder setRatelimit(Ratelimit ratelimit){
			this.ratelimit = ratelimit;
			return this;
		}
		
		/**
		 * Creates a command instance with the values defined in this builder.
		 *
		 * @return The built {@link Command}
		 */
		public Command build(){
			return new Command(this);
		}
	}
	
	public static class CommandCollectionBuilder {
		private String name = "";
		private String[] aliases = {};
		private final Map<String, Command> subCommands = new HashMap<>();
		private Command unknownSubCommandHandler = null;
		private Command.Category category = Category.GENERAL;
		private int helpPagePosition = 0;
		private final Ratelimit ratelimit = NoRatelimit.getInstance();
		private boolean requiresBotOwner = false;
		private boolean requiresGuildOwner = false;
		private Permission requiredPermissions = new Permission("");
		
		private CommandCollectionBuilder(){}
		
		/**
		 * @param name    The name this command should have
		 * @param aliases The list of aliases this command has
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		public CommandCollectionBuilder setName(@NotNull String name, @NotNull String... aliases){
			this.name = name;
			this.aliases = aliases;
			return this;
		}
		
		/**
		 * @param category The {@link Category} this command should be listed in
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		public CommandCollectionBuilder setCategory(@NotNull Command.Category category){
			this.category = category;
			return this;
		}
		
		/**
		 * By default all {@link Command}s on the help page are sorted by their name.
		 * If a custom order is desired this value can be set. All commands have a default of {@code 0} and after sorting
		 * by name they are sorted by this value.
		 *
		 * @param helpPagePosition The position of this command on the help page
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		public CommandCollectionBuilder setHelpPagePosition(int helpPagePosition){
			this.helpPagePosition = helpPagePosition;
			return this;
		}
		
		/**
		 * If this value is set to true only bot owners can execute the command.
		 * Bot owners are stored in {@link BotUtils#botOwners}.
		 *
		 * @param requiresBotOwner Whether only bot owners should be able to execuute the command
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		public CommandCollectionBuilder setRequiresBotOwner(boolean requiresBotOwner){
			this.requiresBotOwner = requiresBotOwner;
			return this;
		}
		
		/**
		 * If this value is set to true only guild owners can execute the command.
		 *
		 * @param requiresGuildOwner Whether only guild owners should be able to execute this command
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		public CommandCollectionBuilder setRequiresGuildOwner(boolean requiresGuildOwner){
			this.requiresGuildOwner = requiresGuildOwner;
			return this;
		}
		
		/**
		 *
		 * @param permissionName
		 * @param defaultPermissions
		 * @return
		 */
		public CommandCollectionBuilder setRequiredPermissions(@NotNull String permissionName, @NotNull discord4j.rest.util.Permission... defaultPermissions){
			this.requiredPermissions = new Permission(permissionName, defaultPermissions);
			return this;
		}
		
		/**
		 * Adds a sub command to this command.
		 * <p>
		 * The first argument which matches with any name or alias of the command will be removed from the argument list.
		 * <p>
		 * Neither {@link Category} nor {@code helpPagePosition} property will have an effect on sub commands.
		 *
		 * @param command The sub command
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		public CommandCollectionBuilder addSubCommand(@NotNull Command command){
			if(subCommands.get(command.getName()) != null || Arrays.stream(command.getAliases()).anyMatch(alias -> subCommands.get(alias) != null)){
				logger.warn("Sub command {} of command {} already got registered", command.getName(), name);
			}else{
				subCommands.put(command.getName().toLowerCase(), command);
				Arrays.stream(command.getAliases()).forEach(alias -> subCommands.put(alias.toLowerCase(), command));
			}
			return this;
		}
		
		/**
		 * Sets a {@link Command} as the handler for all command executions that could not find any sub command with
		 * matching name.
		 * <p>
		 * Other than for sub commands the first argument (if present) will not get removed.
		 * <p>
		 * Neither {@link Category} not {@code helpPagePosition} nor {@code name} property will have an effect.
		 *
		 * @param unknownSubCommandHandler The command that should get executed when no sub command matches
		 * @return This {@link CommandCollectionBuilder} instance to allow chaining
		 */
		public CommandCollectionBuilder setUnknownSubCommandHandler(@NotNull Command unknownSubCommandHandler){
			this.unknownSubCommandHandler = unknownSubCommandHandler;
			return this;
		}
		
		/**
		 * Creates a command instance with the values defined in this builder.
		 *
		 * @return The built {@link Command}
		 */
		public Command build(){
			if(unknownSubCommandHandler == null){
				logger.warn("No unknown subcommand handler set for command collection {}", name);
				unknownSubCommandHandler = builder().setExecutor((event, language, prefix, args) -> Mono.empty()).build();
			}
			return builder()
					.setName(name, aliases)
					.setCategory(category)
					.setSubCommands(subCommands)
					.setHelpPagePosition(helpPagePosition)
					.setRatelimit(ratelimit)
					.setRequiresBotOwner(requiresBotOwner)
					.setRequiresGuildOwner(requiresGuildOwner)
					.setRequiredPermissions(requiredPermissions)
					.setExecutor((event, language, prefix, args) -> {
						if(!args.isEmpty()){
							Command command = subCommands.get(args.get(0).toLowerCase());
							if(command != null){
								return command.execute(event, language, prefix, args.subList(1, args.size()), false);
							}
						}
						return unknownSubCommandHandler.execute(event, language, prefix, args, false);
					})
					.build();
		}
	}
	
	public interface CommandExecutor {
		/**
		 *
		 * @param event
		 * @param language
		 * @param prefix
		 * @param args
		 * @return
		 */
		Mono<?> execute(@NotNull MessageCreateEvent event, @NotNull String language, @NotNull String prefix, @NotNull ArgumentList args);
	}
	
	public enum Category {
		GENERAL(1, "commandcategory.general")
		;
		private final int helpPage;
		private final String nameKey;
		Category(int helpPage, @NotNull String nameKey){
			this.helpPage = helpPage;
			this.nameKey = nameKey;
		}
		public int getHelpPage() { return helpPage; }
		public String getName(String lang){ return BotUtils.getLanguageString(lang, nameKey); }
		public static Category getCategoryByHelpPage(int helpPage){
			Category[] categories = Category.values();
			for (Category category : categories) {
				if (category.helpPage == helpPage) return category;
			}
			logger.warn("The requested category with helpPage {} does not exist", helpPage);
			return null;
		}
		public static Category getCategoryByName(@NotNull String lang, @NotNull String name){
			for (Category category : Category.values()) {
				if (category.getName(lang).equalsIgnoreCase(name)) return category;
			}
			return null;
		}
	}
	
	public static class Permission {
		private final String permissionName;
		private final PermissionSet defaultPermissions;
		private Permission(String permissionName, discord4j.rest.util.Permission... defaultPermissions){
			this.permissionName = permissionName;
			this.defaultPermissions = PermissionSet.of(defaultPermissions);
		}
		public String getPermissionName(){ return permissionName; }
		public PermissionSet getDefaultPermissions(){ return defaultPermissions; }
	}
	
}