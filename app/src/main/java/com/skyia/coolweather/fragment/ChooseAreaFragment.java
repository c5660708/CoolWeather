package com.skyia.coolweather.fragment;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.skyia.coolweather.R;
import com.skyia.coolweather.WeatherActivity;
import com.skyia.coolweather.db.City;
import com.skyia.coolweather.db.County;
import com.skyia.coolweather.db.Province;
import com.skyia.coolweather.util.HttpUtil;
import com.skyia.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Skyia_ccf on 2017/12/7.
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();

    private List<Province> provinceList;//省列表
    private List<City> cityList;//市列表
    private List<County> countyList;//县列表

    private Province selectedProvince;//选中的省份
    private City selectedCity;//选中的市
    private int currentLevel;//当前选中的级别

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }else if (currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();
                }else if (currentLevel == LEVEL_COUNTY){
                    String weatherId = countyList.get(position).getWeatherId();
                    Intent intent = new Intent(getActivity(), WeatherActivity.class);
                    intent.putExtra("weather_id",weatherId);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY){
                    queryCities();
                }else if (currentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

//    查询所有的省，优先从数据库查询，如果没有查到再去服务器查询
private void queryProvinces() {
   titleText.setText("中国");
    backButton.setVisibility(View.GONE);
    provinceList = DataSupport.findAll(Province.class);
    if (provinceList.size() > 0) {
        dataList.clear();
        for (Province p : provinceList) {
            dataList.add(p.getProvinceName());
        }
        adapter.notifyDataSetChanged();
        listView.setSelection(0);
        currentLevel = LEVEL_PROVINCE;
    } else {
        String url = getResources().getString(R.string.url_query_province);
        queryFromServer(url, "province");
    }
}
    //    查询所有的市，优先从数据库查询，如果没有查到再去服务器查询
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId()))
                .find(City.class);
        if (cityList.size() > 0) {
            try {
                dataList.clear();
                for (City c : cityList) {
                    dataList.add(c.getCityName());
                }
                adapter.notifyDataSetChanged();
                listView.setSelection(0);
                currentLevel = LEVEL_CITY;
            } catch (NullPointerException e) {
                String url = getResources().getString(R.string.url_query_province);
                queryFromServer(url, "province");
                int provinceCode = selectedProvince.getProvinceCode();
                url = getResources().getString(R.string.url_query_province) + provinceCode;
                queryFromServer(url, "city");
            }
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String url = getResources().getString(R.string.url_query_province) + provinceCode;
            queryFromServer(url, "city");
        }
    }
    //    查询所有的县，优先从数据库查询，如果没有查到再去服务器查询
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid=?", String.valueOf(selectedCity.getId()))
                .find(County.class);
        if (countyList.size() > 0) {
            try {
                dataList.clear();
                for (County c : countyList) {
                    dataList.add(c.getCountyName());
                }
                adapter.notifyDataSetChanged();
                listView.setSelection(0);
                currentLevel = LEVEL_COUNTY;
            } catch (NullPointerException e) {
                int provinceCode = selectedProvince.getProvinceCode();
                String url = getResources().getString(R.string.url_query_province) + provinceCode;
                queryFromServer(url, "city");
                url = getResources().getString(R.string.url_query_province)
                        + selectedProvince.getProvinceCode() + "/"
                        + selectedCity.getCityCode();
                queryFromServer(url, "county");
            }
        } else {
            String url = getResources().getString(R.string.url_query_province)
                    + selectedProvince.getProvinceCode() + "/"
                    + selectedCity.getCityCode();
            queryFromServer(url, "county");
        }
    }
//  根据传入的地址和类型从服务器上查询数据
    private void queryFromServer(String url, final String type) {
    showProgressDialog();
    HttpUtil.sendOkHttpRequest(url, new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    closeProgressDialog();
                    Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String responseData = response.body().string();
            boolean result = false;
            switch (type) {
                case "province":
                    result = Utility.handleProvinceResponse(responseData);
                    break;
                case "city":
                    result = Utility.handleCityResponse(responseData, selectedProvince.getId());
                    break;
                case "county":
                    result = Utility.handleCountyResponse(responseData, selectedCity.getId());
                    break;
            }
            if (result) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        switch (type) {
                            case "province":
                                queryProvinces();
                                break;
                            case "city":
                                queryCities();
                                break;
                            case "county":
                                queryCounties();
                                break;
                        }
                    }
                });
            }
        }
    });
}
//  显示进度对话框
    private void showProgressDialog() {
        if (progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载");
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
    }
//    关闭进度对话框
    private void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }
}
