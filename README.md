# Chunky Au Naturel

A fork of [Chunky](https://github.com/pop4959/Chunky) with fixes for Forge 1.20.1, specifically addressing freezing issues and compatibility with c2me and other multi-threaded chunk mods.

## About This Fork

This is a modified version of Chunky focused on stability improvements for Forge 1.20/1.20.1. The primary goal is to fix critical bugs that cause chunk generation to freeze or stop during long sessions.

**Original Project:** [pop4959/Chunky](https://github.com/pop4959/Chunky)
**License:** GPL-3.0 (same as original)

## Changes From Upstream

### Bug Fixes

- **Fixed critical deadlock** - Removed synchronized keyword from update() method that caused monitor deadlock between Chunky thread and server thread
- **Fixed server thread congestion** - Added dedicated 4-thread callback executor for chunk completion handlers, moved callbacks off server thread
- **Fixed infinite hang on stuck chunks** - Added 60-second timeout when waiting for pending chunks; task now exits gracefully with warning instead of hanging forever
- **Fixed multiple progress bars bug** - Boss bar operations now run on server thread for thread safety, preventing race conditions in Minecraft's CustomBossEvents
- **Fixed orphaned progress bars** - Added cleanup of Chunky boss bars on server startup and shutdown

### Performance Improvements

- Limited thread pool to max 10 threads (was unbounded)
- Added bounded task queue with 100 capacity
- Moved ticket cleanup to server.execute() to avoid blocking completion chain

### Compatibility

- Improved compatibility with c2me and other multi-threaded chunk mods
- No custom JVM arguments required

## Getting Started

- [Installing](https://github.com/pop4959/Chunky/wiki/Installing)
- [Pre-generating Chunks](https://github.com/pop4959/Chunky/wiki/Pregeneration)
- [Command Reference](https://github.com/pop4959/Chunky/wiki/Commands)

## Building

```bash
./gradlew :chunky-forge:shadowJar :chunky-forge:remapJar
```

The built JAR will be in `forge/build/libs/`.

## Credits

- **Original Author:** [pop4959](https://github.com/pop4959) and [contributors](https://github.com/pop4959/Chunky/graphs/contributors)
- **Original Project:** [Chunky](https://github.com/pop4959/Chunky)

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

This is a fork of Chunky, which is also licensed under GPL-3.0. All original copyrights and attributions remain in effect.
