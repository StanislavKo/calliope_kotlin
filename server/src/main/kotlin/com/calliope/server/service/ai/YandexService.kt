package com.calliope.server.service.ai

import com.calliope.server.model.domain.yandex.*
import com.calliope.server.model.view.KeyValueView
import com.calliope.server.model.view.PageDto
import com.calliope.server.utils.JWTYandexGenerator.getYandexJWT
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.*
import java.util.stream.Collectors
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity

@Service
class YandexService(
    private val mapper: ObjectMapper,
    private val restOperations: RestOperations,
    private val yandexKey: YandexKey,
    private val aspectRatios: List<AspectRatio?>
) {
    private val logger = KotlinLogging.logger {}

    @Value("\${yandex.key}") var yandexKeyStr: String? = null

    fun aspectRatios(): PageDto<KeyValueView> {
        val comparator =
            kotlin.Comparator.comparingDouble<AspectRatio?> { ar2: AspectRatio? -> (ar2?.width!!.toDouble() / ar2?.height!!.toDouble()) }
        val aspectRatiosOutput = aspectRatios
            .sortedWith(comparator)
            .map { it?.title }
            .map { aspectRatio: String? -> KeyValueView(aspectRatio, aspectRatio) }
            .toList()
        return PageDto(
            aspectRatiosOutput,
            1,
            aspectRatiosOutput.size
        )
    }

    fun generateBanner(
        adText: String,
        aspectRatioTitle: String?
    ): String? {
        val aspectRatio = aspectRatios.filter { ar: AspectRatio? -> ar?.title.equals(aspectRatioTitle) }.firstOrNull()
        val width: Int = aspectRatio?.width ?: 1
        val height: Int = aspectRatio?.height ?: 1

        val tokenJWT = authJWT()
        val tokenIAM = authIAMToken(tokenJWT)
        val operationId = generateBannerImpl(tokenIAM, adText, width, height)

        for (i in 0..49) {
            try {
                Thread.sleep(500)
                val image = downloadImage(tokenIAM, operationId)
                if (image != null) {
                    return image
                }
            } catch (e: HttpServerErrorException) {
                break
            } catch (e: Exception) {
                logger.error("Can't load image from Yandex", e)
            }
        }

        return null
    }

    fun downloadImage(
        token: String?,
        operationId: String?
    ): String? {
        try {
            val builder = UriComponentsBuilder.fromUri(URI.create("https://llm.api.cloud.yandex.net:443/operations/$operationId"))
            val headers = HttpHeaders()
            headers["Accept"] = "application/json"
            headers["Content-Type"] = "application/json; charset=UTF-8"
            headers["Authorization"] = "Bearer $token"
            val entity = HttpEntity<String>(
                headers
            )
            logger.info("Yandex entity = {}", entity.body)

            val response1 : ResponseEntity<YandexInternalImage?> = restOperations.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                object : ParameterizedTypeReference<YandexInternalImage?>() {
                }
            )
            logger.info("Yandex response.status = {}", response1.statusCode)

            //            log.info("Yandex response = {}", response1.getBody());
            return response1.body?.response?.image?.orEmpty()
        } catch (e: HttpServerErrorException) {
            logger.warn(
                "Failed to get Yandex image. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
            throw e
        } catch (e: HttpClientErrorException) {
            logger.warn(
                "Failed to get Yandex image. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
        } catch (e: Exception) {
            logger.error("Failed to get Yandex image with error : {}", e.message, e)
        }
        return null
    }

    fun generateBannerImpl(
        token: String?,
        adText: String,
        width: Int,
        height: Int
    ): String? {
        try {
            val builder =
                UriComponentsBuilder.fromUri(URI.create("https://llm.api.cloud.yandex.net:443/foundationModels/v1/imageGenerationAsync"))
            val headers = HttpHeaders()
            headers["Accept"] = "application/json"
            headers["Content-Type"] = "application/json; charset=UTF-8"
            headers["Authorization"] = "Bearer $token"
            val entity = HttpEntity(
                """{
  "modelUri": "art://b1gv5qg479q808e480dc/yandex-art/latest",
  "generationOptions": {
    "seed": ${(Math.random() * Int.MAX_VALUE).toInt()},
    "mimeType": "image/jpeg",
    "aspectRatio": {
      "widthRatio": "$width",
      "heightRatio": "$height"
    }
  },
  "messages": [
    {
      "weight": 1,
      "text": "${adText.replace("\"".toRegex(), "\\\"")}"
    }
  ]
}""",
                headers
            )
            logger.info("Yandex entity = {}", entity.body)

            val response1 = restOperations.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                entity,
                object : ParameterizedTypeReference<YandexInternalImageGenerationId?>() {
                }
            )
            logger.info("Yandex response.status = {}", response1.statusCode)
            logger.info("Yandex response = {}", response1.body)

            return response1.body?.id
        } catch (e: HttpClientErrorException) {
            logger.warn(
                "Failed to get Yandex image. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
        } catch (e: HttpServerErrorException) {
            logger.warn(
                "Failed to get Yandex image. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
        } catch (e: Exception) {
            logger.error("Failed to get Yandex image with error : {}", e.message, e)
        }
        return null
    }

    fun authJWT(
    ): String? {
        try {
            return getYandexJWT(yandexKey)
        } catch (e: Exception) {
            logger.error("Failed to get Yandex token with error : {}", e.message, e)
        }
        return null
    }

    fun authIAMToken(
        tokenJWT: String?
    ): String? {
        try {
            val builder = UriComponentsBuilder.fromUri(URI.create("https://iam.api.cloud.yandex.net/iam/v1/tokens"))
            val headers = HttpHeaders()
            headers["Content-Type"] = "application/json; charset=UTF-8"
            val entity = HttpEntity(
                "{\"jwt\": \"$tokenJWT\"}",
                headers
            )
            logger.info("Yandex entity = {}", entity.body)

            val response1 = restOperations.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                entity,
                object : ParameterizedTypeReference<YandexInternalIamToken?>() {
                }
            )
            logger.info("Yandex response.status = {}", response1.statusCode)
            logger.info("Yandex response = {}", response1.body)

            return response1.body?.iamToken
        } catch (e: Exception) {
            logger.error("Failed to get Yandex token with error : {}", e.message, e)
        }
        return null
    }

}
