package android.tpservicerest;

import java.io.File;

/**
 * Created by benjaminguilbert on 19/01/2017.
 */
public class Brand {

    private String _brandName;
    private String _url;
    private String _classifier;
    private String[] _images;

    //Constructor
    public Brand(String brandName, String url, String classifier, String[] images) {
        _brandName=brandName;
        _url=url;
        _classifier=classifier;
        _images=images;
    }

    public File downloadClassifierFile(){
        
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

    public String[] get_images() {
        return _images;
    }

    public void set_images(String[] _images) {
        this._images = _images;
    }
}
