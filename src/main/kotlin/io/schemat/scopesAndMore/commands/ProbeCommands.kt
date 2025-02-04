package io.schemat.scopesAndMore.commands

import io.schemat.scopesAndMore.probe.Probe
import io.schemat.scopesAndMore.probe.ProbeManager
import io.schemat.scopesAndMore.probes.*
import io.schemat.scopesAndMore.scopes.ScopeManager
import io.schemat.scopesAndMore.scopes.ScopeType
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.UUID
// Command framework
interface SubCommand {
    val name: String
    val description: String
    val usage: String
    val aliases: List<String>
        get() = emptyList()

    fun execute(context: CommandContext)
    fun tabComplete(context: CommandContext): List<String> = emptyList()
}

data class CommandContext(
    val player: Player,
    val args: List<String>,
    val probeManager: ProbeManager,
    val groupManager: ProbeGroupManager,
    val scopeManager: ScopeManager
) {
    fun reply(message: String) = player.sendMessage(message)
    fun getTargetProbe(idArg: String? = null): Probe? {
        return idArg?.let { arg ->
            try {
                UUID.fromString(arg).let { id -> probeManager.getProbe(id) }
            } catch (e: IllegalArgumentException) {
                null
            }
        } ?: run {
            val targetBlock = player.getTargetBlock(null, 5)
            if (!targetBlock.type.isAir) probeManager.getProbeAt(targetBlock.location) else null
        }
    }
}

// Annotation for auto-registration
@Target(AnnotationTarget.CLASS)
annotation class RegisterCommand(val parent: String = "")

// Base command handler
abstract class BaseCommand(
    private val probeManager: ProbeManager,
    private val groupManager: ProbeGroupManager,
    private val scopeManager: ScopeManager
) : CommandExecutor, TabCompleter {
    private val subcommands = mutableMapOf<String, SubCommand>()

    init {
        registerSubcommands()
    }

    private fun registerSubcommands() {
        val subCommandClasses = this::class.java.declaredClasses
            .filter { it.isAnnotationPresent(RegisterCommand::class.java) }
            .filter { SubCommand::class.java.isAssignableFrom(it) }

        for (cmdClass in subCommandClasses) {
            val cmd = cmdClass.getDeclaredConstructor(this::class.java).newInstance(this) as SubCommand
            subcommands[cmd.name] = cmd
            cmd.aliases.forEach { alias -> subcommands[alias] = cmd }
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cThis command can only be used by players")
            return true
        }

        val context = CommandContext(
            player = sender,
            args = args.toList(),
            probeManager = probeManager,
            groupManager = groupManager,
            scopeManager = scopeManager
        )

        val subCmd = args.getOrNull(0)?.lowercase()?.let { subcommands[it] }

        if (subCmd == null) {
            showHelp(sender)
            return true
        }

        subCmd.execute(context)
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (sender !is Player) return emptyList()

        val context = CommandContext(
            player = sender,
            args = args.toList(),
            probeManager = probeManager,
            groupManager = groupManager,
            scopeManager = scopeManager
        )

        return when (args.size) {
            1 -> subcommands.keys.filter { it.startsWith(args[0].lowercase()) }
            else -> {
                val subCmd = subcommands[args[0].lowercase()]
                subCmd?.tabComplete(context) ?: emptyList()
            }
        }
    }

    protected fun showHelp(player: Player) {
        player.sendMessage("§6Available Commands:")
        subcommands.values.distinct().forEach { cmd ->
            player.sendMessage("§f${cmd.usage} §7- ${cmd.description}")
        }
    }
}

// Main probe command implementation
class ProbeCommands(
    probeManager: ProbeManager,
    groupManager: ProbeGroupManager,
    scopeManager: ScopeManager
) : BaseCommand(probeManager, groupManager, scopeManager) {

    @RegisterCommand
    inner class CreateCommand : SubCommand {
        override val name = "create"
        override val description = "Create a probe at your target block"
        override val usage = "/probe create [name]"

        override fun execute(context: CommandContext) {
            val targetBlock = context.player.getTargetBlock(null, 5)
            if (targetBlock.type.isAir) {
                context.reply("§cYou must be looking at a block")
                return
            }

            val name = context.args.getOrNull(1)
            val probe = context.probeManager.createProbe(targetBlock.location, context.player, name)

            context.reply("""
                §aProbe created!
                §7ID: §f${probe.id}
                §7Location: §f${probe.location.blockX}, ${probe.location.blockY}, ${probe.location.blockZ}
                §7Face: §f${probe.labelFace}
                ${if (name != null) "§7Name: §f$name" else ""}
            """.trimIndent())
        }
    }

    @RegisterCommand
    inner class ListCommand : SubCommand {
        override val name = "list"
        override val description = "List all probes"
        override val usage = "/probe list"

        override fun execute(context: CommandContext) {
            val probes = context.probeManager.getAllProbes()
            if (probes.isEmpty()) {
                context.reply("§7No probes found")
                return
            }

            context.reply("§6Probes:")
            probes.forEach { probe ->
                context.reply("""
                    §7ID: §f${probe.id}
                    §7Location: §f${probe.location.blockX}, ${probe.location.blockY}, ${probe.location.blockZ}
                    §7Name: §f${probe.name ?: "None"}
                    §7Value: §f${probe.getHexValue()}
                """.trimIndent())
            }
        }
    }

    @RegisterCommand
    inner class GroupCommand : SubCommand {
        override val name = "group"
        override val description = "Manage probe groups"
        override val usage = "/probe group <create|add|remove|list|format|signal|endian|readorder>"

        private val subCommands = mapOf(
            "create" to ::handleCreate,
            "add" to ::handleAdd,
            "remove" to ::handleRemove,
            "list" to ::handleList,
            "value" to ::handleValue,
            "format" to ::handleFormat,
            "signal" to ::handleSignal,
            "endian" to ::handleEndian,
            "readorder" to ::handleReadOrder
        )

        override fun execute(context: CommandContext) {
            val subCmd = context.args.getOrNull(1)?.lowercase()
            val handler = subCommands[subCmd]

            if (handler == null) {
                showGroupHelp(context)
                return
            }

            handler.invoke(context)
        }

        override fun tabComplete(context: CommandContext): List<String> {
            return when (context.args.size) {
                2 -> subCommands.keys.filter { it.startsWith(context.args[1].lowercase()) }
                3 -> when (context.args[1].lowercase()) {
                    "add", "remove", "format", "signal", "endian", "readorder" -> context.groupManager.getAllGroups()
                        .flatMap { listOf(it.id.toString(), it.name) }
                        .filter { it.startsWith(context.args[2]) }
                    else -> emptyList()
                }
                4 -> when (context.args[1].lowercase()) {
                    "format" -> InterpreterRegistry.getAllInterpreters().keys
                        .filter { it.startsWith(context.args[3].lowercase()) }
                    "signal" -> listOf("binary", "hex")
                        .filter { it.startsWith(context.args[3].lowercase()) }
                    "endian" -> listOf("big", "little")
                        .filter { it.startsWith(context.args[3].lowercase()) }
                    "readorder" -> listOf("msb", "lsb")
                        .filter { it.startsWith(context.args[3].lowercase()) }
                    else -> emptyList()
                }
                else -> emptyList()
            }
        }

        private fun showGroupHelp(context: CommandContext) {
            context.reply("""
            §6Group Commands:
            §f/probe group create <name> §7- Create a new group
            §f/probe group add <groupId> [probeId] §7- Add a probe to group (creates group if needed)
            §f/probe group remove <groupId> [probeId] §7- Remove a probe from group
            §f/probe group list §7- List all groups
            §f/probe group value <groupId> §7- Get group value
            §f/probe group format <groupId> <format> §7- Set group format
            §f/probe group signal <groupId> <binary|hex> §7- Set signal type
            §f/probe group endian <groupId> <big|little> §7- Set endianness
            §f/probe group readorder <groupId> <msb|lsb> §7- Set bit read order
        """.trimIndent())
        }

        private fun handleCreate(context: CommandContext) {
            val name = context.args.getOrNull(2) ?: run {
                context.reply("§cUsage: /probe group create <name>")
                return
            }

            try {
                val group = context.groupManager.createGroup(name)
                context.reply("§aCreated group '${group.name}' with ID: ${group.id}")
            } catch (e: IllegalArgumentException) {
                context.reply("§c${e.message}")
            }
        }
        private fun handleAdd(context: CommandContext) {
            val groupId = context.args.getOrNull(2) ?: run {
                context.reply("§cUsage: /probe group add <groupId/name> [probeId]")
                return
            }

            // Try to get existing group or create new one
            val group = context.groupManager.getGroup(groupId) ?: run {
                try {
                    context.groupManager.createGroup(groupId).also {
                        context.reply("§aCreated new group '${it.name}'")
                    }
                } catch (e: IllegalArgumentException) {
                    context.reply("§c${e.message}")
                    return
                }
            }

            // Try to get existing probe or create new one
            val probe = context.getTargetProbe(context.args.getOrNull(3)) ?: run {
                // Check if player is looking at a valid block for probe creation
                val targetBlock = context.player.getTargetBlock(null, 5)
                if (targetBlock.type.isAir) {
                    context.reply("§cYou must be looking at a block to create a new probe")
                    return
                }

                // Create new probe
                context.probeManager.createProbe(targetBlock.location, context.player).also {
                    context.reply("""
                §aCreated new probe:
                §7ID: §f${it.id}
                §7Location: §f${it.location.blockX}, ${it.location.blockY}, ${it.location.blockZ}
            """.trimIndent())
                }
            }

            group.addProbe(probe)
            context.reply("§aAdded probe ${probe.id} to group '${group.name}'")
            context.reply("§7Current group value: §f${group.getInterpretedValue()}")
        }

        private fun handleRemove(context: CommandContext) {
            val groupId = context.args.getOrNull(2) ?: run {
                context.reply("§cUsage: /probe group remove <groupId/name> [probeId]")
                return
            }

            val group = context.groupManager.getGroup(groupId) ?: run {
                context.reply("§cGroup not found with ID or name: $groupId")
                return
            }

            val probe = context.getTargetProbe(context.args.getOrNull(3)) ?: run {
                context.reply("§cNo probe found. Either look at a probe or specify a probe ID")
                return
            }

            group.removeProbe(probe)
            context.reply("§aRemoved probe ${probe.id} from group '${group.name}'")
        }

        private fun handleList(context: CommandContext) {
            val groups = context.groupManager.getAllGroups()
            if (groups.isEmpty()) {
                context.reply("§7No groups found")
                return
            }

            context.reply("§6Probe Groups:")
            groups.forEach { group ->
                context.reply("""
                    §7ID: §f${group.id}
                    §7Name: §f${group.name}
                    §7Probes: §f${group.getProbes().size}
                    §7Format: §f${group.interpreter.name}
                    §7Signal: §f${group.signalType}
                    §7Endianness: §f${group.endianness}
                """.trimIndent())
            }
        }

        private fun handleValue(context: CommandContext) {
            val groupId = context.args.getOrNull(2) ?: run {
                context.reply("§cUsage: /probe group value <groupId/name>")
                return
            }

            val group = context.groupManager.getGroup(groupId) ?: run {
                context.reply("§cGroup not found with ID or name: $groupId")
                return
            }

            context.reply("""
                §aGroup value: §f${group.getInterpretedValue()}
            """.trimIndent())
        }

        private fun handleFormat(context: CommandContext) {
            val groupId = context.args.getOrNull(2) ?: run {
                context.reply("§cUsage: /probe group format <groupId/name> <format>")
                return
            }

            val group = context.groupManager.getGroup(groupId) ?: run {
                context.reply("§cGroup not found with ID or name: $groupId")
                return
            }

            var formatId = context.args.getOrNull(3)?.lowercase()
            if (formatId == null) {
                formatId = group.interpreter.name.lowercase()
            }
            val (interpreter, signalType) = InterpreterRegistry.getInterpreter(formatId) ?: run {
                // Build help message dynamically from registry
                val formats = InterpreterRegistry.getAllInterpreters()
                    .map { (id, pair) -> "§f- $id §7- ${pair.first.name} format" }
                    .joinToString("\n")

                context.reply("""
            §6Available Formats:
            $formats
        """.trimIndent())
                return
            }

            group.signalType = signalType
            group.interpreter = interpreter

            context.reply("""
        §aFormat set to: §f${interpreter.name}
        §aCurrent value: §f${group.getInterpretedValue()}
    """.trimIndent())
        }
    }

    private fun handleSignal(context: CommandContext) {
        val groupId = context.args.getOrNull(2) ?: run {
            context.reply("§cUsage: /probe group signal <groupId/name> <binary|hex>")
            return
        }

        val group = context.groupManager.getGroup(groupId) ?: run {
            context.reply("§cGroup not found with ID or name: $groupId")
            return
        }

        when (context.args.getOrNull(3)?.lowercase()) {
            "binary" -> group.signalType = SignalType.BINARY
            "hex" -> group.signalType = SignalType.HEX
            else -> {
                context.reply("""
                    §6Available Signal Types:
                    §f- binary §7- Binary signal (0 = off, >0 = on)
                    §f- hex §7- Hexadecimal signal (0-15 value)
                """.trimIndent())
                return
            }
        }

        context.reply("""
            §aSignal type set to: §f${group.signalType}
            §aCurrent value: §f${group.getInterpretedValue()}
        """.trimIndent())
    }

    private fun handleEndian(context: CommandContext) {
        val groupId = context.args.getOrNull(2) ?: run {
            context.reply("§cUsage: /probe group endian <groupId/name> <big|little>")
            return
        }

        val group = context.groupManager.getGroup(groupId) ?: run {
            context.reply("§cGroup not found with ID or name: $groupId")
            return
        }

        when (context.args.getOrNull(3)?.lowercase()) {
            "big" -> group.endianness = Endianness.BIG
            "little" -> group.endianness = Endianness.LITTLE
            else -> {
                context.reply("""
                    §6Available Endian Types:
                    §f- big §7- Big endian (MSB first)
                    §f- little §7- Little endian (LSB first)
                """.trimIndent())
                return
            }
        }

        context.reply("""
            §aEndianness set to: §f${group.endianness}
            §aCurrent value: §f${group.getInterpretedValue()}
        """.trimIndent())
    }

    private fun handleReadOrder(context: CommandContext) {
        val groupId = context.args.getOrNull(2) ?: run {
            context.reply("§cUsage: /probe group readorder <groupId/name> <msb|lsb>")
            return
        }

        val group = context.groupManager.getGroup(groupId) ?: run {
            context.reply("§cGroup not found with ID or name: $groupId")
            return
        }

        when (context.args.getOrNull(3)?.lowercase()) {
            "msb" -> group.readOrder = ReadOrder.MSB_FIRST
            "lsb" -> group.readOrder = ReadOrder.LSB_FIRST
            else -> {
                context.reply("""
                    §6Available Read Orders:
                    §f- msb §7- Most significant bit first (leftmost)
                    §f- lsb §7- Least significant bit first (rightmost)
                """.trimIndent())
                return
            }
        }

        context.reply("""
            §aRead order set to: §f${group.readOrder}
            §aCurrent value: §f${group.getInterpretedValue()}
        """.trimIndent())
    }

    @RegisterCommand
    inner class ScopeCommand : SubCommand {
        override val name = "scope"
        override val description = "Manage value scopes"
        override val usage = "/probe scope <create|remove|list> [args]"

        private val subCommands = mapOf(
            "create" to ::handleCreate,
            "remove" to ::handleRemove,
            "list" to ::handleList
        )

        override fun execute(context: CommandContext) {
            val subCmd = context.args.getOrNull(1)?.lowercase()
            val handler = subCommands[subCmd]

            if (handler == null) {
                context.reply(
                    """ 
            §6Scope Commands:
            §f/probe scope create <groupId/name> <name> <type> §7- Create a value scope
            §f/probe scope remove <scopeId or name> §7- Remove a value scope
            §f/probe scope list §7- List all value scopes
        """.trimIndent()
                )
                return
            }

            handler.invoke(context)
        }

        private fun handleCreate(context: CommandContext) {
            val groupId = context.args.getOrNull(2) ?: run {
                context.reply("§cUsage: /probe scope create <groupId/name> <name> <type>")
                return
            }

            val name = context.args.getOrNull(3) ?: run {
                context.reply("§cUsage: /probe scope create <groupId/name> <name> <type>")
                return
            }

            if (context.scopeManager.getAllScopes().any { it.name == name }) {
                context.reply("§cScope name already in use: $name")
                return
            }

            val typeName = context.args.getOrNull(4) ?: run {
                "value"
            }

            val type = when(typeName) {
                "value" -> ScopeType.VALUE
                "time_series" -> ScopeType.TIME_SERIES
                else -> {
                    context.reply("§cInvalid scope type: $typeName")
                    return
                }
            }

            val group = context.groupManager.getGroup(groupId) ?: run {
                context.reply("§cGroup not found with ID or name: $groupId")
                return
            }

            val targetBlock = context.player.getTargetBlock(null, 5)


            val scope = context.scopeManager.createScope(name, type, targetBlock.location, group, context.player)
            context.reply(
                """
            §aScope created!
            §7ID: §f${scope.id}
            §7Location: §f${scope.location.blockX}, ${scope.location.blockY}, ${scope.location.blockZ}
            §7Group: §f${group.name}
        """.trimIndent()
            )
        }

        private fun handleRemove(context: CommandContext) {
            val scopeId = context.args.getOrNull(2) ?: run {
                context.reply("§cUsage: /probe scope remove <scopeId or name>")
                return
            }

            val scope = context.scopeManager.getScope(UUID.fromString(scopeId).toString()) ?: run {
                context.reply("§cScope not found with ID: $scopeId")
                return
            }

            context.scopeManager.removeScope(scope.id.toString())
            context.reply("§aRemoved scope with ID: $scopeId")
        }

        private fun handleList(context: CommandContext) {
            val scopes = context.scopeManager.getAllScopes()
            if (scopes.isEmpty()) {
                context.reply("§7No scopes found")
                return
            }

            context.reply("§6Value Scopes:")
            scopes.forEach { scope ->
                context.reply(
                    """
                §7ID: §f${scope.id}
                §7Name: §f${scope.name}
                §7Location: §f${scope.location.blockX}, ${scope.location.blockY}, ${scope.location.blockZ}
                §7Group: §f${scope.probeGroup.name}
            """.trimIndent()
                )
            }
        }

        override fun tabComplete(context: CommandContext): List<String> {
            return when (context.args.size) {
                2 -> subCommands.keys.filter { it.startsWith(context.args[1].lowercase()) }
                3 -> when (context.args[1].lowercase()) {
                    "remove" -> context.scopeManager.getAllScopes()
                        .flatMap { listOf(it.id.toString(), it.name) }
                        .filter { it.startsWith(context.args[2]) }
                    else -> emptyList()
                }
                else -> emptyList()
            }
        }
    }


}

fun StringBuilder.appendSection(title: String, value: String) {
    append("§7$title: §f$value\n")
}