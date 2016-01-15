package net.sigmabeta.chipbox.model.objects

data class AudioConfig(val sampleRate: Int,
                       val bufferSizeBytes: Int,
                       val minimumLatency: Int)