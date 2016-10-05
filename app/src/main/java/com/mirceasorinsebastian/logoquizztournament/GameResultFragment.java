package com.mirceasorinsebastian.logoquizztournament;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;


public class GameResultFragment extends Fragment {
    private View fragmentView;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    UserStats userStats;

    public GameResultFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        userStats = (UserStats) getArguments().getSerializable(UserStats.EXTRA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_game_result, container, false);

        //Set game winner message
        TextView resultTextView = (TextView) fragmentView.findViewById(R.id.resultTextView);
        if (getArguments() != null) {
            mParam1 = getArguments().getString("message");
            mParam2 = getArguments().getString("snackBarMsg");
            resultTextView.setText(mParam1);
            if(!mParam2.equals(""))
                Snackbar.make(fragmentView, mParam2, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
        }

        //Set playAgainButton Listener
        Button playAgainButton = (Button) fragmentView.findViewById(R.id.playAgainButton);
        playAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playAgain();
            }
        });

        return fragmentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
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

    public interface OnFragmentInteractionListener {
        void showLoading();
    }

    public void playAgain() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference queueRef = database.getReference("queue");

        if(userStats.getUserQP() >= 10) {
            HashMap key = new HashMap(); HashMap key2 = new HashMap();

            key2.put("type", "newGameTwoRequest");
            key2.put("QP", Integer.toString(userStats.getUserQP()));
            key2.put("ENTERTIME", String.valueOf(System.currentTimeMillis()));

            key.put(GoogleSignInActivity.user.getUid().toString(), key2);
            queueRef.updateChildren(key, null);

            if (mListener != null)
                mListener.showLoading();
        } else {
            Snackbar.make(fragmentView, "Not enough QP", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
        }
    }
}
