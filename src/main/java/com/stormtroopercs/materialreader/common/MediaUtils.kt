/*******************************************************************************
 * This file is part of MaterialReader.
 *
 * MaterialReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.stormtroopercs.materialreader.common

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.min

object MediaUtils {
    private const val TAG = "MediaUtils"

    fun muxFiles(
        outputFile: File,
        inputFiles: Array<File>,
        successCallback: Runnable,
        failureCallback: FunctionOneArgNoReturn<Exception?>
    ) {
        Thread(Runnable {
            class InputFile internal constructor(
                val file: File,
                val extractor: MediaExtractor,
                private val mTrackIds: MutableMap<Int, Int>
            ) : Closeable {
                fun getOutputTrackId(inputTrackId: Int): Int {
                    return mTrackIds.get(inputTrackId)!!
                }

                @Throws(IOException::class)
                override fun close() {
                    extractor.release()
                }
            }

            var muxer: MediaMuxer?=null

            val inputFilesToClose = ArrayList<InputFile>()

            Log.i(TAG, "muxFiles: " + outputFile)
            try {
                muxer = MediaMuxer(
                    outputFile.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )

                for (inputFile in inputFiles) {
                    val mediaExtractor = MediaExtractor()

                    val path = inputFile.getAbsolutePath()

                    mediaExtractor.setDataSource(path)

                    val trackIds = HashMap<Int, Int>()

                    for (inputTrackId in 0..<mediaExtractor.getTrackCount()) {
                        mediaExtractor.selectTrack(inputTrackId)

                        val format = mediaExtractor.getTrackFormat(inputTrackId)

                        val outputTrackId = muxer.addTrack(format)

                        trackIds.put(inputTrackId, outputTrackId)

                        Log.i(
                            TAG, ("Track "
                                    + outputTrackId
                                    + ": path '"
                                    + path
                                    + "' format "
                                    + format.toString())
                        )
                    }

                    mediaExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                    inputFilesToClose.add(
                        InputFile(
                            inputFile,
                            mediaExtractor,
                            trackIds
                        )
                    )
                }

                val inputFilesToRead = ArrayList<InputFile>(inputFilesToClose)

                Log.i(TAG, "Starting mux for " + outputFile)

                muxer.start()

                val sampleBuffer = ByteBuffer.allocateDirect(1024 * 1024) // 1MiB
                val bufferInfo = MediaCodec.BufferInfo()

                while (!inputFilesToRead.isEmpty()) {
                    var minTime = Long.MAX_VALUE

                    for (file in inputFilesToRead) {
                        val sampleTime = file.extractor.getSampleTime()
                        minTime = min(minTime, sampleTime)
                    }

                    run {
                        val iterator = inputFilesToRead.iterator()
                        while (iterator.hasNext()) {
                            val file = iterator.next()

                            val extractor = file.extractor

                            while (extractor.getSampleTime() == minTime) {
                                sampleBuffer.clear()

                                val readResult = extractor.readSampleData(sampleBuffer, 0)

                                if (readResult < 0) {
                                    iterator.remove()
                                    Log.i(
                                        MediaUtils.TAG, "No bytes to read from "
                                                + file.file.getAbsolutePath()
                                    )
                                    break
                                } else {
                                    val outputTrackId = file.getOutputTrackId(
                                        extractor.getSampleTrackIndex()
                                    )

                                    sampleBuffer.limit(
                                        readResult
                                    )
                                    sampleBuffer.position(0)

                                    var flags = 0

                                    if ((extractor.getSampleFlags()
                                                and MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                                    ) {
                                        flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                                    }

                                    if ((extractor.getSampleFlags()
                                                and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0
                                    ) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            flags = flags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
                                        }
                                    }

                                    bufferInfo.set(
                                        0,
                                        sampleBuffer.remaining(),
                                        extractor.getSampleTime(),
                                        flags
                                    )

                                    muxer.writeSampleData(
                                        outputTrackId,
                                        sampleBuffer,
                                        bufferInfo
                                    )

                                    if (!extractor.advance()) {
                                        iterator.remove()
                                        Log.i(
                                            MediaUtils.TAG,
                                            "Finished writing track " + outputTrackId
                                        )
                                        break
                                    }
                                }
                            }
                        }
                    }
                }

                Log.i(TAG, "Stopping muxer...")
                muxer.stop()

                Log.i(TAG, "Mux complete for " + outputFile)

                successCallback.run()
            } catch (e: Exception) {
                failureCallback.apply(e)
            } finally {
                if (muxer != null) {
                    try {
                        Log.i(TAG, "Releasing muxer...")
                        muxer.release()
                    } catch (e: Exception) {
                        Log.e(TAG, "Got exception during release in finally()", e)
                    }
                }

                for (file in inputFilesToClose) {
                    try {
                        file.close()
                    } catch (e: IOException) {
                        Log.e(
                            TAG,
                            "Failed to clean up input file "
                                    + file.file.getAbsolutePath(),
                            e
                        )
                    }
                }
            }
        }).start()
    }
}
