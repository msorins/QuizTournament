package com.mirceasorinsebastian.quiztournament;

import android.content.SharedPreferences;
import android.util.Log;


import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;


public class myFirebaseInstanceIDService extends FirebaseInstanceIdService {




    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.i("Token", "Refreshed token: " + refreshedToken);

        //Get userID
        SharedPreferences sharedPreferences = getSharedPreferences("com.mirceasorinsebastian.logoquizztournament", MODE_PRIVATE);
        String userID = sharedPreferences.getString("userID", "");

        if(!userID.equals("")){
            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            database.getReference("connectedUsers").child(userID).child("TOKEN").setValue(refreshedToken);
        }


        sendRegistrationToServer(refreshedToken);

    }

    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
    }
}
