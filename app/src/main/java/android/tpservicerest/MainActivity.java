package android.tpservicerest;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_features2d;
import org.bytedeco.javacpp.opencv_nonfree;
import org.bytedeco.javacv.CanvasFrame;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.R.attr.maxHeight;
import static android.R.attr.maxWidth;
import static android.widget.ImageView.ScaleType;
import static org.bytedeco.javacpp.opencv_highgui.imread;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // defined at each class creation
    static String tag = MainActivity.class.getName();
    // Request Code of the Capture activity
    static int Capture_RequestCode = 1;
    // Request Code of the Library activity
    static int Library_RequestCode = 2;

    private Button btnAnalysis;
    private Button btnCapture;
    private Button btnLibrary;
    private ImageView imageCaptured;
    private TextView textViewJson;
    private String url = "http://www-rech.telecom-lille.fr/nonfreesift/";

    private RequestQueue mRequestQueue;

    //Brands variables
    private ListOfBrands allBrands;

    ScaleType SCALE_TYPE = ScaleType.CENTER;
    //VOCABULARY variables
    private File vocabulary;
    private opencv_core.Mat vocabularyMat;
    private opencv_nonfree.SIFT detector; //create SIFT feature point extracter
    private opencv_features2d.BOWImgDescriptorExtractor bowide;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnAnalysis = (Button) findViewById(R.id.btnAnalysis);
        btnCapture = (Button) findViewById(R.id.btnCapture);
        btnLibrary = (Button) findViewById(R.id.btnLibrary);
        imageCaptured = (ImageView) findViewById(R.id.imageCaptured);
        textViewJson = (TextView) findViewById(R.id.textViewJson);

        btnAnalysis.setOnClickListener(this);
        btnCapture.setOnClickListener(this);
        btnLibrary.setOnClickListener(this);


        mRequestQueue = Volley.newRequestQueue(getApplicationContext());

        allBrands = new ListOfBrands();

        downloadJsonFile(url+"index.json", mRequestQueue);
        downloadVocabulary(this,url+"vocabulary.yml",mRequestQueue);


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //Click on Capture Button
            case R.id.btnCapture:
                Intent mediaCapture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(mediaCapture, Capture_RequestCode);
                break;
            //Click on Library Button
            case R.id.btnLibrary:
                Intent mediaLibrary = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(mediaLibrary, Library_RequestCode);
                break;
            //Click on Analysis Button
            case R.id.btnAnalysis:

                //filePath = mImageUri.getPath();
                //Log.i(tag,this.filePath);
                analyse();

                break;
            default:
                break;
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
                        textViewJson.append("Nb brands loaded : " + allBrands.size());
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
                            //Upload vocabulary
                            vocabulary = File.createTempFile("vocabulary", ".yml", context.getCacheDir());
                            FileOutputStream outputStream = new FileOutputStream(vocabulary);
                            outputStream.write(response.getBytes());
                            outputStream.close();
                            if(vocabulary.exists()){
                                Log.i(tag, vocabulary.getPath()+" Saved, size=" + vocabulary.length());
                            }else {
                                Log.i(tag, vocabulary.getPath()+" error to save");
                            }
                            //set the vocabulary
                            setVocabulary();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(tag, "Vocabulary.yml Not Saved");
                    }
                }
        );
        queue.add(stringRequest);
    }

    protected void downloadImage(String brandToAnalyse){
        String  BrandUrl = url +"/train_images/"+brandToAnalyse+"_13.jpg" ;
        ImageRequest imageRequest = new ImageRequest(BrandUrl,
                new Response.Listener<Bitmap>(){
                    @Override
                    public void onResponse(Bitmap response) {
                        try{
                            imageCaptured.setImageBitmap(response);

                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                maxWidth,maxHeight,SCALE_TYPE, Bitmap.Config.RGB_565,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(tag,"Image Load Error");
                    }
                }

        );
    }

    /**
     * Set the vocabulary into a Mat opencv
     */
    protected void setVocabulary(){
        Log.i(tag,"read vocabulary from file... ");
        Loader.load(opencv_core.class);
        opencv_core.CvFileStorage storage = opencv_core.cvOpenFileStorage(vocabulary.getAbsolutePath(), null, opencv_core.CV_STORAGE_READ);
        Pointer p = opencv_core.cvReadByName(storage, null, "vocabulary", opencv_core.cvAttrList());
        opencv_core.CvMat cvMat = new opencv_core.CvMat(p);
        vocabularyMat = new opencv_core.Mat(cvMat);
        Log.i(tag,"vocabulary loaded " + vocabularyMat.rows() + " x " + vocabularyMat.cols());
        opencv_core.cvReleaseFileStorage(storage);

        // default parameters ""opencv2/features2d/features2d.hpp""
        detector = new opencv_nonfree.SIFT(0, 3, 0.04, 10, 1.6);

        //create a matcher with FlannBased Euclidien distance (possible also with BruteForce-Hamming)
        final opencv_features2d.FlannBasedMatcher matcher;
        matcher = new opencv_features2d.FlannBasedMatcher();

        //create BoF (or BoW) descriptor extractor

        bowide = new opencv_features2d.BOWImgDescriptorExtractor(detector.asDescriptorExtractor(), matcher);

        //Set the dictionary with the vocabulary we created in the first step
        bowide.setVocabulary(vocabularyMat);
        Log.i(tag,"Vocab is set");
    }


    protected void analyse(){

        opencv_core.Mat response_hist = new opencv_core.Mat();
        opencv_features2d.KeyPoint keypoints = new opencv_features2d.KeyPoint();
        opencv_core.Mat inputDescriptors = new opencv_core.Mat();

        //opencv_core.Mat imageTest = imread(this.filePath);
        File testFile = ToCache(this,"Data_BOW/TestImage/Pepsi_15.jpg","Pepsi_15.jpg");
        String path = testFile.getAbsolutePath();
        Log.i(tag,testFile.getAbsolutePath());
        opencv_core.Mat imageTest = imread(testFile.getAbsolutePath());
        detector.detectAndCompute(imageTest, opencv_core.Mat.EMPTY, keypoints, inputDescriptors);
        bowide.compute(imageTest, keypoints, response_hist);

        // Finding best match
        float minf = Float.MAX_VALUE;
        String bestMatch = null;

        long timePrediction = System.currentTimeMillis();
        // loop for all classes
        for (int i = 0; i < allBrands.size(); i++) {
            // classifier prediction based on reconstructed histogram
            float res = allBrands.get(i).get_classifierDesc().predict(response_hist, true);
            //System.out.println(class_names[i] + " is " + res);
            if (res < minf) {
                minf = res;
                bestMatch = allBrands.get(i).get_brandName();
            }
        }
        timePrediction = System.currentTimeMillis() - timePrediction;
        Log.i(tag,testFile.getName() + "  predicted as " + bestMatch + " in " + timePrediction + " ms");

        //Display image in ImageView
        downloadImage(bestMatch);
    }



    public static File ToCache(Context context, String Path, String fileName) {
        InputStream input;
        FileOutputStream output;
        byte[] buffer;
        String filePath = context.getCacheDir() + "/" + fileName;
        File file = new File(filePath);
        AssetManager assetManager = context.getAssets();

        try {
            input = assetManager.open(Path);
            buffer = new byte[input.available()];
            input.read(buffer);
            input.close();

            output = new FileOutputStream(filePath);
            output.write(buffer);
            output.close();
            return file;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
