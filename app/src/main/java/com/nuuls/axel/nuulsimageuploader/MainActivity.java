package com.nuuls.axel.nuulsimageuploader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    // TODO: I PROBABLY CAN PASS THIS 2 PUBLIC VARS TO THE THREAD LIKE THE REST OF THE VARS AND MAKE THEM PRIVATE
    public static Bitmap bitmap;
    public static TextView textView;
    private ImageView image;
    private Button btnCamera;
    private Button btnUpload;

    private File photoFile;
    private Uri photoFileUri;
    private String picPath;
    //private String uriPicPath;

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

        // if im receiving an image from other app
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleSendImage(intent);
            }
        }

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getPackageManager()) != null) {
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
                    // TODO: MOVER ESTO A LA PARTE QUE OCURRE DESPUES DE TOMAR LA FOTO
                    // TODO: EN ESTA PARTE SE CREA UN FICHERO QUE SI SE CANCELA LA FOTO TIENE SER
                    // TODO: BORRADO O SINO SE QUEDA UN FICHERO DE 0kb CREADO.
                    try {
                        // createImageFile() returns an empty file
                        photoFile = createImageFile();
                        picPath = photoFile.getAbsolutePath();
                        Log.e("MEME0>>", picPath);
                        //uriPicPath = "file://" + photoFile.getAbsolutePath();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //if the photoFile contains the picture
                    if (photoFile != null) {
                        // ESTE INTENT ES EL QUE SE LE AÑADE LA FOTO AL ARCHIVO photoFile
                        intent = getPhotoFileUri(intent, photoFile);
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
                // TODO: convertir la uri en picpath de alguna manera. uri.getPath() me da /Pictures/XD_20180923_125937_2376754762601641010.jpg
                // TODO: y necesito /storage/emulated/0/Pictures/XD_20180923_125937_2376754762601641010.jpg
                httpPostTask.execute(picPath);
            }
        });
    }


    private void handleSendImage(Intent intent) {
        //TODO leer el bitmap con el IS y en un OS copiarla dentro de mi directorio de ContentProvider
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        Log.e("URI>>", imageUri.toString());
        photoFile = new File(imageUri.getPath());
        picPath = photoFile.getAbsolutePath();
        Log.e("ABSOLUTEPATH>>", picPath);
        InputStream is = null;
        try {
            is = getContentResolver().openInputStream(photoFileUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        bitmap = BitmapFactory.decodeStream(is);

        //la uri de la app de photos de google viene asi, sin sufijo y con content://:
        //content://com.google.android.apps.photos.contentprovider/-1/1/
        // content%3A%2F%2Fmedia%2Fexternal%2Fimages%2Fmedia%2F16/REQUIRE_ORIGINAL/NONE/1138816201

    }

    //sets the URI of the photo and attaches the uri to the picture
    private Intent getPhotoFileUri(Intent intent, File photoFile) {
        //returns a content uri for a given file
        photoFileUri = FileProvider.getUriForFile(getApplicationContext(),
                getPackageName() + ".fileprovider",
                photoFile);
        Log.e("MEME1>>", photoFileUri.toString());
        Log.e("TEST>>", photoFileUri.getPath());
        //this line attaches the picture to the uri of the file we created before
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
            Toast.makeText(this, "CANCELED", Toast.LENGTH_LONG).show();
        }
    }

    private void removeExifGeolocation() throws IOException {
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
    }

    // Puts the bitmap into the imageView to display the image
    private void insertImageIntoImageView() {
        // TODO: AQUI SE PETA PORQUE LA IMAGEN NO SE PONE BIEN EN EL BITMAP
        Bitmap bitmapResized = Bitmap.createScaledBitmap(bitmap, image.getWidth(), image.getHeight(), true);
        //resize the bitmap to display the pic inside the frame
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
