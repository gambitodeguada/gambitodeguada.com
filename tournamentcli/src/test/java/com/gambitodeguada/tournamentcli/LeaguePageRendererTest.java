package com.gambitodeguada.tournamentcli;

import io.micronaut.serde.ObjectMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
