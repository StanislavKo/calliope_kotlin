package com.calliope.server.service.advertisement

import com.calliope.core.mysql.repositories.AdvertisementRepository
import com.calliope.core.mysql.repositories.UserRepository
import org.springframework.stereotype.Service

@Service
class AdvertisementService(
    private val userRepository: UserRepository,
    private val advertisementRepository: AdvertisementRepository
)
