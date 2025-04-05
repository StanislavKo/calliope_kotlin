package com.calliope.server.configuration

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

@Configuration
class RestConfiguration {
    @Bean
    fun objectMapper(
        builder: Jackson2ObjectMapperBuilder
    ): ObjectMapper {
        val objectMapper = builder.createXmlMapper(false).build<ObjectMapper>()
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        objectMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        objectMapper.registerModule(JavaTimeModule())

        return objectMapper
    }

    @Bean
    fun mappingJackson2HttpMessageConverter(
        objectMapper: ObjectMapper
    ): MappingJackson2HttpMessageConverter {
        val jsonConverter = MappingJackson2HttpMessageConverter()
        jsonConverter.objectMapper = objectMapper
        jsonConverter.defaultCharset = Charset.forName("UTF-8")
        return jsonConverter
    }

    @Bean
    fun restTemplate(
        mapper: ObjectMapper
    ): RestOperations {
        val restTemplate = RestTemplate()
        restTemplate.messageConverters.add(0, StringHttpMessageConverter(StandardCharsets.UTF_8))
        val converter = restTemplate
            .messageConverters
            .stream()
            .filter { conv: HttpMessageConverter<*> -> conv.javaClass == MappingJackson2HttpMessageConverter::class.java }
            .findFirst().orElse(null)
        (converter as MappingJackson2HttpMessageConverter).objectMapper = mapper
        converter.defaultCharset = Charset.forName("UTF-8")
        return restTemplate
    }
}
