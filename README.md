# ImpSave v0.24

ImpSave is a helpful companion for the [Imperialism PC game](https://en.wikipedia.org/wiki/Imperialism_%28video_game%29).

It has two primary functions:
  1. Automatically backing up saved games, allowing to restore to an earlier turn.
  2. Patching the Imperialism.exe binary (GOG version) to fix a few crash bugs.

To use it, simply place the ImpSave-0.24.jar inside your Imperialism folder and run it.
You'll need to have Java installed.

The program will bring up a window that lets you start Imperialism and apply patches
to it (if it's a known version of the binary, such as the GOG release).

While the program is running, it will automatically monitor the `Save` folder for
new and updated saved games and make back ups. A restore tab lets you restore any
saved game and annotate saves with additional comments.

## Save game backups

This program addresses a major limitation with Imperialism - only a single autosave slot.

By backing up autosaves (and other save slots) continuously, the program allows you to
restore the game to an arbitrary past turn. This way, you can reconsider your decisions 
from many turns ago, or recover from a deterministic crash bug in your most recent save
file.

The functionality is also supported and very useful in multiplayer, since all players
require an autosave at the same turn in order to restore a saved game. Without ImpSave,
this is very hard to manage, especially when one or more players crash or lose connection.

## Patching functionality

In addition to save game backups, ImpSave includes the functionality to patch the
Imperialism.exe binary with some bug fixes. This is an optional feature and is not
required for the backup functionality, but the patches address a number of crash bugs
that exist in the program. The patching functionality assumes a base version of the
GOG Imperialism binary.

## Origins

This program was conceived when the author would play Imperialism with a group of
friends in a LAN setting in the basement of a video store, circa 2013. Since
stability issues plagued Imperialism, he decided to write a handy utility to
automatically back up saved games, so that games could be restored reliably in a
multiplayer setting.

In addition, since the group encountered a few common crash bugs, the author also
reverse-engineered the binary and fixed a few of them, and built the functionality
to patch the Imperialism binary into the program.

It is now many years later when ImpSave is being shared publicly via GitHub, with
a bit of recent rework and improvements. ImpSave is being released in memory of
Aaron Kaufman, who was a regular in our play group and who tragically passed away
in 2021. RIP and may you live on in our memories.
