package com.gambitodeguada.tournamentcli;

import io.micronaut.context.annotation.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * The names the page shows, by FIDE id, for the players whose name the API writes without its accents.
 */
@ConfigurationProperties("chesstournament64")
public class PlayerNames {
    private Map<String, String> names = new HashMap<>();

    public Map<String, String> getNames() {
        return names;
    }

    public void setNames(Map<String, String> names) {
        this.names = names;
    }

    public String nameOf(TournamentDocument.Player player) {
        if (player.fideId() == null) {
            return player.name();
        }
        return names.getOrDefault(String.valueOf(player.fideId()), player.name());
    }
}
