package de.l0c4lh057.templatebot.main;

import de.l0c4lh057.templatebot.commands.ArgumentList;
import de.l0c4lh057.templatebot.commands.Command;
import de.l0c4lh057.templatebot.commands.Commands;
import de.l0c4lh057.templatebot.data.DataHandler;
import de.l0c4lh057.templatebot.data.DiscordCache;
import de.l0c4lh057.templatebot.utils.BotUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.NonNull;

public class Events {
	
	private static final Logger logger = LogManager.getLogger("Events");
	
	/**
	 * Registers most of the important events this bot needs to function
	 *
	 * @param client The {@link GatewayDiscordClient} on which the events should get registered on
	 * @return An empty {@link Mono} containing all the event subscriptions
	 */
	@NonNull
	public static Mono<Void> registerEvents(@NonNull GatewayDiscordClient client){
		logger.info("Registering all events");
		final String selfId = client.getSelfId().asString();
		return Mono.when(
				/* Successfully logged in */
				client.on(ReadyEvent.class)
						.doOnNext(event -> logger.info("Logged in as {}", event.getSelf().getTag())),
				
				/* Put all guilds in database when joining them */
				client.on(GuildCreateEvent.class)
						.map(GuildCreateEvent::getGuild)
						.map(Guild::getId)
						.flatMap(DataHandler::initializeGuild),
				
				/* Command Handler */
				client.on(MessageCreateEvent.class)
						// ignore bots and webhooks
						.filter(event -> !event.getMessage().getAuthor().map(User::isBot).orElse(true))
						// add members to the member cache here instead of DiscordCache class to ensure that it is saved before command execution
						.doOnNext(event -> event.getMember().ifPresent(DiscordCache::addMember))
						.flatMap(event -> Mono.justOrEmpty(event.getMessage().getAuthor())
								// put user in database if message came from DM
								.flatMap(user -> {
									if(event.getGuildId().isPresent()) return Mono.empty();
									else return DataHandler.initializeUser(user.getId());
								})
								.then(
										event.getGuildId().map(id -> BotUtils.getGuildPrefix(id).zipWith(BotUtils.getGuildLanguage(id)))
												.orElseGet(() -> event.getMessage().getAuthor().map(User::getId).map(id -> BotUtils.getUserPrefix(id)
														.zipWith(BotUtils.getUserLanguage(id))).orElseThrow()
												)
								)
								.flatMap(TupleUtils.function((String prefix, String language) -> {
									String content = event.getMessage().getContent();
									String strippedContent = null;
									if(content.startsWith("<@" + selfId + ">")){
										strippedContent = content.substring(3 + selfId.length());
										if(strippedContent.startsWith(" ")) strippedContent = strippedContent.substring(1);
									}else if(content.startsWith("<@!" + selfId + ">")){
										strippedContent = content.substring(4 + selfId.length());
										if(strippedContent.startsWith(" ")) strippedContent = strippedContent.substring(1);
									}else if(content.startsWith(prefix)){
										strippedContent = content.substring(prefix.length());
									}
									// message does not start with command prefix
									if(strippedContent == null) return Mono.empty();
									int spaceIndex = strippedContent.indexOf(' ');
									if(spaceIndex == 0) return Mono.empty();
									Command command = Commands.getCommand(spaceIndex > 0 ? strippedContent.substring(0, spaceIndex) : strippedContent);
									// command does not exist
									if(command == null) return Mono.empty();
									
									ArgumentList args = spaceIndex == -1 ? ArgumentList.empty() : ArgumentList.of(strippedContent.substring(spaceIndex + 1));
									return command.execute(event, language, prefix, args);
								}))
						)
		);
	}
	
}
