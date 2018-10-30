package de.leonkoth.blockparty.command;

import de.leonkoth.blockparty.BlockParty;
import de.leonkoth.blockparty.arena.Arena;
import de.pauhull.utils.locale.storage.LocaleString;
import lombok.Getter;
import org.bukkit.command.CommandSender;

import static de.leonkoth.blockparty.locale.BlockPartyLocale.*;

public class BlockPartyDisableCommand extends SubCommand {

    public static String SYNTAX = "/bp disable <Arena>";

    @Getter
    private LocaleString description = COMMAND_DISABLE;

    public BlockPartyDisableCommand(BlockParty blockParty) {
        super(false, 2, "disable", "blockparty.admin.disable", blockParty);
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {

        Arena arena = Arena.getByName(args[1]);
        if (arena == null) {
            ARENA_DOESNT_EXIST.message(PREFIX, sender, "%ARENA%", args[1]);
            return false;
        }

        arena.setEnabled(false);
        ARENA_DISABLE_SUCCESS.message(PREFIX, sender, "%ARENA%", args[1]);

        return true;

    }

    @Override
    public String getSyntax() {
        return SYNTAX;
    }

}
