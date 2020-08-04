package de.l0c4lh057.templatebot.data;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.channel.*;
import discord4j.core.event.domain.guild.*;
import discord4j.core.event.domain.role.RoleCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.event.domain.role.RoleUpdateEvent;
import discord4j.core.object.ExtendedPermissionOverwrite;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.util.PermissionUtil;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DiscordCache {
	
	/**
	 * Registers all events needed to update the cache
	 *
	 * @param client The {@link GatewayDiscordClient} on which the events should get registered on
	 * @return An empty {@link Mono} containing all the event subscriptions
	 */
	public static Mono<Void> registerEvents(@NonNull GatewayDiscordClient client){
		return Mono.when(
				client.on(TextChannelUpdateEvent.class).map(TextChannelUpdateEvent::getCurrent).doOnNext(DiscordCache::addChannel),
				//client.on(VoiceChannelUpdateEvent.class).map(VoiceChannelUpdateEvent::getCurrent).doOnNext(DiscordCache::addChannel),
				client.on(NewsChannelUpdateEvent.class).map(NewsChannelUpdateEvent::getCurrent).doOnNext(DiscordCache::addChannel),
				client.on(StoreChannelUpdateEvent.class).map(StoreChannelUpdateEvent::getCurrent).doOnNext(DiscordCache::addChannel),
				client.on(TextChannelCreateEvent.class).map(TextChannelCreateEvent::getChannel).doOnNext(DiscordCache::addChannel),
				//client.on(VoiceChannelCreateEvent.class).map(VoiceChannelCreateEvent::getChannel).doOnNext(DiscordCache::addChannel),
				client.on(NewsChannelCreateEvent.class).map(NewsChannelCreateEvent::getChannel).doOnNext(DiscordCache::addChannel),
				client.on(StoreChannelCreateEvent.class).map(StoreChannelCreateEvent::getChannel).doOnNext(DiscordCache::addChannel),
				client.on(TextChannelDeleteEvent.class).map(TextChannelDeleteEvent::getChannel).doOnNext(DiscordCache::removeChannel),
				//client.on(VoiceChannelDeleteEvent.class).map(VoiceChannelDeleteEvent::getChannel).doOnNext(DiscordCache::removeChannel),
				client.on(NewsChannelDeleteEvent.class).map(NewsChannelDeleteEvent::getChannel).doOnNext(DiscordCache::removeChannel),
				client.on(StoreChannelDeleteEvent.class).map(StoreChannelDeleteEvent::getChannel).doOnNext(DiscordCache::removeChannel),
				
				client.on(MemberChunkEvent.class).map(MemberChunkEvent::getMembers).doOnNext(members -> members.forEach(DiscordCache::addMember)),
				client.on(MemberJoinEvent.class).map(MemberJoinEvent::getMember).doOnNext(DiscordCache::addMember),
				client.on(MemberUpdateEvent.class).doOnNext(event -> addMember(event.getGuildId(), event.getMemberId(), event.getCurrentRoles())),
				client.on(MemberLeaveEvent.class).doOnNext(event -> removeMember(event.getGuildId(), event.getUser().getId())),
				
				client.on(GuildCreateEvent.class).map(GuildCreateEvent::getGuild).flatMap(DiscordCache::addGuild),
				client.on(GuildUpdateEvent.class).map(GuildUpdateEvent::getCurrent).flatMap(DiscordCache::addGuild),
				client.on(GuildDeleteEvent.class).filter(event -> !event.isUnavailable()).map(GuildDeleteEvent::getGuildId).doOnNext(DiscordCache::removeGuild),
				
				client.on(RoleCreateEvent.class).map(RoleCreateEvent::getRole).doOnNext(DiscordCache::addRole),
				client.on(RoleUpdateEvent.class).map(RoleUpdateEvent::getCurrent).doOnNext(DiscordCache::addRole),
				client.on(RoleDeleteEvent.class).doOnNext(event -> removeRole(event.getGuildId(), event.getRoleId()))
				
				// adding members in MessageCreateEvent is in the events class to make sure it happens before command execution
		);
	}
	
	private static final Map<Long, MinimalGuild> guilds = new HashMap<>();
	
	/**
	 * Gets the cached {@link MinimalGuild}.
	 *
	 * @param guildId The ID of the guild you want to get
	 * @return An empty {@link Optional} if the guild is not cached, otherwise an {@link Optional} containing the {@link MinimalGuild}
	 */
	public static Optional<MinimalGuild> getGuild(Snowflake guildId){
		return Optional.ofNullable(guilds.get(guildId.asLong()));
	}
	
	private static void addChannel(GuildChannel channel){
		getGuild(channel.getGuildId()).ifPresent(guild -> guild.addChannel(new MinimalChannel(channel.getId(), channel.getPermissionOverwrites(), channel instanceof TextChannel && ((TextChannel)channel).isNsfw())));
	}
	private static void removeChannel(GuildChannel channel){
		getGuild(channel.getGuildId()).ifPresent(guild -> guild.removeChannel(channel.getId()));
	}
	public static void addMember(Member member){
		addMember(member.getGuildId(), member.getId(), member.getRoleIds());
	}
	private static void addMember(Snowflake guildId, Snowflake userId, Set<Snowflake> roleIds){
		getGuild(guildId).ifPresent(guild -> guild.addMember(new MinimalMember(userId, roleIds)));
	}
	private static void removeMember(Snowflake guildId, Snowflake userId){
		getGuild(guildId).ifPresent(guild -> guild.removeMember(userId));
	}
	private static void addRole(Role role){
		getGuild(role.getGuildId()).ifPresent(guild -> guild.addRole(new MinimalRole(role.getRawPosition(), role.getGuildId(), role.getId(), role.getPermissions())));
	}
	private static void removeRole(Snowflake guildId, Snowflake roleId){
		getGuild(guildId).ifPresent(guild -> guild.removeRole(roleId));
	}
	private static void removeGuild(Snowflake guildId){
		guilds.remove(guildId.asLong());
	}
	private static Mono<Void> addGuild(Guild guild){
		MinimalGuild minimalGuild = new MinimalGuild(
				guild.getId(),
				guild.getOwnerId(),
				new HashMap<>(),
				new HashMap<>(),
				new HashMap<>()
		);
		return Mono.when(
				Mono.fromRunnable(() -> guilds.put(guild.getId().asLong(), minimalGuild)),
				guild.getRoles().doOnNext(role -> minimalGuild.addRole(new MinimalRole(role.getRawPosition(), role.getGuildId(), role.getId(), role.getPermissions())))
		);
	}
	
	public static class MinimalRole {
		private final int position;
		private final Snowflake guildId;
		private final Snowflake id;
		private final PermissionSet permissions;
		private MinimalRole(int position, Snowflake guildId, Snowflake id, PermissionSet permissions){
			this.position = position;
			this.guildId = guildId;
			this.id = id;
			this.permissions = permissions;
		}
		/**
		 *
		 * @return
		 */
		public int getRawPosition(){ return position; }
		/**
		 *
		 * @return
		 */
		public Snowflake getGuildId(){ return guildId; }
		/**
		 *
		 * @return
		 */
		public Snowflake getId(){ return id; }
		/**
		 *
		 * @return
		 */
		public PermissionSet getPermissions(){ return permissions; }
	}
	
	public static class MinimalGuild {
		private final Snowflake id;
		private final Snowflake ownerId;
		private final Map<Long, MinimalRole> roles;
		private final Map<Long, MinimalChannel> channels;
		private final Map<Long, MinimalMember> members;
		private MinimalGuild(Snowflake id, Snowflake ownerId, @Nullable Map<Long, MinimalRole> roles,
												 @Nullable Map<Long, MinimalChannel> channels, @Nullable Map<Long, MinimalMember> members){
			this.id = id;
			this.ownerId = ownerId;
			this.roles = roles == null ? new HashMap<>() : roles;
			this.channels = channels == null ? new HashMap<>() : channels;
			this.members = members == null ? new HashMap<>() : members;
			this.members.values().forEach(member -> member.guild = this);
		}
		/**
		 *
		 * @return
		 */
		public Snowflake getId(){ return id; }
		/**
		 *
		 * @return
		 */
		public Snowflake getOwnerId(){ return ownerId; }
		/**
		 *
		 * @return
		 */
		public Stream<MinimalRole> getRoles(){
			return roles.values().stream()
					.sorted(Comparator.comparing(MinimalRole::getRawPosition).thenComparing(MinimalRole::getId));
		}
		/**
		 *
		 * @param channelId
		 * @return
		 */
		public Optional<MinimalChannel> getChannel(Snowflake channelId){
			return Optional.ofNullable(channels.get(channelId.asLong()));
		}
		/**
		 *
		 * @param memberId
		 * @return
		 */
		public Optional<MinimalMember> getMember(Snowflake memberId){
			return Optional.ofNullable(members.get(memberId.asLong()));
		}
		/**
		 *
		 * @param roleId
		 * @return
		 */
		public Optional<MinimalRole> getRole(Snowflake roleId){
			return Optional.ofNullable(roles.get(roleId.asLong()));
		}
		private void addMember(MinimalMember member){
			member.guild = this;
			members.put(member.getId().asLong(), member);
		}
		private void addRole(MinimalRole role){
			roles.put(role.getId().asLong(), role);
		}
		private void addChannel(MinimalChannel channel){
			channels.put(channel.getId().asLong(), channel);
		}
		private void removeMember(Snowflake userId){
			members.remove(userId.asLong());
		}
		private void removeRole(Snowflake roleId){
			roles.remove(roleId.asLong());
		}
		private void removeChannel(Snowflake channelId){
			channels.remove(channelId.asLong());
		}
	}
	
	public static class MinimalChannel {
		private final Snowflake id;
		private final Set<ExtendedPermissionOverwrite> permissionOverwrites;
		private final boolean nsfw;
		private MinimalChannel(Snowflake id, Set<ExtendedPermissionOverwrite> permissionOverwrites, boolean nsfw){
			this.id = id;
			this.permissionOverwrites = permissionOverwrites;
			this.nsfw = nsfw;
		}
		/**
		 *
		 * @return
		 */
		public Snowflake getId(){ return id; }
		/**
		 *
		 * @return
		 */
		public Set<ExtendedPermissionOverwrite> getPermissionOverwrites(){ return permissionOverwrites; }
		/**
		 *
		 * @return
		 */
		public boolean isNsfw() { return nsfw; }
	}
	
	public static class MinimalMember {
		private MinimalGuild guild;
		private final Snowflake id;
		private final Set<Snowflake> roleIds;
		private MinimalMember(Snowflake id, Set<Snowflake> roleIds){
			this.guild = null;
			this.id = id;
			this.roleIds = roleIds;
		}
		public MinimalGuild getGuild(){ return guild; }
		public Snowflake getId(){ return id; }
		/**
		 *
		 * @return
		 */
		public Stream<MinimalRole> getRoles(){
			return guild.getRoles()
					.filter(role -> role.getId().equals(role.getGuildId()) || roleIds.contains(role.getId()));
		}
		/**
		 *
		 * @return
		 */
		public PermissionSet getBasePermissions(){
			if(guild.getOwnerId().equals(id)) return PermissionSet.all();
			return PermissionUtil.computeBasePermissions(PermissionSet.none(), getRoles().map(MinimalRole::getPermissions).collect(Collectors.toList()));
		}
		/**
		 *
		 * @param channelId
		 * @return
		 */
		public PermissionSet getEffectivePermissions(Snowflake channelId){
			if(guild.getOwnerId().equals(id)) return PermissionSet.all();
			PermissionSet basePermissions = getBasePermissions();
			List<Snowflake> sortedRoleIds = getRoles().map(MinimalRole::getId).collect(Collectors.toList());
			MinimalChannel channel = guild.getChannel(channelId).orElse(null);
			if(channel != null){
				List<PermissionOverwrite> overwrites = channel.getPermissionOverwrites().stream()
						.filter(overwrite -> overwrite.getRoleId().map(roleIds::contains).orElse(false))
						.sorted(Comparator.comparingInt(overwrite -> sortedRoleIds.indexOf(overwrite.getTargetId())))
						.collect(Collectors.toList());
				PermissionOverwrite memberOverwrite = channel.getPermissionOverwrites().stream()
						.filter(overwrite -> overwrite.getMemberId().map(id::equals).orElse(false))
						.findAny().orElse(null);
				return PermissionUtil.computePermissions(getBasePermissions(), null, overwrites, memberOverwrite);
			}
			return basePermissions;
		}
	}
	
}
