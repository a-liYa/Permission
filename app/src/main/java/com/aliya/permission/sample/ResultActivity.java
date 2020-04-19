package com.aliya.permission.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.aliya.permission.ResultHelper;
import com.aliya.permission.sample.utils.T;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        findViewById(R.id.tv_result).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                ResultHelper.startActivityForResult(v.getContext(),
                        new Intent(v.getContext(), NeverAskActivity.class), 100,
                        new ResultHelper.OnActivityResultCallback() {
                            @Override
                            public void onActivityResult(int requestCode, int resultCode,
                                                         Intent data) {
                                T.showShort(v.getContext(), "onActivityResult：" + requestCode);
                            }
                        });
            }
        });
    }
}
