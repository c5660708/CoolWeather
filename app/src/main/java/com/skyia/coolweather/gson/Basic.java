package com.skyia.coolweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Skyia_ccf on 2017/12/7.
 */

public class Basic {
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }
}
