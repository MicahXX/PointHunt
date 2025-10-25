# Point Hunt

[![Website](https://img.shields.io/badge/Website-micahcode.com-blue?style=for-the-badge)](https://micahcode.com)
<img src="https://img.shields.io/github/followers/MicahXX?style=for-the-badge" alt="Followers Badge"/>
<img src="https://img.shields.io/github/stars/MicahXX/OwnWebsite?style=for-the-badge" alt="Stars Badge"/>

A small, fun Minecraft Paper plugin (1.21.8) that turns mining and mob kills into a points-based minigame. Features a configurable timer, leaderboard, and automatic server shutdown when the game ends.

## Table of contents
- Features
- Requirements
- Installation
- Quick start
- Commands
- Configuration 
- Troubleshooting
- Contributing
- Links
- License & Author

## Features
- Earn points by mining blocks and killing mobs
- In-game leaderboard to show top players
- Configurable match timer
- Automatic server shutdown when the timer reaches zero

## Requirements
- Java (See troubleshooting if you get "no Java" errors)
- PaperMC server: Paper 1.21.8
  - Download: https://papermc.io/downloads/paper

## Installation
1. Download Paper 1.21.8 from the PaperMC website.
2. Download the Point Hunt plugin .jar file (from Releases, Modrinth, or your chosen distribution).
3. Place the Paper jar and a startup script in the same folder (example startup script generation: https://docs.papermc.io/misc/tools/start-script-gen).
   - Example Java line (adjust memory and jar name as needed):
     java -Xms2G -Xmx2G -jar paper-1.21.8-60.jar nogui
   - If you prefer to keep your existing startup script, simply replace the jar filename with the Paper jar name.
4. Run the generated .bat/.sh file once to generate the server folders and accept the EULA:
   - Accept EULA: edit eula.txt and set `eula=true`
5. Stop the server (type stop in the console), then:
   - Create a `plugins/` folder if it doesn't exist.
   - Put the Point Hunt `.jar` into the `plugins/` folder.
6. Start the server again.
7. Join locally: Multiplayer → Direct Connection → `localhost`
8. Give yourself operator permissions (in the server console):
   - `op YourMinecraftName`

## Quick start / Usage
- View available commands:
  - `/hunt`
- Set the game timer and start a match:
  - `/hunt setTimer 1h`  (example: 1h, 30m, 10m)
- The game will track points while the timer runs, show leaderboard entries, and shut down the server when time runs out.

## Commands
- `/hunt` — Show help and available subcommands
- `/hunt setTimer <time>` — Start the game with the provided duration (examples: `1h`, `30m`, `10m`)
- (Add any additional plugin-specific commands here if available)

## Configuration (overview)
- On first run, a configuration file will be created in `plugins/PointHunt/`.
- Typical settings you may find or want to change:
  - point values for specific blocks and mobs
  - timer defaults
  - automatic shutdown enable/disable
  - leaderboard size
- Edit the config and restart the server to apply changes.

## Troubleshooting
- "No Java" or "Java not found" errors:
  - Follow PaperMC's Java installation guide: https://docs.papermc.io/misc/java-install/
- Server memory/lag:
  - Adjust `-Xms` and `-Xmx` in your startup script based on available RAM.
- Plugin doesn't load:
  - Check `plugins/PointHunt/` for config errors in the console.
  - Ensure you are using Paper 1.21.8 and a compatible Java version.

## Contributing
Contributions, bug reports and feature requests are welcome. If you want to help:
1. Open an issue describing the bug or feature.
2. Fork, make changes, and open a pull request with a clear description of your change.

## Links
- Modrinth: https://modrinth.com/plugin/pointhunt
- Portfolio: https://micahcode.com

## License & Author
Made by MicahCode (MicahXX)  
If you use or modify this plugin, please keep attribution to the original author.
