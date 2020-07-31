package de.l0c4lh057.templatebot.utils.ratelimits;

import discord4j.common.util.Snowflake;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserChannelRatelimit extends Ratelimit {
	private final Map<Long, Map<Long, Bucket>> buckets = new HashMap<>();
	UserChannelRatelimit(List<Bandwidth> bandwidths){
		this.bandwidths = bandwidths;
	}
	@Override
	public boolean isRatelimited(@Nullable Snowflake guildId, @NotNull Snowflake channelId, @NotNull Snowflake userId) {
		return isRatelimited(buckets, channelId, userId);
	}
}
