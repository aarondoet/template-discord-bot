package de.l0c4lh057.templatebot.main;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Objects;

public class Credentials {
	
	private static final Dotenv dotenv = Dotenv.load();
	
	/**
	 * The token of your discord bot used to authenticate
	 */
	public static final String BOT_TOKEN = Objects.requireNonNull(dotenv.get("BOT_TOKEN"));
	
	/**
	 * The host of your SQL server, defaults to {@code 127.0.0.1}
	 */
	public static final String SQL_HOST = dotenv.get("SQL_HOST", "127.0.0.1");
	/**
	 * The port of your SQL server, defaults to {@code 5432}
	 */
	public static final int SQL_PORT = Integer.parseInt(dotenv.get("SQL_PORT", "5432"));
	/**
	 * The username of your SQL database user
	 */
	public static final String SQL_USERNAME = dotenv.get("SQL_USERNAME");
	/**
	 * The password of your SQL database user
	 */
	public static final String SQL_PASSWORD = dotenv.get("SQL_PASSWORD");
	/**
	 * The name of your SQL database
	 */
	public static final String SQL_DATABASE = dotenv.get("SQL_DATABASE");
	
}
