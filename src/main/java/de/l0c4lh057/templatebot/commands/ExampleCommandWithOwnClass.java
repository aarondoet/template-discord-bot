package de.l0c4lh057.templatebot.commands;

import reactor.util.annotation.NonNull;

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
		return (event, language, prefix, args) -> event.getMessage().getRestChannel()
				.createMessage("this is a test command to demonstrate how to create commands with their own class");
	}
	
	@Override
	public boolean isUsableInGuilds() {
		return true;
	}
	
	@Override
	public boolean isUsableInDMs() {
		return true;
	}
	
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
}
