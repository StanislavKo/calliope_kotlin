package com.calliope.server.utils

import java.io.ByteArrayOutputStream
import java.io.IOException

object WavUtils {
    @JvmStatic
    @Throws(IOException::class)
    fun pcmToWav( //            short[] pcmdata,
        data: ByteArray,
        srate: Int,
        channel: Int,
        format: Int
    ): ByteArray {
        val os = ByteArrayOutputStream()

        val header = ByteArray(44)

        //        byte[] data = get16BitPcm(pcmdata);
        val totalDataLen = (data.size + 36).toLong()
        val bitrate = (srate * channel * format).toLong()

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xffL).toByte()
        header[5] = ((totalDataLen shr 8) and 0xffL).toByte()
        header[6] = ((totalDataLen shr 16) and 0xffL).toByte()
        header[7] = ((totalDataLen shr 24) and 0xffL).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = format.toByte()
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channel.toByte()
        header[23] = 0
        header[24] = (srate and 0xff).toByte()
        header[25] = ((srate shr 8) and 0xff).toByte()
        header[26] = ((srate shr 16) and 0xff).toByte()
        header[27] = ((srate shr 24) and 0xff).toByte()
        header[28] = ((bitrate / 8) and 0xffL).toByte()
        header[29] = (((bitrate / 8) shr 8) and 0xffL).toByte()
        header[30] = (((bitrate / 8) shr 16) and 0xffL).toByte()
        header[31] = (((bitrate / 8) shr 24) and 0xffL).toByte()
        header[32] = ((channel * format) / 8).toByte()
        header[33] = 0
        header[34] = 16
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (data.size and 0xff).toByte()
        header[41] = ((data.size shr 8) and 0xff).toByte()
        header[42] = ((data.size shr 16) and 0xff).toByte()
        header[43] = ((data.size shr 24) and 0xff).toByte()

        os.write(header, 0, 44)
        os.write(data)
        os.close()

        return os.toByteArray()
    }

    fun get16BitPcm(data: ShortArray): ByteArray {
        val resultData = ByteArray(2 * data.size)
        var iter = 0
        for (sample in data) {
            val maxSample = ((sample * Short.MAX_VALUE)).toInt().toShort()
            resultData[iter++] = (maxSample.toInt() and 0x00ff).toByte()
            resultData[iter++] = ((maxSample.toInt() and 0xff00) ushr 8).toByte()
        }
        return resultData
    }
}
