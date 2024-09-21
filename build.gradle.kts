plugins {
	alias(libs.plugins.mod.publish)
	alias(libs.plugins.fabric.loom)
	`maven-publish`
	java
}

val modVersion = "2.0.1"
val releaseVersion = "${modVersion}+mc${libs.versions.minecraft.get()}"
version = releaseVersion
group = "me.senseiwells"

repositories {
	mavenCentral()
	maven("https://maven.parchmentmc.org/")
}

dependencies {
	minecraft(libs.minecraft)
	@Suppress("UnstableApiUsage")
	mappings(loom.layered {
		officialMojangMappings()
		parchment("org.parchmentmc.data:parchment-${libs.versions.parchment.get()}@zip")
	})
	modImplementation(libs.fabric.loader)
	modImplementation(libs.fabric.api)

	includeModImplementation(libs.permissions) {
		exclude(libs.fabric.api.get().group)
	}
}

loom {
	accessWidenerPath.set(file("src/main/resources/chunk-debug.accesswidener"))
}

java {
	withSourcesJar()
}

tasks {
	processResources {
		inputs.property("version", project.version)
		filesMatching("fabric.mod.json") {
			expand(mutableMapOf(
				"version" to project.version,
				"minecraft_dependency" to libs.versions.minecraft.get().replaceAfterLast('.', "x"),
				"fabric_loader_dependency" to libs.versions.fabric.loader.get(),
			))
		}
	}

	jar {
		from("LICENSE")
	}

	publishMods {
		file = remapJar.get().archiveFile
		changelog.set(
			"""
			## ChunkDebug $modVersion
			
			ChunkDebug has been completely re-written from scratch.
			
			### Changes
			- Complete network and rendering overhaul
			  - No longer requires EssentialClient on the client, install ChunkDebug on the client instead
              - Scheduled chunk unloading is now synchronized
			  - Added support for permission mods to control who can use ChunkDebug
			- New GUI
			  - Updated the GUI to be more user friendly
			  - You an now set a Chunk Retention to determine how long to keep unloaded chunks rendered for
			  - You can now render the minimap on top of the chunk debug screen to allow you to line things up easier
              - You can disable rendering of the chunk generation stages and/or ticket types
              - You can hide both the chunk debug settings and/or the chunk breakdown by hitting F1 or by clicking the toggle buttons in the bottom corners
              - You can now select regions of chunks by dragging right click
              - General QOL, the gui now doesn't behave weirdly when zooming and correctly centers when jumping to clusters, and the gui isn't grid based any more, so panning is smoother
            - Bug Fixes
			  - Chunk Stages are now synchronized correctly with the client
              - "Inaccessible" chunks are displayed correctly now
            """.trimIndent()
		)
		type = STABLE
		modLoaders.add("fabric")

		displayName = "ChunkDebug $modVersion for ${libs.versions.minecraft.get()}"
		version = releaseVersion

		modrinth {
			accessToken = providers.environmentVariable("MODRINTH_API_KEY")
			projectId = "zQxjhDPq"
			minecraftVersions.add(libs.versions.minecraft)

			requires {
				id = "P7dR8mSH"
			}
		}
	}

	publishing {
		publications {
			create<MavenPublication>("mavenJava") {
				from(project.components.getByName("java"))
			}
		}
	}
}

private fun DependencyHandler.includeModImplementation(provider: Provider<*>, action: Action<ExternalModuleDependency>) {
	this.include(provider, action)
	this.modImplementation(provider, action)
}