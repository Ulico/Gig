package com.adrianrusso.gig.Activites;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.adrianrusso.gig.Adapters.RequestListAdapter;
import com.adrianrusso.gig.Modules.Request;
import com.adrianrusso.gig.Modules.Room;
import com.adrianrusso.gig.R;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TracksPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class HostActivity extends AppCompatActivity {

  private static final String REDIRECT_URI = "https://localhost:8080";
  private static final int REQUEST_CODE = 1337;

  private static String clientId;
  private static SpotifyAppRemote mSpotifyAppRemote;
  private static Room room;
  private static SpotifyApi api;
  private static boolean keepRoom, apiConnected;
  private static SharedPreferences prefs;

  private TextView size;
  private RequestListAdapter requestListAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_host);

    clientId = getClientId();
    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    if (api == null)
      api = new SpotifyApi();
    if (room == null)
      room = Room.newRoom(prefs);
    size = findViewById(R.id.size);
    keepRoom = false;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    requestListAdapter = new RequestListAdapter(this, R.layout.adapter_view_layout, room.getRequests());

    size.setText("1");

    ListView listView = findViewById(R.id.requestListHost);
    TextView roomCode = findViewById(R.id.roomCode);

    listView.setAdapter(requestListAdapter);

    if (!apiConnected)
      AuthorizationClient.openLoginActivity(this, REQUEST_CODE, new AuthorizationRequest.Builder(clientId, AuthorizationResponse.Type.TOKEN, REDIRECT_URI).build());

    String stringText = "Room Code: " + room.getCode();
    roomCode.setText(stringText);

    database.getReference("rooms").child(room.getCode()).child("requests").addChildEventListener(listener);

    listView.setOnItemClickListener((parent, view, position, id) -> playOrCue(room.getRequests().get(position).getTrack()));

    database.getReference("rooms").child(room.getCode()).child("size").addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot snapshot) {
        try {
          room.setSize(snapshot.getValue(Integer.class));
        } catch (NullPointerException ignored) {

        }
        size.setText(String.format(Locale.US, "%d", snapshot.getValue(Integer.class)));
      }

      @Override
      public void onCancelled(@NonNull DatabaseError error) {

      }
    });

    SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = (sharedPreferences, key) -> {
      if (key.equals("votePercentToPlay")) {
        int v = 0;
        try {
          v = Integer.parseInt(sharedPreferences.getString("votePercentToPlay", "50"));
          room.setVotePercentToPlay(v / 100.0);
          room.syncToDatabase();
        } catch (NumberFormatException e) {
          Toast.makeText(this, "Please enter a valid percent.", Toast.LENGTH_SHORT).show();
          sharedPreferences.edit().putString("votePercentToPlay", (int) (100 * room.getVotePercentToPlay()) + "").apply();
        }
      }
    };

    prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
  }


  public String getClientId() {
    try {
      InputStreamReader inputStreamReader = new InputStreamReader(getAssets().open("client_id.txt"), StandardCharsets.UTF_8);
      BufferedReader reader = new BufferedReader(inputStreamReader);
      return reader.readLine();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public void addRequestToList(Request r) {
    room.getRequests().add(r);
    requestListAdapter.notifyDataSetChanged();
  }

  public void removeRequestFromList(Request r) {
    room.getRequests().remove(r);
    requestListAdapter.notifyDataSetChanged();
  }

  @Override
  protected void onStart() {
    super.onStart();
    SpotifyAppRemote.connect(this, new ConnectionParams.Builder(clientId).setRedirectUri(REDIRECT_URI).showAuthView(true).build(), new Connector.ConnectionListener() {

      @Override
      public void onConnected(SpotifyAppRemote spotifyAppRemote) {
        mSpotifyAppRemote = spotifyAppRemote;
      }

      @Override
      public void onFailure(Throwable throwable) {
        Toast.makeText(HostActivity.this, "WARNING: Spotify is not installed on this device.", Toast.LENGTH_SHORT).show();
      }
    });
  }

  ChildEventListener listener = new ChildEventListener() {
    @Override
    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
      Log.d("mine", "addied");
      Request r = Objects.requireNonNull(snapshot.getValue(Request.class));
      if (r.getTrack() == null) {
        api.getService().searchTracks(r.getQuery(), new Callback<TracksPager>() {
          @Override
          public void success(TracksPager tracksPager, Response response) {
            Track track = tracksPager.tracks.items.get(0);
            r.setTrack(track);
            for (Request r : room.getRequests()) {
              if (r.getTrack().name.equals(track.name)) {
                snapshot.getRef().removeValue();
                return;
              }
            }
            addRequestToList(r);

            snapshot.child("track").getRef().setValue(track);
          }

          @Override
          public void failure(RetrofitError error) {
          }
        });
      } else {
        requestListAdapter.notifyDataSetChanged();
      }
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
      Request r = snapshot.getValue(Request.class);
      if (room.getRequests().contains(r)) {
        room.getRequests().set(room.getRequests().indexOf(r), r);
        if (r.getVotes() >= room.getVotePercentToPlay() * room.getSize()) {
          playOrCue(r.getTrack());
        }
        requestListAdapter.notifyDataSetChanged();
      }
    }

    @Override
    public void onChildRemoved(@NonNull DataSnapshot snapshot) {
      Log.d("mine", "removed");
      Request r = snapshot.getValue(Request.class);
      assert r != null;
      if (r.getTrack() != null)
        removeRequestFromList(r);
    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {
    }
  };

  private void playOrCue(Track track) {
    mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
      try {
        if (playerState.isPaused) {
          mSpotifyAppRemote.getPlayerApi().play("spotify:track:" + track.id);
        } else {
          mSpotifyAppRemote.getPlayerApi().queue("spotify:track:" + track.id);
        }
      } catch (NullPointerException e) {
        Toast.makeText(HostActivity.this, "Failed to play: Spotify cannot be found on this device.", Toast.LENGTH_SHORT).show();
      }
    });
  }


  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == REQUEST_CODE) {
      AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);
      switch (response.getType()) {
        case TOKEN:
          api.setAccessToken(response.getAccessToken());
          apiConnected = true;
          break;
        case ERROR:
          break;
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.mymenu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.mybutton) {
      keepRoom = true;
      startActivity(new Intent(HostActivity.this, SettingsActivity.class));
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onRestart() {
    super.onRestart();
    room.syncToDatabase();
  }

  @Override
  protected void onStop() {
    super.onStop();
    FirebaseDatabase.getInstance().getReference("rooms").child(room.getCode()).child("requests").removeEventListener(listener);
    if (!keepRoom) {
      room.destroy();
      room = null;
    }
  }
}