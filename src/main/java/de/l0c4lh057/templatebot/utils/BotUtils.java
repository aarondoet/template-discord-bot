package de.l0c4lh057.templatebot.utils;

import de.l0c4lh057.templatebot.commands.Command;
import de.l0c4lh057.templatebot.commands.Commands;
import de.l0c4lh057.templatebot.data.DBGuild;
import de.l0c4lh057.templatebot.data.DBUser;
import de.l0c4lh057.templatebot.data.DataHandler;
import discord4j.common.util.Snowflake;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.EmbedFooterData;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.io.File;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BotUtils {
	
	private static final Logger logger = LogManager.getLogger("Utils");
	
	private BotUtils(){}
	
	/* TODO: set the desired values */
	public static final List<Snowflake> botOwners = Collections.singletonList(Snowflake.of(226677096091484160L));
	public static final Color BOT_COLOR = Color.of(0x0);
	public static final String DEFAULT_PREFIX = "=";
	
	public static final Color COLOR_DARK_RED = Color.of(0xFF0000);
	public static final Color COLOR_LIGHT_RED = Color.of(0xFF5246);
	public static final Color COLOR_LIGHT_GREEN = Color.of(0x4BB543);
	
	public static final ReactionEmoji EMOJI_ARROW_LEFT = ReactionEmoji.unicode("\u2B05");
	public static final ReactionEmoji EMOJI_ARROW_RIGHT = ReactionEmoji.unicode("\u27A1");
	public static final ReactionEmoji EMOJI_X = ReactionEmoji.unicode("\u274C");
	public static final ReactionEmoji EMOJI_CHECKMARK = ReactionEmoji.unicode("\u2705");
	
	private static final Map<Long, String> guildPrefixes = new WeakHashMap<>();
	public static Mono<String> getGuildPrefix(@NonNull Snowflake guildId){
		String prefix = guildPrefixes.get(guildId.asLong());
		if(prefix != null) return Mono.just(prefix);
		return DataHandler.getGuild(guildId)
				.map(DBGuild::getPrefix)
				.doOnNext(pref -> guildPrefixes.put(guildId.asLong(), pref));
	}
	private static final Map<Long, String> guildLanguages = new WeakHashMap<>();
	public static Mono<String> getGuildLanguage(Snowflake guildId){
		String language = guildLanguages.get(guildId.asLong());
		if(language != null) return Mono.just(language);
		return DataHandler.getGuild(guildId)
				.map(DBGuild::getLanguage)
				.doOnNext(lang -> guildLanguages.put(guildId.asLong(), lang));
	}
	
	private static final Map<Long, String> userPrefixes = new WeakHashMap<>();
	public static Mono<String> getUserPrefix(Snowflake userId){
		String prefix = userPrefixes.get(userId.asLong());
		if(prefix != null) return Mono.just(prefix);
		return DataHandler.getUser(userId)
				.doOnNext(user -> userLanguages.put(userId.asLong(), user.getLanguage()))
				.map(DBUser::getPrefix)
				.doOnNext(pref -> userPrefixes.put(userId.asLong(), pref));
	}
	private static final Map<Long, String> userLanguages = new WeakHashMap<>();
	public static Mono<String> getUserLanguage(Snowflake userId){
		String language = userLanguages.get(userId.asLong());
		if(language != null) return Mono.just(language);
		return DataHandler.getUser(userId)
				.doOnNext(user -> userPrefixes.put(userId.asLong(), user.getPrefix()))
				.map(DBUser::getLanguage)
				.doOnNext(lang -> userLanguages.put(userId.asLong(), lang));
	}
	
	/**
	 *
	 * @param text
	 * @return
	 */
	public static String escapeRegex(String text){
		return text.replaceAll("[\\<\\(\\[\\{\\\\\\^\\-\\=\\$\\!\\|\\]\\}\\)\\?\\*\\+\\.\\>]", "\\\\$0");
	}
	
	/**
	 *
	 * @param template
	 * @param replace
	 * @return
	 */
	public static String replaceAll(String template, Map<String, String> replace){
		Matcher matcher = Pattern.compile("(" + replace.keySet().stream().map(BotUtils::escapeRegex).collect(Collectors.joining("|")) + ")").matcher(template);
		StringBuffer sb = new StringBuffer();
		while(matcher.find()) matcher.appendReplacement(sb, replace.get(matcher.group(1)));
		matcher.appendTail(sb);
		return sb.toString();
	}
	
	/**
	 * Clamps a value
	 *
	 * @param min   The minimal value allowed
	 * @param value The actual value
	 * @param max   The maximal value allowed
	 * @param <T>   The comparable type of the parameters
	 * @return {@code min} if {@code value} is smaller than it, {@code max} if {@code value} is greater than it, otherwise {@code value}.
	 */
	public static <T extends Comparable<T>> T clamp(T min, T value, T max){
		return min.compareTo(value) > 0 ? min : max.compareTo(value) < 0 ? max : value;
	}
	
	/**
	 *
	 * @param lang
	 * @param args
	 * @return
	 */
	public static Command.Category getHelpPage(String lang, List<String> args){
		if(args.size() == 0) return Command.Category.getCategoryByHelpPage(1);
		try {
			int page = BotUtils.clamp(1, Integer.parseInt(args.get(0)), Command.Category.values().length);
			return Command.Category.getCategoryByHelpPage(page);
		} catch (NumberFormatException ignored){}
		Command.Category category = Command.Category.getCategoryByName(lang, String.join(" ", args));
		if(category != null) return category;
		else return Command.Category.getCategoryByHelpPage(1);
	}
	
	/**
	 *
	 * @param language
	 * @param prefix
	 * @param category
	 * @return
	 */
	public static Consumer<EmbedCreateSpec> getHelpSpec(String language, String prefix, Command.Category category){
		return ecs -> ecs
				.setTitle(getLanguageString(language, "help.category.title", category.getName(language)))
				.setDescription(
						Commands.getCommands(category)
								.sorted(Comparator.comparing(Command::getName))
								.sorted(Comparator.comparing(Command::getHelpPagePosition))
								.map(command -> getLanguageString(language, "help." + command.getName() + ".short", prefix))
								.collect(Collectors.joining("\n"))
				)
				.setFooter(getLanguageString(language, "help.category.footer", category.getHelpPage(), Command.Category.values().length), null)
				.setColor(BOT_COLOR);
	}
	
	/**
	 *
	 * @param language
	 * @param prefix
	 * @param category
	 * @return
	 */
	public static EmbedData getHelpEmbedData(String language, String prefix, Command.Category category){
		return EmbedData.builder()
				.title(getLanguageString(language, "help.category.title", category.getName(language)))
				.description(Commands.getCommands(category)
						.sorted(Comparator.comparing(Command::getName))
						.sorted(Comparator.comparing(Command::getHelpPagePosition))
						.map(command -> getLanguageString(language, "help." + command.getName() + ".short", prefix))
						.collect(Collectors.joining("\n"))
				)
				.footer(EmbedFooterData.builder()
						.text(getLanguageString(language, "help.category.footer", category.getHelpPage(), Command.Category.values().length))
						.build()
				)
				.build();
	}
	
	/**
	 * Initializes the language module of the bot by loading all {@link ResourceBundle}s and {@link Locale}s available.
	 * Also sets the default {@link Locale} to {@link Locale#ENGLISH}.
	 */
	public static void initialize(){
		Locale.setDefault(Locale.ENGLISH);
		try {
			availableLanguages.clear();
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Arrays.stream(Objects.requireNonNull(new File(Objects.requireNonNull(loader.getResource("./")).toURI()).listFiles()))
					.filter(File::isFile)
					.map(File::getName)
					.filter(name -> name.startsWith("strings") && name.endsWith(".properties"))
					.map(name -> name.substring(7, name.length() - 11))
					.forEach(lang -> {
						if(lang.length() == 0) availableLanguages.add(Locale.getDefault().getLanguage());
						else availableLanguages.add(Locale.forLanguageTag(lang).getLanguage());
					});
			loadResourceBundles();
		} catch (URISyntaxException ex) {
			logger.error("Could not list files in resources directory", ex);
			System.exit(-1);
		}
	}
	
	/**
	 * The list of all languages a {@link ResourceBundle} was found for. This is loaded in {@link #initialize()}
	 */
	public static final List<String> availableLanguages = new ArrayList<>();
	private static final Map<String, ResourceBundle> bundles = new HashMap<>();
	private static void loadResourceBundles(){
		availableLanguages.forEach(lang -> bundles.put(lang, ResourceBundle.getBundle("strings", Locale.forLanguageTag(lang))));
	}
	
	/**
	 *
	 * @param language
	 * @param key
	 * @param args
	 * @return
	 */
	public static String getLanguageString(String language, String key, Object... args){
		if(args.length == 0) return bundles.get(language).getString(key);
		else return new MessageFormat(bundles.get(language).getString(key), Locale.forLanguageTag(language)).format(args);
	}
	
}
