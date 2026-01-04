pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net")
        maven("https://maven.architectury.dev/")
    }
}

rootProject.name = "chunky"

sequenceOf(
    "nbt",
    "common",
    "paper",
    "folia",
    "bukkit",
    // "fabric",  // Temporarily disabled due to missing fabric-permissions-api dependency
    "forge",
    "sponge"
).forEach {
    include("${rootProject.name}-$it")
    project(":${rootProject.name}-$it").projectDir = file(it)
}
