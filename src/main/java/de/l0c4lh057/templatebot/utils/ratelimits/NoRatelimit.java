package de.l0c4lh057.templatebot.utils.ratelimits;

import discord4j.common.util.Snowflake;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

public class NoRatelimit extends Ratelimit {
	private static final Ratelimit instance = new NoRatelimit();
	public static Ratelimit getInstance() { return instance; }
	private NoRatelimit(){}
	@Override
	public boolean isRatelimited(@Nullable Snowflake guildId, @NonNull Snowflake channelId, @NonNull Snowflake userId) {
		return false;
	}
}
