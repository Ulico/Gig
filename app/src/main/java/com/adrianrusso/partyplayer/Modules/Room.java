package com.adrianrusso.partyplayer.Modules;

import com.google.firebase.database.FirebaseDatabase;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.List;

public class Room {

  private String code;
  private final List<Request> requests;

  public static Room newRoom() {
    Room r = new Room();
    r.setCode(RandomStringUtils.randomAlphabetic(4).toUpperCase());
    r.sync();
    return r;
  }

  public void addRequest(String query) {
    Request request = new Request(query);
    requests.add(request);
    FirebaseDatabase.getInstance().getReference("rooms/" + code + "/requests/" + (requests.size() - 1)).setValue(request);
  }

  public String getCode() {
    return code;
  }

  public void sync() {
    FirebaseDatabase.getInstance().getReference("rooms/" + code).setValue(this);
  }

  public List<Request> getRequests() {
    return requests;
  }

  public void destroy() {
    FirebaseDatabase.getInstance().getReference("rooms/" + code).removeValue();
  }

  public Room() {
    this.requests = new ArrayList<>();
  }

  public void setCode(String code) {
    this.code = code;
  }
}
