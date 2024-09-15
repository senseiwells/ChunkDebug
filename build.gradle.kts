plugins {
	alias(libs.plugins.mod.publish)
	alias(libs.plugins.fabric.loom)
	`maven-publish`
	java
}

val modVersion = "2.0.1+beta.2"
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
			Updated to 24w37a
			
			- Added new Enderpearl tickets
			- Fixed a bug where you couldn't open chunk debug in singleplayer without cheats
            """.trimIndent()
		)
		type = BETA
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