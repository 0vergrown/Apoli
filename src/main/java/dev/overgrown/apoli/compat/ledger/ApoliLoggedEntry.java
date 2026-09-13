package dev.overgrown.apoli.compat.ledger;

public interface ApoliLoggedEntry {

    String apoli$playerUuid();

    String apoli$playerName();

    void apoli$attribute(String uuid, String name);
}
