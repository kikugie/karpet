@file:Suppress("DEPRECATION", "removal")

package dev.kikugie.karpet

import carpet.api.settings.CarpetRule
import carpet.api.settings.RuleCategory
import carpet.api.settings.Validator
import carpet.settings.Rule
import net.minecraft.server.command.ServerCommandSource

object KarpetSettings {
    @JvmField
    @Rule(
        category = [Reference.MOD_ID, RuleCategory.FEATURE],
        desc = "Allow botting only yourself and accounts linked by server admins"
    )
    var allowOnlyOwnedBots = false

    @JvmField
    @Rule(
        category = [Reference.MOD_ID, RuleCategory.FEATURE],
        desc = "Limit the amount of bots on the server. Admins bypass this restriction",
        strict = false,
        options = ["-1"],
        validate = [BotLimitValidator::class]
    )
    var limitBots = -1

    @JvmField
    @Rule(
        category = [Reference.MOD_ID, RuleCategory.FEATURE],
        desc = "Team assigned to self-botted and fake players",
        strict = false,
        options = ["OFF"],
        validate = [TeamValidator::class]
    )
    var botTeam = ""

//    @JvmField
//    @Rule(
//        category = [Reference.MOD_ID, RuleCategory.FEATURE],
//        desc = "Prevents end pillar generation from filling unnecessary air blocks around them"
//    )
//    var noEndPillarFill = false

    private object BotLimitValidator : Validator<Int>() {
        override fun validate(source: ServerCommandSource?, rule: CarpetRule<Int>, new: Int, str: String): Int? =
            if (new < -1) null else new

        override fun description() = "Value must be -1 for no limits, 0 to disallow new bots or >1"
    }

    private object TeamValidator : Validator<String>() {
        override fun validate(source: ServerCommandSource?, rule: CarpetRule<String>, new: String, str: String): String? {
            return if (new == "OFF") "" else new
        }
    }
}