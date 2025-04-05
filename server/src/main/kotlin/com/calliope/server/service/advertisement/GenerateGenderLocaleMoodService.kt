package com.calliope.server.service.advertisement

import com.calliope.server.model.form.AdGenerateForm
import com.calliope.server.service.ai.OpenaiService
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Consumer
import mu.KotlinLogging

@Service
class GenerateGenderLocaleMoodService(
    private val s3Client: S3Client,
    @param:Qualifier("mainExecutor") private val executor: Executor,
    private val generateCommonService: GenerateCommonService,
    private val openaiService: OpenaiService
) {
    private val logger = KotlinLogging.logger {}

    fun generateGender(
        description: String?,
        form: AdGenerateForm?
    ): CompletableFuture<Void> {
        if (!StringUtils.isEmpty(form!!.gender)) {
            return CompletableFuture.completedFuture(null)
        }
        val t1 = System.currentTimeMillis()
        val gender: String = openaiService.generateGender(description)?.gender!!
        form!!.gender = StringUtils.capitalize(gender)
        val t2 = System.currentTimeMillis()
        logger.info("gender generated in {} milliseconds, is {}", (t2 - t1), gender)
        return CompletableFuture.completedFuture(null)
    }

    fun generateLocale(
        description: String?,
        form: AdGenerateForm
    ): CompletableFuture<Void?> {
        if (!StringUtils.isEmpty(form.language)) {
            return CompletableFuture.completedFuture(null)
        }
        val t1 = System.currentTimeMillis()
        val locale: String = openaiService.generateLocale(description)?.locale!!
        form.language = locale
        val t2 = System.currentTimeMillis()
        logger.info("locale generated in {} milliseconds, is {}", (t2 - t1), locale)
        return CompletableFuture.completedFuture(null)
    }

    fun generateSummary(
        description: String?,
        titleSetter: Consumer<String?>
    ): CompletableFuture<Void?> {
        val t1 = System.currentTimeMillis()
        val summary: String = openaiService.generateSummary(description)?.summary!!
        titleSetter.accept(summary)
        val t2 = System.currentTimeMillis()
        logger.info("summary generated in {} milliseconds, is {}", (t2 - t1), summary)
        return CompletableFuture.completedFuture(null)
    }

    fun generateMood(
        toGenerate: Boolean?,
        description: String?,
        moodIn: String?,
        moodSetter: Consumer<String?>
    ): String? {
        if (java.lang.Boolean.FALSE == toGenerate) {
            return null
        }
        if (StringUtils.isNotEmpty(moodIn)) {
            return moodIn
        }

        val t1 = System.currentTimeMillis()
        val mood: String = openaiService.generateMood(description)?.mood!!
        moodSetter.accept(mood)
        val t2 = System.currentTimeMillis()
        logger.info("mood generated in {} milliseconds, is {}", (t2 - t1), mood)
        return mood
    }
}
