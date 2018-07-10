package net.tnose.app.trisquel;

/**
 * Created by user on 2018/02/09.
 */

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Util {
    static int TRISQUEL_VERSION = 1;

    /* シャッタースピードはたかだか2桁精度なのでdoubleからきれいに変換できる */
    static String doubleToStringShutterSpeed(double ss){
        double inv = 1.0 / ss;
        // inv = 3.0 つまり ss = 0.3333... = 1/3 で切りたいところだが0.3と1/3が前後するので余計な条件が入る
        if(inv >= 3.0 && inv - Math.floor(inv) < 0.1){
            return "1/" + (int)Math.floor(inv);
        }else{
            if(ss >= 4.0 && ss - Math.floor(ss) < 0.1){
                return Integer.toString((int)ss);
            }else {
                return Double.toString(ss);
            }
        }
    }

    static double stringToDoubleShutterSpeed(String ss){
        if(ss.isEmpty()){
            return 0.0;
        }else if(ss.indexOf("1/") < 0){ // 手抜き
            return Double.parseDouble(ss);
        }else{
            return 1.0 / (double)(Integer.parseInt(ss.substring(2)));
        }
    }

    static boolean stringIsZoom(String focalLength){
        return focalLength.indexOf("-") >= 0;
    }

    static Date stringToDateUTC(String s) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return sdf.parse(s);
        }catch (ParseException e){
            return new Date(0);
        }
    }

    static String dateToStringUTC(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(d);
    }

    public class ZoomRange{
        private int wideEnd;
        private int teleEnd;
        public ZoomRange(String s){
            if(stringIsZoom(s)){
                String[] ss = s.split("-");
                wideEnd = Integer.parseInt(ss[0]);
                teleEnd = Integer.parseInt(ss[1]);
            }else{
                wideEnd = Integer.parseInt(s);
                teleEnd = wideEnd;
            }
        }
        public ZoomRange(int w, int t){
            wideEnd = w;
            teleEnd = t;
        }
    }
}
