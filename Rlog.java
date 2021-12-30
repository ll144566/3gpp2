package com.quectel.jnitestexec.cdma2;

import android.util.Log;

public class Rlog {
    public static void v(String tag, String content){
        Log.d(tag,content);
    }
    public static void d(String tag, String content){Log.d("CdmaPduParase",content);}
    public static void e(String tag, String content){Log.d("CdmaPduParase",content);}
    public static void w(String tag, String content){Log.d("CdmaPduParase",content);}
    public static void pii(String tag, String content){}
}
