package com.nuuls.axel.nuulsimageuploader;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements HttpPostTask.onUploadedCallback {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int CAMERA_REQUEST_CODE = 2;
    private TextView textView;
    private ImageView image;
    private Button btnCamera;
    private Button btnUpload;

    private File photoFile;
    private Uri photoFileUri;
    private String picPath;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.imageUrl);
        btnCamera = findViewById(R.id.buttonCamera);
        btnUpload = findViewById(R.id.buttonUpload);
        image = findViewById(R.id.imageView);

        if (bitmap != null) {
            image.setImageBitmap(bitmap);
        } else {
            image.setImageResource(R.drawable.nuulslogo);
        }

        // Handling the intent coming from other app
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            // if im receiving an image from other app
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if (type.startsWith("image/")) {
                    handleSendImage(intent);
                }
            }
        } else {
            //this never reaches because with no permission granted the intent coming from outside stops
            forceAcceptPermission();
        }

        if (intent.resolveActivity(getPackageManager()) != null) {
            // asking permission to write in /Pictures
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                // if it has no permission, asks for it
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    enablePermission();
                }
            }
        }

        btnCamera.setOnClickListener(v -> {
            // if the user granted permission to save files
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                try {
                    photoFile = createImageFile();
                    picPath = photoFile.getAbsolutePath();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // if the photoFile contains the picture
                if (photoFile != null) {
                    // callback to the activity after the camera activity
                    startActivityForResult(getPhotoFileUri(photoFile), CAMERA_REQUEST_CODE);
                }
            } else {
                forceAcceptPermission();
            }
        });

        btnUpload.setOnClickListener(v -> {
            // if the user granted permission to save files
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                // check if the pic hasn't been taken yet so the button doesn't try to upload nothing
                // and crashes the app 4HEad
                if (bitmap == null) {
                    generateToast("Take a picture first :D");
                    return;
                }
                // change the color of the button to the wheel that appears when uploading in the website
                btnUpload.setBackgroundColor(Color.parseColor("#de2761"));
                btnCamera.setEnabled(false);
                btnUpload.setEnabled(false);
                btnUpload.setText(R.string.uploading);
                //start the thread for the upload
                HttpPostTask httpPostTask = new HttpPostTask(MainActivity.this);
                httpPostTask.execute(picPath);
            } else {
                forceAcceptPermission();
            }
        });
    }

    private void forceAcceptPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            // if the user didnt accept the permissions
            enablePermission();
            // if the user checked the "never show again"
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                enablePermission();
            }
        }
    }

    private void enablePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    private void handleSendImage(Intent intent) {
        photoFileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        image.setImageURI(photoFileUri);

        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoFileUri);
            photoFile = createImageFile();
            picPath = photoFile.getAbsolutePath();

            FileOutputStream fos = new FileOutputStream(photoFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            removeExifGeolocation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // gets the URI of the photo and attaches the uri to the picture
    private Intent getPhotoFileUri(File photoFile) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // returns a content uri for a given file
        photoFileUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider",
                photoFile);
        // attaches the picture to the uri of the file we created before
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoFileUri);
        return intent;
    }

    // after getting the pic taken from the camera activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CAMERA_REQUEST_CODE) {
            try {
                image.setImageURI(photoFileUri);
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoFileUri);
                removeExifGeolocation();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (resultCode == RESULT_CANCELED) {
            // this deletes the empty file that gets created before taking the picture in case you cancel it
            getContentResolver().delete(photoFileUri, null, null);
            generateToast(getString(R.string.canceled));
        }
    }

    private void removeExifGeolocation() throws IOException {
        // remove gps info, if there is any
        ExifInterface exif = new ExifInterface(picPath);
        String[] attributes = new String[]{
                ExifInterface.TAG_GPS_VERSION_ID,
                ExifInterface.TAG_GPS_AREA_INFORMATION,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP,
        };
        for (String attribute : attributes) {
            if (exif.getAttribute(attribute) != null)
                exif.setAttribute(attribute, null);
        }
        exif.saveAttributes();
    }

    @Override
    public void onUpload(String result) {
        generateToast("Copied: " + result);

        // copy the url to the clipboard
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("URL from nuuls server xd", result);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }

        // reset the image to nuulsLogo
        image.setImageResource(R.drawable.nuulslogo);
        //display the url
        textView.setText(result);
        btnUpload.setBackgroundColor(Color.parseColor("#4caf50"));
        btnUpload.setText(getString(R.string.upload));
        btnCamera.setEnabled(true);
        btnUpload.setEnabled(true);
        //reset the bitmap so you cant upload the same thing infinitely
        bitmap = null;
    }

    // creates an image file inside the FileProvider directory
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "XD_" + timeStamp + "_";

        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(
                imageFileName,  // prefix
                ".jpg",   // suffix
                storageDir      // directory
        );
    }

    private void generateToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

}
