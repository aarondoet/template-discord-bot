# Template Discord Bot
This is a template bot written in Java using Discord4J. It can be used as a starting point for creating a new discord bot
without the need to create a command handler and implement features like permission checking every time you start a new one.

## What this template takes care of

* Command Handling
    * Aliases
    * Permissions
    * Ratelimits
    * Sub commands (sub sub commands, sub sub sub commands, ...)
    * User friendly exceptions
* Help page system (Including sub commands)
* Localization (You still need to add all the languages)

## Requirements

* PostgreSQL database

## Values to change
You need to adjust a few values to turn this into a bot without placeholder values everywhere.
All class paths will be relative to the main path which is `de.l0c4lh057.templatebot` in this case.

* Refactor the default package name from `de.l0c4lh057.templatebot` to whatever you want it to be.
* Grep for `templatebot` and change it in every occurrence.
* Change `utils.BotUtils#botOwner` to a list of IDs of the people who should be considered bot owner.
* Change `utils.BotUtils#DEFAULT_PREFIX` to your desired default prefix.
* Change `utils.BotUtils#BOT_COLOR` to a color you like. This color will be used as embed color in commands that don't indicate success/failure with color.
* Set required credentials like the bot token in `main.Credentials`.
* Add all exceptions you need to `commands.exceptions` and make them extend the `commands.exceptions.CommandException` class.
* Add an exception handler for every exception you added in `commands.Command#handleExceptions`.
* Add all command categories you want to `commands.Command.Category`. Commands will be grouped by those categories in the help command.
  Categories take a `helpPage` attribute which determines on which page the category should be shown. This number must be unique for every
  category, start at `1` and increment for every category. The second argument is the name key which is the key used to get the name in the current language from the `.properties` files.
* Delete the `commands.ExampleCommandWithOwnClass` class and remove its usage in `commands.Commands` (at the top of `#registerCommands`). Also remove the help description of it in the `strings.properties` file (`help.example.*`). You can use that class as an example for commands that have their own class in case you prefer that style over the builder style.
* Adjust enabled intents in `main.BotMain` (in `setEnabledIntents(IntentSet.of(...))`).
* Create a `strings-{LANGUAGE-CODE}.properties` file for each language you want to support in the `resources` folder, e.g. `strings-de.properties` for German.
* If you want to perform more actions on certain events, add event handlers to `main.Events`.
* Add your commands to `commands.Commands`. You also need to add a short and a detailed description for every command, a detailed description for every subcommand.
  Do that by adding `help.COMMAND_NAME.short` and `help.COMMAND_NAME.detailed` to the properties files. The short version is used when showing the help
  for the category the command is in, the detailed version when showing the help for the command. Sub commands need to have a detailed description even
  if they have sub commands themselves.
  
