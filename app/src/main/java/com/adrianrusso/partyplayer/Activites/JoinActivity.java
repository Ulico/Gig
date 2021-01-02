package com.adrianrusso.partyplayer.Activites;

import android.os.Bundle;
import android.text.InputFilter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.adrianrusso.partyplayer.Modules.Request;
import com.adrianrusso.partyplayer.Modules.Room;
import com.adrianrusso.partyplayer.R;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JoinActivity extends AppCompatActivity {

  private static Room room;
  private static FirebaseDatabase database;
  private ListView listView;
  private static List<String> requestStrings;
  private static AlertDialog sendRequestAlert, joinAlert;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_join);

    Button join = findViewById(R.id.join);
    Button send = findViewById(R.id.sendRequest);
    listView = findViewById(R.id.requestListJoin);
    database = FirebaseDatabase.getInstance();
    requestStrings = new ArrayList<>();

    sendRequestAlert = getSendRequestAlert();
    joinAlert = getJoinAlert();

    join.setOnClickListener(v -> joinAlert.show());
    send.setOnClickListener(v -> sendRequestAlert.show());
  }

  private AlertDialog getJoinAlert() {
    AlertDialog.Builder alert = new AlertDialog.Builder(this);
    alert.setMessage("Enter room code");
    final EditText input = new EditText(this);
    input.setFilters(new InputFilter[] {new InputFilter.AllCaps()});
    alert.setView(input);
    alert.setPositiveButton("Ok", (dialog, whichButton) -> {
      String enteredCode = input.getText().toString();
      if (room != null && enteredCode.equals(room.getCode())) {
        Toast.makeText(this, "Already in room.", Toast.LENGTH_SHORT).show();
      } else {
        joinRoom(enteredCode);
      }

    });

    alert.setNegativeButton("Cancel", (dialog, whichButton) -> {
    });
    return alert.create();
  }


  private AlertDialog getSendRequestAlert() {
    AlertDialog.Builder alert = new AlertDialog.Builder(this);

    alert.setMessage("Enter request");

    final EditText input = new EditText(this);
    alert.setView(input);

    alert.setPositiveButton("Ok", (dialog, whichButton) -> {
      if (room == null) {
        Toast.makeText(this, "Please join room.", Toast.LENGTH_SHORT).show();
      } else {
        database.getReference("rooms").addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(@NonNull DataSnapshot snapshot) {
            String query = input.getText().toString();
            if (snapshot.hasChild(room.getCode())) {
              for (DataSnapshot request : snapshot.child(room.getCode()).child("requests").getChildren()) {
                if (request.child("query").getValue(String.class).equals(query)) {
                  Toast.makeText(JoinActivity.this, "Request already exists.", Toast.LENGTH_SHORT).show();
                  return;
                }
              }
              room.addRequest(query);
            } else {
              Toast.makeText(JoinActivity.this, "Room not found.", Toast.LENGTH_SHORT).show();
            }
          }

          @Override
          public void onCancelled(@NonNull DatabaseError error) {

          }
        });
      }
    });

    alert.setNegativeButton("Cancel", (dialog, whichButton) -> {
    });
    return alert.create();
  }

  ChildEventListener listener = new ChildEventListener() {
    @Override
    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
      addRequestToList(Objects.requireNonNull(snapshot.getValue(Request.class)));
    }

    @Override
    public void onChildRemoved(@NonNull DataSnapshot snapshot) {
      Request r = snapshot.getValue(Request.class);
      if (r.getTrack() != null)
        removeRequestFromList(r);
      else
        Toast.makeText(JoinActivity.this, "Request already exists.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {

    }
  };

  private void joinRoom(String code) {
    database.getReference("rooms").addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot snapshot) {
        if (snapshot.hasChild(code)) {
          Toast.makeText(JoinActivity.this, "Joined room.", Toast.LENGTH_SHORT).show();
          room = snapshot.child(code).getValue(Room.class);
          room.addPerson();
          room.syncToDatabase();
          for (Request r : Objects.requireNonNull(room).getRequests()) {
            if (r.getTrack() != null)
              requestStrings.add(r.formattedString());
          }
          listView.setAdapter(new ArrayAdapter<>(JoinActivity.this, android.R.layout.simple_list_item_1, requestStrings));

          database.getReference("rooms/" + room.getCode() + "/requests").addChildEventListener(listener);
        } else {
          Toast.makeText(JoinActivity.this, "Room not found.", Toast.LENGTH_SHORT).show();
        }
      }

      @Override
      public void onCancelled(@NonNull DatabaseError error) {

      }
    });
  }

  public void addRequestToList(Request r) {
    for (int i = 0; i < room.getRequests().size(); i++) {
      if (room.getRequests().get(i).getQuery().equals(r.getQuery())) {
        room.getRequests().set(i, r);
      }
    }
    requestStrings.add(r.formattedString());
    listView.setAdapter(new ArrayAdapter<>(JoinActivity.this, android.R.layout.simple_list_item_1, requestStrings));
  }

  public void removeRequestFromList(Request r) {
    room.getRequests().remove(r);
    requestStrings.remove(r.formattedString());
    listView.setAdapter(new ArrayAdapter<>(JoinActivity.this, android.R.layout.simple_list_item_1, requestStrings));
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (room != null) {
      joinRoom(room.getCode());
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (room != null) {
      database.getReference("rooms/" + room.getCode() + "/requests").removeEventListener(listener);
      room.removePerson();
      room.syncToDatabase();
    }
  }
}