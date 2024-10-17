plugins {
	alias(libs.plugins.mod.publish)
	alias(libs.plugins.fabric.loom)
	`maven-publish`
	java
}

val modVersion = "2.1.0"
val releaseVersion = "${modVersion}+mc${libs.versions.minecraft.get()}"
version = releaseVersion
group = "me.senseiwells"

repositories {
	mavenCentral()
	maven("https://maven.parchmentmc.org/")
	maven("https://api.modrinth.com/maven")
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

	runtimeOnly(libs.luckperms)

	includeModImplementation(libs.permissions) {
		exclude(libs.fabric.api.get().group)
	}
}

loom {
	accessWidenerPath.set(file("src/main/resources/chunk-debug.accesswidener"))

	runs {
		getByName("server") {
			runDir = "run/server"
		}

		getByName("client") {
			runDir = "run/client"
		}
	}
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
			
			Added a client configuration file. Your settings will now be saved between uses.
			
			You can configure the position of the minimap; you first select the corner you want
			the minimap to render relative to by changing the `Minimap Corner` configuration,
			if you want to further fine-tune the position of the minimap you can enable `Render Minimap`
			which will allow you to click and drag the minimap around. 
			You can also scroll while hovering the minimap to resize it. 
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