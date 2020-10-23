package de.l0c4lh057.templatebot.commands;

import de.l0c4lh057.templatebot.utils.ratelimits.Ratelimit;
import de.l0c4lh057.templatebot.utils.ratelimits.RatelimitFactory;
import de.l0c4lh057.templatebot.utils.ratelimits.RatelimitType;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import io.github.bucket4j.Bandwidth;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.List;

public class ExampleCommandWithOwnClass extends Command {
	public ExampleCommandWithOwnClass(){
		super();
	}
	
	@NonNull
	@Override
	public String getName() {
		return "example";
	}
	
	/**
	 * @return All the aliases of the command (does not include the name returned by {@link #getName()})
	 */
	@NonNull
	@Override
	public String[] getAliases() {
		return new String[]{"exampleAlias"};
	}
	
	@NonNull
	@Override
	public Category getCategory() {
		return Category.GENERAL;
	}
	
	@NonNull
	@Override
	public CommandExecutor getExecutor() {
		return (context, language, prefix, args) -> context.respond("this is a test command to demonstrate how to create commands with their own class");
	}
	
	@Override
	public boolean isUsableInGuilds() {
		return true;
	}
	
	@Override
	public boolean isUsableInDMs() {
		return true;
	}
	
	@Nullable
	@Override
	public Command getSubCommand(@NonNull String subCommand) {
		return null;
	}
	
	@Override
	public int getHelpPagePosition() {
		return super.getHelpPagePosition();
	}
	
	@Override
	public boolean isNsfw() {
		return false;
	}
	
	private final Ratelimit ratelimit = RatelimitFactory.getRatelimit(RatelimitType.GUILD, List.of(Bandwidth.simple(4, Duration.ofMinutes(3))));
	@NonNull
	@Override
	public Ratelimit getRatelimit() {
		// NOTICE: the ratelimit instance should not get created new for every call of this function!
		return ratelimit;
	}
	
	@Nullable
	@Override
	public de.l0c4lh057.templatebot.utils.Permission getRequiredPermissions() {
		return null;
	}
	
	@NonNull
	@Override
	public PermissionSet getPermissionsNeededByBot() {
		return PermissionSet.of(Permission.SEND_MESSAGES);
	}
}
