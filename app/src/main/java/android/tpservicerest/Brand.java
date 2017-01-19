package android.tpservicerest;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by benjaminguilbert on 19/01/2017.
 */
public class Brand {

    // defined at each class creation
    static String tag = MainActivity.class.getName();

    private String _brandName;
    private String _url;
    private String _classifier;
    private List<String> _images;

    //Constructor
    public Brand(){
        this._images = new ArrayList<String>();
    }
    public Brand(String brandName, String url, String classifier, List<String> images) {
        _brandName=brandName;
        _url=url;
        _classifier=classifier;
        _images=images;
    }

    public String getClassifierFile(Context context, String baseUrl, RequestQueue queue){

        final String pathFile = context.getCacheDir()+ this._classifier;
        String url = baseUrl+"/classifiers/"+this._classifier;

        final File classifierFile = new File(pathFile);

        if(!classifierFile.exists()) {

            //Create CallBack
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                classifierFile.mkdirs();
                                FileWriter writer = new FileWriter(classifierFile);
                                writer.append(response);
                                writer.flush();
                                writer.close();
                                Log.i(tag, pathFile+" Saved");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.i(tag, pathFile+" Not Saved");
                        }
                    }
            );
            queue.add(stringRequest);
        }

        return pathFile;
    }

    public String get_brandName() {
        return _brandName;
    }

    public void set_brandName(String _brandName) {
        this._brandName = _brandName;
    }

    public String get_url() {
        return _url;
    }

    public void set_url(String _url) {
        this._url = _url;
    }

    public String get_classifier() {
        return _classifier;
    }

    public void set_classifier(String _classifier) {
        this._classifier = _classifier;
    }

    public List<String> get_images() {
        return _images;
    }

    public void set_images(List<String> _images) {
        this._images = _images;
    }

    public void addImage(String image){
        this._images.add(image);
    }
}
