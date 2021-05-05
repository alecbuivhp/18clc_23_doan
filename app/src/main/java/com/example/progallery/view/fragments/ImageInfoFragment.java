package com.example.progallery.view.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.DialogFragment;

import com.example.progallery.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class ImageInfoFragment extends DialogFragment {

    TextView imgSource, imgLength, imgWidth, imgOrientation, imgDateTime, imgLocation, imgCamera;
    ExifInterface exif;
    private String mediaPath;

    public ImageInfoFragment(String mediaPath) {
        this.mediaPath = mediaPath;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();
        View view = inflater.inflate(R.layout.image_details, null);

        builder.setView(view)
                .setTitle("Image Information")
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        imgSource = view.findViewById(R.id.imgSource);
        imgLength = view.findViewById(R.id.imgLength);
        imgWidth = view.findViewById(R.id.imgWidth);
        imgDateTime = view.findViewById(R.id.imgDateTime);
        imgOrientation = view.findViewById(R.id.imgOrientation);
        imgLocation = view.findViewById(R.id.imgLocation);
        try {
            Uri uri = Uri.fromFile(new File(mediaPath));
            InputStream in = Objects.requireNonNull(getContext()).getApplicationContext().getContentResolver().openInputStream(uri);
            exif = new ExifInterface(in);

            imgSource.setText(exif.getAttribute(ExifInterface.TAG_FILE_SOURCE));
            imgLength.setText(exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH));
            imgWidth.setText(exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH));
            imgDateTime.setText(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL));
            imgOrientation.setText(exif.getAttribute(ExifInterface.TAG_ORIENTATION));
            imgLocation.setText(exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.create();
    }
}
