package com.adrianrusso.gig.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adrianrusso.gig.Modules.Request;
import com.adrianrusso.gig.R;

import java.util.List;

public class RequestListAdapter extends ArrayAdapter<Request> {

  private final Context context;
  private final int resource;

  public RequestListAdapter(Context context, int resource, List<Request> objects) {
    super(context, resource, objects);
    this.context = context;
    this.resource = resource;
  }

  @NonNull
  @Override
  public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
    LayoutInflater inflater = LayoutInflater.from(context);

    if (getItem(position).formattedString().equals("")) {
      return inflater.inflate(R.layout.empty, parent, false);
    } else {
      convertView = inflater.inflate(resource, parent, false);
      ((TextView) convertView.findViewById(R.id.track)).setText(getItem(position).formattedString());
      ((TextView) convertView.findViewById(R.id.votes)).setText(String.valueOf(getItem(position).getVotes()));
      return convertView;
    }
  }
}
