package com.gambitodeguada.chessratingscli;

import io.micronaut.context.annotation.EachProperty;

@EachProperty("players")
public class ClubPlayer {
    private String fullname;
    private String fideid;

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getFideid() {
        return fideid;
    }

    public void setFideid(String fideid) {
        this.fideid = fideid;
    }
}
