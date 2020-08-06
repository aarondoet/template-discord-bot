package de.l0c4lh057.templatebot.data;

import de.l0c4lh057.templatebot.utils.BotUtils;
import discord4j.common.util.Snowflake;
import io.r2dbc.spi.Row;
import reactor.util.annotation.NonNull;

public class DBGuild {
	
	public static final DBGuild defaultGuild = new DBGuild(Snowflake.of(0), BotUtils.DEFAULT_PREFIX, "en");
	
	private final Snowflake id;
	private final String prefix;
	private final String language;
	
	private DBGuild(@NonNull Snowflake id, @NonNull String prefix, @NonNull String language){
		this.id = id;
		this.prefix = prefix;
		this.language = language;
	}
	
	/**
	 * @return
	 */
	@NonNull public Snowflake getId(){ return id; }
	/**
	 * @return
	 */
	@NonNull public String getPrefix(){ return prefix; }
	/**
	 * @return
	 */
	@NonNull public String getLanguage() { return language; }
	
	/**
	 *
	 * @param row
	 * @return
	 */
	@NonNull
	public static DBGuild ofRow(@NonNull Row row){
		return new DBGuild(
				Snowflake.of(row.get("guildId", Integer.class)),
				row.get("prefix", String.class),
				row.get("language", String.class)
		);
	}
	
}
