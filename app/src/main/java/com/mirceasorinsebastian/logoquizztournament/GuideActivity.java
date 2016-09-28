package com.mirceasorinsebastian.logoquizztournament;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class GuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        setTitle("Guide");

        //Background part
        View view = (View) findViewById(android.R.id.content);

        //Repeating background image
        Bitmap backgroundImage = BitmapFactory.decodeResource(getResources(), R.drawable.pattern4);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), backgroundImage);
        bitmapDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        view.setBackground(bitmapDrawable);

    }
}
