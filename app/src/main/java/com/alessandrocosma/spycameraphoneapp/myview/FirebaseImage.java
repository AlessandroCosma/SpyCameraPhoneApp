package com.alessandrocosma.spycameraphoneapp.myview;

import java.util.HashMap;
import java.util.Map;

/** Classe che mi rappresenta un'immagine che scarico dal FirebaseDB */

public class FirebaseImage {

    private String imageUrl;
    private long timestamp;

    public FirebaseImage(){
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public FirebaseImage(String imageUrl, long timestamp){
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("imageUrl", imageUrl);
        result.put("timestamp", timestamp);

        return result;
    }

}
