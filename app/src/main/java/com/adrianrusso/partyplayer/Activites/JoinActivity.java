package com.adrianrusso.partyplayer.Activites;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

  private EditText codeEntry, requestEntry;
  private Room room;
  private FirebaseDatabase database;
  private ListView listView;
  private static List<String> requestStrings;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_join);

    codeEntry = findViewById(R.id.codeEntry);
    requestEntry = findViewById(R.id.requestEntry);
    Button join = findViewById(R.id.join);
    Button send = findViewById(R.id.sendRequest);
    listView = findViewById(R.id.requestListJoin);
    database = FirebaseDatabase.getInstance();
    requestStrings = new ArrayList<>();

    join.setOnClickListener(v -> {
      String enteredCode = codeEntry.getText().toString();
      if (room != null && enteredCode.equals(room.getCode())) {
        Toast.makeText(this, "Already in room.", Toast.LENGTH_SHORT).show();
        return;
      }

      database.getReference("/rooms").addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
          if (snapshot.hasChild(enteredCode)) {
            Toast.makeText(JoinActivity.this, "Joined room.", Toast.LENGTH_SHORT).show();
            room = snapshot.child(enteredCode).getValue(Room.class);
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
    });

    send.setOnClickListener(v -> database.getReference("/rooms").addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot snapshot) {
        if (snapshot.hasChild(room.getCode())) {
          room.addRequest(requestEntry.getText().toString());
        } else {
          Toast.makeText(JoinActivity.this, "Room not found.", Toast.LENGTH_SHORT).show();
        }
      }

      @Override
      public void onCancelled(@NonNull DatabaseError error) {

      }
    }));
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
      removeRequestFromList(snapshot.getValue(Request.class));
    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {

    }
  };

  public void addRequestToList(Request r) {
    room.getRequests().add(r);
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
      database.getReference("rooms/" + room.getCode() + "/requests").addChildEventListener(listener);
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (room != null) {
      database.getReference("rooms/" + room.getCode() + "/requests").removeEventListener(listener);
    }
  }
}