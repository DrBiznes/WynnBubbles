# WynnBubbles
![wynnbubblesbanner](https://github.com/user-attachments/assets/c362b7f7-1966-4c6c-8028-b2dd66cec96e)

Show the people what you're trying to say. Chat floats above your head and changes colors for party, guild, and private chat. A fork of [TalkBubbles](https://modrinth.com/mod/talkbubbles) designed to work specifically with Wynncraft chat. Made for my modpack [World of Wynncraft](https://modrinth.com/modpack/world-of-wynncraft)

![WynnBubblesPreview-ezgif com-resize](https://github.com/user-attachments/assets/c99455f2-220e-46f9-8d02-0a8770c4d6ce)


## What's Up
- Shows chat bubbles above players' heads when they talk
- Different colors for party, guild, and private messages
- Configurable bubble size, position, and colors
- Removes unwanted unicode symbols from chat
- Stops system messages during raids and dungeons from displaying (as happens using TalkBubbles)
- Stacks several bubbles per player, newest at the bottom
- Works with player ghosts
- Sits above whatever is drawn over a player's name (Wynntils lines and badges, title mods)

## Installation
1. Make sure you have Fabric Loader and Fabric API installed
2. Download and install Cloth Config API
3. Download the latest version of WynnBubbles from the versions page
4. Place the downloaded .jar file in your Minecraft mods folder

## Future Features
- Custom icons above player head when casting spells that put messages in chat like War Scream, Heal, Windy Feet
- Custom bubble Border styles for different ranks
- Custom icons above player head when picking buffs in a raid
- Suggest features on the GitHub issues page!

## Configuration
You can configure WynnBubbles through the Mod Menu interface or config file:
1. Install [Mod Menu](https://modrinth.com/mod/modmenu) if you haven't already
2. In the game, go to Mods -> Find "WynnBubbles" -> Click "Configure"
3. Adjust settings like bubble scale, colors, and chat range
4. Toggle showing your own bubbles on/off

## Usage
- Chat normally in-game to see bubbles appear
- Bubbles will show different colors for:
  - Yellow = Party chat
  - Aqua = Guild chat
  - Orange = Private messages
- Bubbles automatically disappear after a configurable time
- Access the mod's configuration through Mod Menu to adjust bubble appearance

## Requirements
- Minecraft 1.21.11
- Fabric Loader 0.18.4 or higher
- Fabric API
- Cloth Config API
- Mod Menu (optional, for the config screen)
- Wynntils (optional, used to match chat to players and ghosts when installed)

## Building and testing
- `./gradlew build` compiles the mod and runs the unit tests
- `./gradlew runClientGameTest` starts a dev client, puts two fake players in a test world, sends Wynncraft-style chat lines and saves screenshots to `build/run/clientGameTest/screenshots`
- If Gradle fails on Windows with "Unable to establish loopback connection", set `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\some\short\path` and point `TEMP`/`TMP` at a short path too

## License
LGPL-3.0, see `LICENSE` and `NOTICE.md`. Versions before 2.0.0 were MIT.

## Go Ham
- I don't know nothing about java so if you wanna fork this and fix it up go ham
- Feel free to use this in your modpack
- If you know how to make the bubbles prettier please help

## Acknowledgments
- Thanks to Globox_Z for the original TalkBubbles mod
- Thanks to CerbonXD and BluSpring for Talk Balloons, which the stacked bubbles follow
- Thanks to everyone who I forced to help test the chat detection
- Thanks to IgbarVonSquid!!!

## Support and My Mods
Please report any bugs or feature suggestions on the Github Issues page, I'll be updating this frequently with community feedback and ideas! You can also [join my discord](https://discord.gg/jqFF64rXZZ) if you need direct support, or want to stay updated with all of my mods.
### Check out all my projects!
>   [World of Wynncraft Modpack](https://modrinth.com/modpack/world-of-wynncraft)

>   [WynnVista](https://modrinth.com/mod/wynnvista)

>   [Wynn Weapon Bigger](https://modrinth.com/mod/wynnweaponbigger)

>   [Nimble ReWynnded](https://modrinth.com/mod/nimble-rewynnded)

>   [Class Keybind Profiles](https://modrinth.com/mod/class-keybind-profiles)

>   [WynnBubbles](https://modrinth.com/mod/wynnbubbles)
