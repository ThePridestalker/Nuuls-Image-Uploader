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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    // bitmap has to be public static to set it to null to avoid uploading the same pic pressing the upload button
    public static Bitmap bitmap;
    private TextView textView;
    private ImageView image;
    private Button btnCamera;
    private Button btnUpload;

    private File photoFile;
    private Uri photoFileUri;
    private String picPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.imageUrl);
        btnCamera = findViewById(R.id.buttonCamera);
        btnUpload = findViewById(R.id.buttonUpload);
        image = findViewById(R.id.imageView);

        // Handling the intent coming from other app
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();


        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                // if it has no permission, asks for it
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    enablePermission();
                }
            }
        }

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if the user granted permission to save files
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    try {
                        photoFile = createImageFile();
                        picPath = photoFile.getAbsolutePath();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // if the photoFile contains the picture
                    if (photoFile != null) {
                        intent = getPhotoFileUri(intent, photoFile);
                        // callback to the activity after the camera activity
                        startActivityForResult(intent, 0);
                    }
                } else {
                    forceAcceptPermission();
                }
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if the user granted permission to save files
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
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
                    // TODO: add a NaM face to imageView while image is being uploaded
                    // start the thread for the upload
                    HttpPostTask httpPostTask = new HttpPostTask(MainActivity.this, image, btnUpload, btnCamera, textView, bitmap);
                    httpPostTask.execute(picPath);

                } else {
                    forceAcceptPermission();
                }
            }
        });
    }

    private void forceAcceptPermission() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            // if the user didnt accept the permissions
            enablePermission();
            // if the user checked the "never show again"
            if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                enablePermission();
            }
        }
    }

    private void enablePermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    private void handleSendImage(Intent intent) {

        final Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        try {
            // get the bitmap from the uri
            Log.e("IMAGE URI>", imageUri.toString());
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // after the layout is built and sizes are set
        // java.lang.IllegalArgumentException: width and height must be > 0  -> I avoid this error using this
        image.post(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmapResized = Bitmap.createScaledBitmap(bitmap, image.getWidth(), image.getHeight(), true);
                image.setImageBitmap(bitmapResized);
            }
        });

        try {
            photoFile = createImageFile();
            picPath = photoFile.getAbsolutePath();
            Log.e("PIC PATH>", picPath);
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
    private Intent getPhotoFileUri(Intent intent, File photoFile) {
        // returns a content uri for a given file
        photoFileUri = FileProvider.getUriForFile(getApplicationContext(),
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
        if (resultCode == RESULT_OK) {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoFileUri);
                insertImageIntoImageView();
                removeExifGeolocation();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (resultCode == RESULT_CANCELED) {
            // this deletes the empty file that gets created before taking the picture in case you cancel it
            getContentResolver().delete(photoFileUri, null, null);
            generateToast("CANCELED");
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

    // Puts the bitmap into the imageView to display the image
    private void insertImageIntoImageView() {
        Bitmap bitmapResized = Bitmap.createScaledBitmap(bitmap, image.getWidth(), image.getHeight(), true);
        // resize the bitmap to display the pic inside the frame
        image.setImageBitmap(bitmapResized);
    }

    // creates an image file inside the FileProvider directory
    private File createImageFile() throws IOException {
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
