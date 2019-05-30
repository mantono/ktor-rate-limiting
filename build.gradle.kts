object Version {
	const val KOTLIN = "1.3.31"
	const val KTOR = "1.1.3"
	const val JVM = "1.8"
	const val COROUTINES = "1.2.1"
	const val JUNIT = "5.3.2"
	const val JACKSON = "2.9.8"
}

plugins {
	id("io.gitlab.arturbosch.detekt") version "1.0.0.RC6-2"
	id("org.jmailen.kotlinter") version "1.21.0"
	id("org.sonarqube") version "2.6.2"
	id("application") apply true
	id("org.jetbrains.kotlin.jvm") version "1.3.21" apply true
	id("java") apply true
	id("maven") apply true
	id("idea") apply true
}

application {
	mainClassName = "com.mantono.MainKt"
}

group = "com.mantono"
version = "0.1.0"
description = "Rate limitre feature for Ktor"

defaultTasks = listOf("test")

repositories {
	mavenLocal()
	jcenter()
	mavenCentral()
	maven(url = "https://dl.bintray.com/kotlin/ktor")
	maven(url = "https://jitpack.io")
}

dependencies {
	compile("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", Version.KOTLIN)
	compile("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", Version.COROUTINES)
	
	// Logging
	implementation("io.github.microutils", "kotlin-logging", "1.6.20")
	// Enable for applications
	// runtime("ch.qos.logback", "logback-classic", "1.2.3")

	// Ktor
	implementation("io.ktor", "ktor-server-core", Version.KTOR)
	implementation("io.ktor", "ktor-server-netty", Version.KTOR)
	implementation("io.ktor", "ktor-client-auth-basic", Version.KTOR)
	implementation("io.ktor", "ktor-auth", Version.KTOR)
	implementation("io.ktor", "ktor-jackson", Version.KTOR)

	// Jackson
	implementation("com.fasterxml.jackson.core", "jackson-core", Version.JACKSON)
	implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", Version.JACKSON)
	implementation("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", Version.JACKSON)

	// Other
	implementation("com.mantono", "pyttipanna", "1.0.0")
	implementation("com.auth0", "java-jwt", "3.7.0")

	// Junit
	testCompile("org.junit.jupiter", "junit-jupiter-api", Version.JUNIT)
	testRuntime("org.junit.jupiter", "junit-jupiter-engine", Version.JUNIT)
}

tasks {
	test {
		useJUnitPlatform()

		// Show test results.
		testLogging {
			events("passed", "skipped", "failed")
		}
		reports {
			junitXml.isEnabled = false
			html.isEnabled = true
		}
	}

	compileKotlin {
		sourceCompatibility = Version.JVM
		kotlinOptions {
			jvmTarget = Version.JVM
		}
	}


	wrapper {
		description = "Generates gradlew[.bat] scripts for faster execution"
		gradleVersion = "5.2.1"
	}
}
