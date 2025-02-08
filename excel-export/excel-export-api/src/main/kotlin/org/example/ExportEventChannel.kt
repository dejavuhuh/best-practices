package org.example

import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Component
class ExportEventChannel : MessageListener {

    val subscribers = mutableMapOf<Long, MutableList<SseEmitter>>()

    fun subscribe(taskId: Long): SseEmitter {
        val emitter = SseEmitter()
        emitter.onError {
            subscribers[taskId]?.remove(emitter)
        }
        emitter.onCompletion {
            subscribers[taskId]?.remove(emitter)
        }

        subscribers.computeIfAbsent(taskId) { mutableListOf() }.add(emitter)
        return emitter
    }

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val topic = String(message.channel)
        val taskId = topic.substringAfter("export:").toLong()
        val progress = String(message.body)

        subscribers[taskId]?.forEach {
            it.send(progress)

            if (progress == "100") {
                it.complete()
            }
        }
    }
}
