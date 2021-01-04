package com.adrianrusso.partyplayer.Modules;

import kaaes.spotify.webapi.android.models.Track;

public class Request {

  private String query;
  private int votes;
  private Track track;

  public Request(String query) {
    this.query = query;
    votes = 1;
  }

  public Request() {
  }

  public void increaseVotes() {
    votes++;
  }

  public String getQuery() {
    return query;
  }

  public int getVotes() {
    return votes;
  }

  public Track getTrack() {
    return track;
  }

  public void setTrack(Track track) {
    this.track = track;
  }

  public String formattedString() {
    return track == null ? "" : String.format("%s (%s)", track.name, track.artists.get(0).name);
  }

  public void setVotes(int votes) {
    this.votes = votes;
  }
}
