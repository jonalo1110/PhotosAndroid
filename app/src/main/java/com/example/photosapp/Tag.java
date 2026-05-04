package com.example.photosapp;

import org.json.JSONException;
import org.json.JSONObject;

public class Tag {
    public static final String PERSON = "person";
    public static final String LOCATION = "location";

    public final String type;
    public final String value;

    public Tag(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("type", type);
        object.put("value", value);
        return object;
    }

    public static Tag fromJson(JSONObject object) throws JSONException {
        return new Tag(object.getString("type"), object.getString("value"));
    }

    public boolean sameAs(Tag other) {
        return type.equalsIgnoreCase(other.type) && value.equalsIgnoreCase(other.value);
    }

    public String displayText() {
        return type + ": " + value;
    }
}
