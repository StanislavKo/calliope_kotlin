package com.calliope.server.service.advertisement

import com.calliope.server.consts.Consts
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import mu.KotlinLogging

@Service
class GenerateCommonService(
    private val s3Client: S3Client
) {
    private val logger = KotlinLogging.logger {}
    @Value("\${advertisement.generation.fs-prefix:}")
    private val fsPrefix: String? = null

    @Value("\${advertisement.generation.docker-fs-prefix:}")
    private val dockerFsPrefix: String? = null

    fun getWavDuration(
        filename: String,
        fsPrefixCopy: String
    ): Float? {
        try {
            logger.info("getWavDuration >> before")

            val command = "docker run --rm " +
                    "  -v " + dockerFsPrefix + "/" + fsPrefixCopy + "://config " +
                    "  mikeoertli/ffprobe-docker:4.4.1.5-amd64 " +
                    "  -i /config/" + filename + " " +
                    "  -show_entries format=duration -v quiet -of csv=\"p=0\" "
            logger.info("getWavDuration >> {}", command)

            val builder = ProcessBuilder("cmd.exe", "/c", command)
            builder.redirectErrorStream(true)
            val p = builder.start()
            val r = BufferedReader(InputStreamReader(p.inputStream))
            var line: String?
            var durationLine: String? = null
            while (true) {
                line = r.readLine()
                if (line == null) {
                    break
                }
                durationLine = line
                println(line)
            }

            logger.info("getWavDuration >> EXIT")

            return durationLine!!.toFloat()
        } catch (e: Exception) {
            logger.error("Can't getWavDuration", e)
        }

        return null
    }

    fun downloadFile(
        uuid: String,
        extension: String,
        filepathOut: String
    ) {
        val data = s3Client
            .getObjectAsBytes(GetObjectRequest.builder().bucket(Consts.BUCKET).key(getS3Key(uuid, extension)).build())
            .asByteArray()
        try {
            FileUtils.writeByteArrayToFile(File(filepathOut), data)
        } catch (e: IOException) {
            logger.error("Can't download file", e)
        }
    }

    fun getS3Key(uuid: String, extension: String): String {
//        return uuid.substring(0, 2) + "/" + uuid.substring(2, 4) + "/" + uuid + "." + extension;
        return "$uuid.$extension"
    }
}
