package net.tnose.app.trisquel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by user on 2018/01/13.
 */

public class FilmRoll {
    public FilmRoll(int id, String name, CameraSpec camera, String manufacturer, String brand, int iso, int exposures) {
        this.id = id;
        this.name = name;
        this.created = new Date();
        this.lastModified = new Date();
        this.camera = camera;
        //this.format = camera.format;
        this.manufacturer = manufacturer;
        this.brand = brand;
        this.iso = iso;
        this.photos = new ArrayList<Photo>();
    }
    public FilmRoll(int id, String name, String created, String lastModified, CameraSpec camera, String manufacturer, String brand, int iso, int exposures) {
        this.id = id;
        this.name = name;
        this.created = Util.stringToDateUTC(created);
        this.lastModified = Util.stringToDateUTC(lastModified);
        this.camera = camera;
        //this.format = camera.format;
        this.manufacturer = manufacturer;
        this.brand = brand;
        this.iso = iso;
        this.photos = new ArrayList<Photo>();
    }
    public FilmRoll(int id, String name, String created, String lastModified, CameraSpec camera,
                    String manufacturer, String brand, int iso, int exposures, ArrayList<Photo> photos) {
        this.id = id;
        this.name = name;
        this.created = Util.stringToDateUTC(created);
        this.lastModified = Util.stringToDateUTC(lastModified);
        this.camera = camera;
        //this.format = camera.format;
        this.manufacturer = manufacturer;
        this.brand = brand;
        this.iso = iso;
        this.photos = photos;
    }
    public int id;
    public String name;
    public Date created;
    public Date lastModified;
    //public int format;
    public String manufacturer;
    public String brand;
    public int iso;
    public int cameraid;
    public CameraSpec camera;
    public List<Photo> photos;

    public String getDateRange(){
        if(photos.size() == 0) return "";

        Date minDate = new Date(Long.MAX_VALUE);
        Date maxDate = new Date(0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (Photo p: photos) {
            Date d = new Date(0);
            try {
                d = sdf.parse(p.date);
            }catch(ParseException e){

            }
            if(minDate.after(d)){
                minDate = d;
            }
            if(maxDate.before(d)){
                maxDate = d;
            }
        }

        if(minDate.equals(maxDate)){
            return sdf.format(minDate);
        }else {
            return sdf.format(minDate) + "-" + sdf.format(maxDate);
        }
    }
    public int getExposures(){
        return photos.size();
    }
}
