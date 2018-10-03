package com.alessandrocosma.spycameraphoneapp.myview;

import com.alessandrocosma.spycameraphoneapp.R;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.os.AsyncTask;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.TextView;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;



public class TakeAPicture extends AppCompatActivity {

    private static final String TAG = TakeAPicture.class.getSimpleName();

    private ImageView mPicture;
    private Button mLogOutButton;
    private Button mPictureButton;
    private TextView imageInfo;
    private FirebaseDatabase mDatabase;
    private FirebaseStorage mStorage;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private DatabaseReference newImageLogReference, downloadedImageLogReference, newRequestReference;
    private ChildEventListener mChildEventListener;


    long startTime = System.currentTimeMillis();

    private long initTimeOfStop = startTime;
    private long endTimeOfStop = startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_apicture);

        Log.d("TakeAPicture", "activity creata");

        mDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        /** Ottengo una referenza al campo /imagesLog/newImage del mio FirebaseDB */
        newImageLogReference = mDatabase.getReference().child("imagesLog/newImage");

        /** Ottengo una referenza al campo /request del FirebaseDB */
        newRequestReference = mDatabase.getReference().child("request").push();

        /** Ottengo una referenza al campo /imagesLog/downoadedImage del FirebaseDB */
        downloadedImageLogReference = mDatabase.getReference().child("imagesLog/downoadedImage").push();


        /** Inizializzo la mia ImageView */
        mPicture = (ImageView) findViewById(R.id.DataBaseImage);

        /** Inizioalizzo il mio TextView per le info della foto */
        imageInfo = (TextView) findViewById(R.id.imageInfo);

        /** Listener per il bottone di acquisizione foto */
        mPictureButton = (Button) findViewById(R.id.pictureButton);
        mPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                takeAPicture();
            }
        });



        /** Listener per la preseza di una nuova foto da scaricare */
        mChildEventListener = newImageLogReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                Log.d("TakeAPicture", "Nuovo figlio inserito");

                FirebaseImage newImage = dataSnapshot.getValue(FirebaseImage.class);

                String key = dataSnapshot.getKey();
                String url = newImage.getImageUrl();
                long timestamp = newImage.getTimestamp();
                Log.d(TAG, "key ="+key);
                Log.d(TAG, "url = " + url);
                Log.d(TAG, "timestamp = " + timestamp);
                // Scarico l'immagine da mostrare
                new imageDownload(mPicture).execute(url, ""+timestamp, key);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });


        /** Listener per il bottone di logout */
        mLogOutButton = (Button) findViewById(R.id.logOutButton);
        mLogOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //rimuovo il listener
                newImageLogReference.removeEventListener(mChildEventListener);

                //effettuo il signOut dell'utente
                mAuth.signOut();

                Intent intent = new Intent(TakeAPicture.this, LoginActivity.class);

                //richiamo l'activity di logIn
                startActivity(intent);

                //chiudo l'activity corrente
                TakeAPicture.this.finish();
            }
        });


    }


    private void takeAPicture(){

        Log.d(TAG, "Invio richiesta di acquisizione foto");
        //aggiungo una nuova query contenente l'id dell utente che la richiede
        newRequestReference.child("uid").setValue(user.getUid());
    }


    // Definisco l'AsyncTask che scaricherà l'immagine e la visualizzera nell' ImageView
    private class imageDownload extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;
        long timestamp;
        String key;

        public imageDownload(ImageView imageView) {
            this.imageView = imageView;
        }

        protected Bitmap doInBackground(String... addresses) {
            Bitmap bitmap = null;
            InputStream in = null;
            try {
                // 1. Recupero e definisco l'url da richiamare
                URL url = new URL(addresses[0]);
                // 1.1 Recupero anche il timestamp che convertiro in data
                timestamp = Long.valueOf(addresses[1]);
                // 1.2 Recupero la chiave del record nel FirebaseDB
                key = addresses[2];

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                // 2. Apro la connessione
                conn.connect();
                in = conn.getInputStream();
                // 3. Download e decodifico l'immagine bitmap
                bitmap = BitmapFactory.decodeStream(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            //mostro l'immagine a video
            imageView.setImageBitmap(result);

            //stampo le info su data e ora di scatto della foto
            Date date = new Date(timestamp);
            DateFormat dateFormat = new SimpleDateFormat ("'Date of photo:'    EEE, d MMM yyyy kk:mm:ss");
            imageInfo.setText(dateFormat.format (date));

            //chiamo la funzione che mi sposta il record dell'imagine scaricata,
            //dal nodo imagesLog/newImage al nodo imagesLog/downloadImage
            removeFromFirebase(newImageLogReference, downloadedImageLogReference, key);


        }
    }

    /** Funzione per spostare un record avente chiave 'key' dal path 'fromPath' al path 'toPath' */
    private void removeFromFirebase(final DatabaseReference fromPath, final DatabaseReference toPath, final String key) {
        fromPath.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            // Now "DataSnapshot" holds the key and the value at the "fromPath".
            // Let's move it to the "toPath". This operation duplicates the
            // key/value pair at the "fromPath" to the "toPath".
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                toPath.child(dataSnapshot.getKey())
                        .setValue(dataSnapshot.getValue(), new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError == null) {
                                    Log.i(TAG, "onComplete: success");
                                    // In order to complete the move, we are going to erase
                                    // the original copy by assigning null as its value.
                                    fromPath.child(key).setValue(null);
                                }
                                else {
                                    Log.e(TAG, "onComplete: failure:" + databaseError.getMessage() + ": "
                                            + databaseError.getDetails());
                                }
                            }
                        });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "onCancelled: " + databaseError.getMessage() + ": "
                        + databaseError.getDetails());
            }
        });
    }

    @Override
    protected void onStart() {

        endTimeOfStop = System.currentTimeMillis();

        /** Se il tempo di stop dell'activity è stato maggiore di 5 minuti */
        if (endTimeOfStop - initTimeOfStop > TimeUnit.MINUTES.toMillis(5)) {

            //rimuovo il listener
            newImageLogReference.removeEventListener(mChildEventListener);

            //effettuo il signOut dell'utente
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("sessionTimedOut", true);

            //richiamo l'activity di logIn
            startActivity(intent);
            super.onStart();
            //chiudo l'activity corrente
            this.finish();
        } else {
            super.onStart();
        }


    }

    @Override
    protected void onStop() {

        initTimeOfStop = System.currentTimeMillis();

        super.onStop();
    }
}

