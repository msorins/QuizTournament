package com.mirceasorinsebastian.logoquizztournament;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.AndroidException;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.Serializable;
import java.util.HashMap;

class UserStats implements Serializable{
    public static final String EXTRA = "com.mirceasorinsebastian.logoquizztournament";
    private int userQP, userGamesPlayed, userGamesWon;
    private String userNickname;

    public int getUserQP() {
        return this.userQP;
    }
    public void setUserQP(int value) {
        this.userQP = value;
    }

    public int getUserGamesPlayed() {
        return this.userGamesPlayed;
    }
    public void setUserGamesPlayed(int value) {
        this.userGamesPlayed = value;
    }

    public int getUserGamesWon() {
        return this.userGamesWon;
    }
    public void setUserGamesWon(int value) {
        this.userGamesWon = value;
    }

    public String getUserNickname() {
        return this.userNickname;
    }
    public void setUserNickname(String value) {
        this.userNickname = value;
    }
}

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public TextView QPTextView, userStatisticsTextView, announcesTextView;

    UserStats userStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Feature available soon for premium users", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        userStats = new UserStats();

        //Background part
        View view = (View) findViewById(android.R.id.content);

        //Repeating background image
        Bitmap backgroundImage = BitmapFactory.decodeResource(getResources(), R.drawable.pattern1);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), backgroundImage);
        bitmapDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        view.setBackground(bitmapDrawable);


        //GET References to UI parts
        QPTextView = (TextView) findViewById(R.id.QPTextView);
        userStatisticsTextView = (TextView) findViewById(R.id.userStatisticsTextView);
        announcesTextView = (TextView) findViewById(R.id.announcesTextView);

        //If user is logged in
        if(GoogleSignInActivity.user != null)
        {
            TextView userNameTextView = (TextView) findViewById(R.id.userNameTextView);
            userNameTextView.setText( "Hi, " + GoogleSignInActivity.user.getDisplayName() );
            Log.i("User status", "logged In");
        }

        FirebaseMessaging.getInstance().subscribeToTopic("news");
        FirebaseUserConnection();
        FirebaseGetAnnounces();

        setUserIdShared();
    }

    //Setting logged in user acces to his DataBase Node
    public void FirebaseUserConnection() {
        final FirebaseDatabase  database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("connectedUsers");

        //Set current GameRoom to none
        DatabaseReference userGameRoomRef = database.getReference("connectedUsers/"+GoogleSignInActivity.user.getUid().toString()+"/GAME_ROOM");
        userGameRoomRef.setValue("none");

        DatabaseReference getFromDB = database.getReference("connectedUsers/"+GoogleSignInActivity.user.getUid().toString());
        getFromDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.child("QP").exists()) {
                    database.getReference("connectedUsers/"+GoogleSignInActivity.user.getUid().toString()+"/QP").setValue("200");
                    userStats.setUserQP(200);
                    QPTextView.setText("200");
                } else {
                    userStats.setUserQP( Integer.parseInt(dataSnapshot.child("QP").getValue().toString()) );
                    QPTextView.setText(dataSnapshot.child("QP").getValue().toString());
                }

                if(!dataSnapshot.child("GAMES_PLAYED").exists() || !dataSnapshot.child("GAMES_WON").exists()) {
                    database.getReference("connectedUsers/"+GoogleSignInActivity.user.getUid().toString()+"/GAMES_PLAYED").setValue("0");
                    database.getReference("connectedUsers/"+GoogleSignInActivity.user.getUid().toString()+"/GAMES_WON").setValue("0");
                    userStats.setUserGamesPlayed(0); userStats.setUserGamesWon(0);
                    userStatisticsTextView.setText("0/0");
                } else {
                    userStats.setUserGamesPlayed( Integer.parseInt(dataSnapshot.child("GAMES_PLAYED").getValue().toString()) );
                    userStats.setUserGamesWon( Integer.parseInt(dataSnapshot.child("GAMES_WON").getValue().toString()) );
                    userStatisticsTextView.setText(dataSnapshot.child("GAMES_WON").getValue().toString() + "/" + dataSnapshot.child("GAMES_PLAYED").getValue().toString());
                }

                if(!dataSnapshot.child("NICKNAME").exists()) {
                    userStats.setUserNickname( GoogleSignInActivity.user.getDisplayName().substring(0, 6) );
                    database.getReference("connectedUsers/"+GoogleSignInActivity.user.getUid().toString()).child("NICKNAME").setValue(userStats.getUserNickname());
                }
                else
                    userStats.setUserNickname( dataSnapshot.child("NICKNAME").getValue().toString() );
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.i("dbOnChangeFailed", "Failed to read value.", error.toException());
            }
        });
    }

    public void startGame(View v) {
        final FirebaseDatabase  database = FirebaseDatabase.getInstance();

        if(userStats.getUserQP() >= 10) {
            //Set current connection time
            DatabaseReference userTimeRef = database.getReference("connectedUsers/"+GoogleSignInActivity.user.getUid().toString()+"/TIME");
            userTimeRef.setValue(String.valueOf(System.currentTimeMillis()));

            DatabaseReference queueRef = database.getReference("queue");

            HashMap key = new HashMap(); HashMap key2 = new HashMap();
            key2.put("type", "newGameTwoRequest");
            key2.put("QP", Integer.toString(userStats.getUserQP()));
            key2.put("ENTERTIME", String.valueOf(System.currentTimeMillis()));

            key.put(GoogleSignInActivity.user.getUid().toString(), key2);
            queueRef.updateChildren(key, null);

            Intent i = new Intent(getApplicationContext(), GameActivity.class);
            i.putExtra(UserStats.EXTRA, (Serializable) userStats);
            startActivity(i);
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Not enough QP", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
        }

    }

    public void addQuizz(View v) {
        Intent i = new Intent(getApplicationContext(), AddQuizzActivity.class);
        startActivity(i);
    }

    public void FirebaseGetAnnounces() {
        final FirebaseDatabase  database = FirebaseDatabase.getInstance();

        DatabaseReference homeAnnouncesDB = database.getReference("homeAnnounces/");
        homeAnnouncesDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                    announcesTextView.setText(dataSnapshot.getValue().toString());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.i("dbOnChangeFailed", "Failed to read value.", error.toException());
            }
        });
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.addQuizz) {
            addQuizz(findViewById(android.R.id.content));
        } else if (id == R.id.guide) {
            Intent i = new Intent(getApplicationContext(), GuideActivity.class);
            startActivity(i);
        } else if( id == R.id.settings) {
            Intent i = new Intent(getApplicationContext(), ProfileActivity.class);
            i.putExtra(UserStats.EXTRA, (Serializable) userStats);
            startActivity(i);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void setUserIdShared() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPreferences = getSharedPreferences(userStats.EXTRA, MODE_PRIVATE);
                sharedPreferences.edit().putString("userID", GoogleSignInActivity.user.getUid().toString()).apply();
                Log.i("setUserIdShared", "done");
            }
        });

        String token = FirebaseInstanceId.getInstance().getToken();
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        database.getReference("connectedUsers").child(GoogleSignInActivity.user.getUid().toString()).child("TOKEN").setValue(token);
    }
}
