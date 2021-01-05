package com.adrianrusso.gig.Modules;

import java.util.Objects;

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
    return track == null ? "Loading..." : String.format("%s\n(%s)", track.name, track.artists.get(0).name);
  }

  public void setVotes(int votes) {
    this.votes = votes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Request request = (Request) o;
    return Objects.equals(query, request.query);
  }

  @Override
  public int hashCode() {
    return Objects.hash(query);
  }
}
