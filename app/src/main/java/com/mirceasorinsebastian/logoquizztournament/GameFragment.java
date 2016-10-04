package com.mirceasorinsebastian.logoquizztournament;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;


public class GameFragment extends Fragment {
    private String crtRoomId, PLAYER1_ID, PLAYER2_ID, PLAYER1_WINS, PLAYER2_WINS;
    private Integer crtPlayerNumber;

    private ProgressBar timerProgressBar;

    private View fragmentView;
    private String mParam1;
    private String mParam2;
    private OnFragmentInteractionListener mListener;

    private TextView lettersNumberTextView;
    GameStats gameStats;

    public GameFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameStats = (GameStats) getArguments().getSerializable(GameStats.EXTRA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_game, container, false);

        if (getArguments() != null) {
            mParam1 = getArguments().getString("imageUri");
            setImageUri(Uri.parse(mParam1));
            Log.i("imageUri: ", mParam1);
        }

        //SetUp players name and score
        if(crtPlayerNumber == 1) {
            FirebaseSetUIUserInfo(PLAYER1_ID, true);
            FirebaseSetUIUserInfo(PLAYER2_ID, false);

            setCrtPlayerScore(PLAYER1_WINS + "pc");
            setAdvPlayerScore(PLAYER2_WINS + "pc");
        } else {
            FirebaseSetUIUserInfo(PLAYER1_ID, false);
            FirebaseSetUIUserInfo(PLAYER2_ID, true);

            setCrtPlayerScore(PLAYER2_WINS + "pc");
            setAdvPlayerScore(PLAYER1_WINS + "pc");
        }

        //SetUp Button click listener
        FloatingActionButton sendButton = (FloatingActionButton) fragmentView.findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitResult();
            }
        });

        final FirebaseDatabase database = FirebaseDatabase.getInstance();

        //SetUP the timer ProgressBar
        timerProgressBar = (ProgressBar) fragmentView.findViewById(R.id.timerProgressBar);

        //Start the actual quizz
        startQuizz();

        //SetUP the explination for lettersButton
        FloatingActionButton lettersButton = (FloatingActionButton) fragmentView.findViewById(R.id.lettersButton);
        lettersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(fragmentView, "Numbers of letters", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
            }
        });

        lettersButton.bringToFront();


        lettersNumberTextView = (TextView) fragmentView.findViewById(R.id.lettersNumberTextView);
        lettersNumberTextView.setText(String.valueOf(gameStats.getNumberOfLetters()));

        return fragmentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;

            GameActivity gameActivity = (GameActivity) getActivity();
            crtRoomId = gameActivity.crtRoomId;
            crtPlayerNumber = gameActivity.crtPlayerNumber;
            PLAYER1_ID = gameActivity.PLAYER1_ID;
            PLAYER2_ID = gameActivity.PLAYER2_ID;
            PLAYER1_WINS = gameActivity.PLAYER1_WINS;
            PLAYER2_WINS = gameActivity.PLAYER2_WINS;

        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public Double crtValueTimer, crtPlayerTimer;
    public  boolean canSubmit;
    public String crtPlayerResult;

    public void startQuizz() {
        Log.i("startQuizz", "STARTING");
        canSubmit = true;

        //ProgressBar animation
        ObjectAnimator animation = ObjectAnimator.ofInt(timerProgressBar, "progress", 0);
        animation.setDuration(20100); // 1.0 second
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.start();

        new CountDownTimer(20100, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long leftTime = millisUntilFinished / 1000;
                setTime(String.valueOf(leftTime) + " s");
            }

            @Override
            public void onFinish() {
                setTime("Done !");
                if (mListener != null)
                    mListener.showLoading();

                sendResultToServer();
            }
        }.start();

        new CountDownTimer(20100, 10) {
            @Override
            public void onTick(long millisUntilFinished) {
                crtValueTimer =  Double.valueOf(millisUntilFinished) / Double.valueOf(1000);
            }

            @Override
            public void onFinish() {
                crtValueTimer =  0.0;
            }
        }.start();
    }

    public void submitResult() {
        canSubmit = false;

        crtPlayerTimer = crtValueTimer;
        crtPlayerResult = getAnswerEditText();

        Snackbar.make(fragmentView, "Answer sent", Snackbar.LENGTH_LONG).setAction("Action", null).show();

        if(getActivity() != null) {
            InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(getWindowsTokenAnswerEditText(), 0);
        }

    }

    public void sendResultToServer() {
        if(canSubmit)
            submitResult();

        if(gameStats.getIsGameRunning()) {
            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference updatesFromDB = database.getReference("rooms/" + crtRoomId);

            if (crtPlayerNumber == 1) {
                updatesFromDB.child("PLAYER1_RESULT").setValue(crtPlayerResult);
                HashMap key = new HashMap();  key.put("PLAYER1_STATUS", "done");
                updatesFromDB.updateChildren(key, null);  key.put("PLAYER1_TIMER", String.valueOf(crtPlayerTimer));
                updatesFromDB.updateChildren(key, null);
            } else {
                updatesFromDB.child("PLAYER2_RESULT").setValue(crtPlayerResult);
                HashMap key = new HashMap();  key.put("PLAYER2_STATUS", "done");
                updatesFromDB.updateChildren(key, null);  key.put("PLAYER2_TIMER", String.valueOf(crtPlayerTimer));
                updatesFromDB.updateChildren(key, null);
            }
        }
    }


    public void FirebaseSetUIUserInfo(String uid, final boolean crtUser) {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference userGetFromDB = database.getReference("connectedUsers/"+uid);


        userGetFromDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(crtUser) {
                    setCrtPlayerName(dataSnapshot.child("NICKNAME").getValue().toString());
                }  else {
                    setAdvPlayerName(dataSnapshot.child("NICKNAME").getValue().toString());
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.i("dbOnChangeFailed", "Failed to read value.", error.toException());
            }
        });
    }


    public void setTime(String str) {
        TextView timeLeftTextView = (TextView) fragmentView.findViewById(R.id.timeLeftTextView);
        timeLeftTextView.setText(str);
    }

    public void setCrtPlayerScore(String str) {
        TextView timeLeftTextView = (TextView) fragmentView.findViewById(R.id.crtPlayerScoreTextView);
        timeLeftTextView.setText(str);
    }

    public void setAdvPlayerScore(String str) {
        TextView timeLeftTextView = (TextView) fragmentView.findViewById(R.id.advPlayerScoreTextView);
        timeLeftTextView.setText(str);
    }

    public void setCrtPlayerName(String str) {
        TextView timeLeftTextView = (TextView) fragmentView.findViewById(R.id.crtPlayerNameTextView);
        timeLeftTextView.setText(str);
    }

    public void setAdvPlayerName(String str) {
        TextView timeLeftTextView = (TextView) fragmentView.findViewById(R.id.advPlayerNameTextView);
        timeLeftTextView.setText(str);
    }

    public void setImageUri(Uri uri) {
        ImageView urlImageView = (ImageView) fragmentView.findViewById(R.id.urlImageView);
        urlImageView.setImageURI(uri);
    }

    public String getAnswerEditText() {
        EditText answerEditText = (EditText) fragmentView.findViewById(R.id.answerEditText);
        if (answerEditText == null)
            return "";
        return answerEditText.getText().toString();
    }

    public IBinder getWindowsTokenAnswerEditText() {
        EditText answerEditText = (EditText) fragmentView.findViewById(R.id.answerEditText);
        return answerEditText.getWindowToken();
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void showLoading();
    }
}
