package android.tpservicerest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

    private Button btRequest;
    private TextView textViewJson;
    private String url = "http://www-rech.telecom-lille.fr/nonfreesift/index.json";

    private RequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btRequest = (Button) findViewById(R.id.btRequest);
        textViewJson = (TextView) findViewById(R.id.textViewJson);
        btRequest.setOnClickListener(this);


        mRequestQueue = Volley.newRequestQueue(getApplicationContext());

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btRequest:
                downloadFile(url, mRequestQueue);
        }
    }

    protected void downloadFile(String url, RequestQueue queue){

        //Create CallBack
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        textViewJson.setText("Response is: " + response.substring(0, 500));
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        textViewJson.setText("DID NOT WORK :/");
                    }
                }
        );
        queue.add(stringRequest);
    }
}
