package de.l0c4lh057.templatebot.utils.ratelimits;

import discord4j.common.util.Snowflake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NoRatelimit extends Ratelimit {
	private static final Ratelimit instance = new NoRatelimit();
	public static Ratelimit getInstance() { return instance; }
	private NoRatelimit(){}
	@Override
	public boolean isRatelimited(@Nullable Snowflake guildId, @NotNull Snowflake channelId, @NotNull Snowflake userId) {
		return false;
	}
}
