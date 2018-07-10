package net.tnose.app.trisquel;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by user on 2018/01/13.
 */

public class CameraSpec {
    public CameraSpec(int id, int type, String mount, String manufacturer, String modelName, int format,
                      int shutterSpeedGrainSize, Double fastestShutterSpeed, Double slowestShutterSpeed,
                      boolean bulbAvailable, String shutterSpeedSteps, int evGrainSize, int evWidth){
        this.id = id;
        this.type = type;
        this.created = new Date();
        this.lastModified = new Date();
        this.mount = mount;
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.format = format;
        this.shutterSpeedGrainSize = shutterSpeedGrainSize;
        this.fastestShutterSpeed = fastestShutterSpeed;
        this.slowestShutterSpeed = slowestShutterSpeed;
        this.bulbAvailable = bulbAvailable;
        if(shutterSpeedSteps.length() > 0) {
            String[] sssAsArray = shutterSpeedSteps.split(", ");
            ArrayList<Double> list = new ArrayList<Double>();
            for (String speed : sssAsArray) {
                list.add(Double.parseDouble(speed));
            }
            this.shutterSpeedSteps = list.toArray(new Double[list.size()]);
        }
        this.evGrainSize = evGrainSize > 3 ? 3 : (evGrainSize < 1 ? 1 : evGrainSize);
        this.evWidth = evWidth > 3 ? 3 : (evWidth < 1 ? 1 : evWidth);
    }
    public CameraSpec(int id, int type, String created, String lastModified, String mount, String manufacturer, String modelName, int format,
                       int shutterSpeedGrainSize, Double fastestShutterSpeed, Double slowestShutterSpeed,
                       boolean bulbAvailable, String shutterSpeedSteps, int evGrainSize, int evWidth){
        this.id = id;
        this.type = type;
        this.created = Util.stringToDateUTC(created);
        this.lastModified = Util.stringToDateUTC(lastModified);
        this.mount = mount;
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.format = format;
        this.shutterSpeedGrainSize = shutterSpeedGrainSize;
        this.fastestShutterSpeed = fastestShutterSpeed;
        this.slowestShutterSpeed = slowestShutterSpeed;
        this.bulbAvailable = bulbAvailable;
        if(shutterSpeedSteps.length() > 0) {
            String[] sssAsArray = shutterSpeedSteps.split(", ");
            ArrayList<Double> list = new ArrayList<Double>();
            for (String speed : sssAsArray) {
                try{
                    list.add(Double.parseDouble(speed));
                }catch(NumberFormatException e){
                }
            }
            this.shutterSpeedSteps = list.toArray(new Double[list.size()]);
        }
        this.evGrainSize = evGrainSize > 3 ? 3 : (evGrainSize < 1 ? 1 : evGrainSize);
        this.evWidth = evWidth > 3 ? 3 : (evWidth < 1 ? 1 : evWidth);
    }
    public int id;
    public int type;
    public Date created;
    public Date lastModified;
    public String mount;
    public String manufacturer;
    public String modelName;
    public int format;
    public int shutterSpeedGrainSize; //0: custom, 1: 一段, 2: 半段, 3: 1/3段
    public Double fastestShutterSpeed;
    public Double slowestShutterSpeed;
    public boolean bulbAvailable;
    public Double[] shutterSpeedSteps;
    public int evGrainSize; //1: 一段, 2: 半段, 3: 1/3段
    public int evWidth;  // 1～3まで

    public String toString(){
        return manufacturer + " "+ modelName;
    }
}
