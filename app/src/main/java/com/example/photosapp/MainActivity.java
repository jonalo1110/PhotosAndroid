package com.example.photosapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int PICK_PHOTO = 42;
    private static final String[] TAG_TYPES = {Tag.PERSON, Tag.LOCATION};

    private PhotoRepository repository;
    private String currentAlbumId;
    private int currentPhotoIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new PhotoRepository(this);
        showHome();
    }

    @Override
    public void onBackPressed() {
        if (currentAlbumId != null) {
            currentAlbumId = null;
            showHome();
        } else {
            super.onBackPressed();
        }
    }

    private void showHome() {
        currentAlbumId = null;
        setContentView(R.layout.activity_home);
        findViewById(R.id.createAlbumButton).setOnClickListener(v -> promptCreateAlbum());
        findViewById(R.id.searchButton).setOnClickListener(v -> showSearch());
        renderAlbumList();
    }

    private void renderAlbumList() {
        LinearLayout list = findViewById(R.id.albumList);
        TextView empty = findViewById(R.id.emptyHomeText);
        list.removeAllViews();
        empty.setVisibility(repository.albums.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Album album : repository.albums) {
            View row = inflater.inflate(R.layout.row_album, list, false);
            ((TextView) row.findViewById(R.id.albumNameText)).setText(album.name);
            ((TextView) row.findViewById(R.id.albumCountText)).setText(album.photos.size() + " photo(s)");
            row.findViewById(R.id.openAlbumButton).setOnClickListener(v -> showAlbum(album.id));
            row.findViewById(R.id.renameAlbumButton).setOnClickListener(v -> promptRenameAlbum(album));
            row.findViewById(R.id.deleteAlbumButton).setOnClickListener(v -> confirmDeleteAlbum(album));
            list.addView(row);
        }
    }

    private void promptCreateAlbum() {
        promptForText("Create album", "Album name", "", value -> {
            if (repository.hasAlbumName(value, null)) {
                toast("An album with that name already exists.");
                return;
            }
            repository.albums.add(new Album(value.trim()));
            repository.save();
            renderAlbumList();
        });
    }

    private void promptRenameAlbum(Album album) {
        promptForText("Rename album", "Album name", album.name, value -> {
            if (repository.hasAlbumName(value, album.id)) {
                toast("An album with that name already exists.");
                return;
            }
            album.name = value.trim();
            repository.save();
            renderAlbumList();
        });
    }

    private void confirmDeleteAlbum(Album album) {
        new AlertDialog.Builder(this)
                .setTitle("Delete album")
                .setMessage("Delete \"" + album.name + "\" and its photos from this app?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.albums.remove(album);
                    repository.save();
                    renderAlbumList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAlbum(String albumId) {
        currentAlbumId = albumId;
        setContentView(R.layout.activity_album);
        Album album = repository.findAlbum(albumId);
        if (album == null) {
            showHome();
            return;
        }
        ((TextView) findViewById(R.id.albumTitle)).setText(album.name);
        findViewById(R.id.backHomeButton).setOnClickListener(v -> showHome());
        findViewById(R.id.addPhotoButton).setOnClickListener(v -> pickPhoto());
        renderPhotoList(album);
    }

    private void renderPhotoList(Album album) {
        LinearLayout list = findViewById(R.id.photoList);
        TextView empty = findViewById(R.id.emptyAlbumText);
        list.removeAllViews();
        empty.setVisibility(album.photos.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < album.photos.size(); i++) {
            Photo photo = album.photos.get(i);
            View row = inflater.inflate(R.layout.row_photo, list, false);
            ImageView thumb = row.findViewById(R.id.photoThumb);
            thumb.setImageURI(Uri.parse(photo.uri));
            ((TextView) row.findViewById(R.id.photoNameText)).setText(photo.name);
            ((TextView) row.findViewById(R.id.photoTagsText)).setText(photo.tagSummary());
            int index = i;
            row.findViewById(R.id.viewPhotoButton).setOnClickListener(v -> showPhoto(album.id, index));
            row.findViewById(R.id.movePhotoButton).setOnClickListener(v -> promptMovePhoto(album, photo));
            row.findViewById(R.id.removePhotoButton).setOnClickListener(v -> confirmRemovePhoto(album, photo));
            list.addView(row);
        }
    }

    private void pickPhoto() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PHOTO || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Album album = repository.findAlbum(currentAlbumId);
        if (album == null) {
            return;
        }
        Uri uri = data.getData();
        int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
        getContentResolver().takePersistableUriPermission(uri, flags);
        album.photos.add(new Photo(uri.toString(), displayNameFor(uri)));
        repository.save();
        showAlbum(album.id);
    }

    private String displayNameFor(Uri uri) {
        String result = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null || result.trim().isEmpty()) {
            result = uri.getLastPathSegment();
        }
        return result == null ? "Photo" : result;
    }

    private void confirmRemovePhoto(Album album, Photo photo) {
        new AlertDialog.Builder(this)
                .setTitle("Remove photo")
                .setMessage("Remove \"" + photo.name + "\" from this album?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    album.photos.remove(photo);
                    repository.save();
                    showAlbum(album.id);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptMovePhoto(Album source, Photo photo) {
        List<Album> targets = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Album album : repository.albums) {
            if (!album.id.equals(source.id)) {
                targets.add(album);
                names.add(album.name);
            }
        }
        if (targets.isEmpty()) {
            toast("Create another album before moving a photo.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Move to album")
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    source.photos.remove(photo);
                    targets.get(which).photos.add(photo);
                    repository.save();
                    showAlbum(source.id);
                })
                .show();
    }

    private void showPhoto(String albumId, int index) {
        Album album = repository.findAlbum(albumId);
        if (album == null || album.photos.isEmpty()) {
            showAlbum(albumId);
            return;
        }
        currentAlbumId = albumId;
        currentPhotoIndex = Math.max(0, Math.min(index, album.photos.size() - 1));
        setContentView(R.layout.activity_photo);
        findViewById(R.id.backAlbumButton).setOnClickListener(v -> showAlbum(albumId));
        setupTagTypeSpinner(findViewById(R.id.tagTypeSpinner));
        findViewById(R.id.prevPhotoButton).setOnClickListener(v -> showPhoto(albumId, currentPhotoIndex - 1));
        findViewById(R.id.nextPhotoButton).setOnClickListener(v -> showPhoto(albumId, currentPhotoIndex + 1));
        findViewById(R.id.addTagButton).setOnClickListener(v -> addTagFromInputs(album));
        renderPhotoDisplay(album);
    }

    private void renderPhotoDisplay(Album album) {
        Photo photo = album.photos.get(currentPhotoIndex);
        ((TextView) findViewById(R.id.photoTitle)).setText(photo.name);
        ((ImageView) findViewById(R.id.photoFull)).setImageURI(Uri.parse(photo.uri));
        ((TextView) findViewById(R.id.photoPositionText)).setText((currentPhotoIndex + 1) + " of " + album.photos.size());
        findViewById(R.id.prevPhotoButton).setEnabled(currentPhotoIndex > 0);
        findViewById(R.id.nextPhotoButton).setEnabled(currentPhotoIndex < album.photos.size() - 1);
        renderTagList(photo);
    }

    private void renderTagList(Photo photo) {
        LinearLayout list = findViewById(R.id.tagList);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        if (photo.tags.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No tags");
            empty.setTextColor(getColor(R.color.muted));
            empty.setTextSize(15);
            list.addView(empty);
            return;
        }
        for (Tag tag : new ArrayList<>(photo.tags)) {
            View row = inflater.inflate(R.layout.row_tag, list, false);
            ((TextView) row.findViewById(R.id.tagText)).setText(tag.displayText());
            row.findViewById(R.id.deleteTagButton).setOnClickListener(v -> {
                photo.tags.remove(tag);
                repository.save();
                renderTagList(photo);
            });
            list.addView(row);
        }
    }

    private void addTagFromInputs(Album album) {
        Photo photo = album.photos.get(currentPhotoIndex);
        Spinner spinner = findViewById(R.id.tagTypeSpinner);
        EditText input = findViewById(R.id.tagValueInput);
        String value = input.getText().toString().trim();
        if (value.isEmpty()) {
            toast("Enter a tag value.");
            return;
        }
        Tag tag = new Tag((String) spinner.getSelectedItem(), value);
        for (Tag existing : photo.tags) {
            if (existing.sameAs(tag)) {
                toast("That tag is already on this photo.");
                return;
            }
        }
        photo.tags.add(tag);
        repository.save();
        input.setText("");
        hideKeyboard(input);
        renderTagList(photo);
    }

    private void showSearch() {
        currentAlbumId = null;
        setContentView(R.layout.activity_search);
        findViewById(R.id.backSearchButton).setOnClickListener(v -> showHome());
        setupTagTypeSpinner(findViewById(R.id.searchTypeOne));
        setupTagTypeSpinner(findViewById(R.id.searchTypeTwo));
        setupAutocomplete((AutoCompleteTextView) findViewById(R.id.searchValueOne));
        setupAutocomplete((AutoCompleteTextView) findViewById(R.id.searchValueTwo));
        findViewById(R.id.runSearchButton).setOnClickListener(v -> runSearch());
        ((TextView) findViewById(R.id.searchStatusText)).setText("Searches include every album.");
    }

    private void setupAutocomplete(AutoCompleteTextView input) {
        input.setThreshold(1);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<String> suggestions = suggestionsStartingWith(s.toString());
                input.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_dropdown_item_1line, suggestions));
                if (!suggestions.isEmpty()) {
                    input.showDropDown();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private List<String> suggestionsStartingWith(String prefix) {
        String lower = prefix.trim().toLowerCase(Locale.US);
        Set<String> values = new LinkedHashSet<>();
        if (lower.isEmpty()) {
            return new ArrayList<>();
        }
        for (Album album : repository.albums) {
            for (Photo photo : album.photos) {
                for (Tag tag : photo.tags) {
                    if (tag.value.toLowerCase(Locale.US).startsWith(lower)) {
                        values.add(tag.value);
                    }
                }
            }
        }
        return new ArrayList<>(values);
    }

    private void runSearch() {
        String typeOne = (String) ((Spinner) findViewById(R.id.searchTypeOne)).getSelectedItem();
        String valueOne = ((AutoCompleteTextView) findViewById(R.id.searchValueOne)).getText().toString().trim();
        String typeTwo = (String) ((Spinner) findViewById(R.id.searchTypeTwo)).getSelectedItem();
        String valueTwo = ((AutoCompleteTextView) findViewById(R.id.searchValueTwo)).getText().toString().trim();
        boolean hasOne = !valueOne.isEmpty();
        boolean hasTwo = !valueTwo.isEmpty();
        if (!hasOne && !hasTwo) {
            toast("Enter at least one tag value.");
            return;
        }
        boolean useAnd = ((RadioButton) findViewById(R.id.andRadio)).isChecked();
        List<SearchResult> results = new ArrayList<>();
        for (Album album : repository.albums) {
            for (int i = 0; i < album.photos.size(); i++) {
                Photo photo = album.photos.get(i);
                boolean one = hasOne && matches(photo, typeOne, valueOne);
                boolean two = hasTwo && matches(photo, typeTwo, valueTwo);
                boolean include = hasOne && hasTwo ? (useAnd ? one && two : one || two) : one || two;
                if (include) {
                    results.add(new SearchResult(album, photo, i));
                }
            }
        }
        renderSearchResults(results);
    }

    private boolean matches(Photo photo, String type, String value) {
        String lower = value.toLowerCase(Locale.US);
        for (Tag tag : photo.tags) {
            boolean typeMatch = tag.type.equalsIgnoreCase(type);
            boolean valueMatch = tag.value.toLowerCase(Locale.US).startsWith(lower);
            if (typeMatch && valueMatch) {
                return true;
            }
        }
        return false;
    }

    private void renderSearchResults(List<SearchResult> results) {
        LinearLayout list = findViewById(R.id.searchResultsList);
        TextView status = findViewById(R.id.searchStatusText);
        list.removeAllViews();
        status.setText(results.size() + " result(s)");
        LayoutInflater inflater = LayoutInflater.from(this);
        for (SearchResult result : results) {
            View row = inflater.inflate(R.layout.row_photo, list, false);
            ((ImageView) row.findViewById(R.id.photoThumb)).setImageURI(Uri.parse(result.photo.uri));
            ((TextView) row.findViewById(R.id.photoNameText)).setText(result.photo.name + " (" + result.album.name + ")");
            ((TextView) row.findViewById(R.id.photoTagsText)).setText(result.photo.tagSummary());
            row.findViewById(R.id.viewPhotoButton).setOnClickListener(v -> showPhoto(result.album.id, result.index));
            row.findViewById(R.id.movePhotoButton).setVisibility(View.GONE);
            row.findViewById(R.id.removePhotoButton).setVisibility(View.GONE);
            list.addView(row);
        }
    }

    private void setupTagTypeSpinner(Spinner spinner) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TAG_TYPES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void promptForText(String title, String hint, String initialValue, TextCallback callback) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setText(initialValue);
        input.setSelectAllOnFocus(true);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setPadding(pad, 0, pad, 0);
        wrapper.addView(input);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(wrapper)
                .setPositiveButton("OK", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        toast("Name cannot be empty.");
                    } else {
                        callback.onText(value);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        input.requestFocus();
    }

    private void hideKeyboard(View view) {
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private interface TextCallback {
        void onText(String value);
    }

    private static class SearchResult {
        final Album album;
        final Photo photo;
        final int index;

        SearchResult(Album album, Photo photo, int index) {
            this.album = album;
            this.photo = photo;
            this.index = index;
        }
    }
}
