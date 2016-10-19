package com.mirceasorinsebastian.quiztournament;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

class GameStats implements Serializable {
    public static final String EXTRA = "com.mirceasorinsebastian.quiztournamentGameStats";
    private boolean isGameRunning;
    private int numberOfLetters, crtPlayerNumber;
    private String gameMode, aiNickname, gameCategory;

    public boolean getIsGameRunning() {
        return this.isGameRunning;
    }

    public void setIsGameRunning(Boolean status) {
        this.isGameRunning = status;
        Log.i("setISGameRunning: ", "set");
    }

    public int getNumberOfLetters() {
        return this.numberOfLetters;
    }

    public void setNumberOfLetters(int value) {
        this.numberOfLetters = value;
    }

    public String getGameMode() {
        return this.gameMode;
    }

    public void setGameMode(String value){
        this.gameMode = value;
    }

    public int getCrtPlayerNumber() {
        return this.crtPlayerNumber;
    }

    public void setCrtPlayerNumber(int value) {
        this.crtPlayerNumber = value;
    }

    public String getAiNickname() {
        return this.aiNickname;
    }

    public void setAiNickname(String value) {
        this.aiNickname = value;
    }

    public String getGameCategory() {
        return this.gameCategory;
    }

    public void setGameCategory(String value) {
        this.gameCategory = value;
    }
}

public class GameActivity extends AppCompatActivity implements GameLoadingFragment.OnFragmentInteractionListener, GameFragment.OnFragmentInteractionListener, GameResultFragment.OnFragmentInteractionListener {

    public String crtRoomId, savedRoomId, PLAYER1_ID, PLAYER2_ID, PLAYER1_WINS, PLAYER2_WINS;
    public String crtGAME_STATUS = "waiting";
    public Uri imageUri;

    public FirebaseDatabase database;
    public DataSnapshot roomDataSnapshot;

    public boolean waitRoomProcess = false;

    public GameLoadingFragment gameLoadingFragment;
    public GameFragment gameFragment;
    public GameResultFragment gameResultFragment;
    android.support.v4.app.FragmentManager supportFragmentManager;
    Handler timeStapHandler;
    Runnable run;

    GameStats gameStats;
    UserStats userStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        userStats = (UserStats) getIntent().getSerializableExtra(UserStats.EXTRA);

        gameStats = new GameStats();
        gameStats.setIsGameRunning(false);

        gameLoadingFragment = new GameLoadingFragment();
        gameFragment = new GameFragment();
        gameResultFragment = new GameResultFragment();
        supportFragmentManager = getSupportFragmentManager();

        //Start updating user TimeStap
        updateTimeStap();

        //Set default Loading Fragment
        showLoading();

        //Background part
        View view = (View) findViewById(android.R.id.content);

        //Repeating background image
        Bitmap backgroundImage = BitmapFactory.decodeResource(getResources(), R.drawable.gplaypattern);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), backgroundImage);
        bitmapDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        view.setBackground(bitmapDrawable);

        FirebaseListenUserRoom();
    }


    //Listen for users profile
    public void FirebaseListenUserRoom() {
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference getFromDB = database.getReference("connectedUsers/"+GoogleSignInActivity.user.getUid().toString());


        getFromDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                crtRoomId = dataSnapshot.child("GAME_ROOM").getValue().toString();

                //If the user is currently in a room
                if(crtRoomId.equals("Pending") || crtRoomId.equals("none")) {
                    Log.i("GameActivity status:", "User is NOT in a room");
                    Log.i("146: setIsGameRunning(False)", "yeap");
                    if(gameStats.getIsGameRunning())
                        exitPlayer();

                    gameStats.setIsGameRunning(false);
                } else {
                    if (!gameStats.getIsGameRunning()) {
                        Log.i("GameActivity status: ", "User is in a room: " + crtRoomId);
                        savedRoomId = crtRoomId;
                        gameStats.setIsGameRunning(true);
                        FirebaseListenRoomInfo();
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.i("dbOnChangeFailed", "Failed to read value.", error.toException());
            }
        });
    }

    //#############
    //Quizz Game Section
    //#############

    //Listener for the current room the user is in
    public void FirebaseListenRoomInfo() {
        if(crtRoomId.equals("Pending") || crtRoomId.equals("none"))
            return;

        database = FirebaseDatabase.getInstance();
        final DatabaseReference gameGetFromDB = database.getReference("rooms/"+crtRoomId);


        gameGetFromDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!gameStats.getIsGameRunning()){
                    gameGetFromDB.removeEventListener(this);
                    Log.i("FirebaseListenRoomInfo: ", "exited");
                }else {
                    try {
                        if(!waitRoomProcess)
                            quizzCoreCode(dataSnapshot);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    roomDataSnapshot = dataSnapshot;
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.i("dbOnChangeFailed", "Failed to read value.", error.toException());
            }
        });
    }

    public void quizzCoreCode(DataSnapshot dataSnapshot) throws IOException {
        Log.i("GameActivity - status: ", crtGAME_STATUS);
        Log.i("isGameRunning:", Boolean.toString(gameStats.getIsGameRunning()) );

        String PLAYER1_STATUS="", PLAYER2_STATUS="", GAME_ROUNDS="", GAME_MODE="", AI_NICKNAME="";
        if(dataSnapshot.child("PLAYER1_ID").getValue() != null)
             PLAYER1_ID = dataSnapshot.child("PLAYER1_ID").getValue().toString();
        if(dataSnapshot.child("PLAYER2_ID").getValue() != null)
             PLAYER2_ID = dataSnapshot.child("PLAYER2_ID").getValue().toString();
        if(dataSnapshot.child("PLAYER1_STATUS").getValue() != null)
             PLAYER1_STATUS = dataSnapshot.child("PLAYER1_STATUS").getValue().toString();
        if(dataSnapshot.child("PLAYER2_STATUS").getValue() != null)
             PLAYER2_STATUS = dataSnapshot.child("PLAYER2_STATUS").getValue().toString();
        if(dataSnapshot.child("GAME_ROUNDS").getValue() != null )
            GAME_ROUNDS = dataSnapshot.child("GAME_ROUNDS").getValue().toString();
        if(dataSnapshot.child("GAME_MODE").getValue() != null)
            GAME_MODE = dataSnapshot.child("GAME_MODE").getValue().toString();
        if(dataSnapshot.child("GAME_STATUS").getValue() != null)
            crtGAME_STATUS = dataSnapshot.child("GAME_STATUS").getValue().toString();
        if(dataSnapshot.child("PLAYER1_WINS").getValue() != null)
            PLAYER1_WINS = dataSnapshot.child("PLAYER1_WINS").getValue().toString();
        if(dataSnapshot.child("PLAYER2_WINS").getValue() != null)
            PLAYER2_WINS = dataSnapshot.child("PLAYER2_WINS").getValue().toString();
        if(dataSnapshot.child("AI_NICKNAME").getValue() != null)
            AI_NICKNAME = dataSnapshot.child("AI_NICKNAME").getValue().toString();
        if(dataSnapshot.child("GAME_CATEGORY").getValue() != null)
            gameStats.setGameCategory(dataSnapshot.child("GAME_CATEGORY").getValue().toString());
        else
            gameStats.setGameCategory("random");

        if(dataSnapshot.child("GAME_MODE").getValue() != null)
             gameStats.setGameMode(dataSnapshot.child("GAME_MODE").getValue().toString());

        gameStats.setAiNickname(AI_NICKNAME);

        //STEP0 AND STEP1: establish crtPlayerNumber and set it to connected

        if (crtGAME_STATUS.equals("waitingForPlayers") && PLAYER1_ID.equals(GoogleSignInActivity.user.getUid().toString()) && PLAYER1_STATUS.equals("waiting")) {
            waitRoomProcess = true;
            database.getReference("rooms/" + crtRoomId + "/PLAYER1_STATUS").setValue("connected");
            database.getReference("rooms/" + crtRoomId + "/PLAYER1_FOCUS").setValue("focused");
            gameStats.setCrtPlayerNumber(1);
            waitRoomProcess = false;
        }

        if(!GAME_MODE.equals("AI"))
            if (crtGAME_STATUS.equals("waitingForPlayers")  && PLAYER2_ID.equals(GoogleSignInActivity.user.getUid().toString()) && PLAYER2_STATUS.equals("waiting")) {
                waitRoomProcess = true;
                database.getReference("rooms/" + crtRoomId + "/PLAYER2_STATUS").setValue("connected");
                database.getReference("rooms/" + crtRoomId + "/PLAYER2_FOCUS").setValue("focused");
                gameStats.setCrtPlayerNumber(2);
                waitRoomProcess = false;
            }

        //STEP2: PREPARING
        if (crtGAME_STATUS.equals("preparing") && ((gameStats.getCrtPlayerNumber() == 1 && PLAYER1_STATUS.equals("connected")) || (gameStats.getCrtPlayerNumber() == 2 && PLAYER2_STATUS.equals("connected")))) {
            waitRoomProcess = true;
            showRoundFeedbackToast(dataSnapshot);
            String GAME_QUIZZ = dataSnapshot.child("GAME_QUIZZ").getValue().toString();
            gameStats.setNumberOfLetters(Integer.valueOf(dataSnapshot.child("GAME_ANSWERLETTERS").getValue().toString())); // set answer's number of letters
            getQuizzImage(GAME_QUIZZ); // here set user value to ready
        }


        //STEP3: RUNNING
        if (crtGAME_STATUS.equals("running") && ((gameStats.getCrtPlayerNumber() == 1 && PLAYER1_STATUS.equals("ready")) || (gameStats.getCrtPlayerNumber() == 2 && PLAYER2_STATUS.equals("ready")))) {
            waitRoomProcess = true;
            if(gameStats.getCrtPlayerNumber() == 1 )
                database.getReference("rooms/" + crtRoomId + "/PLAYER1_STATUS").setValue("ingame");
            else
                database.getReference("rooms/" + crtRoomId + "/PLAYER2_STATUS").setValue("ingame");
            showGameUI();
            waitRoomProcess = false;
        }

        //STEP4: GAME IS FINISHED
        if(crtGAME_STATUS.equals("finished") && gameStats.getIsGameRunning()) {
            showRoundFeedbackToast(dataSnapshot);
            gameStats.setIsGameRunning(false);

            String GAME_WINNER = "";
            if(dataSnapshot.child("GAME_WINNER").getValue() != null)
               GAME_WINNER = dataSnapshot.child("GAME_WINNER").getValue().toString();

            if((gameStats.getCrtPlayerNumber() == 1 && GAME_WINNER.equals("PLAYER1")) || (gameStats.getCrtPlayerNumber() == 2 && GAME_WINNER.equals("PLAYER2")))
                showResultUI("Congratulation, you Won!", "+10 QP");
            else
                if(GAME_WINNER.equals("ABANDON"))
                    showResultUI("Game halted! The opponent exited the game.", "");
                else
                    showResultUI("Better luck next time, you Lost!", "-10 QP");

            //Set user Room to none
             database.getReference("connectedUsers/"+GoogleSignInActivity.user.getUid().toString()).child("GAME_ROOM").setValue("none");

            if(gameStats.getCrtPlayerNumber() == 1 )
                database.getReference("rooms/" + crtRoomId + "/PLAYER1_STATUS").setValue("exited");
            else
                database.getReference("rooms/" + crtRoomId + "/PLAYER2_STATUS").setValue("exited");

        }

        //if((crtPlayerNumber == 1 && PLAYER1_STATUS.equals("exited")) || (crtPlayerNumber == 2 && PLAYER2_STATUS.equals("exited")))
            //gameStats.setIsGameRunning(false);
    }

    public void showRoundFeedbackToast(DataSnapshot obj) {
        String msg;
        Toast toast;

        if(gameStats.getCrtPlayerNumber() == 1 ) {
            if(obj.child("GAME_PLAYER1_ROUND_MESSAGE").getValue() != null) {
                msg = obj.child("GAME_PLAYER1_ROUND_MESSAGE").getValue().toString();
                toast = Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 310);
                toast.show();
            }
        }
        if(gameStats.getCrtPlayerNumber() == 2) {
            if(obj.child("GAME_PLAYER2_ROUND_MESSAGE").getValue() != null) {
                msg = obj.child("GAME_PLAYER2_ROUND_MESSAGE").getValue().toString();
                toast = Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 310);
                toast.show();
            }
        }



    }

    //Download the QuizzImage from Firebase Storage
    public void getQuizzImage(String id) throws IOException {
        Log.i("getQuizzImage: ", "called, id: " + id);

        final FirebaseDatabase databaseRef = FirebaseDatabase.getInstance();
        final FirebaseStorage storageRef = FirebaseStorage.getInstance();
        final StorageReference  imgRef = storageRef.getReference("imgQuizzes/" + id);

        final File localFile = File.createTempFile("quizz", "jpg", getApplicationContext().getCacheDir());

        imgRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                //Uri of the local TempFile
                imageUri = Uri.fromFile(localFile);

                waitRoomProcess = false;
                Log.i("crtImageUri: ", imageUri.toString());
                //SetPlayer to ready
                if(gameStats.getCrtPlayerNumber() == 1 ) {
                    DatabaseReference playerIdRef = databaseRef.getReference("rooms/"+crtRoomId+"/PLAYER1_STATUS");
                    playerIdRef.setValue("ready");
                }
                if(gameStats.getCrtPlayerNumber() == 2) {
                    DatabaseReference playerIdRef = databaseRef.getReference("rooms/"+crtRoomId+"/PLAYER2_STATUS");
                    playerIdRef.setValue("ready");
                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });

    }

    //#############
    //Instantiate Fragments Section
    //#############

    @Override
    public  void showLoading() {
        Log.i("UI", "showLoadingUI- called");

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        if(!isFinishing())
            getSupportFragmentManager().beginTransaction().replace(R.id.mainFrameLayout, gameLoadingFragment, "gameLoadingFragment").commitAllowingStateLoss();
    }

    public void showGameUI() {
        Log.i("UI", "showGameUI - called");
        if(imageUri != null) {
            Bundle b = new Bundle(); b.putString("imageUri", imageUri.toString()); b.putSerializable(GameStats.EXTRA, (Serializable) gameStats);
            gameFragment = new GameFragment();
            gameFragment.setArguments(b);
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        if(!isFinishing())
            getSupportFragmentManager().beginTransaction().replace(R.id.mainFrameLayout, gameFragment, "gameFragment").commitAllowingStateLoss();
    }

    public void showResultUI(String msg, String snackBarMsg) {
        Log.i("UI", "showResultUI - called");

        Bundle b = new Bundle(); b.putString("message", msg); b.putString("snackBarMsg", snackBarMsg);
        b.putSerializable(UserStats.EXTRA, (Serializable) userStats); b.putSerializable(GameStats.EXTRA, (Serializable) gameStats);
        gameResultFragment = new GameResultFragment();
        gameResultFragment.setArguments(b);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        if(!isFinishing())
            getSupportFragmentManager().beginTransaction().replace(R.id.mainFrameLayout, gameResultFragment, "gameResultFragment").commitAllowingStateLoss();
    }


    //#############
    //Exit & Pause Situations
    //#############

    public void exitPlayer() {
        Log.i("exitPlayer: ", "Player exited");
        if(gameStats.getIsGameRunning()) {
            Log.i("exitPlayer: ", "Really exiting");
            final FirebaseDatabase database = FirebaseDatabase.getInstance();

            DatabaseReference updatesFromDB = database.getReference("rooms/"+savedRoomId);
            if(gameStats.getCrtPlayerNumber() == 1)
                updatesFromDB.child("PLAYER1_STATUS").setValue("exited");
            if(gameStats.getCrtPlayerNumber() == 2)
                updatesFromDB.child("PLAYER2_STATUS").setValue("exited");

            Log.i("379: setIsGameRunning(False)", "yeap");
            gameStats.setIsGameRunning(false);
        }
    }

    public void updateTimeStap() {
        final FirebaseDatabase  database = FirebaseDatabase.getInstance();
        timeStapHandler = new Handler();
        run = new Runnable() {
            @Override
            public void run() {
                //Insert Code to be run every second
                DatabaseReference userTimeRef = database.getReference("connectedUsers/"+GoogleSignInActivity.user.getUid().toString()+"/TIME");
                userTimeRef.setValue(String.valueOf(System.currentTimeMillis()));
                timeStapHandler.postDelayed(this, 3000);
            }
        };
        timeStapHandler.post(run);
    }

    public void stopUpdatingTimeStap() {
        timeStapHandler.removeCallbacks(run);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("onDestroy:", "called");
        Log.i("isGameRunning:", Boolean.toString(gameStats.getIsGameRunning()) );
        exitPlayer();
        stopUpdatingTimeStap();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i("onBackPressed:", "called");
        Log.i("isGameRunning:", Boolean.toString(gameStats.getIsGameRunning()) );
        exitPlayer();
        stopUpdatingTimeStap();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i("onBackPressed:", "called");
        Log.i("isGameRunning:", Boolean.toString(gameStats.getIsGameRunning()) );
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            stopUpdatingTimeStap();
            exitPlayer();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("pausePlayer: ", "Player paused");
        Log.i("isGameRunning:", Boolean.toString(gameStats.getIsGameRunning()) );
        if(gameStats.getIsGameRunning()) {
            final FirebaseDatabase database = FirebaseDatabase.getInstance();

            DatabaseReference updatesFromDB = database.getReference("rooms/"+savedRoomId);
            if(gameStats.getCrtPlayerNumber() == 1)
                updatesFromDB.child("PLAYER1_FOCUS").setValue("unfocused");
            if(gameStats.getCrtPlayerNumber() == 2)
                updatesFromDB.child("PLAYER2_FOCUS").setValue("unfocused");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("pausePlayer: ", "Player resumed");
        Log.i("isGameRunning:", Boolean.toString(gameStats.getIsGameRunning()) );
        if(gameStats.getIsGameRunning()) {
            final FirebaseDatabase database = FirebaseDatabase.getInstance();

            DatabaseReference updatesFromDB = database.getReference("rooms/"+savedRoomId);
            if(gameStats.getCrtPlayerNumber() == 1)
                updatesFromDB.child("PLAYER1_FOCUS").setValue("focused");
            if(gameStats.getCrtPlayerNumber() == 2)
                updatesFromDB.child("PLAYER2_FOCUS").setValue("focused");
        }
    }

}
