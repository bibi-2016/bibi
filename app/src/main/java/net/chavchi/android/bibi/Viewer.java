package net.chavchi.android.bibi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.health.PackageHealthStats;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;


class MastersAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater inflater;
    private BcCommunicator communicator = null;

    MastersAdapter(Context con, BcCommunicator com) {
        context = con;
        communicator = com;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return communicator.tcp_masters.size() + communicator.cloud_masters.size();
    }

    @Override
    public Object getItem(int i) {
        if (i < communicator.tcp_masters.size()) {
            return communicator.tcp_masters.get(i);
        } else {
            return communicator.cloud_masters.get(i - communicator.tcp_masters.size());
        }
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View row = inflater.inflate(R.layout.view_masteradapter, viewGroup, false);

        final int tcp_masters_count = communicator.tcp_masters.size();
        final String full_name    = (i < tcp_masters_count ? communicator.tcp_masters.get(i).full_name    : communicator.cloud_masters.get(i - tcp_masters_count).full_name);
        final String name         = (i < tcp_masters_count ? communicator.tcp_masters.get(i).name         : communicator.cloud_masters.get(i - tcp_masters_count).name);
        final String event_status = (i < tcp_masters_count ? communicator.tcp_masters.get(i).events_status : communicator.cloud_masters.get(i - tcp_masters_count).events_status);

        ((TextView) row.findViewById(R.id.textView_masteradapter_fullname)).setText(full_name);
        ((TextView) row.findViewById(R.id.textView_masteradapter_name)).setText(name);
        ((TextView) row.findViewById(R.id.textView_masteradapter_status)).setText(event_status);

        return row;
    }
}


public class Viewer extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private MastersAdapter mastersAdapter = null;
    private ListView listView = null;
    //
    // activity life cycle
    //
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BOut.trace("Viewer::onCreate");

        // create user interface
        setContentView(R.layout.activity_viewer);
        // create a toolbar
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar_viewer));
        // enable the back button on toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mastersAdapter = new MastersAdapter(this, ((Bibi)getApplicationContext()).viewer.communicator);
        listView = (ListView)findViewById(R.id.listView_recorders);
        listView.setAdapter(mastersAdapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        BOut.println("Client::onResume");

        ((Bibi)getApplicationContext()).viewer.setMastersAdapter(mastersAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        BOut.println("Client::onPause");

        ((Bibi)getApplicationContext()).viewer.setMastersAdapter(null);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String full_name = ((TextView)view.findViewById(R.id.textView_masteradapter_fullname)).getText().toString();

        ((Bibi)getApplicationContext()).monitorMaster(full_name);
        startActivity(new Intent(this, Monitor.class));
    }

/*
    @Override
    public void onRestart() {
        super.onRestart();
        BOut.println("Client::onRestart");
    }

    @Override
    public void onStart() {
        super.onStart();
        BOut.println("Client::onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        BOut.println("Client::onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BOut.println("Client::onDestroy");
    }
*/
}