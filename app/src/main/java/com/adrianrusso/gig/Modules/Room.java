package com.adrianrusso.gig.Modules;

import android.content.SharedPreferences;
import android.util.Log;

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

  public static Room newRoom(SharedPreferences prefs) {
    Room r = new Room();
    r.setSize(1);
    r.setCode(RandomStringUtils.randomAlphabetic(4).toUpperCase());
    r.setVotePercentToPlay(0.01 * Double.parseDouble(prefs.getString("votePercentToPlay", String.valueOf(DEFAULT_VOTE_PERCENT_TO_PLAY * 100))));
    r.syncToDatabase();
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
    Log.d("mine", votePercentToPlay + "");
  }
}
