package de.l0c4lh057.templatebot.data;

import de.l0c4lh057.templatebot.commands.PermissionManager;
import de.l0c4lh057.templatebot.main.Credentials;
import discord4j.common.util.Snowflake;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.time.Duration;
import java.util.Objects;

public class DataHandler {
	
	private static final Logger logger = LogManager.getLogger("DataHandler");
	
	private static final ConnectionPool pool;
	
	static {
		PostgresqlConnectionFactory connectionFactory = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
				.host(Credentials.SQL_HOST)
				.port(Credentials.SQL_PORT)
				.username(Objects.requireNonNull(Credentials.SQL_USERNAME))
				.password(Credentials.SQL_PASSWORD)
				.database(Credentials.SQL_DATABASE)
				.connectTimeout(Duration.ofSeconds(3))
				.build()
		);
		// TODO: adjust max pool size
		ConnectionPoolConfiguration configuration = ConnectionPoolConfiguration.builder(connectionFactory)
				.maxSize(10)
				.build();
		pool = new ConnectionPool(configuration);
	}
	
	/**
	 * Gets one of the {@link Connection}s inside the {@link ConnectionPool}.
	 *
	 * @return A database connection
	 */
	@NonNull
	private static Mono<Connection> getConnection(){
		return pool.create();
	}
	
	private enum Tables {
		GUILDS("guilds"),
		USERS("users"),
		PERMISSIONS("permissions")
		;
		private final String name;
		Tables(@NonNull String name){
			this.name = name;
		}
		/**
		 * @return The name of the table in the database
		 */
		@NonNull public String getName() { return name; }
	}
	
	/**
	 * Creates all missing tables.
	 *
	 * @return An empty {@link Mono}
	 */
	@NonNull
	public static Mono<Void> initialize(){
		String createGuildsTable = "CREATE TABLE IF NOT EXISTS " + Tables.GUILDS.getName() + " (" +
				"guildId BIGINT," +
				"prefix VARCHAR(10)," +
				"language VARCHAR(5)," +
				"PRIMARY KEY(guildId)" +
				")";
		String createUsersTable = "CREATE TABLE IF NOT EXISTS " + Tables.USERS.getName() + " (" +
				"userId BIGINT," +
				"prefix VARCHAR(10)," +
				"language VARCHAR(5)," +
				"PRIMARY KEY(userId)" +
				")";
		String createPermissionsTable = "CREATE TABLE IF NOT EXISTS " + Tables.PERMISSIONS.getName() + " (" +
				"permissionName TEXT," +
				"guildId BIGINT," +
				"targetId BIGINT," +
				"isUser BOOLEAN," +
				"isWhitelist BOOLEAN," +
				"PRIMARY KEY(guildId, targetId, isUser)" +
				")";
		return Mono.fromRunnable(() -> logger.info("Initializing database"))
				.then(
						getConnection()
								.flatMap(connection -> Flux.from(connection.createBatch()
										.add(createGuildsTable)
										.add(createUsersTable)
										.add(createPermissionsTable)
										.execute())
										.then(Mono.from(connection.close()))
								)
				);
	}
	
	/**
	 * Puts the default values into the database for the provided ID. Nothing happens if the guild is already saved.
	 *
	 * @param guildId The ID of the guild that should get put into the database
	 * @return A {@link Mono} that upon success emits {@code true} if the guild got inserted into the database or
	 * {@code false} if the guild was already saved.
	 */
	@NonNull
	public static Mono<Boolean> initializeGuild(@NonNull Snowflake guildId){
		return getConnection().flatMap(con -> Mono.from(con.createStatement("INSERT INTO " + Tables.GUILDS.getName() + " (guildId, prefix, language) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING")
				.bind("$1", guildId.asLong())
				.bind("$2", DBGuild.defaultGuild.getPrefix())
				.bind("$3", DBGuild.defaultGuild.getLanguage())
				.execute())
				.flatMapMany(Result::getRowsUpdated).next()
				.map(i -> i > 0)
		);
	}
	
	/**
	 * Puts the default values into the database for the provided ID. Nothing happens if the user is already saved.
	 *
	 * @param userId The ID of the user that should get put into the database.
	 * @return A {@link Mono} that upon success emits {@code true} if the user got inserted into the database or
	 * {@code false} if the user was already saved.
	 */
	@NonNull
	public static Mono<Boolean> initializeUser(@NonNull Snowflake userId){
		return getConnection().flatMap(con -> Mono.from(con.createStatement("INSERT INTO " + Tables.USERS.getName() + " (userId, prefix, language) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING")
				.bind("$1", userId.asLong())
				.bind("$2", DBUser.defaultUser.getPrefix())
				.bind("$3", DBUser.defaultUser.getLanguage())
				.execute())
				.flatMapMany(Result::getRowsUpdated).next()
				.map(i -> i > 0)
		);
	}
	
	/**
	 * Retrieves the stored data of the guild with the provided ID.
	 *
	 * @param guildId The ID of the guild to get the data from
	 * @return A {@link Mono} emitting the {@link DBGuild} upon completion
	 */
	@NonNull
	public static Mono<DBGuild> getGuild(@NonNull Snowflake guildId){
		return getConnection().flatMap(con -> Mono.from(con.createStatement("SELECT * FROM " + Tables.GUILDS.getName() + " WHERE guildId=$1 LIMIT 1")
				.bind("$1", guildId.asLong())
				.execute())
				.flatMap(result -> Mono.from(result.map((row, rowMetadata) -> DBGuild.ofRow(row))))
		);
	}
	
	/**
	 * Retrieves the stored data of the user with the provided ID.
	 *
	 * @param userId The ID of the user to get the data from
	 * @return A {@link Mono} emitting the {@link DBUser} upon completion
	 */
	@NonNull
	public static Mono<DBUser> getUser(@NonNull Snowflake userId){
		return getConnection().flatMap(con -> Mono.from(con.createStatement("SELECT * FROM " + Tables.USERS.getName() + " WHERE userId=$1 LIMIT 1")
				.bind("$1", userId.asLong())
				.execute())
				.flatMap(result -> Mono.from(result.map((row, rowMetadata) -> DBUser.ofRow(row))))
		);
	}
	
	/**
	 * Retrieves all black- and whitelisted users and roles for the permission in the provided guild.
	 *
	 * @param permName The name of the permission you want to check
	 * @param guildId  The ID of the guild you want to get the data of
	 * @return A {@link Flux} emitting all entries for the permission inside the guild upon success
	 */
	@NonNull
	public static Flux<PermissionManager.CommandPermission> getPermissions(@NonNull String permName, @NonNull Snowflake guildId){
		return getConnection().flatMapMany(con -> Flux.from(con.createStatement("SELECT * FROM " + Tables.PERMISSIONS.getName() + " WHERE permissionName=$1 AND guildId=$2")
				.bind("$1", permName)
				.bind("$2", guildId.asLong())
				.execute())
				.flatMap(result -> Mono.from(result.map((row, rowMetadata) -> PermissionManager.CommandPermission.ofRow(row))))
		);
	}
	
}
