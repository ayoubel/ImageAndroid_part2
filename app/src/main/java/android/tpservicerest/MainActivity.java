package android.tpservicerest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // defined at each class creation
    static String tag = MainActivity.class.getName();

    private Button btRequest;
    private TextView textViewJson;
    private String url = "http://www-rech.telecom-lille.fr/nonfreesift/index.json";

    private RequestQueue mRequestQueue;
    private List<Brand> allBrands;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btRequest = (Button) findViewById(R.id.btRequest);
        textViewJson = (TextView) findViewById(R.id.textViewJson);
        btRequest.setOnClickListener(this);


        mRequestQueue = Volley.newRequestQueue(getApplicationContext());

        allBrands = new ArrayList<Brand>();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btRequest:
                downloadJsonFile(url, mRequestQueue);
        }
    }

    protected void downloadJsonFile(String url, RequestQueue queue){

        //Create CallBack
        JsonRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray brands = response.getJSONArray("brands");
                            int nbBrands = brands.length();
                            for(int i=0; i<nbBrands;i++){
                                JSONObject jsonBrand = brands.getJSONObject(i);
                                Brand brand = new Brand();
                                brand.set_brandName(jsonBrand.getString("brandname"));
                                brand.set_url(jsonBrand.getString("url"));
                                brand.set_classifier(jsonBrand.getString("classifier"));
                                JSONArray images = jsonBrand.getJSONArray("images");
                                for(int j=0;j<images.length();j++){
                                    brand.addImage(images.getString(i));
                                }

                                allBrands.add(brand);
                                Log.i(tag,"brand : "+brand.get_brandName());
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        textViewJson.setText("Nombre de brand : " + allBrands.size());
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        textViewJson.setText("DID NOT WORK :/");
                    }
                }
        );
        queue.add(jsonRequest);
    }
}
