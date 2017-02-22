package android.tpservicerest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.soundcloud.android.crop.Crop;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_features2d;
import org.bytedeco.javacpp.opencv_nonfree;
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

    ScaleType SCALE_TYPE = ScaleType.CENTER;

    //Contains true if this app can get back the index.json, else false
    private boolean connectionIsOk;

    private boolean analyseFinished;

    //display elements
    private Button btnAnalysis;
    private Button btnCapture;
    private Button btnLibrary;
    private ImageView imageCaptured;
    private TextView textViewJson;

    //request variables
    private String url = "http://www-rech.telecom-lille.fr/nonfreesift/";
    private RequestQueue mRequestQueue;

    //Image treatment variables
    private Uri mImageUri;
    private Uri mImageCroppedUri;
    private File captureFile;
    private String filePath;

    //Brands variables
    private ListOfBrands allBrands;
    private Brand bestBrandMatch;

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

        btnAnalysis.setEnabled(false);


        btnAnalysis.setOnClickListener(this);
        btnCapture.setOnClickListener(this);
        btnLibrary.setOnClickListener(this);
        imageCaptured.setOnClickListener(this);

        mRequestQueue = Volley.newRequestQueue(getApplicationContext());

        allBrands = new ListOfBrands();

        //Get all necessaries files from the web site
        downloadJsonFile(url+"index.json");
        downloadVocabulary(this,url+"vocabulary.yml");


    }

    @Override
    public void onClick(View v) {



        switch (v.getId()) {
            //Click on Capture Button
            case R.id.btnCapture:
                try {
                    captureFile  = File.createTempFile("capture",".jpg",this.getExternalFilesDir(Environment.DIRECTORY_PICTURES));
                    filePath = captureFile.getAbsolutePath();
                    mImageUri = FileProvider.getUriForFile(this,"tpservicerest.fileprovider",captureFile);
                    Intent mediaCapture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    mediaCapture.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
                    Log.i(tag, "Start capture image to "+filePath);
                    startActivityForResult(mediaCapture, Capture_RequestCode);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            //Click on Library Button
            case R.id.btnLibrary:
                Intent mediaLibrary = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(mediaLibrary, Library_RequestCode);
                break;
            //Click on image to crop it
            case R.id.imageCaptured:
                if(analyseFinished){
                    String url = bestBrandMatch.get_url();
                    Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse( url ) );
                    startActivity(intent);
                }else{
                    Uri destination = Uri.fromFile(new File(getCacheDir(), "cropped"));
                    Crop.of(mImageUri, destination).withMaxSize(300,300).start(this);
                }

                break;
            //Click on Analysis Button
            case R.id.btnAnalysis:
                analyse();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        analyseFinished = false;
        // if image is captured by the camera
        if(requestCode == Capture_RequestCode && resultCode == RESULT_OK){


            Log.i(tag, "Image captured in "+filePath);
            Bitmap bmp = BitmapFactory.decodeFile(filePath);

            imageCaptured.setImageBitmap(bmp);

            //allow analyse if there is an image and connection is ok
            if(connectionIsOk){
                btnAnalysis.setEnabled(true);
            }
        }else
        // if image is selected in the phone library
        if (requestCode == Library_RequestCode && resultCode == RESULT_OK){
            mImageUri = data.getData();

            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(mImageUri, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            filePath = cursor.getString(columnIndex);
            cursor.close();

            Log.i(tag,"Image selected from Library in " + filePath);

            imageCaptured.setImageURI(mImageUri);

            //allow analyse if there is an image and connection is ok
            if(connectionIsOk){
                btnAnalysis.setEnabled(true);
            }

        }else
        // if the image was cropped
        if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, data);
        }
    }


    /**
     * Download the index file from the web site
     * Create all brands and save them in app
     * @param url the absolute path of the web site
     */
    protected void downloadJsonFile(String url){
        final ProgressDialog progressDialogJson = ProgressDialog.show(this, "Loading Json File", "Loading. Please wait...", false);
        //Create CallBack
        JsonRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        connectionIsOk = true;
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
                                    Log.i(tag,"downloadJsonFile : brand : "+brand.get_brandName());
                                }
                                else {
                                    Log.i(tag,"downloadJsonFile : brand : "+brand.get_brandName() + " already created.");
                                }

                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        textViewJson.append("Nb brands loaded : " + allBrands.size());
                        progressDialogJson.dismiss();
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        connectionIsOk = false;
                        textViewJson.setText("No Connection.");
                        progressDialogJson.dismiss();
                    }
                }
        );
        mRequestQueue.add(jsonRequest);

    }

    /**
     * Download the vocabulary file from the web site
     * @param context context of this app
     * @param url the absolute path of the web site
     */
    protected void downloadVocabulary(final Context context, String url){
        final ProgressDialog progressDialogVoca = ProgressDialog.show(this, "Loading Vocabulary", "Loading. Please wait...", false);
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
                                Log.i(tag, vocabulary.getPath()+"downloadVocabulary : Saved, size=" + vocabulary.length());
                            }else {
                                Log.i(tag, vocabulary.getPath()+"downloadVocabulary :  error to save");
                            }
                            //set the vocabulary
                            setVocabulary();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        progressDialogVoca.dismiss();
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(tag, "downloadVocabulary : Vocabulary.yml Not Saved");
                        progressDialogVoca.dismiss();
                    }
                }
        );
        mRequestQueue.add(stringRequest);
    }


    /**
     * Download a brand's image from the web site
     * and display in this application
     * File must be in the folder "train-images/"
     * @param fileName of the file to download
     */
    protected void downloadImage(String fileName){
        String  BrandUrl = url +"train-images/"+fileName ;
        Log.i(tag, "DownLoad Image : "+BrandUrl);
        ImageRequest imageRequest = new ImageRequest(BrandUrl,
                new Response.Listener<Bitmap>(){
                    @Override
                    public void onResponse(Bitmap response) {
                        try{
                            imageCaptured.setImageBitmap(response);
                            Log.i(tag,"downloadImage : width = "+response.getWidth());


                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                maxWidth,maxHeight,SCALE_TYPE, Bitmap.Config.RGB_565,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(tag,"downloadImage : Image Load Error");
                    }
                }

        );
        mRequestQueue.add(imageRequest);
    }

    /**
     * Set the vocabulary into a descriptor
     */
    protected void setVocabulary(){
        Log.i(tag,"Read vocabulary from file... ");
        Loader.load(opencv_core.class);
        opencv_core.CvFileStorage storage = opencv_core.cvOpenFileStorage(vocabulary.getAbsolutePath(), null, opencv_core.CV_STORAGE_READ);
        Pointer p = opencv_core.cvReadByName(storage, null, "vocabulary", opencv_core.cvAttrList());
        opencv_core.CvMat cvMat = new opencv_core.CvMat(p);
        vocabularyMat = new opencv_core.Mat(cvMat);
        Log.i(tag,"Vocabulary loaded " + vocabularyMat.rows() + " x " + vocabularyMat.cols());
        opencv_core.cvReleaseFileStorage(storage);
    }


    /**
     * Analyse the image captured and display the result
     */
    protected void analyse(){
        final ProgressDialog progressDialogAnal = ProgressDialog.show(this, "Loading Vocabulary", "Loading. Please wait...", false);
        // default parameters ""opencv2/features2d/features2d.hpp""
        detector = new opencv_nonfree.SIFT(0, 3, 0.04, 10, 1.6);

        //create a matcher with FlannBased Euclidien distance (possible also with BruteForce-Hamming)
        final opencv_features2d.FlannBasedMatcher matcher;
        matcher = new opencv_features2d.FlannBasedMatcher();

        //create BoF (or BoW) descriptor extractor
        bowide = new opencv_features2d.BOWImgDescriptorExtractor(detector.asDescriptorExtractor(), matcher);

        //Set the dictionary with the vocabulary we created in the first step
        bowide.setVocabulary(vocabularyMat);
        Log.i(tag,"Vocabulary is set");

        opencv_core.Mat response_hist = new opencv_core.Mat();
        opencv_features2d.KeyPoint keypoints = new opencv_features2d.KeyPoint();
        opencv_core.Mat inputDescriptors = new opencv_core.Mat();

        //File testFile = ToCache(this,"Data_BOW/TestImage/Coca_15.jpg","Coca_15.jpg");
        //filePath = testFile.getAbsolutePath();
        Log.i(tag,filePath);
        opencv_core.Mat imageTest = imread(filePath,1);
        detector.detectAndCompute(imageTest, opencv_core.Mat.EMPTY, keypoints, inputDescriptors);
        bowide.compute(imageTest, keypoints, response_hist);

        // Finding best match
        float minf = Float.MAX_VALUE;
        bestBrandMatch = null;

        long timePrediction = System.currentTimeMillis();
        // loop for all classes
        for (int i = 0; i < allBrands.size(); i++) {
            // classifier prediction based on reconstructed histogram
            float res = allBrands.get(i).get_classifierDesc().predict(response_hist, true);
            Log.i(tag,"Compare File with brand "+allBrands.get(i).get_brandName()+ " -> res = "+res);
            if (res < minf) {
                minf = res;
                bestBrandMatch = allBrands.get(i);
            }
        }
        timePrediction = System.currentTimeMillis() - timePrediction;
        Log.i(tag,"File predicted as " + bestBrandMatch.get_brandName() + " in " + timePrediction + " ms");

        //Download end Display brand image result in ImageView
        downloadImage(bestBrandMatch.get_images().get(0));
        btnAnalysis.setEnabled(false);
        progressDialogAnal.dismiss();
        analyseFinished = true;
    }


    /**
     * Save in the application's cache a file from the assets folder
     *
     * @param context of the application
     * @param Path from the assets folder
     * @param fileName of the file to save
     * @return a file save in the cache
     */
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


    /**
     * Save the image cropped in the imageView
     *
     * @param resultCode of the crop activity
     * @param result of the crop activity
     */
    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            mImageCroppedUri = Crop.getOutput(result);
            filePath = mImageCroppedUri.getPath();
            imageCaptured.setImageURI(mImageCroppedUri);
            Log.i("Image cropped in ", Crop.getOutput(result).getPath());
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
