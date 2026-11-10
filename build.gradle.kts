plugins {
    id("com.android.library") version "9.3.1" apply false
}

val libraryGroup = providers.gradleProperty("group").orElse("io.github.fallenleaves089")
val libraryVersion = providers.gradleProperty("version").orElse("1.1.3")

group = libraryGroup.get()
version = libraryVersion.get()

tasks.register("publishToMavenLocal") {
    dependsOn(":retrofitkit:publishToMavenLocal")
}
