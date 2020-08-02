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
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.Optional;

public class Events {
	
	private static final Logger logger = LogManager.getLogger("Events");
	
	/**
	 * Registers most of the important events this bot needs to function
	 *
	 * @param client The {@link GatewayDiscordClient} on which the events should get registered on
	 * @return An empty {@link Mono} containing all the event subscriptions
	 */
	public static Mono<Void> registerEvents(@NotNull GatewayDiscordClient client){
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
						// put all users in database
						.flatMap(event -> Mono.justOrEmpty(event.getMessage().getAuthor())
								.flatMap(user -> DataHandler.initializeUser(user.getId()))
								.then(Mono.fromCallable(() -> {
									String content = event.getMessage().getContent();
									Mono<String> prefixMono;
									Mono<String> languageMono;
									if(event.getGuildId().isPresent()){
										// called in a guild
										prefixMono = BotUtils.getGuildPrefix(event.getGuildId().get());
										languageMono = BotUtils.getGuildLanguage(event.getGuildId().get());
									}else{
										// called in DMs
										prefixMono = BotUtils.getUserPrefix(event.getMessage().getAuthor().map(User::getId).orElseThrow());
										languageMono = BotUtils.getUserLanguage(event.getMessage().getAuthor().map(User::getId).orElseThrow());
									}
									return prefixMono.zipWith(languageMono)
											.flatMap(TupleUtils.function((prefix, language) -> {
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
												
												ArgumentList args = spaceIndex == -1 ? new ArgumentList() : ArgumentList.of(strippedContent.substring(spaceIndex + 1));
												return command.execute(event, language, prefix, args);
											}));
								}))
						)
		);
	}
	
}
