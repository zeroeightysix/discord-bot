# testing bot

A small discord bot written in kotlin for fun.

Please don't expect a variety of features or support - for now, this project is just me writing code for the sake of it.

If you wish to run this yourself, you will need:

* MariaDB running locally on port 3306
* A discord bot application

Provide the following environment variables to run the bot: `DISCORD_TOKEN`, `GUILD_ID`, `DB_USER`, `DB_PASSWORD` and `DB_NAME`.
Guild ID is the ID of the discord server you wish to push the bots commands to.

## Features

The focus of this bot is to have a neat internal API for writing readable commands.

Command metadata is defined in `resources/commands.xml`. Each entry is loaded at runtime - class names must match those in the XML file.
Currently implemented is:

* A 'conversation' API, allowing for user input after the initial command
* Caching
* Image editing commands
* Economy commands
* Probably more by the time I update this README