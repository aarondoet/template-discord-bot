package de.l0c4lh057.templatebot.utils.ratelimits;

import discord4j.common.util.Snowflake;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuildRatelimit extends Ratelimit {
	private final Map<Long, Bucket> buckets = new HashMap<>();
	private final Map<Long, Bucket> noGuildBuckets = new HashMap<>();
	GuildRatelimit(List<Bandwidth> bandwidths){
		this.bandwidths = bandwidths;
	}
	@Override
	public boolean isRatelimited(@Nullable Snowflake guildId, @NonNull Snowflake channelId, @NonNull Snowflake userId) {
		if(guildId == null) return isRatelimited(noGuildBuckets, userId);
		return isRatelimited(buckets, guildId);
	}
}
