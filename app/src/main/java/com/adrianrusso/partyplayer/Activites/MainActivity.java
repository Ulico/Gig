package com.adrianrusso.partyplayer.Activites;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.adrianrusso.partyplayer.R;

public class MainActivity extends AppCompatActivity {


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.host).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HostActivity.class)));
    findViewById(R.id.join).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, JoinActivity.class)));
  }
}