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

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    public static Bitmap bitmap;
    public static TextView textView;
    private ImageView image;
    private String picPath;
    private String uriPicPath;
    private Button btnCamera;
    private Button btnUpload;

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
                    File photoFile = null;
                    //asking permission to write in /Pictures
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                        } else {
                            generateToast("Accept the permissions FeelsWeirdMan");
                        }
                    }
                    try {
                        photoFile = createImageFile();
                    } catch (IOException e) {
                        // Error occurred while creating the file
                        e.printStackTrace();
                    }
                    if (photoFile != null) {
                        FileProvider.getUriForFile(getApplicationContext(),
                                getPackageName() + ".fileprovider",
                                photoFile);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                                FileProvider.getUriForFile(getApplicationContext(),
                                        getPackageName() + ".fileprovider",
                                        photoFile));
                        startActivityForResult(intent, 0);
                    }
                }
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                btnUpload.setText("UPLOADING...");
                //start the thread for the upload
                HttpPostTask httpPostTask = new HttpPostTask(MainActivity.this, image, btnUpload, btnCamera);
                httpPostTask.execute(picPath);
            }
        });
    }

    // after getting the pic taken from the camera app
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                //TODO: RESIZE THE PIC BEFORE INSERTING IT IN THE IMAGEVIEW
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(uriPicPath));
                //resize the bitmap to display the pic inside the frame
                Bitmap bitmapResized = Bitmap.createScaledBitmap(bitmap, image.getWidth(), image.getHeight(), true);
                image.setImageBitmap(bitmapResized);
                //remove gps info, if there is any
                ExifInterface exif = new ExifInterface(picPath);
                String[] attributes = new String[] {
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

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "XD_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  // prefix
                ".jpg",   // suffix
                storageDir      // directory
        );

        // to recover the pic and put it in the bitmap
        uriPicPath = "file:" + imageFile.getAbsolutePath();
        // to upload the picture to the server
        picPath = imageFile.getAbsolutePath();
        return imageFile;
    }

    public void generateToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

}
