package com.chat.base.entity


import android.text.TextUtils
import com.chat.base.R
import com.chat.base.WKBaseApplication
import com.xinbida.wukongim.WKIM
import com.xinbida.wukongim.entity.WKMsg
import com.xinbida.wukongim.msgmodel.WKMessageContent
import org.json.JSONObject


class GlobalMessage {
    var setting: Int = 0
    lateinit var message_idstr: String
    var message_seq: Long = 0
    lateinit var client_msg_no: String
    lateinit var from_uid: String
    var timestamp: Long = 0L
    var is_deleted: Int = 0
    lateinit var channel: GlobalChannel
    lateinit var from_channel: GlobalChannel
    lateinit var payload: Map<String, Any>

    // 改为可写，便于本地搜索时（fromWKMsg）直接注入 WKMessageContent，避免再依赖 payload。
    var messageContent: WKMessageContent? = null

    fun getMessageModel(): WKMessageContent? {
        if (messageContent == null) {
            // 仅当外部未注入 messageContent，且确实有 payload 时，才从 payload 解码。
            if (::payload.isInitialized) {
                val jsonObject = JSONObject(payload)
                messageContent = WKIM.getInstance().msgManager.getMsgContentModel(jsonObject)
            }
        }
        return messageContent
    }

    fun getContentType(): Int {
        // 本地构造的 GlobalMessage 不一定有 payload，优先从已注入的 messageContent 读取类型。
        val mc = messageContent
        if (mc != null) {
            return mc.type
        }
        if (!::payload.isInitialized) {
            return 0
        }
        val type = payload["type"]
        if (type is Int) {
            return type
        }
        return 0
    }


    fun getHtmlText(): String {
        val content = getMessageModel()?.content
        if (!TextUtils.isEmpty(content)) {
            return content!!.replace("<mark>", "<font color=#f65835>")
                .replace("</mark>", "</font>")
        }
        return ""
    }

    fun getHtmlWithField(field: String): String {
        if (!::payload.isInitialized) {
            return ""
        }
        val content = payload[field]
        if (content is String && !TextUtils.isEmpty(content)) {
            return WKBaseApplication.getInstance().application.getString(R.string.last_message_file) + " " + content.replace(
                "<mark>",
                "<font color=#f65835>"
            )
                .replace("</mark>", "</font>")
        }
        return ""
    }

    companion object {
        /**
         * 从本地 SDK 的 WKMsg 构造 GlobalMessage，使群聊「查找聊天记录」可在
         * 服务端搜索接口不可用（如 WuKongIM 搜索插件未启用）时仍能基于本地数据库工作。
         * 客户端 UI 仍按原 GlobalMessage 模型渲染，无需改动适配器。
         */
        @JvmStatic
        fun fromWKMsg(msg: WKMsg): GlobalMessage {
            val gm = GlobalMessage()
            gm.message_idstr = msg.messageID ?: ""
            gm.message_seq = msg.messageSeq.toLong()
            gm.client_msg_no = msg.clientMsgNO ?: ""
            gm.from_uid = msg.fromUID ?: ""
            gm.timestamp = msg.timestamp.toLong()
            gm.is_deleted = msg.isDeleted

            val ch = GlobalChannel()
            ch.channel_id = msg.channelID ?: ""
            ch.channel_type = msg.channelType
            val sdkChannel = WKIM.getInstance().channelManager.getChannel(
                msg.channelID, msg.channelType
            )
            if (sdkChannel != null) {
                ch.channel_name = if (!TextUtils.isEmpty(sdkChannel.channelRemark))
                    sdkChannel.channelRemark
                else
                    sdkChannel.channelName ?: ""
            }
            gm.channel = ch

            val fromCh = GlobalChannel()
            fromCh.channel_id = msg.fromUID ?: ""
            fromCh.channel_type =
                com.xinbida.wukongim.entity.WKChannelType.PERSONAL
            if (!TextUtils.isEmpty(msg.fromUID)) {
                val sdkFromChannel = WKIM.getInstance().channelManager.getChannel(
                    msg.fromUID, com.xinbida.wukongim.entity.WKChannelType.PERSONAL
                )
                if (sdkFromChannel != null) {
                    fromCh.channel_name = if (!TextUtils.isEmpty(sdkFromChannel.channelRemark))
                        sdkFromChannel.channelRemark
                    else
                        sdkFromChannel.channelName ?: ""
                }
            }
            gm.from_channel = fromCh

            // 通过 WKMessageContent.encodeMsg() 复用 SDK / 业务子类已实现的序列化逻辑，
            // 让 payload 字段覆盖适配器可能读取的所有内容（如文件类型的 "name"）。
            val payloadMap = HashMap<String, Any>()
            payloadMap["type"] = msg.type
            val mc = msg.baseContentMsgModel
            if (mc != null) {
                try {
                    val json = mc.encodeMsg()
                    if (json != null) {
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val v = json.opt(k) ?: continue
                            payloadMap[k] = v
                        }
                    }
                } catch (e: Exception) {
                    // 忽略 encode 异常，至少 type 已就位
                }
            }
            gm.payload = payloadMap
            gm.messageContent = mc
            return gm
        }
    }
}