package com.adrianrusso.partyplayer.Activites;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
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
import java.util.ArrayList;
import java.util.List;
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
  private static List<String> requestStrings;
  private static SpotifyApi api;

  private ListView listView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_host);

    clientId = getClientId();
    api = new SpotifyApi();
    room = Room.newRoom();
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    requestStrings = new ArrayList<>();

    listView = findViewById(R.id.requestListHost);
    TextView roomCode = findViewById(R.id.roomCode);

    updateAccessToken();

    String stringText = "Room Code: " + room.getCode();
    roomCode.setText(stringText);

    database.getReference("rooms/" + room.getCode() + "/requests").addChildEventListener(new ChildEventListener() {
      @Override
      public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
        Request r = Objects.requireNonNull(snapshot.getValue(Request.class));
        if (r.getTrack() == null) {
          api.getService().searchTracks(r.getQuery(), new Callback<TracksPager>() {
            @Override
            public void success(TracksPager tracksPager, Response response) {
              Track track = tracksPager.tracks.items.get(0);
              r.setTrack(track);
              addRequestToList(r);
              snapshot.child("/track").getRef().setValue(track);
            }

            @Override
            public void failure(RetrofitError error) {
            }
          });
        } else {
          requestStrings.add(r.formattedString());
          listView.setAdapter(new ArrayAdapter<>(HostActivity.this, android.R.layout.simple_list_item_1, requestStrings));
        }
      }

      @Override
      public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

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
    });
    listView.setOnItemClickListener((parent, view, position, id) -> {
      try {
        mSpotifyAppRemote.getPlayerApi().play("spotify:track:" + room.getRequests().get(position).getTrack().id);
      } catch (NullPointerException e) {
        Toast.makeText(HostActivity.this, "Failed to play: Spotify cannot be found on this device.", Toast.LENGTH_SHORT).show();
      }
    });
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
    requestStrings.add(r.formattedString());
    listView.setAdapter(new ArrayAdapter<>(HostActivity.this, android.R.layout.simple_list_item_1, requestStrings));
  }

  public void removeRequestFromList(Request r) {
    room.getRequests().remove(r);
    requestStrings.remove(r.formattedString());
    listView.setAdapter(new ArrayAdapter<>(HostActivity.this, android.R.layout.simple_list_item_1, requestStrings));
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

  public void updateAccessToken() {
    AuthorizationClient.openLoginActivity(this, REQUEST_CODE, new AuthorizationRequest.Builder(clientId, AuthorizationResponse.Type.TOKEN, REDIRECT_URI).build());
  }

  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == REQUEST_CODE) {
      AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);
      switch (response.getType()) {
        case TOKEN:
          api.setAccessToken(response.getAccessToken());
          break;
        case ERROR:
          break;
      }
    }
  }

  @Override
  protected void onRestart() {
    super.onRestart();
    room.syncToDatabase();
  }

  @Override
  protected void onStop() {
    super.onStop();
    room.destroy();
  }
}