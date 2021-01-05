package com.adrianrusso.partyplayer.Activites;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.adrianrusso.partyplayer.Adapters.RequestListAdapter;
import com.adrianrusso.partyplayer.Modules.Request;
import com.adrianrusso.partyplayer.Modules.Room;
import com.adrianrusso.partyplayer.R;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class JoinActivity extends AppCompatActivity {

  private static Room room;
  private static FirebaseDatabase database;
  private static AlertDialog sendRequestAlert, joinAlert;
  private static Map<String, Boolean> voted;

  private RequestListAdapter requestListAdapter;
  private ListView listView;
  private TextView size;
  private List<Request> requestsToBeShown;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_join);

    Button join = findViewById(R.id.join);
    Button send = findViewById(R.id.sendRequest);
    listView = findViewById(R.id.requestListJoin);
    size = findViewById(R.id.size);
    database = FirebaseDatabase.getInstance();
    if (voted == null)
      voted = new HashMap<>();
    sendRequestAlert = getSendRequestAlert();
    joinAlert = getJoinAlert();
    requestsToBeShown = new ArrayList<>();

    join.setOnClickListener(v -> joinAlert.show());
    send.setOnClickListener(v -> sendRequestAlert.show());

    listView.setOnItemClickListener((parent, view, position, id) -> {
      Request r = room.getRequests().get(position);
      if (voted.get(r.getQuery()) == null) {
        r.increaseVotes();
        room.syncToDatabase();
        voted.put(r.getQuery(), true);
      }
    });
  }

  private AlertDialog getJoinAlert() {
    AlertDialog.Builder alert = new AlertDialog.Builder(this);
    alert.setMessage("Enter room code:");
    final EditText input = new EditText(this);
    input.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(4)});
    input.setGravity(Gravity.CENTER);
    alert.setPositiveButton("Ok", (dialog, whichButton) -> {
      String enteredCode = input.getText().toString();
      if (room != null && enteredCode.equals(room.getCode())) {
        Toast.makeText(this, "Already in room.", Toast.LENGTH_SHORT).show();
      } else {
        joinRoom(enteredCode);
      }
    });

    AlertDialog a = alert.create();
    a.setView(input, 50, 0, 50, 0);
    return a;
  }


  private AlertDialog getSendRequestAlert() {
    AlertDialog.Builder alert = new AlertDialog.Builder(this);
    alert.setMessage("Enter request:");
    final EditText input = new EditText(this);
    input.setGravity(Gravity.CENTER);
    alert.setPositiveButton("Send", (dialog, whichButton) -> {
      if (room == null) {
        Toast.makeText(this, "Please join room.", Toast.LENGTH_SHORT).show();
      } else {
        database.getReference("rooms").addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(@NonNull DataSnapshot snapshot) {
            String query = input.getText().toString();
            input.setText("");
            if (snapshot.hasChild(room.getCode())) {
              for (DataSnapshot request : snapshot.child(room.getCode()).child("requests").getChildren()) {
                if (Objects.equals(request.child("query").getValue(String.class), query)) {
                  Toast.makeText(JoinActivity.this, "Request already exists.", Toast.LENGTH_SHORT).show();
                  return;
                }
              }
              room.addRequest(query);
              voted.put(query, true);
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

    AlertDialog a = alert.create();
    a.setView(input, 50, 0, 50, 0);
    return a;
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
      if (Objects.requireNonNull(r).getTrack() != null)
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

          requestsToBeShown = new ArrayList<>(room.getRequests());
          requestListAdapter = new RequestListAdapter(JoinActivity.this, R.layout.adapter_view_layout, requestsToBeShown);
          listView.setAdapter(requestListAdapter);

          database.getReference("rooms").child(room.getCode()).child("requests").addChildEventListener(listener);
          database.getReference("rooms").child(room.getCode()).child("size").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
              if (!snapshot.exists()) {
                room = null;
                size.setText("");
              } else {
                size.setText(String.format(Locale.US, "%d", snapshot.getValue(Integer.class)));
              }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
          });
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
    if (requestsToBeShown.contains(r)) {
      requestsToBeShown.set(requestsToBeShown.indexOf(r), r);
    } else {
      requestsToBeShown.add(r);
    }
    if (room.getRequests().contains(r)) {
      room.getRequests().set(room.getRequests().indexOf(r), r);
    } else {
      room.getRequests().add(r);
    }
    requestListAdapter.notifyDataSetChanged();
  }

  public void removeRequestFromList(Request r) {
    room.getRequests().remove(r);
    requestsToBeShown.remove(r);
    requestListAdapter.notifyDataSetChanged();
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (room != null) {
      joinRoom(room.getCode());
    } else {
      joinAlert.show();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (room != null) {
      database.getReference("rooms").child(room.getCode()).child("requests").removeEventListener(listener);
      room.removePerson();
      room.syncToDatabase();
    }
  }
}