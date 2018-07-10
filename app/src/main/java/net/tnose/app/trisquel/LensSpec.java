package net.tnose.app.trisquel;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by user on 2018/01/13.
 */

public class LensSpec {
    public LensSpec(int id, String mount, int body, String manufacturer,
                    String modelName, String focalLength, String fSteps){
        this.id = id;
        this.created = new Date();
        this.lastModified = new Date();
        this.mount = mount;
        this.body = body;
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.focalLength = focalLength;
        if(fSteps.length() > 0) {
            String[] fsAsArray = fSteps.split(", ");
            ArrayList<Double> list = new ArrayList<Double>();
            for (String speed : fsAsArray) {
                list.add(Double.parseDouble(speed));
            }
            this.fSteps = list.toArray(new Double[list.size()]);
        }
    }
    public LensSpec(int id, String created, String lastModified, String mount, int body, String manufacturer,
                    String modelName, String focalLength, String fSteps){
        this.id = id;
        this.created = Util.stringToDateUTC(created);
        this.lastModified = Util.stringToDateUTC(lastModified);
        this.mount = mount;
        this.body = body;
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.focalLength = focalLength;
        if(fSteps.length() > 0) {
            String[] fsAsArray = fSteps.split(", ");
            ArrayList<Double> list = new ArrayList<Double>();
            for (String speed : fsAsArray) {
                list.add(Double.parseDouble(speed.replace("[","").replace("]","")));
            }
            this.fSteps = list.toArray(new Double[list.size()]);
        }
    }
    public int id;
    public int body;
    public Date created;
    public Date lastModified;
    public String mount;
    public String manufacturer;
    public String modelName;
    public String focalLength;
    public double getMinFocalLength() {
        return 0.0;
    }
    public double getMaxFocalLength() {
        return 0.0;
    }
    public Double[] fSteps;

    @Override
    public String toString(){
        return modelName; /* なんかMaterialBetterSpinnerがtoStringを呼んでるみたいなので、一時的にモデル名にする */
    }
}
