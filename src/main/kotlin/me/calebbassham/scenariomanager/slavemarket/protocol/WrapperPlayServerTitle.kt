package me.calebbassham.scenariomanager.slavemarket.protocol

/**
 * PacketWrapper - ProtocolLib wrappers for Minecraft packets
 * Copyright (C) dmulloy2 <http://dmulloy2.net>
 * Copyright (C) Kristian S. Strangeland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.wrappers.EnumWrappers.TitleAction
import com.comphenix.protocol.wrappers.WrappedChatComponent

class WrapperPlayServerTitle : AbstractPacket {

    /**
     * Retrieve Action.
     *
     * @return The current Action
     */
    /**
     * Set Action.
     *
     * @param value - new value.
     */
    var action: TitleAction
        get() = handle.titleActions.read(0)
        set(value) {
            handle.titleActions.write(0, value)
        }

    /**
     * Retrieve 0 (TITLE).
     *
     *
     * Notes: chat
     *
     * @return The current 0 (TITLE)
     */
    /**
     * Set 0 (TITLE).
     *
     * @param value - new value.
     */
    var title: WrappedChatComponent
        get() = handle.chatComponents.read(0)
        set(value) {
            handle.chatComponents.write(0, value)
        }

    /**
     * Retrieve 2 (TIMES).
     *
     *
     * Notes: int
     *
     * @return The current 2 (TIMES)
     */
    /**
     * Set 2 (TIMES).
     *
     * @param value - new value.
     */
    var fadeIn: Int
        get() = handle.integers.read(0)
        set(value) {
            handle.integers.write(0, value)
        }

    /**
     * Retrieve Stay.
     *
     * @return The current Stay
     */
    /**
     * Set Stay.
     *
     * @param value - new value.
     */
    var stay: Int
        get() = handle.integers.read(1)
        set(value) {
            handle.integers.write(1, value)
        }

    /**
     * Retrieve Fade Out.
     *
     * @return The current Fade Out
     */
    /**
     * Set Fade Out.
     *
     * @param value - new value.
     */
    var fadeOut: Int
        get() = handle.integers.read(2)
        set(value) {
            handle.integers.write(2, value)
        }

    constructor() : super(PacketContainer(TYPE), TYPE) {
        handle.modifier.writeDefaults()
    }

    constructor(packet: PacketContainer) : super(packet, TYPE) {}

    companion object {
        val TYPE = PacketType.Play.Server.TITLE
    }
}