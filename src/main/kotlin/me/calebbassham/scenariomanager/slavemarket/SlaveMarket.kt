package me.calebbassham.scenariomanager.slavemarket

import me.calebbassham.scenariomanager.ScenarioManagerUtils
import me.calebbassham.scenariomanager.api.SimpleScenario
import me.calebbassham.scenariomanager.api.TeamAssigner
import me.calebbassham.scenariomanager.api.settings.SimpleScenarioSetting
import me.calebbassham.scenariomanager.api.settings.onlineplayer.OnlinePlayerArrayScenarioSetting
import me.calebbassham.scenariomanager.api.settings.timespan.TimeSpanScenarioSetting
import me.calebbassham.scenariomanager.api.uhc.TeamProvider
import me.calebbassham.scenariomanager.slavemarket.protocol.broadcastTitle
import me.calebbassham.scenariomanager.slavemarket.protocol.sendActionBar
import me.calebbassham.scenariomanager.slavemarket.protocol.sendTitle
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.CompletableFuture

class SlaveMarket : SimpleScenario(), TeamAssigner {

    var config: Config? = null

    val diamonds = SimpleScenarioSetting("Diamonds", "How many diamonds each slave owner starts with.", 48)
    val bidTime = TimeSpanScenarioSetting("BidLength", "How long the bid for each slave will last.", 20 * 10)
    val slaveOwners = OnlinePlayerArrayScenarioSetting("SlaveOwners", "The slave owners.", emptyArray())

    override val settings = listOf(diamonds, bidTime, slaveOwners)

    private var owners: Array<SlaveOwner>? = null

    fun isOwner(player: Player) = owners?.map { it.uniqueId }?.contains(player.uniqueId) == true

    fun getSlaveOwner(player: Player): SlaveOwner? = owners?.firstOrNull { player.uniqueId == it.uniqueId }

    private var currentBid: BidTask? = null

    private lateinit var teamProvider: TeamProvider

    override fun onAssignTeams(teams: TeamProvider, players: Array<Player>): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()

        val config = DefaultConfig()
        this.config = config

        teamProvider = teams

        owners = slaveOwners.value.mapNotNull { it.player }.map { SlaveOwner(it, diamonds.value) }.toTypedArray()
        owners?.forEach { teams.mustRegisterTeam().addEntry(it.name) }

        if (config.useMap) {
            var ownerSpawn = 0
            for (player in players) {

                val owner = getSlaveOwner(player)

                if (owner != null) {
                    config?.ownerSpawns?.get(ownerSpawn)?.let {
                        player.teleport(it)
                        owner.spawnNumber = ownerSpawn
                    }

                    ownerSpawn++
                } else {
                    player.teleport(config?.slaveSpawn)
                }
            }
        }

        when (config.STATUS_MESSAGE_TYPE) {
            StatusMessageType.CHAT -> broadcast("Slave Market auctions are starting in 10 seconds. The slave owners are ${owners?.map { it.displayName }?.format()}. The action timer begins after the first bid. The minimum bid is 0. Bid with /bid <amount>.")
            StatusMessageType.TITLE -> {

                broadcast("The auction timer starts after the first bid.")

                for (player in Bukkit.getOnlinePlayers()) {
                    if (isOwner(player)) {
                        sendTitle(player, "You are a slave owner", "Use /bid [amount] to bid on slaves", 0, 20 * 5, 0)
                    } else {
                        sendTitle(player, "Owners", owners?.map { it.displayName }?.format() ?: "none", 0, 20 * 5, 0)
                    }
                }

            }
        }


        val playerIter = players.filter { owners?.map { it.uniqueId }?.contains(it.uniqueId) == false }.map { it.uniqueId }.iterator()

        currentBid = BidTask(playerIter.next(), playerIter, future).also { it.runTaskTimer(plugin, 20 * 10, 1) }

        return future
    }

    override fun onPlayerStart(player: Player) {
        val owner = owners?.firstOrNull { it.uniqueId == player.uniqueId } ?: return
        val balance = owner.balance

        if (balance < 1) return
        player.inventory.addItem(ItemStack(Material.DIAMOND, balance))
    }

    inner class BidTask(private val slaveUniqueId: UUID, private val players: Iterator<UUID>, private val future: CompletableFuture<Void>) : BukkitRunnable() {

        private val slave: Player? get() = Bukkit.getPlayer(slaveUniqueId)

        private val slaveName: String get() = slave?.displayName ?: "$slaveUniqueId (offline player)"

        var bid: Int? = null
            private set

        private var bidder: SlaveOwner? = null

        private var ticks = bidTime.value.ticks
        private var firstRun = true
        private var isRunning = false

        override fun run() {
            val config = config ?: return

            if (config.STATUS_MESSAGE_TYPE == StatusMessageType.CHAT) {
                if (firstRun) {
                    broadcast("A bid has started for $slaveName.")
                    firstRun = false
                }
            }

            when (config.STATUS_MESSAGE_TYPE) {
                StatusMessageType.CHAT -> if (isRunning && ticks != 0.toLong()) {
                    if (ticks % 100 == 0L || ticks <= 100 && ticks % 20 == 0L) {
                        broadcast("${ScenarioManagerUtils.formatTicks(ticks)} remaining on the current bid.")
                    }
                }

                StatusMessageType.TITLE -> if (ticks % 20 == 0L) {
                    broadcastTitle(slaveName, "${ScenarioManagerUtils.formatTicks(ticks)} | ${bid?.toString()?.plus(" diamonds")
                        ?: "no bid"}", 0, 25, 0)
                }
            }

            if (config.BALANCE_MESSAGE_TYPE == BalanceMessageType.ACTION_BAR && ticks % 20 == 0L) {
                owners?.forEach {
                    val player = it.player
                    if (player != null) {
                        var balance = it.balance
                        if (bidder?.uniqueId == player.uniqueId) balance -= (bid ?: 0)

                        sendActionBar(player, "$balance diamonds")
                    }
                }
            }

            if (ticks <= 0) {
                cancel()

                when (config.STATUS_MESSAGE_TYPE) {
                    StatusMessageType.CHAT -> broadcast("${bidder?.displayName} has bought $slaveName for $bid diamonds.")

                    StatusMessageType.TITLE -> {
                        val bidder = bidder

                        if (bidder != null) {
                            broadcastTitle(slaveName, "bought by ${teamProvider.getPlayerTeam(bidder.offlinePlayer)?.prefix}${bidder.displayName}", 0, 55, 0)
                        }
                    }
                }

                bidder?.let {
                    it.balance -= bid ?: 0
                }

                val team = teamProvider.getPlayerTeam(Bukkit.getOfflinePlayer(bidder?.uniqueId))
                team?.addEntry(slave?.name)

                if (config.useMap) {
                    val spawnNumber = bidder?.spawnNumber
                    if (spawnNumber != null) {
                        val spawnLoc = config.ownerSpawns?.get(spawnNumber)
                        config.ownerSpawns?.get(spawnNumber)?.let { slave?.teleport(spawnLoc) }
                    }
                }

                if (players.hasNext()) {
                    currentBid = BidTask(players.next(), players, future).apply { runTaskTimer(plugin, 50, 1) }
                } else {
                    broadcast("All slaves have been purchased.")

                    object : BukkitRunnable() {
                        override fun run() {
                            future.complete(null)
                            this@SlaveMarket.config = null
                        }
                    }.runTaskLater(plugin, 20 * 3)
                }

                return
            }

            if (bid != null) {
                ticks--
                isRunning = true
            }
        }

        fun bid(bid: Int, bidder: Player) {
            val config = config ?: return

            if (bid < this.bid ?: 0) {
                sendMessage(bidder, "You must bid at least ${this.bid ?: 0}.")
                return
            }

            broadcast("${bidder.displayName} has bid $bid on $slaveName.")

            if (ticks < 100) {
                ticks += 100

                if (config.STATUS_MESSAGE_TYPE == StatusMessageType.CHAT) {
                    broadcast("5 seconds have been added to the auction.")
                }
            }

            this.bid = bid
            this.bidder = owners?.firstOrNull { bidder.uniqueId == it.uniqueId }
        }
    }

    inner class BidCmd : CommandExecutor {
        override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
            val currentBid = currentBid ?: run {
                sendMessage(sender, "There is no bid in progress.")
                return true
            }

            if (sender !is Player) {
                sendMessage(sender, "Only players can use this command.")
                return true
            }

            if (args.size > 1) {
                sendMessage(sender, "/$label [amount]")
                return true
            }

            if (owners?.firstOrNull { it.uniqueId == sender.uniqueId } == null) {
                sendMessage(sender, "Only slave owners can bid on a slave.")
                return true
            }

            val amount = if (args.size == 1) {
                args[0].toIntOrNull() ?: run {
                    sendMessage(sender, "\"${args[0]}\" is not a number amount.")
                    return true
                }
            } else {
                (currentBid.bid ?: 0) + 1
            }

            currentBid.bid(amount, sender)
            return true
        }
    }

    interface Config {
        val STATUS_MESSAGE_TYPE: StatusMessageType
        val BALANCE_MESSAGE_TYPE: BalanceMessageType

        val useMap: Boolean
        val world: World?
        val slaveSpawn: Location?
        val ownerSpawns: Array<out Location>?
    }

    inner class DefaultConfig : Config {

        private val isUsingProtocolLib = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")

        override val STATUS_MESSAGE_TYPE = if (isUsingProtocolLib) StatusMessageType.TITLE else StatusMessageType.CHAT
        override val BALANCE_MESSAGE_TYPE = if (isUsingProtocolLib) BalanceMessageType.ACTION_BAR else BalanceMessageType.CHAT

        override val useMap: Boolean

        override val world: World?


        override val slaveSpawn: Location?
        override val ownerSpawns: Array<out Location>?


        init {
            val config = plugin.config

            world = Bukkit.getWorld(config.getString("world"))

            val parseLocation: (path: String) -> Location = { path ->
                val x = config.getInt("$path.x")
                val y = config.getDouble("$path.y")
                val z = config.getInt("$path.z")

                val yaw = config.getDouble("$path.yaw").toFloat()
                val pitch = config.getDouble("$path.pitch").toFloat()

                Location(world, x + 0.5, y, z + 0.5, yaw, pitch)
            }

            useMap = config.getBoolean("use map")

            slaveSpawn = parseLocation("slave spawn")

            ownerSpawns = config.getConfigurationSection("owner spawns").getKeys(false)
                .map { parseLocation("owner spawns.$it") }
                .toTypedArray()
        }


    }

    enum class StatusMessageType { CHAT, TITLE }
    enum class BalanceMessageType { CHAT, ACTION_BAR }

}