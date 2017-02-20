package android.tpservicerest;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.bytedeco.javacpp.opencv_ml;

import java.io.File;
import java.io.FileOutputStream;
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
    private File _classifierFile;
    private opencv_ml.CvSVM _classifierDesc;

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

    /**
     * Get the classifier file of this brand and save it in cache
     * open the file to write the resultant descriptor
     * @param context context of this app
     * @param baseUrl the absolute path of the web site
     * @param queue Queue that contains all callback
     */
    public void getClassifierFile(final Context context, String baseUrl, RequestQueue queue){

        final String fileName = this._classifier.substring(0,this._classifier.indexOf('.'));
        String url = baseUrl+"/classifiers/"+this._classifier;


        if( _classifierFile == null || !_classifierFile.exists()) {

            //Create CallBack
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                _classifierFile = File.createTempFile(fileName, ".xml", context.getCacheDir());
                                FileOutputStream outputStream = new FileOutputStream(_classifierFile);
                                outputStream.write(response.getBytes());
                                outputStream.close();
                                //write the resultant descriptor
                                _classifierDesc = new opencv_ml.CvSVM();
                                _classifierDesc.load(_classifierFile.getAbsolutePath());
                                if(_classifierFile.exists()){
                                    Log.i(tag, _classifierFile.getPath()+" Saved, size=" + _classifierFile.length());
                                }else {
                                    Log.i(tag, _classifierFile.getPath()+" error to save");
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.i(tag, _classifierFile.getPath()+" Not Saved");
                        }
                    }
            );
            queue.add(stringRequest);
        }else {
            Log.i(tag, _classifierFile.getPath()+" already Saved");
        }
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

    public opencv_ml.CvSVM get_classifierDesc() {
        return _classifierDesc;
    }

    public void set_classifierDesc(opencv_ml.CvSVM _classifierDesc) {
        this._classifierDesc = _classifierDesc;
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

    /**
     * Method to override equals
     * @param o Object
     * @return true if object's brandName is equal to this brandName
     */
    @Override
    public boolean equals(Object o){
        if(o instanceof Brand){
            Brand object = (Brand) o;
            if(this.get_brandName().equals(object.get_brandName())){
                return true;
            }
        }
        return false;
    }



}
