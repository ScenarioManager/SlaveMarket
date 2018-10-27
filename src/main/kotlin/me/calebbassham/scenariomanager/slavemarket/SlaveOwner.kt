package me.calebbassham.scenariomanager.slavemarket

import org.bukkit.entity.Player
import java.util.*

class SlaveOwner(val uniqueId: UUID, val name: String, val displayName: String, var balance: Int) {
    constructor(player: Player, balance: Int): this(player.uniqueId, player.name, player.displayName, balance)
}