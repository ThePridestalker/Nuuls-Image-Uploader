package com.nuuls.axel.nuulsimageuploader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Environment.getExternalStorageDirectory;
import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    public static Bitmap bitmap;
    public static TextView textView;
    private ImageView image;
    //private String picPath;
    //private String uriPicPath;
    private Button btnCamera;
    private Button btnUpload;

    private File photoFile = null;
    private String picPath;
    private String uriPicPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.imageUrl);
        btnCamera = findViewById(R.id.buttonCamera);
        btnUpload = findViewById(R.id.buttonUpload);
        image = findViewById(R.id.imageView);

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    //create the File where the photo should go
                    //File photoFile = null;
                    //asking permission to write in /Pictures
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                        } else {
                            generateToast("Accept the permissions FeelsWeirdMan");
                        }
                    }
                    // photoFile.getAbsolutePath() for Path
                    // "file://" + photoFile.getAbsolutePath() for Uri
                    try {
                        photoFile = createImageFile();
                        picPath = photoFile.getAbsolutePath();
                        uriPicPath = "file://" + photoFile.getAbsolutePath();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //if the photoFile contains the picture
                    if (photoFile != null) {
                        intent = attachFileToIntent(intent, photoFile);
                        //callback to the activity after the camera activity
                        startActivityForResult(intent, 0);
                    }
                }
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // check if the pic hasn't been taken yet so the button doesn't try to upload nothing
                if (bitmap == null) {
                    generateToast("Take a picture first :D");
                    return;
                }
                // change the color of the button to the wheel that appears when uploading in the website
                btnUpload.setBackgroundColor(Color.parseColor("#de2761"));
                btnCamera.setEnabled(false);
                btnUpload.setEnabled(false);
                btnUpload.setText("UPLOADING...");
                // TODO: add a NaM face while image is being uploaded
                //start the thread for the upload
                HttpPostTask httpPostTask = new HttpPostTask(MainActivity.this, image, btnUpload, btnCamera);
                httpPostTask.execute(picPath);
            }
        });
    }

    //gets the Uri of the file inside the FileProvider and attaches it to the intent
    private Intent attachFileToIntent(Intent intent, File photoFile) {
        Uri photoFileUri = FileProvider.getUriForFile(getApplicationContext(),
                getPackageName() + ".fileprovider",
                photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoFileUri);
        return intent;
    }

    // after getting the pic taken from the camera activity
    // the image is in the Provider
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                insertImageIntoImageView();
                //remove gps info, if there is any
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "CANCELED", Toast.LENGTH_LONG).show();
        }
    }

    // gets the image from the FileProvider through the ContentResolver and attaches it to a bitmap
    // then attaches the bitmap into the imageView to show it
    private void insertImageIntoImageView() throws IOException {
        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(uriPicPath));
        //resize the bitmap to display the pic inside the frame
        Bitmap bitmapResized = Bitmap.createScaledBitmap(bitmap, image.getWidth(), image.getHeight(), true);
        image.setImageBitmap(bitmapResized);
    }

    // creates an image file inside the FileProvider directory
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "XD_" + timeStamp + "_";
        File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  // prefix
                ".jpg",   // suffix
                storageDir      // directory
        );

        return imageFile;
    }

    private void generateToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

}
