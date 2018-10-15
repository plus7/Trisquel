package net.tnose.app.trisquel;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by user on 2018/01/13.
 */

public class Photo {
    public Photo(int id, int filmrollid, int index, String date, int cameraid, int lensid,
                 double focalLength, double aperture, double shutterSpeed, double expCompensation,
                 double ttlLightMeter, String location, double latitude, double longitude, String memo, String accessories){
        Log.d("new Photo",
                   "id:" + Integer.toString(id) + ", " +
                        "filmroll:" + Integer.toString(filmrollid) + ", " +
                           "index:" + Integer.toString(index) + ", " +
                           "date:" + date + ", "+
                           "lensid:" + Integer.toString(lensid));

        this.id = id;
        this.filmrollid = filmrollid;
        this.index = index; //内部的にはゼロオリジンで管理する
        this.date = date;
        this.lensid = lensid;
        this.cameraid = cameraid;
        this.focalLength = focalLength;
        this.aperture = aperture;
        this.shutterSpeed = shutterSpeed;
        this.expCompensation = expCompensation;
        this.ttlLightMeter = ttlLightMeter;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.memo = memo;
        this.accessories = new ArrayList<>();
        for(String idStr: accessories.split("/")){
            if(!idStr.isEmpty()) this.accessories.add(Integer.parseInt(idStr));
        }
    }
    public int id;
    public int filmrollid;
    public int index;
    public String date;
    public int lensid;
    public int cameraid;
    public double focalLength;
    public double aperture;
    public double shutterSpeed;
    public double expCompensation;
    public double ttlLightMeter;
    public String location;
    public double latitude;
    public double longitude;
    public String memo;
    public ArrayList<Integer> accessories;

    public boolean isValidLatLng(){
        return (latitude <= 90 && latitude >= -90 && longitude <= 180 && longitude >= -180);
    }

    public String getAccessoriesStr(){
        StringBuilder sb = new StringBuilder("/");
        for (Integer a: accessories) {
            sb.append(a);
            sb.append('/');
        }
        return sb.toString();
    }
}
