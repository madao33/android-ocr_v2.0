package com.example.ocrv20;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity  {

    private static final int REQUEST_PERMISSION_CODE = 101;
    private List<bpItem> bpItems = new ArrayList<bpItem>();

    private String[] Text=new String[4];
    private ListView listView;
    private int listcount1;
    //初始化openCV库
    private void iniLoadOpenCV(){
        boolean success = OpenCVLoader.initDebug();
        if(success)
            Log.i("opencvInit:","openCV Libraries loaded success!");
        else
            Toast.makeText(this.getApplicationContext(),
                    "opencvInit WARNING:Could not load OpenCV Libraries!",
                    Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iniLoadOpenCV();
        //初始化时请求权限
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (!checkPermission()) { //没有或没有全部授权
                requestPermissions(); //请求权限
            }else if(ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.
                    PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity.this, new
                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        FloatingActionButton processBtn = this.findViewById(R.id.newCapture);
        Button btn_ocrData_main = findViewById(R.id.btn_bpData_main);
        Button btn_docData_main = findViewById(R.id.btn_ocrData_main);
        Button btn_caseData_main = findViewById(R.id.btn_docData_main);


        //设置按钮监听事件
        processBtn.setOnClickListener(new
                View.OnClickListener(){
            @Override
            public void onClick(View v){
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, CaptureActivity.class);
            startActivity(intent);
//            MainActivity.this.finish();
            }
        });

        //设置血压计数据回放按钮
        btn_ocrData_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent ocr_intent = new Intent(MainActivity.this,ocrHistoryDataListActivity.class);
                startActivity(ocr_intent);
            }
        });

        //设置文档识别数据回放按钮
        btn_docData_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent doc_intent = new Intent(MainActivity.this,docHistoryDataListActivity.class);
                startActivity(doc_intent);
            }
        });

        //设置病历数据提取解果回放
        btn_caseData_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent case_intent = new Intent(MainActivity.this,caseHistoryListActivity.class);
                startActivity(case_intent);
            }
        });

    }


    //检查权限
    private boolean checkPermission() {
        //是否有权限
        boolean haveCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        boolean haveWritePermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        return haveCameraPermission && haveWritePermission;

    }

    // 请求所需权限
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermissions() {
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
    }

    private void searchData(){
        String[] data = new String[3];
        DataBaseHelper dbHelper = new DataBaseHelper(MainActivity.this, "ocr_bp",null,1);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query("bp",null,null,null,null,null,null);
        listcount1 = cursor.getCount();
        Log.i("appTest:","databases size:"+listcount1);
        while(cursor.moveToNext()){
            data[0] = cursor.getString(7); //time
            data[1] = cursor.getString(5); //bitmap uri
            data[2] = cursor.getString(1); //typeInfo
            Log.i("appTest:","search data:"+data[0]+" "+data[1]+" "+data[2]);
            Uri imageUri = Uri.parse(data[1]);
            Bitmap bitmap = getBitmapFromUri(imageUri);
            bpItem bpItem = new bpItem(data[0],bitmap,data[2]);
            bpItems.add(bpItem);
        }
        cursor.close();

        DocDataBaseHelper dbDocHelper = new DocDataBaseHelper(MainActivity.this, "ocr_doc",null,1);
        SQLiteDatabase db_doc = dbDocHelper.getWritableDatabase();
        Cursor cursor_doc = db_doc.query("doc",null,null,null,null,null,null);
        Log.i("appTest:","doc databases size:"+cursor_doc.getCount());
        while(cursor_doc.moveToNext()){
            data[0] = cursor_doc.getString(5); //time
            data[1] = cursor_doc.getString(3); //bitmap uri
            data[2] = cursor_doc.getString(1);
            Log.i("appTest:","search data:"+data[0]+" "+data[1]);
            Uri imageUri = Uri.parse(data[1]);
            Bitmap bitmap = getBitmapFromUri(imageUri);
            bpItem bpItem = new bpItem(data[0],bitmap,data[2]);
            bpItems.add(bpItem);
        }
        cursor.close();
    }

    private String[] getData(int index){
        String[] data = new String[8];
        DataBaseHelper dbHelper = new DataBaseHelper(MainActivity.this, "ocr_bp", null, 1);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query("bp", null, null, null, null, null, null);
        listcount1 = cursor.getCount();
        if(index >= listcount1) {
            DocDataBaseHelper dbHelperdoc = new DocDataBaseHelper(MainActivity.this, "ocr_doc", null, 1);
            SQLiteDatabase dbdoc = dbHelperdoc.getWritableDatabase();
            cursor = dbdoc.query("bp", null, null, null, null, null, null);
            index = index - listcount1;

        }else if(cursor.moveToFirst()){
            if (index < cursor.getCount()){
                cursor.move(index);
                for(int i=0;i<8;i++)
                    data[i] = cursor.getString(i);
                Log.i("appTest: ", "database--->" + index + " "
                        + data[0] + " " + data[1] + " " + data[2] + " " + data[3] + " " + data[4] + " " +
                        data[5] + " " + data[6]+" "+data[7]);
            }
        }
        cursor.close();
        return data;
    }

    private Bitmap getBitmapFromUri(Uri uri)
    {
        try
        {
            // 读取uri所在的图片
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            return bitmap;
        }
        catch (Exception e)
        {
            Log.e("[Android]", e.getMessage());
            Log.e("[Android]", "目录为：" + uri);
            e.printStackTrace();
            return null;
        }
    }

}
