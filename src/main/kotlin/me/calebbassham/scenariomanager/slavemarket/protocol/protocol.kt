package me.calebbassham.scenariomanager.slavemarket.protocol

import com.comphenix.protocol.wrappers.EnumWrappers
import com.comphenix.protocol.wrappers.WrappedChatComponent
import org.bukkit.entity.Player

private fun titlePacket(title: String) = WrapperPlayServerTitle().apply {
    this.action = EnumWrappers.TitleAction.TITLE
    this.title = WrappedChatComponent.fromText(title)
}

private fun subtitlePacket(subtitle: String) = WrapperPlayServerTitle().apply {
    this.action = EnumWrappers.TitleAction.SUBTITLE
    this.title = WrappedChatComponent.fromText(subtitle)
}

private fun timesPacket(fadeIn: Int, stay: Int, fadeOut: Int) = WrapperPlayServerTitle().apply {
    this.action = EnumWrappers.TitleAction.TIMES
    this.fadeIn = fadeIn
    this.stay = stay
    this.fadeOut = fadeOut
}

private fun actionBarPacket(text: String) = WrapperPlayServerTitle().apply {
    this.action = EnumWrappers.TitleAction.ACTIONBAR
    this.title = WrappedChatComponent.fromText(text)
}

internal fun sendTitle(player: Player, title: String, subtitle: String, fadeIn: Int, stay: Int, fadeOut: Int) {
    titlePacket(title).sendPacket(player)
    subtitlePacket(subtitle).sendPacket(player)
    timesPacket(fadeIn, stay, fadeOut).sendPacket(player)
}

internal fun broadcastTitle(title: String, subtitle: String, fadeIn: Int, stay: Int, fadeOut: Int) {
    titlePacket(title).broadcastPacket()
    subtitlePacket(subtitle).broadcastPacket()
    timesPacket(fadeIn, stay, fadeOut).broadcastPacket()
}

internal fun sendActionBar(player: Player, message: String) =
    actionBarPacket(message).sendPacket(player)

internal fun broadcastActionBar(message: String) =
    actionBarPacket(message).broadcastPacket()