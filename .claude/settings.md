# Chunky Au Naturel - Project Context

## Overview

This is a fork of Chunky (a Minecraft chunk pregenerator) with fixes for Forge 1.20.1. The focus is on stability improvements, particularly fixing deadlock and threading issues that occur when running alongside c2me or other multi-threaded chunk mods.

## Project Structure

- `common/` - Platform-agnostic core code (GenerationTask, commands, etc.)
- `forge/` - Forge-specific implementation (1.20/1.20.1)
- `fabric/` - Fabric implementation (disabled in this fork)
- `bukkit/` - Bukkit/Spigot implementation
- `paper/` - Paper-specific extensions
- `folia/` - Folia support
- `sponge/` - Sponge implementation
- `nbt/` - NBT parsing utilities

## Key Files

- `common/src/main/java/org/popcraft/chunky/GenerationTask.java` - Main chunk generation loop
- `forge/src/main/java/org/popcraft/chunky/platform/ForgeWorld.java` - Forge chunk loading
- `forge/src/main/java/org/popcraft/chunky/ChunkyForge.java` - Forge mod entry point
- `common/src/main/java/org/popcraft/chunky/util/TaskScheduler.java` - Thread pool management

## Building

```bash
# Build Forge JAR only
./gradlew :chunky-forge:shadowJar :chunky-forge:remapJar

# Output: forge/build/libs/Chunky-*.jar
```

Note: Fabric module is disabled in `settings.gradle.kts` due to missing dependencies.

## Key Changes From Upstream

1. **Threading fixes** - Callbacks run on dedicated executor, not server thread
2. **Deadlock prevention** - Removed problematic synchronized blocks
3. **Timeout handling** - 60-second timeout on pending chunks
4. **Boss bar thread safety** - All UI operations on server thread

## Testing

When testing changes:
1. Use with c2me mod to verify compatibility
2. Test long generation sessions (1000+ chunks)
3. Verify progress bar appears/disappears correctly
4. Test pause/resume functionality
5. Test server restart during generation

## License

GPL-3.0 - Must maintain source availability and license notices.
