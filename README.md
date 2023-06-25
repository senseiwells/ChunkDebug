# ChunkDebug

## What is ChunkDebug?

Chunk debug is a tool that was originaly made for [CarpetClient 1.12.2](https://github.com/X-com/CarpetClient). I have ported it to modern versions of Minecraft, ChunkDebug supports all versions past Minecraft 1.16.5.

This tool allows you to have a live view of currently loaded chunks in your worlds with a GUI and minimap, it provides you with information such as why the chunk is currently loaded and what type of loaded chunk it is.

![2021-12-28_00 16 50](https://user-images.githubusercontent.com/66843746/147515139-4c2d4bbb-d8e4-416c-9933-eecc2a957a91.png)
![2021-12-28_00 17 04](https://user-images.githubusercontent.com/66843746/147515143-7b08b16f-e5de-412e-a31f-b2c7f60af582.png)

## How to use ChunkDebug

The [ChunkDebug](https://github.com/senseiwells/ChunkDebug) mod is for server side, the mod sends chunk data to the client where it is displayed by [EssentialClient](https://github.com/senseiwells/EssentialClient). You must have EssentialClient installed on the Client and ChunkDebug installed on the server (or on the client in the case of singleplayer), they must be compatible versions so just make sure you have the most up to date versions of the mods!

Installing ChunkDebug on the server is easy, just install ChunkDebug from the [releases page](https://github.com/senseiwells/ChunkDebug/releases/tag/v1.0.1), once you have downloaded it put it in your server's mod folder, or client mod folder. To be able to view the ChunkDebug map you must install EssentialClient from it's [releases page](https://github.com/senseiwells/EssentialClient/releases), after installing this on your client you can join a server with ChunkDebug installed, press ESC, then EssentialClient menu, then Chunk Debug Map. This will open the ChunkDebug map ready for you to use!
