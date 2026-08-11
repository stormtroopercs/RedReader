@file:Suppress("UnstableApiUsage")

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.kotlin.parcelize)
	alias(libs.plugins.compose.compiler)
	alias(libs.plugins.hilt.android)
	alias(libs.plugins.ksp)
	alias(libs.plugins.spotless)
	pmd
	checkstyle

	alias(libs.plugins.kotlin.jvm) apply false
}

android {
	compileSdk = libs.versions.sdk.compile.get().toInt()
	ndkVersion = libs.versions.ndk.get()
	namespace = "org.quantumbadger.redreader"

	defaultConfig {
		applicationId = "org.quantumbadger.redreader"
		minSdk = libs.versions.sdk.min.get().toInt()
		targetSdk = libs.versions.sdk.target.get().toInt()
		versionCode = 118
		versionName = "1.26"

		vectorDrawables.generatedDensities("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	androidResources {
		additionalParameters.add("--no-version-vectors")
	}

	buildTypes {
		getByName("release") {
			isMinifyEnabled = true
			isShrinkResources = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}

	compileOptions {
		encoding = "UTF-8"
		isCoreLibraryDesugaringEnabled = true
		JavaVersion.toVersion(libs.versions.java.get()).let {
			sourceCompatibility = it
			targetCompatibility = it
		}
	}

	lint {
		checkReleaseBuilds = false
		abortOnError = true
		warningsAsErrors = true

		error.add("DefaultLocale")

		baseline = file("config/lint/lint-baseline.xml")
		lintConfig = file("config/lint/lint.xml")
	}

	packaging {
		resources.excludes.add("META-INF/*")
	}

	testOptions {
		animationsDisabled = true
		unitTests {
			isIncludeAndroidResources = true
		}
	}

	buildFeatures {
		buildConfig = true
		compose = true
	}
    buildToolsVersion = "36.0.0"
}

dependencies {
	coreLibraryDesugaring(libs.jdk.desugar)

	implementation(libs.kotlinx.serialization.json)
	implementation(libs.kotlinx.serialization.json.okio)
	implementation(libs.kotlin.reflect)

	implementation(libs.hilt.android)
	ksp(libs.hilt.compiler)
	implementation(libs.androidx.hilt.navigation.compose)
	implementation(libs.androidx.hilt.work)


	implementation(libs.work.runtime.ktx)
	implementation(libs.work.gcm)

	implementation(libs.androidx.annotation)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.androidx.core)
	implementation(libs.androidx.fragment)
	implementation(libs.androidx.preference)
	implementation(libs.androidx.recyclerview)
	implementation(libs.androidx.swiperefreshlayout)
	implementation(libs.androidx.window)

	implementation(libs.google.flexbox)
	implementation(libs.google.material)

	implementation(libs.jackson.core)
	implementation(libs.commons.lang)
	implementation(libs.commons.text)

	implementation(libs.okhttp)
	implementation(libs.netcipher.webkit)
	implementation(libs.media3.exoplayer)
	implementation(libs.media3.ui)
	implementation(libs.zstd) {
		artifact {
			type = "aar"
		}
	}

	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.compose.material3)
	implementation(libs.androidx.compose.runtime)
	implementation(libs.androidx.compose.ui)
	implementation(libs.androidx.compose.ui.graphics)
	implementation(libs.androidx.compose.ui.tooling)
	implementation(libs.androidx.compose.constraintlayout)
	implementation(libs.androidx.navigation.compose)

	testImplementation(libs.junit)
	testImplementation(libs.robolectric)

	androidTestImplementation(libs.androidx.test.core)
	androidTestImplementation(libs.androidx.test.espresso.core)
	androidTestImplementation(libs.androidx.test.espresso.contrib)
	androidTestImplementation(libs.androidx.test.rules)
	androidTestImplementation(libs.androidx.test.junit)
	androidTestImplementation("androidx.compose.ui:ui-test-junit4")
	androidTestImplementation(platform(libs.androidx.compose.bom))
}

pmd {
	toolVersion = libs.versions.pmd.get()
}

tasks.register("pmd", Pmd::class) {
	dependsOn.add("assembleDebug")
	ruleSetFiles = files("${project.rootDir}/config/pmd/rules.xml")
	ruleSets = emptyList()
	source("src/main/java/org/quantumbadger")
	include("**/*.java")
	isConsoleOutput = true
}

spotless {
	lineEndings = com.diffplug.spotless.LineEnding.UNIX

	kotlin {
		target("**/*.kt")
		targetExclude("**/build/**/*.kt")
		targetExclude("**/generated/**/*.kt")
		ktlint("1.6.0").editorConfigOverride(mapOf(
			"ktlint_standard_property-naming" to "disabled",
			"ktlint_standard_max-line-length" to "disabled",
			"ktlint_standard_function-naming" to "disabled",
			"ktlint_standard_comment-wrapping" to "disabled"
		))
		trimTrailingWhitespace()
		endWithNewline()
	}

	kotlinGradle {
		target("*.kts")
		targetExclude("**/build/**/*.kts")
		ktlint("1.6.0")
	}

	java {
		target("**/*.java")
		targetExclude("**/build/**/*.java")
		targetExclude("**/generated/**/*.java")
		googleJavaFormat()
		licenseHeaderFile("${project.rootDir}/config/checkstyle/copyright.java.txt", "/*")
		trimTrailingWhitespace()
		endWithNewline()
	}
}

tasks.register("Checkstyle", Checkstyle::class) {
	source("src/main/java/org/quantumbadger")
	ignoreFailures = false
	isShowViolations = true
	include("**/*.java", "**/*.kt")
	classpath = files()
	maxWarnings = 0
	configFile = rootProject.file("${project.rootDir}/config/checkstyle/checkstyle.xml")
}

tasks.withType<JavaCompile> {
	options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}
