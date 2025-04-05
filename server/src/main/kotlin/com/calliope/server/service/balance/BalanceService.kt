package com.calliope.server.service.balance

import com.calliope.core.mysql.model.User
import com.calliope.core.mysql.model.UserBalance
import com.calliope.core.mysql.model.UserBalanceUsage
import com.calliope.core.mysql.repositories.UserBalanceRepository
import com.calliope.core.mysql.repositories.UserBalanceUsageRepository
import com.calliope.core.mysql.repositories.UserRepository
import com.calliope.server.exception.CustomError
import com.calliope.server.exception.CustomErrorCode
import com.calliope.server.exception.CustomErrorException
import com.calliope.server.model.domain.balance.SpendType
import com.calliope.server.model.form.OperationGeneratePriceForm
import com.calliope.server.model.view.OperationPriceView
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.stream.Collectors
import mu.KotlinLogging

@Service
class BalanceService(
    private val mapper: ObjectMapper,
    private val userRepository: UserRepository,
    private val userBalanceRepository: UserBalanceRepository,
    private val userBalanceUsageRepository: UserBalanceUsageRepository
) {
    private val logger = KotlinLogging.logger {}

    @Transactional(isolation = Isolation.SERIALIZABLE)
    fun spend(
        user: User?,
        type: SpendType,
        linkeId: String?,
        typeDetails: String?,
        price: Double
    ) {
        var userBalanceUsage = UserBalanceUsage()
        userBalanceUsage.user = user
        userBalanceUsage.type = type.name
        userBalanceUsage.linkedId = linkeId
        userBalanceUsage.typeDetails = typeDetails
        userBalanceUsage.price = price
        userBalanceUsage = userBalanceUsageRepository.save(userBalanceUsage)

        val userBalance = userBalanceRepository.findByUser(user)
        userBalance!!.budget = userBalance!!.budget!! - price
        userBalanceRepository.save<UserBalance>(userBalance)
    }

    fun estimate(
        user: User?,
        operation: String?,
        operationDetails: String?
    ): OperationPriceView {
        when (operation) {
            "generate" -> {
                try {
                    val operationGeneratePriceForm = mapper.readValue(
                        operationDetails,
                        OperationGeneratePriceForm::class.java
                    )

                    val description: MutableList<String> = mutableListOf()

                    val priceBasic1 = 0.03
                    val priceBasic: Double = priceBasic1 * operationGeneratePriceForm.copies!!
                    if (operationGeneratePriceForm.copies!! > 1) {
                        description.add("Basic price: $0.03 * " + operationGeneratePriceForm.copies!! + " copies = $" + priceBasic.toString())
                    } else {
                        description.add("Basic price: $0.03")
                    }

                    var priceMusic = 0.0
                    if (operationGeneratePriceForm.music!!) {
                        val priceMusic1: Double = operationGeneratePriceForm.seconds!! / 10.0 * 0.02
                        priceMusic = priceMusic1 * operationGeneratePriceForm.copies!!
                        if (operationGeneratePriceForm.copies!! > 1) {
                            description.add("Background music price: $" + priceMusic1.toString() + " * " + operationGeneratePriceForm.copies!! + " copies = $" + priceMusic.toString())
                        } else {
                            description.add("Background music price: $$priceMusic")
                        }
                    }

                    var priceBanner = 0.0
                    if (operationGeneratePriceForm.banner!!) {
                        val priceBanner1 = 0.02
                        priceBanner = priceBanner1 * operationGeneratePriceForm.copies!!
                        if (operationGeneratePriceForm.copies!! > 1) {
                            description.add("Banner price: $" + priceBanner1.toString() + " * " + operationGeneratePriceForm.copies!! + " copies = $" + priceBanner.toString())
                        } else {
                            description.add("Banner price: $$priceBanner")
                        }
                    }

                    val price = priceBasic + priceMusic + priceBanner
                    val priceBd = BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP)

                    return OperationPriceView(
                        operation,
                        price,
                        priceBd.toString(),
                        description.stream().collect(Collectors.joining("<br/>"))
                    )
                } catch (e: Exception) {
                    logger.error("Can't estimate generate operation price", e)
                    throw CustomErrorException(
                        CustomError("Can't estimate generate operation price"),
                        CustomErrorCode.SYSTEM
                    )
                }
                throw CustomErrorException(
                    CustomError("Unrecognized operation"),
                    CustomErrorCode.BAD_PARAMETER
                )
            }

            else -> throw CustomErrorException(
                CustomError("Unrecognized operation"),
                CustomErrorCode.BAD_PARAMETER
            )
        }
    }

    companion object {
        private val SDF_MESSAGE = SimpleDateFormat("EEE, d MMM yyyy HH:mm")
    }
}
