/*
   Copyright 2017-2020 Charles Korn.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import java.io.FileInputStream
import java.nio.file.Files
import java.io.ByteArrayInputStream


buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("com.google.cloud:google-cloud-storage:1.113.1")
    }
}

val updateInfoFile = buildDir.resolve("updateInfo").resolve("latest.json")

val generateUpdateInfoFile = tasks.register("generateUpdateInfoFile") {
    description = "Generates update information file."
    group = "Distribution"

    inputs.property("version", { project.version.toString() })
    outputs.file(updateInfoFile)

    doLast {
        val version = project.version.toString()
        val content = """
            {
              "url": "https://github.com/batect/batect/releases/tag/$version",
              "version": "$version",
              "files": [
                {
                  "name": "batect",
                  "url": "https://github.com/batect/batect/releases/download/$version/batect"
                },
                {
                  "name": "batect.cmd",
                  "url": "https://github.com/batect/batect/releases/download/$version/batect.cmd"
                }
              ]
            }
        """
            .replace(" ", "")
            .replace("\n", "")

        Files.write(updateInfoFile.toPath(), content.toByteArray(Charsets.UTF_8))
    }
}

tasks.register("uploadUpdateInfoFile") {
    description = "Uploads update information file to publicly-accessible location."
    group = "Distribution"

    dependsOn(generateUpdateInfoFile)
    inputs.file(updateInfoFile)
    outputs.upToDateWhen { false }

    doLast {
        val environmentVariableName = "GCP_SERVICE_ACCOUNT_KEY"
        val serviceAccountKey = System.getenv(environmentVariableName)

        if (serviceAccountKey == null) {
            throw RuntimeException("'$environmentVariableName' environment variable not set.")
        }

        val credentials = GoogleCredentials
            .fromStream(ByteArrayInputStream(serviceAccountKey.toByteArray(Charsets.UTF_8)))
            .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))

        val storage = StorageOptions.newBuilder()
            .setProjectId("batect-updates-prod")
            .setCredentials(credentials)
            .build()
            .getService()

        val blobId = BlobId.of("batect-updates-prod-public", "v1/latest.json")
        val blobInfo = BlobInfo.newBuilder(blobId)
            .setCacheControl("public, max-age=300")
            .setContentType("application/json")
            .build()

        storage.create(blobInfo, Files.readAllBytes(updateInfoFile.toPath()))
    }
}