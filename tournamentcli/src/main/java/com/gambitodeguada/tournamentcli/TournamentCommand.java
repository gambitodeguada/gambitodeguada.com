package com.gambitodeguada.tournamentcli;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "tournament",
        description = "Fetches the JSON of a league group from Chess Tournament 64 and writes its page (standings, pairings and results) in markdown",
        mixinStandardHelpOptions = true)
public class TournamentCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "URL",
            description = "The address of the JSON of the tournament, e.g. https://app.chesstournament64.com/api/v1/tournaments/6")
    URI url;

    @Option(names = {"-o", "--output"}, paramLabel = "FILE",
            description = "The page to write, e.g. ../torneos/v-liga-gambito-de-guada-grupo-a.md. Prints the page when omitted.")
    Path output;

    @Inject
    HttpClient httpClient;

    @Inject
    LeaguePageRenderer renderer;

    public static void main(String[] args) {
        System.exit(PicocliRunner.execute(TournamentCommand.class, args));
    }

    @Override
    public Integer call() {
        try {
            TournamentDocument document = httpClient.toBlocking().retrieve(HttpRequest.GET(url), TournamentDocument.class);
            String page = renderer.render(document);
            if (output == null) {
                System.out.print(page);
            } else {
                Files.writeString(output, page);
                System.err.println("Successfully wrote " + url + " to: " + output.toAbsolutePath().normalize());
            }
            return 0;
        } catch (HttpClientException e) {
            System.err.println("Error fetching " + url + ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
        return 1;
    }
}
