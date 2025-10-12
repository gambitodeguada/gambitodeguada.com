package com.gambitodeguada.chessratingscli;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import jakarta.inject.Inject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Command(name = "chessratings", description = "Fetches the Club Players FIDE ratings and outputs them in a markdown table",
        mixinStandardHelpOptions = true)
public class RatingsCommand implements Runnable {
    private static final Comparator<FidePlayer> COMPARATOR = (o1, o2) -> {
        int comparison = compareRatings(o1.rating(), o2.rating());
        if (comparison != 0) {
            return comparison;
        }
        comparison = compareRatings(o1.rapidRating(), o2.rapidRating());
        if (comparison != 0) {
            return comparison;
        }
        return compareRatings(o1.blitzRating(), o2.blitzRating());
    };
    public static final String HTTPS_RATINGS_FIDE_COM = "https://ratings.fide.com";

    @Inject
    List<ClubPlayer> players;

    public static void main(String[] args) throws Exception {
        PicocliRunner.run(RatingsCommand.class, args);
    }

    public void run() {
        List<FidePlayer> ratedPlayers = fetchPlayerRatings(players);
        ratedPlayers.sort(COMPARATOR);
        String output = """
    ---
    layout: page
    title: Jugadores
    ---

    """ + markdownTable(ratedPlayers);

        try {
            Path outputPath = Paths.get("..").resolve("jugadores.md");
            Files.writeString(outputPath, output);
            System.out.println("Successfully wrote ratings to: " + outputPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private List<FidePlayer> fetchPlayerRatings(List<ClubPlayer> players) {
        try (HttpClient httpClient = HttpClient.create(new URL(HTTPS_RATINGS_FIDE_COM))) {
            BlockingHttpClient blockingHttpClient = httpClient.toBlocking();
            List<FidePlayer> ratedPlayers = new ArrayList<>();
            for (ClubPlayer player : players) {
                ratedPlayers.add(fetchPlayer(blockingHttpClient, player.getFideid()));
            }
            return ratedPlayers;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private FidePlayer fetchPlayer(BlockingHttpClient blockingHttpClient, String fideId) {
        String html = blockingHttpClient.retrieve("/profile/" + fideId);
        return parseFidePlayer(html);
    }

    private String markdownTable(List<FidePlayer> players) {
        StringBuilder sb = new StringBuilder();
        sb.append("| Name | FIDE ID | Standard Rating | Rapid Rating | Blitz Rating |\n");
        sb.append("|:----:|:--------:|:----------------:|:-------------:|:-------------:|\n");

        for (FidePlayer player : players) {
            sb.append("|")
                    .append(player.name()).append("|")
                    .append("[").append(player.fideid()).append("](https://ratings.fide.com/profile/").append(player.fideid()).append(")").append("|")
                    .append(player.rating() != null ? player.rating() : "").append("|")
                    .append(player.rapidRating() != null ? player.rapidRating() : "").append("|")
                    .append(player.blitzRating() != null ? player.blitzRating() : "").append("|\n");
        }
        return sb.toString();
    }

    public static int compareRatings(Integer o1Rating, Integer o2Rating) {
        if (o1Rating == null && o2Rating == null) {
            return 0;
        }
        if (o1Rating == null) {
            return 1;
        }
        if (o2Rating == null) {
            return -1;
        }
        return o2Rating.compareTo(o1Rating);
    }

    public static FidePlayer parseFidePlayer(String html) {
        Document doc = Jsoup.parse(html);

        // Name and FIDE ID
        String name = extractText(doc, "h1.player-title");
        String fideIdRaw = extractText(doc, "p.profile-info-id");
        String fideId = fideIdRaw.replaceAll("\\D+", ""); // Remove non-digits

        // Ratings (null if "Not rated" or missing)
        Integer standard = parseRating(doc.selectFirst(".profile-standart p"));
        Integer rapid = parseRating(doc.selectFirst(".profile-rapid p"));
        Integer blitz = parseRating(doc.selectFirst(".profile-blitz p"));

        return new FidePlayer(name, standard, fideId, blitz, rapid);
    }

    private static String extractText(Document doc, String selector) {
        Element el = doc.selectFirst(selector);
        return el != null ? el.text().trim() : "";
    }

    private static Integer parseRating(Element el) {
        if (el == null) return null;
        String text = el.text().trim();
        if (text.equalsIgnoreCase("Not rated") || text.isEmpty()) return null;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
