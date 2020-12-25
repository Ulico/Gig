package com.adrianrusso.partyplayer.Activites;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.adrianrusso.partyplayer.Modules.Room;
import com.adrianrusso.partyplayer.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class JoinActivity extends AppCompatActivity {

  private EditText codeEntry, requestEntry;
  private Room room;
  private FirebaseDatabase database;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_join);

    codeEntry = findViewById(R.id.codeEntry);
    requestEntry = findViewById(R.id.requestEntry);
    Button join = findViewById(R.id.join);
    Button send = findViewById(R.id.sendRequest);
    database = FirebaseDatabase.getInstance();

    join.setOnClickListener(v -> {
      String enteredCode = codeEntry.getText().toString();
      database.getReference("/rooms").addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
          if (snapshot.child(enteredCode).exists()) {
            room = snapshot.child(enteredCode).getValue(Room.class);
            Toast.makeText(JoinActivity.this, "Joined room.", Toast.LENGTH_SHORT).show();
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
        if (snapshot.child(room.getCode()).exists()) {
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
}