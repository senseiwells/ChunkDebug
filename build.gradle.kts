plugins {
	alias(libs.plugins.mod.publish)
	alias(libs.plugins.fabric.loom)
	alias(libs.plugins.explosion)
	`maven-publish`
	java
}

val modVersion = "2.2.4"
val releaseVersion = "${modVersion}+${libs.versions.minecraft.get()}"
version = releaseVersion
group = "me.senseiwells"

repositories {
	mavenCentral()
	maven("https://maven.parchmentmc.org/")
	maven("https://api.modrinth.com/maven")
	maven("https://maven2.bai.lol")
	maven("https://maven.supersanta.me/snapshots")
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

	include(modImplementation(libs.keybinds.get())!!)

	// modCompileOnly(explosion.fabric(libs.c2me))

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
			
			Fixed compatability issue with Worldthreader
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
				artifactId = "chunk-debug"
			}
		}

		repositories {
			val mavenUrl = System.getenv("MAVEN_URL")
			if (mavenUrl != null) {
				maven {
					url = uri(mavenUrl)
					val mavenUsername = System.getenv("MAVEN_USERNAME")
					val mavenPassword = System.getenv("MAVEN_PASSWORD")
					if (mavenUsername != null && mavenPassword != null) {
						credentials {
							username = mavenUsername
							password = mavenPassword
						}
					}
				}
			}
		}
	}
}

private fun DependencyHandler.includeModImplementation(provider: Provider<*>, action: Action<ExternalModuleDependency>) {
	include(provider, action)
	modImplementation(provider, action)
}