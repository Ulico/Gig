package com.adrianrusso.partyplayer.Modules;

import com.google.firebase.database.FirebaseDatabase;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.List;

public class Room {

  private String code;
  private int size;
  private final List<Request> requests;
  private double votePercentToPlay;

  public final static double DEFAULT_VOTE_PERCENT_TO_PLAY = 0.5;

  public static Room newRoom() {
    Room r = new Room();
    r.setSize(1);
    r.setCode(RandomStringUtils.randomAlphabetic(4).toUpperCase());
    r.syncToDatabase();
    r.setVotePercentToPlay(DEFAULT_VOTE_PERCENT_TO_PLAY);
    return r;
  }

  public void addRequest(String query) {
    Request request = new Request(query);
    requests.add(request);
    FirebaseDatabase.getInstance().getReference("rooms").child(code).child("requests").child(String.valueOf(requests.size() - 1)).setValue(request);
  }

  public String getCode() {
    return code;
  }

  public void syncToDatabase() {
    FirebaseDatabase.getInstance().getReference("rooms").child(code).setValue(this);
  }

  public List<Request> getRequests() {
    return requests;
  }

  public void destroy() {
    FirebaseDatabase.getInstance().getReference("rooms").child(code).removeValue();
  }

  public Room() {
    this.requests = new ArrayList<>();
  }

  public void setCode(String code) {
    this.code = code;
  }

  public int getSize() {
    return size;
  }

  public void addPerson() {
    size++;
    syncToDatabase();
  }

  public void removePerson() {
    size--;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public double getVotePercentToPlay() {
    return votePercentToPlay;
  }

  public void setVotePercentToPlay(double votePercentToPlay) {
    this.votePercentToPlay = votePercentToPlay;
  }
}
