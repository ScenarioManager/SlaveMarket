package me.calebbassham.scenariomanager.slavemarket

import me.calebbassham.scenariomanager.api.scenarioManager
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class SlaveMarketPlugin : JavaPlugin() {

    override fun onEnable() {
        val slaveMarket = SlaveMarket()

        scenarioManager.register(slaveMarket, this)

        Bukkit.getPluginCommand("bid").executor = slaveMarket.BidCmd()
    }

}