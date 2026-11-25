package com.gambitodeguada.tournamentcli;

import com.gambitodeguada.tournamentcli.TournamentDocument.Game;
import com.gambitodeguada.tournamentcli.TournamentDocument.Player;
import com.gambitodeguada.tournamentcli.TournamentDocument.Round;
import com.gambitodeguada.tournamentcli.TournamentDocument.StandingRow;
import com.gambitodeguada.tournamentcli.TournamentDocument.Tournament;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Writes the Jekyll page of a league group (a round robin between individual players) from its tournament JSON.
 */
@Singleton
public class LeaguePageRenderer {
    private static final String FIDE_PROFILE = "https://ratings.fide.com/profile/";
    /** A group of a league is named "Grupo A - V Liga Gambito de Guada". */
    private static final Pattern GROUP_NAME = Pattern.compile("^Grupo ([A-Z]) - (.+)$");
    /** The tie-breaks the page names, by the base code of the FIDE Tie-Break Regulations. */
    private static final Map<String, String> TIEBREAK_NAMES = Map.of(
            "DE", "Encuentro directo",
            "KS", "Sistema Koya: puntos contra los jugadores con el 50% o más de los puntos",
            "SB", "Sonneborn-Berger",
            "WIN", "Número de victorias",
            "BH", "Buchholz");
    private static final String[] NUMBER_WORDS = {"cero", "un", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho"};
    /** What Chess Tournament 64 prints instead of the rank and the score of a player whose results do not count. */
    private static final String WITHDRAWN = "\u2013";
    private static final String WITHDRAWN_NOTE = WITHDRAWN + ": se retiró antes de jugar la mitad de las partidas; los resultados siguen en la tabla cruzada y no cuentan para la clasificación (Reglamento General de Competiciones de la FIDE, 6.6). El rendimiento sigue usando todas las partidas.";

    private final PlayerNames playerNames;

    public LeaguePageRenderer(PlayerNames playerNames) {
        this.playerNames = playerNames;
    }

    public String render(TournamentDocument document) {
        Tournament tournament = document.tournament();
        if (!"round_robin".equals(tournament.pairingSystem()) || tournament.teamTournament() || document.standings() == null) {
            throw new IllegalArgumentException("Tournament " + tournament.id() + " (" + tournament.name()
                    + ") is not a round robin between individual players");
        }
        Map<String, Player> players = document.players().stream().collect(Collectors.toMap(Player::id, Function.identity()));
        List<StandingRow> standings = document.standings().players();
        Set<String> marked = markedPlayers(document.players(), standings);
        Matcher group = GROUP_NAME.matcher(tournament.name());
        Character groupLetter = group.matches() ? group.group(1).charAt(0) : null;
        List<String> movements = movements(standings, groupLetter);
        List<String> tiebreaks = document.standings().tiebreaks();

        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("layout: page\n");
        sb.append("title: ").append(yamlScalar(tournament.name())).append('\n');
        sb.append("description: \"").append(description(document, group).replace("\"", "\\\"")).append("\"\n");
        sb.append("---\n\n");

        sb.append("### Clasificación\n\n");
        sb.append("| # | **Jugador** | **Puntuación** | **Partidas** | **Media** |");
        tiebreaks.forEach(code -> sb.append(" **").append(code).append("** |"));
        sb.append("\n|---|-------------|----------------|--------------|-----------|");
        tiebreaks.forEach(code -> sb.append("-".repeat(code.length() + 6)).append('|'));
        sb.append('\n');
        for (StandingRow row : standings) {
            // The API already drops a not-counted player's games from every other row. Chess Tournament 64
            // prints the marker instead of that row's rank and figures; the games stay in the rounds below.
            boolean counted = row.countsResults();
            double average = row.points() - row.games() / 2.0;
            sb.append("| ").append(counted ? Integer.toString(row.rank()) : WITHDRAWN)
                    .append(" | ").append(linkedName(player(players, row.playerId()), marked))
                    .append(" | ").append(String.format("%-14s", counted ? number(row.points()) : WITHDRAWN))
                    .append(" | ").append(String.format("%-12s", counted ? Integer.toString(row.games()) : WITHDRAWN))
                    .append(" | ").append(String.format("%-9s", counted ? number(average) : WITHDRAWN))
                    .append(" |");
            for (String code : tiebreaks) {
                String value = WITHDRAWN;
                if (counted) {
                    Double tiebreak = row.tiebreak(code);
                    value = tiebreak != null ? number(tiebreak) : "";
                }
                sb.append(' ').append(String.format("%-" + (code.length() + 4) + "s", value)).append(" |");
            }
            sb.append('\n');
        }
        long relegated = standings.stream().filter(StandingRow::relegated).count();
        long promoted = standings.stream().filter(StandingRow::promoted).count();
        if (promoted > 0 || relegated > 0) {
            sb.append("{:").append(promoted > 0 ? " .liga-ascensos" : "").append(relegated > 0 ? " .liga-descensos" : "").append("}\n");
        }
        sb.append('\n');
        if (!marked.isEmpty()) {
            sb.append(WITHDRAWN_NOTE).append("\n\n");
        }
        if (promoted > 0) {
            sb.append("**Ascensos**: ").append(promotion(promoted, groupLetter, true)).append(".\n\n");
        }
        if (relegated > 0) {
            sb.append("**Descensos**: ").append(relegation(relegated, groupLetter, true)).append(".\n\n");
        }

        sb.append("## Emparejamientos por Ronda\n\n");
        for (Round round : document.rounds()) {
            sb.append("### Ronda ").append(round.number()).append("\n\n");
            if (round.games().isEmpty() && round.byes().isEmpty()) {
                // a round that is not paired yet
                sb.append("Emparejamientos pendientes.\n\n");
                continue;
            }
            sb.append("| **Mesa** | **Blancas** | **Resultado** | **Negras** |\n");
            sb.append("|----------|-------------|---------------|------------|\n");
            for (Game game : round.games()) {
                sb.append("| ").append(game.board())
                        .append(" | ").append(plainName(player(players, game.white()), marked))
                        .append(" | ").append(result(game))
                        .append(" | ").append(plainName(player(players, game.black()), marked))
                        .append(" |\n");
            }
            sb.append('\n');
            if (!round.byes().isEmpty()) {
                sb.append("**Descansa**: ")
                        .append(round.byes().stream()
                                .map(bye -> plainName(player(players, bye.playerId()), marked))
                                .collect(Collectors.joining("; ")))
                        .append("\n\n");
            }
        }

        sb.append("## Información del Torneo\n\n");
        sb.append("- **Formato**: ").append(format(tournament)).append('\n');
        if (groupLetter != null) {
            sb.append("- **Grupo**: ").append(groupLetter).append('\n');
        }
        sb.append("- **Total de rondas**: ").append(document.rounds().size()).append('\n');
        sb.append("- **Jugadores**: ").append(document.players().size()).append(" participantes\n\n");

        sb.append("### Sistema de Puntuación\n\n");
        sb.append("- Victoria: ").append(points(tournament.scoring().win())).append('\n');
        sb.append("- Tablas: ").append(points(tournament.scoring().draw())).append('\n');
        sb.append("- Derrota: ").append(points(tournament.scoring().loss())).append("\n\n");

        if (!tiebreaks.isEmpty()) {
            sb.append("### Desempates\n\n");
            sb.append("En caso de empate a puntos se aplican, por este orden:\n\n");
            for (int i = 0; i < tiebreaks.size(); i++) {
                String code = tiebreaks.get(i);
                String name = TIEBREAK_NAMES.get(code.split("[:+-]")[0]);
                sb.append(i + 1).append(". **").append(code).append("**").append(name != null ? ": " + name : "").append('\n');
            }
            sb.append('\n');
        }

        sb.append("### Leyenda de Resultados\n\n");
        sb.append("- **1-0**: Victoria de las blancas\n");
        sb.append("- **0-1**: Victoria de las negras\n");
        sb.append("- **½-½**: Tablas\n");
        sb.append("- **...**: Partida pendiente\n");
        if (hasForfeit(document)) {
            sb.append("- **+ / -**: Victoria de las blancas por incomparecencia\n");
            sb.append("- **- / +**: Victoria de las negras por incomparecencia\n");
        }
        sb.append('\n');

        sb.append("### Notas\n\n");
        for (String movement : movements) {
            sb.append("- ").append(movement).append('\n');
        }
        sb.append("- Los resultados se actualizarán al finalizar cada partida\n");
        sb.append("- La Media se calcula restando a la puntuación el número de partidas dividida entre dos\n");
        return sb.toString();
    }

    private static List<String> movements(List<StandingRow> standings, Character groupLetter) {
        long promoted = standings.stream().filter(StandingRow::promoted).count();
        long relegated = standings.stream().filter(StandingRow::relegated).count();
        List<String> movements = new ArrayList<>();
        if (promoted > 0) {
            movements.add(capitalize(promotion(promoted, groupLetter, false)));
        }
        if (relegated > 0) {
            movements.add(capitalize(relegation(relegated, groupLetter, false)));
        }
        return movements;
    }

    /** The players who go up, to the group with the letter before this one. */
    private static String promotion(long count, Character groupLetter, boolean withColor) {
        String target = groupLetter != null && groupLetter > 'A' ? " al Grupo " + (char) (groupLetter - 1) : "";
        return movement(count, "primer", "primeros", "verde", "asciende", withColor) + target;
    }

    /** The players who go down, to the group with the letter after this one. */
    private static String relegation(long count, Character groupLetter, boolean withColor) {
        String target = groupLetter != null ? " al Grupo " + (char) (groupLetter + 1) : "";
        return movement(count, "último", "últimos", "rojo", "desciende", withColor) + target;
    }

    private static String movement(long count, String singular, String plural, String color, String verb, boolean withColor) {
        if (count == 1) {
            return "el " + singular + " clasificado" + (withColor ? " (fila en " + color + ")" : "") + " " + verb;
        }
        String number = count < NUMBER_WORDS.length ? NUMBER_WORDS[(int) count] : String.valueOf(count);
        return "los " + number + " " + plural + " clasificados" + (withColor ? " (filas en " + color + ")" : "") + " " + verb + "n";
    }

    private static String description(TournamentDocument document, Matcher group) {
        String name = document.tournament().name();
        String subject = "de " + name;
        if (group.matches()) {
            String league = group.group(2);
            subject = "del Grupo " + group.group(1) + (league.contains("Liga") ? " de la " : " de ") + league;
        }
        return "Clasificación, emparejamientos y resultados " + subject + ", round robin a " + document.rounds().size()
                + " rondas entre " + document.players().size() + " jugadores.";
    }

    private static String format(Tournament tournament) {
        int cycles = tournament.roundRobinCycles() != null ? tournament.roundRobinCycles() : 1;
        return switch (cycles) {
            case 1 -> "Single Round Robin (todos contra todos sólo una vez)";
            case 2 -> "Double Round Robin (todos contra todos dos veces)";
            default -> "Round Robin (todos contra todos " + cycles + " veces)";
        };
    }

    private static String result(Game game) {
        if (game.whiteResult() == null || game.blackResult() == null) {
            return "...";
        }
        String white = score(game.whiteResult());
        String black = score(game.blackResult());
        if (isForfeit(game.whiteResult()) || isForfeit(game.blackResult())) {
            return white + " / " + black;
        }
        return white.equals("½") && black.equals("½") ? "½-½" : white + " - " + black;
    }

    private static boolean isForfeit(String trfResult) {
        return "+".equals(trfResult) || "-".equals(trfResult);
    }

    private static boolean hasForfeit(TournamentDocument document) {
        return document.rounds().stream()
                .flatMap(round -> round.games().stream())
                .anyMatch(game -> isForfeit(game.whiteResult()) || isForfeit(game.blackResult()));
    }

    /** Players who withdrew, or whose standing row the API says is not counted. */
    private static Set<String> markedPlayers(List<Player> players, List<StandingRow> standings) {
        Set<String> marked = new HashSet<>();
        for (Player player : players) {
            if (player.withdrawn()) {
                marked.add(player.id());
            }
        }
        for (StandingRow row : standings) {
            if (!row.countsResults()) {
                marked.add(row.playerId());
            }
        }
        return marked;
    }

    /** A result as the FIDE TRF writes it: W, D and L are a win, a draw and a loss that are not rated. */
    private static String score(String trfResult) {
        return switch (trfResult) {
            case "1", "W" -> "1";
            case "=", "D" -> "½";
            case "0", "L" -> "0";
            default -> trfResult;
        };
    }

    private String linkedName(Player player, Set<String> marked) {
        String name = playerNames.nameOf(player);
        String linked = player.fideId() != null ? "[" + name + "](" + FIDE_PROFILE + player.fideId() + ")" : name;
        return marked.contains(player.id()) ? linked + " " + WITHDRAWN : linked;
    }

    private String plainName(Player player, Set<String> marked) {
        String name = playerNames.nameOf(player);
        return marked.contains(player.id()) ? name + " " + WITHDRAWN : name;
    }

    private static Player player(Map<String, Player> players, String id) {
        Player player = players.get(id);
        if (player == null) {
            throw new IllegalArgumentException("The tournament has no player with id " + id);
        }
        return player;
    }

    private static String points(double value) {
        return number(value) + (value == 1 ? " punto" : " puntos");
    }

    private static String number(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String yamlScalar(String text) {
        return text.matches("[\\p{L}\\p{N}][\\p{L}\\p{N} .,()-]*") ? text : "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
