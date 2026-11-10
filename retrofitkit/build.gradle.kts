import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.android.library")
    `maven-publish`
}

description = "Android Retrofit toolkit: encrypted token storage, BigDecimal precision protection, and dynamic URLs"

android {
    namespace = "io.github.fallenleaves089.retrofitkit"
    compileSdk = 37

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.4.0")

    // Gson
    implementation("com.google.code.gson:gson:2.14.0")

    // AndroidX Security (EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0")

    // AndroidX Core
    implementation("androidx.core:core-ktx:1.19.0")

    // Local unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.4.0")
    testImplementation("org.mockito:mockito-core:5.10.0")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = project.group.toString()
                artifactId = "android-retrofit-kit"
                version = project.version.toString()

                pom {
                    name.set("android-retrofit-kit")
                    description.set("Android Retrofit toolkit with encrypted token storage, origin-safe auth injection, and BigDecimal precision protection")
                    url.set("https://github.com/Fallen-leaves089/android-retrofit-kit")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    scm {
                        url.set("https://github.com/Fallen-leaves089/android-retrofit-kit")
                        connection.set("scm:git:https://github.com/Fallen-leaves089/android-retrofit-kit.git")
                        developerConnection.set("scm:git:ssh://git@github.com/Fallen-leaves089/android-retrofit-kit.git")
                    }
                }
            }
        }
    }
}
