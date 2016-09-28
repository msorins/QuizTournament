package com.mirceasorinsebastian.logoquizztournament;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.firebase.database.FirebaseDatabase;

public class ProfileActivity extends AppCompatActivity {
    EditText nicknameEditText;
    UserStats userStats;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setTitle("Settings");

        userStats = (UserStats) getIntent().getSerializableExtra(UserStats.EXTRA);

        //Background part
        View view = (View) findViewById(android.R.id.content);

        //Repeating background image
        Bitmap backgroundImage = BitmapFactory.decodeResource(getResources(), R.drawable.pattern5);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), backgroundImage);
        bitmapDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        view.setBackground(bitmapDrawable);

        nicknameEditText = (EditText) findViewById(R.id.nicknameEditText);
        nicknameEditText.setText(userStats.getUserNickname());
    }

    public void save(View v){
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        database.getReference("connectedUsers/"+GoogleSignInActivity.user.getUid().toString()).child("NICKNAME").setValue(nicknameEditText.getText().toString());

        Snackbar.make(findViewById(android.R.id.content), "Saved", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
    }
}
