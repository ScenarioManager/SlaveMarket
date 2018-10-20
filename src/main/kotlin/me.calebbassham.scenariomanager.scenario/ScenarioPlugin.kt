package me.calebbassham.scenariomanager.scenario

import me.calebbassham.scenariomanager.api.scenarioManager
import org.bukkit.plugin.java.JavaPlugin

class ScenarioPlugin : JavaPlugin() {

    override fun onEnable() {
        scenarioManager.register(Scenario(), this)
    }

}