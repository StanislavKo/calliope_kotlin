package com.calliope.server.schedule

import com.calliope.server.model.domain.azure.AzureInternalVoice
import com.calliope.server.model.domain.google.GoogleInternalVoice
import com.calliope.server.service.ai.AwsService
import com.calliope.server.service.ai.AzureService
import com.calliope.server.service.ai.GoogleService
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.polly.model.Voice
import mu.KotlinLogging

@Service
@EnableScheduling
class VoicesRefresher(
    val azureService: AzureService,
    val googleService: GoogleService,
    val awsService: AwsService,
    azureVoices: MutableList<AzureInternalVoice>,
    val googleVoices: MutableList<GoogleInternalVoice>,
    val awsVoices: MutableList<Voice>
) {
    private val logger = KotlinLogging.logger {}
    val azureVoices: List<AzureInternalVoice> = azureVoices

    @Scheduled(cron = "10 3 * * * ?")
    @Throws(Exception::class)
    fun refreshGoogleVoices() {
        logger.info("refreshGoogleVoices begin")

        val newGoogleVoices = googleService.initVoices()

        googleVoices.clear()
        googleVoices.addAll(newGoogleVoices!!)

        logger.info("refreshGoogleVoices end {}", googleVoices.size)
    }

    @Scheduled(cron = "10 4 * * * ?")
    @Throws(Exception::class)
    fun refreshAwsVoices() {
        logger.info("refreshAwsVoices begin")

        val newAwsVoices = awsService.initVoices()

        awsVoices.clear()
        awsVoices.addAll(newAwsVoices)

        logger.info("refreshAwsVoices end {}", awsVoices.size)
    }
}
