package blog.bocharoviliyav.swaggerhub

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.create
import org.slf4j.Logger
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime

data class SwaggerHubRequest(
    val api: String,
    val owner: String,
    val version: String,
    val swagger: String,
    val isPrivate: Boolean = false
)

abstract class SwaggerhubPluginExtension {
    abstract val owner: Property<String>
    abstract val api: Property<String>
    abstract val version: Property<String>
    abstract val token: Property<String>
    abstract val inputFile: Property<String>
    abstract val skipOnError: Property<Boolean>
    abstract val format: Property<String>
    abstract val private: Property<Boolean>
    abstract val host: Property<String>
    abstract val port: Property<Int>
    abstract val protocol: Property<String>
}


class SwaggerhubPlugin : Plugin<Project> {

    var fName = "tmp/swaggerhub/uploadPlugin"

    fun generateOutput(fName: String, p: Project) {
        val time = LocalDateTime.now()
        val out = p.layout.buildDirectory.file(fName).get().asFile
        out.writeText("$time", Charset.forName("UTF-8"))
    }

    override fun apply(project: Project) {
        with(project) {
            val extension = extensions.create<SwaggerhubPluginExtension>("swaggerhubUpload")

            tasks.register("swaggerhubUpload", SwaggerhubUploadTask::class.java) {
                //dependsOn("test")
                //inputs.files(layout.files(tasks.getByName("test").outputs))

                outputs.file(layout.buildDirectory.file(fName))

                api = extension.api.getOrElse("")
                inputFile = extension.inputFile.getOrElse("")
                token = extension.token.getOrElse("")
                owner = extension.owner.getOrElse("")
                version = extension.version.getOrElse(project.version.toString())
                skipOnError = extension.skipOnError.getOrElse(true)

                format = extension.format.getOrElse("json")
                port = extension.port.getOrElse(443)
                private = extension.private.getOrElse(true)
                host = extension.host.getOrElse("api.swaggerhub.com")
                protocol = extension.protocol.getOrElse("https")

                doLast {
                    generateOutput(fName, project)
                }
            }

        }
    }
}

open class SwaggerhubUploadTask : DefaultTask() {
    @get:Input
    lateinit var owner: String

    @get:Input
    lateinit var api: String

    @get:Input
    lateinit var version: String

    @get:Input
    lateinit var token: String

    @get:InputFile
    lateinit var inputFile: String

    @get:Input
    var skipOnError: Boolean = true

    @get:Input
    var format: String = "json"

    @get:Input
    var private: Boolean = true

    @get:Input
    var host: String = "api.swaggerhub.com"

    @get:Input
    var protocol: String = "https"

    @get:Input
    var port: Int = 443

    private var swaggerHubClient: SwaggerHubClient? = null

    @TaskAction
    @Throws(GradleException::class)
    fun uploadDefinition() {
        swaggerHubClient = SwaggerHubClient(host, port, protocol, token)
        LOGGER.info("Uploading to $host api: $api, owner: $owner, version: $version, inputFile: $inputFile, format: $inputFile, private: $inputFile")
        try {

            val content = String(Files.readAllBytes(Paths.get(inputFile)), Charset.forName("UTF-8"))
            val swaggerHubRequest = SwaggerHubRequest(api, owner, version, content, private)

            swaggerHubClient!!.saveDefinition(swaggerHubRequest, skipOnError)
        } catch (e: IOException) {
            val message = e.message ?: "IO exception was happen"
            if (!skipOnError) {
                throw GradleException(message, e)
            } else {
                LOGGER.info(message)
            }
        } catch (e: GradleException) {
            val message = e.message ?: "Gradle exception was happen"
            if (!skipOnError) {
                throw GradleException(message, e)
            } else {
                LOGGER.info(message)
            }
        }
    }

    companion object {
        private val LOGGER: Logger = Logging.getLogger("root")
    }
}

class SwaggerHubClient(
    private val host: String,
    private val port: Int,
    private val protocol: String,
    private val token: String
) {

    @Throws(GradleException::class)
    fun saveDefinition(swaggerHubRequest: SwaggerHubRequest, skipOnError: Boolean) {
        val client = OkHttpClient()
        val httpUrl: HttpUrl = getUploadUrl(swaggerHubRequest)
        val mediaType: MediaType? = "application/json".toMediaTypeOrNull()
        val httpRequest: Request = buildPostRequest(httpUrl, mediaType!!, swaggerHubRequest.swagger)
        try {
            LOGGER.info("Start uploading OpenApi definition")
            val response: Response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful && !skipOnError) {
                throw GradleException("Failed to upload definition: ${response.body?.string()}")
            }
        } catch (e: IOException) {
            throw GradleException("Failed to upload definition", e)
        }
        return
    }

    private fun buildPostRequest(httpUrl: HttpUrl, mediaType: MediaType, content: String): Request {
        return Request.Builder()
            .url(httpUrl)
            .addHeader("Content-Type", mediaType.toString())
            .addHeader("Authorization", token)
            .addHeader("User-Agent", "swaggerhub-gradle-plugin")
            .post(content.toRequestBody(mediaType))
            .build()
    }

    private fun getUploadUrl(swaggerHubRequest: SwaggerHubRequest): HttpUrl {
        return getBaseUrl(swaggerHubRequest.owner, swaggerHubRequest.api)
            .addEncodedQueryParameter("version", swaggerHubRequest.version)
            .addEncodedQueryParameter("isPrivate", swaggerHubRequest.isPrivate.toString())
            .build()
    }

    private fun getBaseUrl(owner: String, api: String): HttpUrl.Builder {
        return HttpUrl.Builder()
            .scheme(protocol)
            .host(host)
            .port(port)
            .addPathSegment("apis")
            .addEncodedPathSegment(owner)
            .addEncodedPathSegment(api)
    }


    companion object {
        private val LOGGER: Logger = Logging.getLogger("root")
    }

}
