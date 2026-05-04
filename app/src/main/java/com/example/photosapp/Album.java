package com.example.photosapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Album {
    public final String id;
    public String name;
    public final List<Photo> photos;

    public Album(String name) {
        this(UUID.randomUUID().toString(), name, new ArrayList<>());
    }

    public Album(String id, String name, List<Photo> photos) {
        this.id = id;
        this.name = name;
        this.photos = photos;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("name", name);
        JSONArray photoArray = new JSONArray();
        for (Photo photo : photos) {
            photoArray.put(photo.toJson());
        }
        object.put("photos", photoArray);
        return object;
    }

    public static Album fromJson(JSONObject object) throws JSONException {
        JSONArray photoArray = object.optJSONArray("photos");
        List<Photo> photos = new ArrayList<>();
        if (photoArray != null) {
            for (int i = 0; i < photoArray.length(); i++) {
                photos.add(Photo.fromJson(photoArray.getJSONObject(i)));
            }
        }
        return new Album(object.getString("id"), object.getString("name"), photos);
    }
}
