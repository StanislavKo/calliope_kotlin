package com.calliope.server.model.form

class OperationGeneratePriceForm {
    constructor(seconds: Int?, music: Boolean?, banner: Boolean?, copies: Int?) {
        this.seconds = seconds
        this.music = music
        this.banner = banner
        this.copies = copies
    }

    var seconds: Int? = null
    var music: Boolean? = null
    var banner: Boolean? = null
    var copies: Int? = null
}
