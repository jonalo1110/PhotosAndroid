package com.example.photosapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Photo {
    public final String id;
    public final String uri;
    public final String name;
    public final List<Tag> tags;

    public Photo(String uri, String name) {
        this(UUID.randomUUID().toString(), uri, name, new ArrayList<>());
    }

    public Photo(String id, String uri, String name, List<Tag> tags) {
        this.id = id;
        this.uri = uri;
        this.name = name;
        this.tags = tags;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("uri", uri);
        object.put("name", name);
        JSONArray tagArray = new JSONArray();
        for (Tag tag : tags) {
            tagArray.put(tag.toJson());
        }
        object.put("tags", tagArray);
        return object;
    }

    public static Photo fromJson(JSONObject object) throws JSONException {
        JSONArray tagArray = object.optJSONArray("tags");
        List<Tag> tags = new ArrayList<>();
        if (tagArray != null) {
            for (int i = 0; i < tagArray.length(); i++) {
                tags.add(Tag.fromJson(tagArray.getJSONObject(i)));
            }
        }
        return new Photo(
                object.getString("id"),
                object.getString("uri"),
                object.getString("name"),
                tags
        );
    }

    public String tagSummary() {
        if (tags.isEmpty()) {
            return "No tags";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append(tags.get(i).displayText());
        }
        return builder.toString();
    }
}
