package com.adrianrusso.partyplayer.Activites;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.adrianrusso.partyplayer.Modules.Request;
import com.adrianrusso.partyplayer.Modules.Room;
import com.adrianrusso.partyplayer.R;
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
  private static String clientId;
  private static final int REQUEST_CODE = 1337;

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
    requestStrings = new ArrayList<>();
    listView = findViewById(R.id.requestList);

    updateAccessToken();

    String stringText = "Room Code: " + room.getCode();
    ((TextView) findViewById(R.id.roomCode)).setText(stringText);

    FirebaseDatabase.getInstance().getReference("rooms/" + room.getCode()).addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot snapshot) {
        if (snapshot.exists()) {
          room = snapshot.getValue(Room.class);
          for (int i = 0; i < Objects.requireNonNull(room).getRequests().size(); i++) {
            Request r = room.getRequests().get(i);
            if (r.getTrack() == null) {
              int finalI = i;
              api.getService().searchTracks(r.getQuery(), new Callback<TracksPager>() {
                @Override
                public void success(TracksPager tracksPager, Response response) {
                  Track track = tracksPager.tracks.items.get(0);
                  r.setTrack(track);
                  requestStrings.add(r.formattedString());
                  snapshot.child("/requests/" + finalI + "/track").getRef().setValue(track);
                  listView.setAdapter(new ArrayAdapter<>(HostActivity.this, android.R.layout.simple_list_item_1, requestStrings));
                }

                @Override
                public void failure(RetrofitError error) {
                }
              });
            }
          }
        }
      }

      @Override
      public void onCancelled(@NonNull DatabaseError error) {

      }
    });

    listView.setOnItemClickListener((parent, view, position, id) -> mSpotifyAppRemote.getPlayerApi().play("spotify:track:" + room.getRequests().get(position).getTrack().id));
  }

  public String getClientId() {
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(getAssets().open("client_id.txt"), StandardCharsets.UTF_8))) {

      return reader.readLine();
    } catch (IOException e) {
      return null;
    }
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
          Log.d("mine", "here");
          api.setAccessToken(response.getAccessToken());
          break;
        case ERROR:
          Log.d("mine", "her2e");
          break;
      }
    }
  }

  @Override
  protected void onStop() {
    room.destroy();
    super.onStop();
  }
}