package com.my.myapplication;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PrefActivity extends PreferenceActivity {

    //首选项设置页
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);
    }
}