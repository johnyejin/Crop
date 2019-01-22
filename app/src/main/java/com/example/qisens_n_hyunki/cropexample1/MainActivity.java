/*
 * onResume에서 setContentView를 하는 이유
 *     -> imageCropActivity에서 main으로 돌아왔을때 그렸던게 초기화됨
 */

package com.example.qisens_n_hyunki.cropexample1;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(new CropView(MainActivity.this));

    }
}
