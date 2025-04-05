package com.calliope.server.model.domain.yandex

import com.fasterxml.jackson.annotation.JsonProperty

class YandexKey {
    var id: String? = null

    @JsonProperty("service_account_id")
    var serviceAccountId: String? = null

    @JsonProperty("created_at")
    var createdAt: String? = null

    @JsonProperty("key_algorithm")
    var keyAlgorithm: String? = null

    @JsonProperty("public_key")
    var publicKey: String? = null

    @JsonProperty("private_key")
    var privateKey: String? = null
}
