package com.gambitodeguada.tournamentcli;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/**
 * The parts of the tournament JSON of Chess Tournament 64 that the page needs.
 *
 * @see <a href="https://app.chesstournament64.com/schemas/tournament-v1.schema.json">tournament-v1.schema.json</a>
 */
@Serdeable
public record TournamentDocument(Tournament tournament,
                                 List<Player> players,
                                 @Nullable Standings standings,
                                 List<Round> rounds) {

    @Serdeable
    public record Tournament(String id,
                             String name,
                             String pairingSystem,
                             boolean teamTournament,
                             @Nullable Integer roundRobinCycles,
                             Scoring scoring) {
    }

    @Serdeable
    public record Scoring(double win, double draw, double loss) {
    }

    @Serdeable
    public record Player(String id, String name, @Nullable Long fideId) {
    }

    @Serdeable
    public record Standings(List<String> tiebreaks, List<StandingRow> players) {
    }

    @Serdeable
    public record StandingRow(int rank, String playerId, double points, int games, List<TiebreakValue> tiebreaks,
                              boolean promoted, boolean relegated) {

        /** The value of a tie-break, or null when it does not apply to the row. */
        @Nullable
        public Double tiebreak(String code) {
            return tiebreaks.stream().filter(t -> t.code().equals(code)).findFirst().map(TiebreakValue::value).orElse(null);
        }
    }

    @Serdeable
    public record TiebreakValue(String code, @Nullable Double value) {
    }

    @Serdeable
    public record Round(int number, List<Game> games, List<Bye> byes) {
    }

    @Serdeable
    public record Game(int board, String white, String black, @Nullable String whiteResult, @Nullable String blackResult) {
    }

    @Serdeable
    public record Bye(String playerId) {
    }
}
