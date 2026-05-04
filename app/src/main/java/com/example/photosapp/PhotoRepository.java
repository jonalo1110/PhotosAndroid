package com.example.photosapp;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class PhotoRepository {
    private static final String PREFS = "photos_repository";
    private static final String KEY_ALBUMS = "albums";

    private final SharedPreferences preferences;
    public final List<Album> albums = new ArrayList<>();

    public PhotoRepository(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    public void load() {
        albums.clear();
        String raw = preferences.getString(KEY_ALBUMS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                albums.add(Album.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
            albums.clear();
        }
    }

    public void save() {
        JSONArray array = new JSONArray();
        try {
            for (Album album : albums) {
                array.put(album.toJson());
            }
            preferences.edit().putString(KEY_ALBUMS, array.toString()).apply();
        } catch (JSONException ignored) {
            // In-memory data remains usable; a later valid save can recover persistence.
        }
    }

    public Album findAlbum(String id) {
        for (Album album : albums) {
            if (album.id.equals(id)) {
                return album;
            }
        }
        return null;
    }

    public Photo findPhoto(String albumId, String photoId) {
        Album album = findAlbum(albumId);
        if (album == null) {
            return null;
        }
        for (Photo photo : album.photos) {
            if (photo.id.equals(photoId)) {
                return photo;
            }
        }
        return null;
    }

    public boolean hasAlbumName(String name, String exceptId) {
        for (Album album : albums) {
            boolean sameAlbum = exceptId != null && album.id.equals(exceptId);
            if (!sameAlbum && album.name.equalsIgnoreCase(name.trim())) {
                return true;
            }
        }
        return false;
    }
}
