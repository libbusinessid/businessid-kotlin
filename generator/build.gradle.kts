// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("businessid.kotlin-base")
    id("com.google.protobuf")
    application
}

description = "Reads businessid-rules.binpb, runs the 25 load checks, emits Kotlin."

// Protobuf lives here and only here. Nothing in this module reaches the
// published library, and the library never depends on this module.
dependencies {
    implementation(libs.protobuf.javalite)
    testImplementation(libs.protobuf.java)
    testImplementation(libs.kotest.property)
    testImplementation(libs.jazzer.junit)
}

protobuf {
    protoc {
        artifact = libs.protoc.get().toString()
    }
    generateProtoTasks {
        all().configureEach {
            // The descriptor set is what DescriptorTableTest compares the hand
            // written wire scan table against: protoc reads the very schema
            // rules.lock pins, so a schema change this repository did not follow
            // fails the build instead of passing silently.
            generateDescriptorSet = true
            descriptorSetOptions.includeImports = true
            descriptorSetOptions.path =
                layout.buildDirectory.file("descriptors/schemas.desc").get().asFile.absolutePath
            builtins {
                named("java") {
                    option("lite")
                }
            }
        }
    }
}

sourceSets {
    main {
        proto {
            // The schemas are the ones rules.lock pins, read from spec/ directly
            // so a drift between the checked digest and the compiled schema is
            // impossible.
            srcDir(rootProject.layout.projectDirectory.dir("build/proto"))
        }
    }
}

application {
    mainClass.set("io.libbusinessid.generator.MainKt")
}

val stageProtoSchemas = tasks.register<Sync>("stageProtoSchemas") {
    description = "Places spec/*.proto under the package path protoc expects."
    val spec = rootProject.layout.projectDirectory.dir("spec")
    into(rootProject.layout.buildDirectory.dir("proto"))
    from(spec.file("rules.proto")) { into("libbusinessid/ir/v1") }
    from(spec.file("conformance.proto")) { into("libbusinessid/conformance/v1") }
    from(spec.file("testee.proto")) { into("libbusinessid/testee/v1") }
}

tasks.named("generateProto") {
    dependsOn(stageProtoSchemas)
}

tasks.named("processProtoResources") {
    dependsOn(stageProtoSchemas)
}

// Detekt must not judge protoc output.
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    exclude("**/generated/**")
}

tasks.test {
    dependsOn(tasks.named("generateProto"))
    val descriptors = layout.buildDirectory.file("descriptors/schemas.desc")
    inputs.file(descriptors).withPropertyName("descriptorSet")
    systemProperty("businessid.descriptor.set", descriptors.get().asFile.absolutePath)
}
