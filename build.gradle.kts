plugins {
	alias(libs.plugins.mod.publish)
	alias(libs.plugins.fabric.loom)
	`maven-publish`
	java
}

val modVersion = "2.0.0-beta.2"
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
				"fabric_api_dependency" to libs.versions.fabric.api.get()
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
            
            """.trimIndent()
		)
		type = STABLE
		modLoaders.add("fabric")

		displayName = "ServerReplay $modVersion for ${libs.versions.minecraft.get()}"
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
