package com.example.aei2.appserver;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button btnGet;
    TextView textView;
    String url = "http://www-rech.telecom-lille.fr/nonfreesift";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGet = (Button) findViewById(R.id.button);
        textView = (TextView) findViewById(R.id.Display_Data);

        btnGet.setOnClickListener(this);


    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                DownloadFiles();

        }

    }

    private void DownloadFiles() {
        //Create Queue
        RequestQueue queue = Volley.newRequestQueue(this);
        //Create CallBack
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                textView.setText("Response is: " + response.substring(0, 500));
            }

        },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        textView.setText("DID NOT WORK :/");
                    }
                }
        );
        queue.add(stringRequest);
    }

}

