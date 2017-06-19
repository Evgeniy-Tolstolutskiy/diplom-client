package com.diplom.routeoptimization.util;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by Евгений on 01.06.2017.
 */

public class Util {

    private static String city = "Kharkiv";
    private static String transport = "subway";

    public static String getProperty(String key, Context context) {
        try {
            Properties properties = new Properties();
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
            return properties.getProperty(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getCity() {
        return city;
    }

    public static String getTransport() {
        return transport;
    }

    public static void setCity(String city) {
        if (city.equals("Харьков")) {
            Util.city = "Kharkiv";
        }
    }

    public static void setTransport(String transport) {
        if (transport.equals("метро")) {
            Util.transport = "subway";
        }
        if (transport.equals("автобус")) {
            Util.transport = "bus";
        }
    }
}
