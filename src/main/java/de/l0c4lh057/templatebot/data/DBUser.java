package de.l0c4lh057.templatebot.data;

import de.l0c4lh057.templatebot.utils.BotUtils;
import discord4j.common.util.Snowflake;
import io.r2dbc.spi.Row;

public class DBUser {
	
	public static final DBUser defaultUser = new DBUser(Snowflake.of(0), BotUtils.DEFAULT_PREFIX, "en");
	
	private final Snowflake id;
	private final String prefix;
	private final String language;
	
	private DBUser(Snowflake id, String prefix, String language){
		this.id = id;
		this.prefix = prefix;
		this.language = language;
	}
	
	public Snowflake getId(){ return id; }
	public String getPrefix(){ return prefix; }
	public String getLanguage() { return language; }
	
	public static DBUser ofRow(Row row){
		return new DBUser(
				Snowflake.of(row.get("guildId", Integer.class)),
				row.get("prefix", String.class),
				row.get("language", String.class)
		);
	}
	
}
