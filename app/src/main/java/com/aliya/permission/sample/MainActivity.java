package com.aliya.permission.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.aliya.fast.adapter.FastAdapter;
import com.aliya.fast.entity.ListEntity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView mListView;

    static List<ListEntity> sList = new ArrayList<>();

    static {
        sList.add(new ListEntity(PermissionSampleActivity.class, "Activity使用示例"));
        sList.add(new ListEntity(NeverAskActivity.class, "不再询问处理示例"));
        sList.add(new ListEntity(ResultActivity.class, "OnActivityResult"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = findViewById(R.id.list_view);
        mListView.setAdapter(new FastAdapter(sList));

    }

}
