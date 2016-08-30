package com.qrcode.demo;

import android.Manifest;
import android.os.Bundle;
import android.view.View;

import com.tbruyelle.rxpermissions.RxPermissions;
import com.thinkcore.activity.TAppActivity;
import com.thinkcore.utils.TActivityUtils;

public class MainActivity extends TAppActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.Button_test).setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.Button_test) {
//        RxPermissions.getInstance(this)
//                .request(Manifest.permission.CAMERA)
//                .subscribe(granted -> {
//                    if (granted) { // Always true pre-M
//                        TActivityUtils.jumpToActivity(mContext, CaptureActivity.class);
//                    } else {
//                      makeText("操作失败");
//                    }
//                });
            RxPermissions.getInstance(this).ensure(Manifest.permission.CAMERA);
            TActivityUtils.jumpToActivity(mContext, CaptureActivity.class);
        }
    }
}
