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


import java.lang.reflect.InvocationTargetException

import org.bukkit.entity.Player

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import com.google.common.base.Objects

abstract class AbstractPacket
/**
 * Constructs a new strongly typed wrapper for the given packet.
 *
 * @param handle - handle to the raw packet data.
 * @param type - the packet type.
 */
protected constructor(handle: PacketContainer?, type: PacketType) {
    // The packet we will be modifying
    /**
     * Retrieve a handle to the raw packet data.
     *
     * @return Raw packet data.
     */
    var handle: PacketContainer
        protected set

    init {
        // Make sure we're given a valid packet
        if (handle == null)
            throw IllegalArgumentException("Packet handle cannot be NULL.")
        if (!Objects.equal(handle.type, type))
            throw IllegalArgumentException(handle.handle.toString()
                + " is not a packet of type " + type)

        this.handle = handle
    }

    /**
     * Send the current packet to the given receiver.
     *
     * @param receiver - the receiver.
     * @throws RuntimeException If the packet cannot be sent.
     */
    fun sendPacket(receiver: Player) {
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(receiver,
                handle)
        } catch (e: InvocationTargetException) {
            throw RuntimeException("Cannot send packet.", e)
        }

    }

    /**
     * Send the current packet to all online players.
     */
    fun broadcastPacket() {
        ProtocolLibrary.getProtocolManager().broadcastServerPacket(handle)
    }

    /**
     * Simulate receiving the current packet from the given sender.
     *
     * @param sender - the sender.
     * @throws RuntimeException if the packet cannot be received.
     */
    fun receivePacket(sender: Player) {
        try {
            ProtocolLibrary.getProtocolManager().recieveClientPacket(sender,
                handle)
        } catch (e: Exception) {
            throw RuntimeException("Cannot receive packet.", e)
        }

    }
}