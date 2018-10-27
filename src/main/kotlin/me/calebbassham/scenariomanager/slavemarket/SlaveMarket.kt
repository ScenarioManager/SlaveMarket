package me.calebbassham.scenariomanager.slavemarket

import me.calebbassham.scenariomanager.ScenarioManagerUtils
import me.calebbassham.scenariomanager.api.SimpleScenario
import me.calebbassham.scenariomanager.api.TeamAssigner
import me.calebbassham.scenariomanager.api.settings.SimpleScenarioSetting
import me.calebbassham.scenariomanager.api.settings.onlineplayer.OnlinePlayerArrayScenarioSetting
import me.calebbassham.scenariomanager.api.settings.timespan.TimeSpanScenarioSetting
import me.calebbassham.scenariomanager.api.uhc.TeamProvider
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.CompletableFuture

class SlaveMarket : SimpleScenario(), TeamAssigner {

    val diamonds = SimpleScenarioSetting("Diamonds", "How many diamonds each slave owner starts with.", 48)
    val bidTime = TimeSpanScenarioSetting("BidLength", "How long the bid for each slave will last.", 20 * 10)
    val slaveOwners = OnlinePlayerArrayScenarioSetting("SlaveOwners", "The slave owners.", emptyArray())

    override val settings = listOf(diamonds, bidTime, slaveOwners)


    private var owners: Array<SlaveOwner>? = null

    private var currentBid: BidTask? = null

    private lateinit var teamProvider: TeamProvider

    override fun onAssignTeams(teams: TeamProvider, players: Array<Player>): CompletableFuture<Void> {
        teamProvider = teams

        owners = slaveOwners.value.mapNotNull { it.player }.map { SlaveOwner(it, diamonds.value) }.toTypedArray()

        owners?.forEach { teams.mustRegisterTeam().addEntry(it.name) }

        broadcast("Slave Market auctions are starting. The slave owners are ${owners?.map { it.displayName }?.format()}.")

        val future = CompletableFuture<Void>()

        val playerIter = players.filter { owners?.map { it.uniqueId }?.contains(it.uniqueId) == false }.map { it.uniqueId }.iterator()

        currentBid = BidTask(playerIter.next(), playerIter, future).also { it.runTaskTimer(plugin, 0, 1) }

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
            if (firstRun) {
                broadcast("A bid has started for $slaveName. The timer will begin after the first bid. The minimum bid is 0. Bid with /bid <amount>.")
                firstRun = false
            }

            if (isRunning && ticks != 0.toLong()) {
                if (ticks % 100 == 0L || ticks <= 100 && ticks % 20 == 0L) {
                    broadcast("${ScenarioManagerUtils.formatTicks(ticks)} remaining on the current bid.")
                }
            }

            if (ticks <= 0) {
                cancel()

                broadcast("${bidder?.displayName} has bought $slaveName for $bid diamonds.")
                bidder?.balance?.minus(bid ?: 0)
                val team = teamProvider.getPlayerTeam(Bukkit.getOfflinePlayer(bidder?.uniqueId))
                team?.addEntry(slave?.name)

                if (players.hasNext()) {
                    currentBid = BidTask(players.next(), players, future).apply { runTaskTimer(plugin, 10, 1) }
                } else {
                    broadcast("All slaves have been purchased.")
                    future.complete(null)
                }

                return
            }

            if (bid != null) {
                ticks--
                isRunning = true
            }
        }

        fun bid(bid: Int, bidder: Player) {

            if (bid < this.bid ?: 0) {
                sendMessage(bidder, "You must bid at least ${this.bid ?: 0}.")
                return
            }

            broadcast("${bidder.displayName} has bid $bid on $slaveName.")

            if (ticks < 100) {
                ticks += 100
                broadcast("5 seconds have been added to the auction.")
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

}