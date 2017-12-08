package com.skyia.coolweather.util;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.skyia.coolweather.db.City;
import com.skyia.coolweather.db.County;
import com.skyia.coolweather.db.Province;
import com.skyia.coolweather.gson.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Skyia_ccf on 2017/12/7.
 */

public class Utility {
//    解析和处理服务器返回的省级数据
public static boolean handleProvinceResponse(String response) {
    if (!TextUtils.isEmpty(response)) {
        try {
            JSONArray allProvinces = new JSONArray(response);
            for (int i = 0; i < allProvinces.length(); i++) {
                JSONObject object = allProvinces.getJSONObject(i);
                Province province = new Province();
                province.setProvinceCode(object.getInt("id"));
                province.setProvinceName(object.getString("name"));
                province.save();
            }
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    } else return false;
}

    public static boolean handleCityResponse(String response, int provinceId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCity = new JSONArray(response);
                for (int i = 0; i < allCity.length(); i++) {
                    JSONObject object = allCity.getJSONObject(i);
                    City city = new City();
                    city.setCityName(object.getString("name"));
                    city.setCityCode(object.getInt("id"));
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
        } else return false;
    }

    public static boolean handleCountyResponse(String response, int cityId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCounties = new JSONArray(response);
                for (int i = 0; i < allCounties.length(); i++) {
                    JSONObject object = allCounties.getJSONObject(i);
                    County county = new County();
                    county.setCountyName(object.getString("name"));
                    county.setWeatherId(object.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
        } else return false;
    }

    public static Weather handleWeatherResponse(String response){
    try {
        JSONObject jsonObject = new JSONObject(response);
        JSONArray jsonArray = jsonObject.getJSONArray("HeWeather");
        String weatherContent = jsonArray.getJSONObject(0).toString();
        return new Gson().fromJson(weatherContent,Weather.class);
    } catch (JSONException e) {
        e.printStackTrace();
    }
    return null;
    }
}
