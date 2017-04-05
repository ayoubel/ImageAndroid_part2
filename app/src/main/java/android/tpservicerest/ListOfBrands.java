package android.tpservicerest;

import java.util.ArrayList;

/**
 * Created by benjaminguilbert on 07/02/2017.
 * To override the contains method with specific comportment
 */
public class ListOfBrands extends ArrayList<Brand> {

    /**
     * Search if this contains already a Brand with the same name of o.
     * @param o Object
     * @return true if it contains a brand with the same name, else false
     */
    @Override
    public boolean contains(Object o) {
        for (int i = 0; i < this.size(); i++) {
            if (this.get(i).equals(o)) {
                return true;
            }
        }
        return false;
    }
}
