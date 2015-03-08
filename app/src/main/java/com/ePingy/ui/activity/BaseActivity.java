package com.ePingy.ui.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import com.ePingy.app.EPingyApplication;
import com.ePingy.R;

/**
 * Created by jhansi on 06/03/15.
 */
public abstract class BaseActivity extends ActionBarActivity {

    protected Context context;
    protected Toolbar toolbar;
    protected EPingyApplication application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResource());
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initView() {
        application = (EPingyApplication) getApplication();
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    protected abstract int getLayoutResource();

}