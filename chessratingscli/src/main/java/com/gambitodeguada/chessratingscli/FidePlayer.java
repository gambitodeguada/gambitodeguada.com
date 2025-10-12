package com.gambitodeguada.chessratingscli;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record FidePlayer(String name, @Nullable Integer rating, String fideid, @Nullable Integer blitzRating, @Nullable Integer rapidRating) {
}
