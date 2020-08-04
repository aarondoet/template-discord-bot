package de.l0c4lh057.templatebot.utils.ratelimits;

import discord4j.common.util.Snowflake;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.local.LocalBucketBuilder;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Ratelimit {
	protected List<Bandwidth> bandwidths;
	protected Bucket newBucket(){
		LocalBucketBuilder builder = Bucket4j.builder();
		bandwidths.forEach(builder::addLimit);
		return builder.build();
	}
	public abstract boolean isRatelimited(@Nullable Snowflake guildId, @NonNull Snowflake channelId, @NonNull Snowflake userId);
	protected boolean isRatelimited(@NonNull Map<Long, Bucket> buckets, @NonNull Snowflake id){
		return !buckets.computeIfAbsent(id.asLong(), k -> newBucket()).tryConsume(1);
	}
	protected boolean isRatelimited(@NonNull Map<Long, Map<Long, Bucket>> buckets, @NonNull Snowflake id1, @NonNull Snowflake id2){
		return isRatelimited(buckets.computeIfAbsent(id1.asLong(), k -> new HashMap<>()), id2);
	}
}
