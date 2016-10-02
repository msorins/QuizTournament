package com.mirceasorinsebastian.logoquizztournament;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;

public class AddQuizzActivity extends AppCompatActivity implements View.OnClickListener {

    public Uri imageInternalLink = null;
    public Uri imageExternalLink = null;
    public String answerText = null;
    public Integer nrTotalQuizzes = -1;
    ImageView uploadImageView;

    private static final int FILE_SELECT_CODE = 0;

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/jpeg");
        intent.setType("image/png");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d("Select file", "File Uri: " + uri.toString());
                    imageInternalLink = uri;
                    uploadImageView.setImageURI(uri);
                    //uploadImageView.setMaxHeight(100);
                    // Get the path
                    String path = null;
                    try {
                        path = getPath(this, uri);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    Log.d("Select file", "File Path: " + path);
                    // Get the file instance
                    // File file = new File(path);
                    // Initiate the upload
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_quizz);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(imageInternalLink != null) {
                    uploadFile(imageInternalLink);
                }
                else
                    Snackbar.make(view, "Please upload a picture too", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Background part
        View view = (View) findViewById(android.R.id.content);

        //Repeating background image
        Bitmap backgroundImage = BitmapFactory.decodeResource(getResources(), R.drawable.pattern2);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), backgroundImage);
        bitmapDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        view.setBackground(bitmapDrawable);

        findViewById(R.id.selectImageButton).setOnClickListener((View.OnClickListener) this);
        uploadImageView = (ImageView) findViewById(R.id.uploadImageView);


        final EditText answer = (EditText) findViewById(R.id.logoQuizzAnswerEditText);
        Log.i("Answer text:", answer.getText().toString());

        answer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                answerText = charSequence.toString();
                Log.i("Answer text:", answerText);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


        getNrTotalQuizzes();
    }

    public void uploadFile(Uri s) {
        FirebaseStorage storage = FirebaseStorage.getInstance();

        // Create a storage reference from our app
        StorageReference storageRef = storage.getReferenceFromUrl("gs://logoquizz-tournament.appspot.com");

        Uri file = s;
        String extension = s.getPath().replaceFirst("^.*/[^/]*(\\.[^\\./]*|)$", "$1");

        Integer crt = nrTotalQuizzes + 1;
        StorageReference riversRef = storageRef.child("imgQuizzes/"+crt+extension);
        UploadTask uploadTask = riversRef.putFile(file);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                imageExternalLink = taskSnapshot.getDownloadUrl();
                Log.i("File that was just uploaded: ", imageExternalLink.toString());
                makeQuizz(answerText, imageExternalLink);
            }
        });
    }

    public void makeQuizz(String answer, Uri img) {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        //Increment Number of Total Quizzes
        DatabaseReference stats = database.getReference("stats").child("NRQUIZZES");
        nrTotalQuizzes++;
        stats.setValue(nrTotalQuizzes);

        //Add a new Quizz
        DatabaseReference quizzesRef = database.getReference("pendingQuizzes");
        HashMap key = new HashMap();  HashMap info = new HashMap();
        info.put("ANSWER", answer.toString());
        info.put("URL", img.toString());
        info.put("BY", GoogleSignInActivity.user.getUid().toString());

        key.put(nrTotalQuizzes.toString(), info);

        quizzesRef.updateChildren(key, null);

        Snackbar.make(findViewById(android.R.id.content), "Done, thank you :)", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();

    }

    public void getNrTotalQuizzes() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        DatabaseReference getFromDB = database.getReference("stats/NRQUIZZES");
        getFromDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                nrTotalQuizzes = Integer.valueOf( dataSnapshot.getValue().toString() );
                Log.i("nrTotalQuizzes: ", nrTotalQuizzes.toString());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.i("dbOnChangeFailed", "Failed to read value.", error.toException());
            }
        });
    }


    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.selectImageButton) {
            showFileChooser();
        }
    }

}
