package neko.testing.main;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "testing")
public class Datac {
    
    public Datac(String icid, int storeInt, String storeString) {
        this.icid = icid;
        this.storeInt = storeInt;
        this.storeString = storeString;
    }

    public Datac() {
        // empty contructor for...
    }

    @Id
    private String icid;
    private int storeInt;
    private String storeString;
    
    public String getIcid() {
        return icid;
    }
    
    public int getStoreInt() {
        return storeInt;
    }
    public void setStoreInt(int storeInt) {
        this.storeInt = storeInt;
    }
    public String getStoreString() {
        return storeString;
    }
    public void setStoreString(String storeString) {
        this.storeString = storeString;
    }
    
}
