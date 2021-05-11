package com.example.progallery.view.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.exifinterface.media.ExifInterface;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.progallery.R;
import com.example.progallery.helpers.Constant;
import com.example.progallery.model.models.Album;
import com.example.progallery.model.services.MediaFetchService;
import com.example.progallery.view.adapters.AlbumAdapter;
import com.example.progallery.view.fragments.ImageInfoFragment;
import com.example.progallery.view.fragments.SetWallpaperFragment;
import com.example.progallery.view.fragments.VideoInfoFragment;
import com.example.progallery.view.listeners.AlbumListener;
import com.example.progallery.viewmodel.AlbumViewModel;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static com.example.progallery.helpers.Constant.REQUEST_GET_LOCATION;

public class RootViewMediaActivity extends AppCompatActivity {
    protected Toolbar topToolbar;
    protected Toolbar bottomToolbar;
    protected String mediaPath;
    protected boolean isFavorite;
    AlbumViewModel albumViewModel;

    public static boolean isImageFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("image");
    }

    public static boolean isVideoFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("video");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.view_media_menu, menu);
        MediaFetchService service = MediaFetchService.getInstance();
        isFavorite = service.checkFavorite(RootViewMediaActivity.this, mediaPath);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        if (isFavorite) {
            menu.findItem(R.id.btnFavorite).setTitle("Unfavorite");
        } else {
            menu.findItem(R.id.btnFavorite).setTitle("Favorite");
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(Constant.EXTRA_REQUEST, Constant.REQUEST_RETURN);
        setResult(RESULT_OK, returnIntent);
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.deleteImage) {
            deleteMedia();
        } else if (id == R.id.btnShowInfo) {
            if (isImageFile(mediaPath)) {
                showImageInfo();
            } else if (isVideoFile(mediaPath)) {
                showVideoInfo();
            }
        } else if (id == R.id.btnAddAlbum) {
            addToAlbum();
        } else if (id == R.id.btnFavorite) {
            MediaFetchService service = MediaFetchService.getInstance();
            if (isFavorite) {
                service.removeFavorite(RootViewMediaActivity.this, mediaPath);
                Toast.makeText(RootViewMediaActivity.this, "Unfavorited", Toast.LENGTH_SHORT).show();
            } else {
                service.addFavorite(RootViewMediaActivity.this, mediaPath);
                Toast.makeText(RootViewMediaActivity.this, "Favorited", Toast.LENGTH_SHORT).show();
            }
            isFavorite = !isFavorite;
        } else if (id == R.id.btnSetAs) {
            //setImageForWallPaper();
            setImageAsWallpaper();
        } else if (id == R.id.btnSlideshow) {
            Intent intent = new Intent(RootViewMediaActivity.this, SlideshowActivity.class);
            intent.putExtra(Constant.EXTRA_PATH, mediaPath);
            startActivity(intent);
        } else if (id == R.id.btnLocation) {
            Intent intent = new Intent(RootViewMediaActivity.this, LocationPickerActivity.class);
            startActivityForResult(intent, Constant.REQUEST_GET_LOCATION);
        }

        return super.onOptionsItemSelected(item);
    }

    protected void showImageInfo() {
        ImageInfoFragment fragment = new ImageInfoFragment(mediaPath);
        fragment.show(getSupportFragmentManager(), "Image Info");
    }

    protected void showVideoInfo() {
        VideoInfoFragment fragment = new VideoInfoFragment(mediaPath);
        fragment.show(getSupportFragmentManager(), "Video Info");
    }

    protected void deleteMedia() {
        AlertDialog.Builder builder = new AlertDialog.Builder(RootViewMediaActivity.this);

        final View customDialog = getLayoutInflater().inflate(R.layout.confirm_delete_dialog, null);

        builder.setView(customDialog);
        builder.setTitle("Delete media");
        builder.setPositiveButton("OK", (dialog, which) -> {
            Intent returnIntent = new Intent();
            returnIntent.putExtra(Constant.EXTRA_REQUEST, Constant.REQUEST_REMOVE_MEDIA);

            File file = new File(mediaPath);
            if (file.exists()) {
                if (file.delete()) {
                    MediaFetchService service = MediaFetchService.getInstance();
                    boolean delRes = service.deleteMedia(getApplicationContext(), mediaPath);
                    if (delRes) {
                        setResult(RESULT_OK, returnIntent);
                    } else {
                        setResult(RESULT_CANCELED, returnIntent);
                    }
                } else {
                    setResult(RESULT_CANCELED, returnIntent);
                }
                finish();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected void addToAlbum() {
        // Show alert box with list view
        AlertDialog.Builder builder = new AlertDialog.Builder(RootViewMediaActivity.this);
        final View customDialog = getLayoutInflater().inflate(R.layout.choose_album_dialog, null);
        builder.setView(customDialog);
        builder.setTitle(R.string.choose_an_album);

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        RecyclerView recyclerView = customDialog.findViewById(R.id.album_pick_view);
        recyclerView.setHasFixedSize(true);

        AlbumAdapter albumAdapter = new AlbumAdapter(false);
        recyclerView.setAdapter(albumAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(RootViewMediaActivity.this));

        ViewModelProvider.AndroidViewModelFactory factory = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication());
        albumViewModel = new ViewModelProvider(this, factory).get(AlbumViewModel.class);
        albumViewModel.getAlbumsObserver().observe(this, albumList -> {
            if (albumList == null) {
                Toast.makeText(RootViewMediaActivity.this, "Error in fetching data", Toast.LENGTH_SHORT).show();
            } else {
                albumAdapter.setAlbumList(albumList);
            }
        });
        albumViewModel.callService(RootViewMediaActivity.this);

        AlertDialog dialog = builder.create();
        dialog.show();

        albumAdapter.setAlbumListener(new AlbumListener() {
            @Override
            public void onAlbumClick(Album album) {
                dialog.dismiss();

                // Show alert box with copy or move
                AlertDialog.Builder builder = new AlertDialog.Builder(RootViewMediaActivity.this);
                builder.setTitle("Choose a method");

                String[] options = {"Move to album", "Copy to album"};
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                moveMedia(album);
                                break;
                            case 1:
                                copyMedia(album);
                                break;
                        }
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }

            @Override
            public void onOptionAlbumClick(Album album) {
            }
        });
    }

    private void moveMedia(Album targetAlbum) {
        String imageName = mediaPath.substring(mediaPath.lastIndexOf('/'));
        String newPath = targetAlbum.getAlbumPath() + imageName;

        boolean fileMoved = true;
        try {
            Files.move(Paths.get(mediaPath), Paths.get(newPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            fileMoved = false;
            e.printStackTrace();
        }

        if (fileMoved) {
            MediaFetchService service = MediaFetchService.getInstance();
            boolean updateMediaStore = service.updateMediaAlbum(RootViewMediaActivity.this, mediaPath, newPath);
            if (updateMediaStore) {
                Toast.makeText(RootViewMediaActivity.this, "Moved to album", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(RootViewMediaActivity.this, "Failed to move to media", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void copyMedia(Album targetAlbum) {
        String newTitle = mediaPath.substring(mediaPath.lastIndexOf('/'), mediaPath.lastIndexOf('.')) + "_1";
        String fileType = mediaPath.substring(mediaPath.lastIndexOf('.'));
        String newPath = targetAlbum.getAlbumPath() + newTitle + fileType;

        boolean fileCopied = true;
        try {
            Files.copy(Paths.get(mediaPath), Paths.get(newPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            fileCopied = false;
            e.printStackTrace();
        }

        if (fileCopied) {
            MediaFetchService service = MediaFetchService.getInstance();
            boolean addToMediaStore = service.addNewMedia(RootViewMediaActivity.this, newPath, newTitle);
            if (addToMediaStore) {
                Toast.makeText(RootViewMediaActivity.this, "Copied to album", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(RootViewMediaActivity.this, "Failed to copy to media", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setImageForWallPaper() {
        SetWallpaperFragment setWallpaperFragment = new SetWallpaperFragment(mediaPath);
        setWallpaperFragment.show(getSupportFragmentManager(), setWallpaperFragment.getTag());
    }

    private void setImageAsWallpaper() {
        Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setDataAndType(Uri.parse(mediaPath), "image/jpeg");
        intent.putExtra("mimeType", "image/jpeg");
        startActivity(Intent.createChooser(intent, "Set as:"));
    }

    public void shareMedia() {
        try {
            Uri shareBitmap = Uri.parse(mediaPath);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, shareBitmap);
            startActivity(Intent.createChooser(intent, "Share via"));
        } catch (Exception e) {
            Toast.makeText(this, "Share error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        assert data != null;
        if (requestCode == REQUEST_GET_LOCATION) {
            if (resultCode == RESULT_OK) {
                double latitudeGet = data.getDoubleExtra("lat", 0);
                double longitudeGet = data.getDoubleExtra("long", 0);
                try {
                    ExifInterface exif = new ExifInterface(mediaPath);
                    String longitude = gpsInfoConvert(longitudeGet);
                    String latitude = gpsInfoConvert(latitudeGet);
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, (longitudeGet > 0) ? "E" : "W");
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, (latitudeGet > 0) ? "N" : "S");
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, longitude);
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latitude);
                    exif.saveAttributes();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(RootViewMediaActivity.this, "Set location failed", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(RootViewMediaActivity.this, "Set location successfully", Toast.LENGTH_SHORT).show();
            }
        }
    }

    String gpsInfoConvert(Double coordinate) {
        Double coord = coordinate;
        StringBuilder sb = new StringBuilder();

        if (coord < 0) {
            coord = -coord;
        }
        int degrees = (int) Math.floor(coord);
        sb.append(degrees);
        sb.append("/1,");
        coord -= (double) degrees;
        coord *= 60.0;
        int minutes = (int) Math.floor(coord);
        sb.append(minutes);
        sb.append("/1,");
        coord -= (double) minutes;
        coord *= 60.0;
        sb.append((int) Math.floor(coord));
        sb.append("/1");
        return sb.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}