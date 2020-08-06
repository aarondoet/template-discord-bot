package de.l0c4lh057.templatebot.commands;

import de.l0c4lh057.templatebot.commands.exceptions.CommandException;
import de.l0c4lh057.templatebot.data.DataHandler;
import de.l0c4lh057.templatebot.data.DiscordCache;
import discord4j.common.util.Snowflake;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import io.r2dbc.spi.Row;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionManager {
	
	@NonNull
	private static Mono<Void> checkExecutability(@NonNull Snowflake guildId, @NonNull Snowflake userId, @NonNull List<Snowflake> roleIds, @NonNull PermissionSet effectivePermissions, @Nullable Command.Permission requiredPermissions){
		if(DiscordCache.getGuild(guildId).map(DiscordCache.MinimalGuild::getOwnerId).map(userId::equals).orElse(false)) return Mono.empty();
		if(requiredPermissions == null) return Mono.empty();
		return DataHandler.getPermissions(requiredPermissions.getPermissionName(), guildId)
				.groupBy(CommandPermission::isUser)
				.flatMap(rolePermissions -> rolePermissions.groupBy(CommandPermission::isWhitelist)
						.flatMap(Flux::collectList)
				)
				.collectList()
				.flatMap(lists -> {
					boolean onUserBlacklist = lists.stream().filter(perms -> perms.size() > 0 && perms.get(0).isUser() && perms.get(0).isBlacklist()).findAny().map(perms -> perms.stream().anyMatch(perm -> perm.getTargetId().equals(userId))).orElse(false);
					List<Snowflake> userWhitelist = lists.stream().filter(perms -> perms.size() > 0 && perms.get(0).isUser() && perms.get(0).isWhitelist()).findAny().orElseGet(Collections::emptyList).stream().map(CommandPermission::getTargetId).collect(Collectors.toList());
					List<Snowflake> roleWhitelist = lists.stream().filter(perms -> perms.size() > 0 && perms.get(0).isRole() && perms.get(0).isWhitelist()).findAny().orElseGet(Collections::emptyList).stream().map(CommandPermission::getTargetId).collect(Collectors.toList());
					List<Snowflake> roleBlacklist = lists.stream().filter(perms -> perms.size() > 0 && perms.get(0).isRole() && perms.get(0).isBlacklist()).findAny().orElseGet(Collections::emptyList).stream().map(CommandPermission::getTargetId).collect(Collectors.toList());
					boolean hasPerms = effectivePermissions.contains(Permission.ADMINISTRATOR);
					if(onUserBlacklist) return Mono.error(CommandException.missingPermissions("exception.missingpermissions"));
					boolean blacklisted = false;
					if(!hasPerms && roleIds.stream().anyMatch(roleWhitelist::contains)) hasPerms = true;
					if(hasPerms && roleIds.stream().anyMatch(roleBlacklist::contains)){
						blacklisted = true;
						hasPerms = false;
					}
					if(!hasPerms && userWhitelist.stream().anyMatch(userId::equals)) hasPerms = true;
					if(!hasPerms && !blacklisted && userWhitelist.isEmpty() && roleWhitelist.isEmpty()){
						if(requiredPermissions.getDefaultPermissions().containsAll(effectivePermissions)) hasPerms = true;
					}
					if(hasPerms) return Mono.empty();
					else return Mono.error(CommandException.missingPermissions("exception.missingpermissions"));
				});
	}
	
	/**
	 *
	 * @param guildId
	 * @param userId
	 * @param channelId
	 * @param permission
	 * @return An empty {@link Mono} if the user has the needed permissions, otherwise a {@link Mono} containing a
	 * {@link de.l0c4lh057.templatebot.commands.exceptions.CommandException} describing why the permissions are missing.
	 */
	@NonNull
	public static Mono<Void> checkExecutability(@Nullable Snowflake guildId, @NonNull Snowflake userId,
	                                            @NonNull Snowflake channelId, @Nullable Command.Permission permission,
	                                            boolean requiresGuildOwner, boolean requiresNsfwChannel){
		if(guildId == null) return Mono.empty();
		return Mono.justOrEmpty(DiscordCache.getGuild(guildId).flatMap(guild -> guild.getMember(userId)))
				.switchIfEmpty(Mono.error(CommandException.missingPermissions("exception.notcached")))
				.flatMap(member -> {
					if(member.getGuild().getOwnerId().equals(member.getId())) return Mono.empty();
					else if(requiresGuildOwner) return Mono.error(CommandException.missingPermissions("exception.requiresguildowner"));
					else if(requiresNsfwChannel && !member.getGuild().getChannel(channelId).map(DiscordCache.MinimalChannel::isNsfw).orElse(false))
						return Mono.error(CommandException.notExecutable("exception.requiresnsfwchannel"));
					else return checkExecutability(
								guildId,
								userId,
								member.getRoles().map(DiscordCache.MinimalRole::getId).collect(Collectors.toList()),
								member.getEffectivePermissions(channelId),
								permission
						);
				});
	}
	
	/**
	 *
	 * @param guildId
	 * @param userId
	 * @param channelId
	 * @param permission
	 * @param requiresGuildOwner
	 * @return An empty {@link Mono} if the user has the needed permissions, otherwise a {@link Mono} containing a
	 * {@link de.l0c4lh057.templatebot.commands.exceptions.CommandException} describing why the permissions are missing.
	 */
	public static Mono<Void> checkExecutability(@Nullable Snowflake guildId, @NonNull Snowflake userId,
	                                            @NonNull Snowflake channelId, @NonNull Command.Permission permission,
	                                            boolean requiresGuildOwner){
		return checkExecutability(guildId, userId, channelId, permission, requiresGuildOwner, false);
	}
	
	/**
	 * Checks whether a user has the permission to perform a certain action
	 *
	 * @param guildId
	 * @param userId
	 * @param permission
	 * @return An empty {@link Mono} if the user has the needed permissions, otherwise a {@link Mono} containing a
	 * {@link de.l0c4lh057.templatebot.commands.exceptions.CommandException} describing why the permissions are missing.
	 */
	public static Mono<Void> checkExecutability(@Nullable Snowflake guildId, @NonNull Snowflake userId,
																							@NonNull Command.Permission permission, boolean requiresGuildOwner){
		if(guildId == null) return Mono.empty();
		return Mono.justOrEmpty(DiscordCache.getGuild(guildId).flatMap(guild -> guild.getMember(userId)))
				.switchIfEmpty(Mono.error(CommandException.missingPermissions("exception.notcached")))
				.flatMap(member -> {
					if(member.getGuild().getOwnerId().equals(member.getId())) return Mono.empty();
					else if(requiresGuildOwner) return Mono.error(CommandException.missingPermissions("exception.requiresguildowner"));
					else return checkExecutability(
								guildId,
								userId,
								member.getRoles().map(DiscordCache.MinimalRole::getId).collect(Collectors.toList()),
								member.getBasePermissions(),
								permission
						);
				});
	}
	
	public static class CommandPermission {
		private final Snowflake targetId;
		private final boolean isWhitelist;
		private final boolean isUser;
		private CommandPermission(Snowflake targetId, boolean isWhitelist, boolean isUser){
			this.targetId = targetId;
			this.isWhitelist = isWhitelist;
			this.isUser = isUser;
		}
		/**
		 * @return
		 */
		public Snowflake getTargetId(){ return targetId; }
		/**
		 * @return
		 */
		public boolean isWhitelist(){ return isWhitelist; }
		/**
		 * @return
		 */
		public boolean isBlacklist(){ return !isWhitelist; }
		/**
		 * @return
		 */
		public boolean isUser(){ return isUser; }
		/**
		 * @return
		 */
		public boolean isRole(){ return !isUser; }
		/**
		 *
		 * @return
		 */
		public static CommandPermission ofRow(@NonNull Row row){
			return new CommandPermission(
					Snowflake.of(row.get("targetId", Long.class)),
					row.get("isWhitelist", Boolean.class),
					row.get("isUser", Boolean.class)
			);
		}
	}
	
}
