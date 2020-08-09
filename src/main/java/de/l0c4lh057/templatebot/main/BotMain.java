package de.l0c4lh057.templatebot.main;

import de.l0c4lh057.templatebot.commands.Commands;
import de.l0c4lh057.templatebot.data.DataHandler;
import de.l0c4lh057.templatebot.data.DiscordCache;
import de.l0c4lh057.templatebot.utils.BotUtils;
import discord4j.common.retry.ReconnectOptions;
import discord4j.core.DiscordClient;
import discord4j.core.shard.ShardingStrategy;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.store.api.noop.NoOpStoreService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class BotMain {
	
	public static final String CURRENT_VERSION = "0.0.1";
	
	private static final Logger logger = LogManager.getLogger("Main");
	
	public static void main(String[] args){
		logger.info("Program started");
		BotUtils.initialize();
		Commands.registerCommands();
		Mono<Void> onDisconnect = DiscordClient.builder(Credentials.BOT_TOKEN)
				.build()
				.gateway()
				.setSharding(ShardingStrategy.recommended())
				.setGuildSubscriptions(false)
				.setReconnectOptions(ReconnectOptions.builder().setMaxRetries(Integer.MAX_VALUE).build())
				// only listen to needed events
				.setEnabledIntents(IntentSet.of(
						Intent.DIRECT_MESSAGE_REACTIONS,
						Intent.DIRECT_MESSAGES,
						Intent.GUILD_MESSAGE_REACTIONS,
						Intent.GUILD_MESSAGES,
						Intent.GUILDS
				))
				// disable cache, using own cache to only cache needed data in data.DiscordCache
				.setStoreService(new NoOpStoreService())
				.withGateway(client -> Mono.when(
						// register everything that needs the client as parameter here
						Events.registerEvents(client),
						DiscordCache.registerEvents(client),
						client.onDisconnect()
				));
		
		DataHandler.initialize()
				.then(onDisconnect)
				.doOnError(err -> {
					// This should never happen. If it does something really is messed up.
					logger.fatal("Unhandled exception in bots main handler", err);
					System.exit(-1);
				})
				.block();
		logger.warn("Reached the end of the program");
	}

}
