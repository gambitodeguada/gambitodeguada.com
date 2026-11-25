package com.gambitodeguada.tournamentcli;

import io.micronaut.serde.ObjectMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest(startApplication = false)
class LeaguePageRendererTest {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    LeaguePageRenderer renderer;

    @Test
    void rendersThePageOfALeagueGroup() throws IOException {
        TournamentDocument document = objectMapper.readValue(resource("tournament-6.json"), TournamentDocument.class);

        assertEquals(resource("tournament-6.md"), renderer.render(document));
    }

    @Test
    void marksAPlayerWhoWithdrewBeforeHalfTheGames() throws IOException {
        String json = resource("tournament-6.json")
                .replace("\"fideId\":535072288", "\"fideId\":535072288,\"withdrawnFromRound\":3")
                .replace("\"playerId\":\"38\",\"points\":0.5,\"games\":2,\"tiebreaks\":[{\"code\":\"DE\"},{\"code\":\"KS\",\"value\":0.5},{\"code\":\"SB\",\"value\":1.75},{\"code\":\"WIN\",\"value\":0}],\"performance\":1059,\"promoted\":false,\"relegated\":true",
                        "\"playerId\":\"38\",\"points\":0.5,\"games\":2,\"tiebreaks\":[{\"code\":\"DE\"},{\"code\":\"KS\",\"value\":0.5},{\"code\":\"SB\",\"value\":1.75},{\"code\":\"WIN\",\"value\":0}],\"performance\":1059,\"promoted\":false,\"relegated\":true,\"resultsCounted\":false")
                .replace("{\"board\":1,\"white\":\"38\",\"black\":\"42\"}",
                        "{\"board\":1,\"white\":\"38\",\"black\":\"42\",\"whiteResult\":\"-\",\"blackResult\":\"+\"}")
                .replaceFirst("\"byes\":\\[\\]", "\"byes\":[{\"playerId\":\"38\"}]");
        TournamentDocument document = objectMapper.readValue(json, TournamentDocument.class);

        String page = renderer.render(document);

        String note = "\u2013: se retiró antes de jugar la mitad de las partidas; los resultados siguen en la tabla cruzada y no cuentan para la clasificación (Reglamento General de Competiciones de la FIDE, 6.6). El rendimiento sigue usando todas las partidas.";
        String diazRow = page.lines().filter(line -> line.contains("535072288")).findFirst().orElseThrow();
        assertEquals("| \u2013 | [Díaz Martín, Javier](https://ratings.fide.com/profile/535072288) \u2013 | \u2013              | \u2013            | \u2013         | \u2013      | \u2013      | \u2013      | \u2013       |", diazRow);
        assertTrue(page.contains("| 1 | [García Manzanares, Mariano](https://ratings.fide.com/profile/553006135) | 3.5            | 4            | 1.5       |"));
        assertTrue(page.contains("| 2 | Díaz Martín, Javier \u2013 | ½-½ | García Manzanares, Mariano |"));
        assertTrue(page.contains("| 1 | Díaz Martín, Javier \u2013 | - / + | Urosa Lozano, Javier |"));
        assertTrue(page.contains("**Descansa**: Díaz Martín, Javier \u2013"));
        assertTrue(page.contains("- **- / +**: Victoria de las negras por incomparecencia"));
        int standings = page.indexOf("### Clasificación");
        int noteAt = page.indexOf(note);
        int rounds = page.indexOf("## Emparejamientos por Ronda");
        assertTrue(standings < noteAt && noteAt < rounds);
        assertTrue(page.indexOf(note) < page.indexOf("**Descensos**"));
        assertEquals(1, page.split(java.util.regex.Pattern.quote(note), -1).length - 1);
    }

    @Test
    void keepsTheScoreOfAPlayerWhoWithdrewAfterHalfTheGames() throws IOException {
        String json = resource("tournament-6.json")
                .replace("\"fideId\":535072288", "\"fideId\":535072288,\"withdrawnFromRound\":6");
        TournamentDocument document = objectMapper.readValue(json, TournamentDocument.class);

        String page = renderer.render(document);

        assertTrue(page.contains("| 6 | [Díaz Martín, Javier](https://ratings.fide.com/profile/535072288) \u2013 | 0.5            | 2            | -0.5      |"));
        assertTrue(page.contains("| 2 | Díaz Martín, Javier \u2013 | ½-½ | García Manzanares, Mariano |"));
        assertTrue(page.contains("\u2013: se retiró antes de jugar la mitad de las partidas;"));
        assertFalse(page.contains("| \u2013 |"));
    }

    @Test
    void rejectsATournamentThatIsNotARoundRobin() throws IOException {
        String json = resource("tournament-6.json").replace("\"pairingSystem\":\"round_robin\"", "\"pairingSystem\":\"swiss\"");
        TournamentDocument document = objectMapper.readValue(json, TournamentDocument.class);

        assertThrows(IllegalArgumentException.class, () -> renderer.render(document));
    }

    private static String resource(String name) throws IOException {
        try (InputStream inputStream = LeaguePageRendererTest.class.getResourceAsStream("/" + name)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
