/*
   Copyright 2017-2021 Charles Korn.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package batect.docker.build.buildkit.services

import batect.testutils.createForEachTest
import batect.testutils.equalTo
import batect.testutils.given
import batect.testutils.logging.createLoggerForEachTestWithoutCustomSerializers
import batect.testutils.runForEachTest
import batect.testutils.withMessage
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws
import com.squareup.wire.MessageSink
import com.squareup.wire.MessageSource
import fsutil.types.Packet
import fsutil.types.Stat
import okhttp3.Headers
import okio.ByteString.Companion.encodeUtf8
import okio.utf8Size
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

object FileSyncServiceSpec : Spek({
    describe("a BuildKit file sync service") {
        val fileSystem by createForEachTest { Jimfs.newFileSystem(Configuration.unix()) }
        val contextDirectory by createForEachTest { fileSystem.getPath("/context-dir") }
        val dockerfileDirectory by createForEachTest { fileSystem.getPath("/dockerfile-dir") }
        val logger by createLoggerForEachTestWithoutCustomSerializers()

        val messageSource by createForEachTest { FakeMessageSource() }
        val messageSink by createForEachTest { FakeMessageSink() }

        beforeEachTest {
            Files.createDirectories(contextDirectory)
            Files.createDirectories(dockerfileDirectory)
        }

        val statFinishedPacket = Packet(Packet.PacketType.PACKET_STAT)

        given("no directory name is provided") {
            val headers = Headers.Builder().build()
            val service by createForEachTest { FileSyncService(contextDirectory, dockerfileDirectory, headers, logger) }

            it("throws an exception") {
                assertThat({ service.DiffCopy(messageSource, messageSink) }, throws(withMessage("No directory name provided.")))
            }
        }

        given("an empty directory name is provided") {
            val headers = Headers.Builder()
                .add("dir-name", "")
                .build()

            val service by createForEachTest { FileSyncService(contextDirectory, dockerfileDirectory, headers, logger) }

            it("throws an exception") {
                assertThat({ service.DiffCopy(messageSource, messageSink) }, throws(withMessage("No directory name provided.")))
            }
        }

        given("an unknown directory name is provided") {
            val headers = Headers.Builder()
                .add("dir-name", "blah")
                .build()

            val service by createForEachTest { FileSyncService(contextDirectory, dockerfileDirectory, headers, logger) }

            it("throws an exception") {
                assertThat({ service.DiffCopy(messageSource, messageSink) }, throws(withMessage("Unknown directory name 'blah'.")))
            }
        }

        given("the Dockerfile directory is requested") {
            val headers = Headers.Builder()
                .add("dir-name", "dockerfile")
                .add("followpaths", "Dockerfile")
                .add("followpaths", "Dockerfile.dockerignore")
                .add("followpaths", "dockerfile")
                .build()

            val service by createForEachTest { FileSyncService(contextDirectory, dockerfileDirectory, headers, logger) }

            given("the Dockerfile directory is empty") {
                beforeEachTest {
                    messageSink.addCallback(
                        "send PACKET_FIN when final empty PACKET_STAT sent to server",
                        { it == statFinishedPacket },
                        { messageSource.enqueueFinalFinPacket() }
                    )
                }

                runForEachTest { service.DiffCopy(messageSource, messageSink) }

                it("sends only an empty PACKET_STAT packet, and responds with a PACKET_FIN packet when the server sends a PACKET_FIN packet") {
                    assertThat(
                        messageSink.packetsSent,
                        equalTo(
                            listOf(
                                statFinishedPacket,
                                Packet(Packet.PacketType.PACKET_FIN),
                            )
                        )
                    )
                }
            }

            given("the Dockerfile directory contains only a Dockerfile") {
                val dockerfileContent = "This is the Dockerfile!"
                val lastModifiedTime = 1620798740644000000L
                val dockerfileStat = Stat("Dockerfile", 0x1ED, 123, 456, dockerfileContent.utf8Size(), lastModifiedTime)

                beforeEachTest {
                    val dockerfilePath = dockerfileDirectory.resolve("Dockerfile")
                    Files.write(dockerfilePath, dockerfileContent.toByteArray(Charsets.UTF_8))
                    Files.setLastModifiedTime(dockerfilePath, FileTime.from(lastModifiedTime, TimeUnit.NANOSECONDS))
                }

                given("the server does not request the contents of any files") {
                    beforeEachTest {
                        messageSink.addCallback(
                            "send PACKET_FIN when final empty PACKET_STAT sent to server",
                            { it == statFinishedPacket },
                            { messageSource.enqueueFinalFinPacket() }
                        )
                    }

                    runForEachTest { service.DiffCopy(messageSource, messageSink) }

                    it("sends a PACKET_STAT packet for the Dockerfile, an empty PACKET_STAT packet to indicate the enumeration is complete, and then responds with a PACKET_FIN packet when the server sends a PACKET_FIN packet") {
                        assertThat(
                            messageSink.packetsSent,
                            equalTo(
                                listOf(
                                    Packet(Packet.PacketType.PACKET_STAT, dockerfileStat),
                                    statFinishedPacket,
                                    Packet(Packet.PacketType.PACKET_FIN)
                                )
                            )
                        )
                    }
                }

                given("the server requests the contents of the Dockerfile") {
                    beforeEachTest {
                        messageSink.addCallback(
                            "send request for Dockerfile contents when Dockerfile PACKET_STAT sent to server",
                            { it.type == Packet.PacketType.PACKET_STAT && it.stat?.path == "Dockerfile" },
                            { messageSource.enqueue(Packet(Packet.PacketType.PACKET_REQ, ID = it.ID)) }
                        )

                        messageSink.addCallback(
                            "send PACKET_FIN when final empty PACKET_DATA sent to server",
                            { it.type == Packet.PacketType.PACKET_DATA && it.data_.size == 0 },
                            { messageSource.enqueueFinalFinPacket() }
                        )
                    }

                    runForEachTest { service.DiffCopy(messageSource, messageSink) }

                    it("sends the details of the Dockerfile, then responds with the contents of the Dockerfile when requested, and then responds with a PACKET_FIN packet when the server sends a PACKET_FIN packet") {
                        assertThat(
                            messageSink.packetsSent,
                            equalTo(
                                listOf(
                                    Packet(Packet.PacketType.PACKET_STAT, dockerfileStat, ID = 0),
                                    statFinishedPacket,
                                    Packet(Packet.PacketType.PACKET_DATA, ID = 0, data_ = dockerfileContent.encodeUtf8()),
                                    Packet(Packet.PacketType.PACKET_DATA, ID = 0),
                                    Packet(Packet.PacketType.PACKET_FIN)
                                )
                            )
                        )
                    }
                }

                given("the server requests the contents of an unknown file") {
                    beforeEachTest {
                        messageSink.addCallback(
                            "send request for Dockerfile contents when Dockerfile PACKET_STAT sent to server",
                            { it.type == Packet.PacketType.PACKET_STAT && it.stat?.path == "Dockerfile" },
                            { messageSource.enqueue(Packet(Packet.PacketType.PACKET_REQ, ID = 9000)) }
                        )

                        messageSink.addCallback(
                            "send PACKET_FIN when PACKET_ERR sent to server",
                            { it.type == Packet.PacketType.PACKET_ERR },
                            { messageSource.enqueueFinalFinPacket() }
                        )
                    }

                    runForEachTest { service.DiffCopy(messageSource, messageSink) }

                    it("responds to the request for the unknown file with an error") {
                        assertThat(
                            messageSink.packetsSent,
                            equalTo(
                                listOf(
                                    Packet(Packet.PacketType.PACKET_STAT, dockerfileStat, ID = 0),
                                    statFinishedPacket,
                                    Packet(Packet.PacketType.PACKET_ERR, data_ = "Unknown file ID 9000".encodeUtf8()),
                                    Packet(Packet.PacketType.PACKET_FIN)
                                )
                            )
                        )
                    }
                }

                // TODO: stat() call for paths, need different implementations for Unix and Windows (see stat.go, stat_unix.go, stat_windows.go)
            }

            given("the Dockerfile directory contains a Dockerfile and it is over 32 kB in size") {
                val first1KB = "A".repeat(1024)
                val second1KB = "1".repeat(1024)
                val third1KB = "!".repeat(1024)
                val first30KB = first1KB.repeat(30)
                val second30KB = second1KB.repeat(30)
                val third30KB = third1KB.repeat(30)
                val dockerfileContent = first30KB + second30KB + third30KB

                val expectedFirst32KBChunk = first1KB.repeat(30) + second1KB.repeat(2)
                val expectedSecond32KBChunk = second1KB.repeat(28) + third1KB.repeat(4)
                val expectedThird32KBChunk = third1KB.repeat(26)

                val lastModifiedTime = 1620798740644000000L
                val dockerfileStat = Stat("Dockerfile", 0x1ED, 123, 456, dockerfileContent.utf8Size(), lastModifiedTime)

                beforeEachTest {
                    val dockerfilePath = dockerfileDirectory.resolve("Dockerfile")
                    Files.write(dockerfilePath, dockerfileContent.toByteArray(Charsets.UTF_8))
                    Files.setLastModifiedTime(dockerfilePath, FileTime.from(lastModifiedTime, TimeUnit.NANOSECONDS))

                    messageSink.addCallback(
                        "send request for Dockerfile contents when Dockerfile PACKET_STAT sent to server",
                        { it.type == Packet.PacketType.PACKET_STAT && it.stat?.path == "Dockerfile" },
                        { messageSource.enqueue(Packet(Packet.PacketType.PACKET_REQ, ID = it.ID)) }
                    )

                    messageSink.addCallback(
                        "send PACKET_FIN when final empty PACKET_DATA sent to server",
                        { it.type == Packet.PacketType.PACKET_DATA && it.data_.size == 0 },
                        { messageSource.enqueueFinalFinPacket() }
                    )
                }

                runForEachTest { service.DiffCopy(messageSource, messageSink) }

                it("sends the contents of the file in 32 kB chunks") {
                    assertThat(
                        messageSink.packetsSent,
                        equalTo(
                            listOf(
                                Packet(Packet.PacketType.PACKET_STAT, dockerfileStat, ID = 0),
                                statFinishedPacket,
                                Packet(Packet.PacketType.PACKET_DATA, ID = 0, data_ = expectedFirst32KBChunk.encodeUtf8()),
                                Packet(Packet.PacketType.PACKET_DATA, ID = 0, data_ = expectedSecond32KBChunk.encodeUtf8()),
                                Packet(Packet.PacketType.PACKET_DATA, ID = 0, data_ = expectedThird32KBChunk.encodeUtf8()),
                                Packet(Packet.PacketType.PACKET_DATA, ID = 0),
                                Packet(Packet.PacketType.PACKET_FIN)
                            )
                        )
                    )
                }
            }

            given("the Dockerfile directory contains a file other than those requested") {
                val dockerfileContent = "This is the Dockerfile!"
                val lastModifiedTime = 1620798740644000000L
                val dockerfileStat = Stat("Dockerfile", 0x1ED, 123, 456, dockerfileContent.utf8Size(), lastModifiedTime)

                beforeEachTest {
                    val dockerfilePath = dockerfileDirectory.resolve("Dockerfile")
                    Files.write(dockerfilePath, dockerfileContent.toByteArray(Charsets.UTF_8))
                    Files.setLastModifiedTime(dockerfilePath, FileTime.from(lastModifiedTime, TimeUnit.NANOSECONDS))

                    val otherFilePath = dockerfileDirectory.resolve("some-other-file")
                    Files.write(otherFilePath, "This is another file".toByteArray(Charsets.UTF_8))
                }

                beforeEachTest {
                    messageSink.addCallback(
                        "send PACKET_FIN when final empty PACKET_STAT sent to server",
                        { it == statFinishedPacket },
                        { messageSource.enqueueFinalFinPacket() }
                    )
                }

                runForEachTest { service.DiffCopy(messageSource, messageSink) }

                it("only sends the details of the requested file, and not of the other file") {
                    assertThat(
                        messageSink.packetsSent,
                        equalTo(
                            listOf(
                                Packet(Packet.PacketType.PACKET_STAT, dockerfileStat),
                                statFinishedPacket,
                                Packet(Packet.PacketType.PACKET_FIN)
                            )
                        )
                    )
                }
            }
        }

        given("the build context directory is requested") {

            // Need to cover here:
            // - directories
            // - symlinks, pipes etc.
            // - making sure all files are sent with UID and GID set to 0

            // Check behaviour when ADD instruction references a particular file, but the file doesn't exist
        }
    }
})

private class FakeMessageSource : MessageSource<Packet> {
    private var closed = false
    private val messageQueue = ConcurrentLinkedQueue<Packet>()
    private val messagesAvailable = Semaphore(0)

    fun enqueue(packet: Packet) {
        if (closed) throw UnsupportedOperationException("Can't queue a packet on a closed source.")

        messageQueue.offer(packet)
        messagesAvailable.release()
    }

    fun enqueueFinalFinPacket() {
        enqueue(Packet(Packet.PacketType.PACKET_FIN))
        close()
    }

    override fun read(): Packet? {
        messagesAvailable.acquire()
        return messageQueue.poll()
    }

    override fun close() {
        closed = true
        messagesAvailable.release()
    }
}

private class FakeMessageSink : MessageSink<Packet> {
    private val packetStore = Collections.synchronizedList(mutableListOf<Packet>())
    private val callbacks = mutableListOf<Callback>()

    val packetsSent: List<Packet>
        get() = packetStore.toList()

    override fun write(message: Packet) {
        packetStore.add(message)

        callbacks
            .filter { it.criteria(message) }
            .forEach { it.action(message) }
    }

    fun addCallback(description: String, criteria: (Packet) -> Boolean, action: (Packet) -> Unit) {
        callbacks.add(Callback(description, criteria, action))
    }

    override fun cancel(): Unit = throw UnsupportedOperationException("Should never cancel sink.")
    override fun close() {}

    private data class Callback(val description: String, val criteria: (Packet) -> Boolean, val action: (Packet) -> Unit)
}
