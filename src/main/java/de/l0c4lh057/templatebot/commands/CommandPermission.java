package de.l0c4lh057.templatebot.commands;

import discord4j.rest.util.Permission;

public class CommandPermission {
	
	private CommandPermission(CommandPermissionBuilder builder){
	
	}
	
	public static CommandPermissionBuilder builder(){
		return new CommandPermissionBuilder();
	}
	
	private static class CommandPermissionBuilder {
		private boolean usableByEveryone = false;
		private boolean needsBotOwner = false;
		private boolean needsGuildOwner = false;
		private String botPermissions = null;
		private Permission[] defaultPermissions = {};
		
		private CommandPermissionBuilder(){}
		
		/**
		 *
		 * @param usableByEveryone
		 * @return
		 */
		public CommandPermissionBuilder setUsableByEveryone(boolean usableByEveryone){
			this.usableByEveryone = usableByEveryone;
			return this;
		}
		
		public CommandPermission build(){
			return new CommandPermission(this);
		}
	}
	
}
