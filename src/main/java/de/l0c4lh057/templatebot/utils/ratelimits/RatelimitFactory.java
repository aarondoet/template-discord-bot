package de.l0c4lh057.templatebot.utils.ratelimits;

import io.github.bucket4j.Bandwidth;
import reactor.util.annotation.NonNull;

import java.util.List;

public class RatelimitFactory {
	public static Ratelimit getRatelimit(@NonNull RatelimitType ratelimitType, @NonNull List<Bandwidth> bandwidths){
		if(ratelimitType == RatelimitType.GUILD) return new GuildRatelimit(bandwidths);
		else if(ratelimitType == RatelimitType.CHANNEL) return new ChannelRatelimit(bandwidths);
		else if(ratelimitType == RatelimitType.USER) return new UserRatelimit(bandwidths);
		else if(ratelimitType == RatelimitType.MEMBER) return new MemberRatelimit(bandwidths);
		else if(ratelimitType == RatelimitType.USER_PER_CHANNEL) return new UserChannelRatelimit(bandwidths);
		else return NoRatelimit.getInstance();
	}
}
