package dev.overgrown.apoli.compat.ledger.mixin;

import dev.overgrown.apoli.compat.ledger.ApoliLoggedEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "ledger.core.LogEntry", remap = false)
public abstract class LogEntryMixin implements ApoliLoggedEntry {

    @Mutable
    @Shadow @Final private String playerUuid;

    @Mutable
    @Shadow @Final private String playerName;

    @Override
    public String apoli$playerUuid() {
        return this.playerUuid;
    }

    @Override
    public String apoli$playerName() {
        return this.playerName;
    }

    @Override
    public void apoli$attribute(String uuid, String name) {
        this.playerUuid = uuid;
        this.playerName = name;
    }
}
