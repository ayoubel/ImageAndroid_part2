package android.tpservicerest;

import android.content.Context;
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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // defined at each class creation
    static String tag = MainActivity.class.getName();

    private Button btRequest;
    private TextView textViewJson;
    private String url = "http://www-rech.telecom-lille.fr/nonfreesift/";

    private RequestQueue mRequestQueue;
    private ListOfBrands allBrands;
    private File vocabulary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btRequest = (Button) findViewById(R.id.btRequest);
        textViewJson = (TextView) findViewById(R.id.textViewJson);
        btRequest.setOnClickListener(this);


        mRequestQueue = Volley.newRequestQueue(getApplicationContext());

        allBrands = new ListOfBrands();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btRequest:
                downloadJsonFile(url+"index.json", mRequestQueue);
                downloadVocabulary(this,url+"vocabulary.yml",mRequestQueue);
                textViewJson.setText(" Nombre de brand : " + allBrands.size());


        }
    }


    /**
     * Download the index file from the web site
     * Create all brands and save them in app
     * @param url the absolute path of the web site
     * @param queue Queue that contains all callback
     */
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

                                if(!allBrands.contains(brand)){
                                    brand.set_url(jsonBrand.getString("url"));
                                    brand.set_classifier(jsonBrand.getString("classifier"));
                                    JSONArray images = jsonBrand.getJSONArray("images");
                                    for(int j=0;j<images.length();j++){
                                        brand.addImage(images.getString(i));
                                    }
                                    brand.getClassifierFile(MainActivity.this,"http://www-rech.telecom-lille.fr/nonfreesift",mRequestQueue);
                                    allBrands.add(brand);
                                    Log.i(tag,"brand : "+brand.get_brandName());
                                }
                                else {
                                    Log.i(tag,"brand : "+brand.get_brandName() + " already created.");
                                }

                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        textViewJson.append("Nombre de brand : " + allBrands.size());
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

    /**
     * Download the vocabulary file from the web site
     * @param context context of this app
     * @param url the absolute path of the web site
     * @param queue Queue that contains all callback
     */
    protected void downloadVocabulary(final Context context, String url, RequestQueue queue){
        //Create CallBack
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            vocabulary = File.createTempFile("vocabulary", ".yml", context.getCacheDir());
                            FileOutputStream outputStream = new FileOutputStream(vocabulary);
                            outputStream.write(response.getBytes());
                            outputStream.close();
                            if(vocabulary.exists()){
                                Log.i(tag, vocabulary.getPath()+" Saved, size=" + vocabulary.length());
                            }else {
                                Log.i(tag, vocabulary.getPath()+" error to save");
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(tag, vocabulary.getPath()+" Not Saved");
                    }
                }
        );
        queue.add(stringRequest);
    }


}
