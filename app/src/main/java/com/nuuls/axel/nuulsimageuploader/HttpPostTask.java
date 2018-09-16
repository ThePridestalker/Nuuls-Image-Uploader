package com.nuuls.axel.nuulsimageuploader;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class HttpPostTask extends AsyncTask<String, Void, String> {
    private static final MediaType MEDIA_TYPE_PNG = MediaType.get("image/png");
    private static final String uploadURL = "https://i.nuuls.com/upload";
    private final OkHttpClient client = new OkHttpClient();

    Context context;
    ImageView image;
    Button btnUpload;
    Button btnCamera;

    public HttpPostTask(Context context, ImageView image, Button btnUpload, Button btnCamera) {
        this.context = context;
        this.image = image;
        this.btnUpload = btnUpload;
        this.btnCamera = btnCamera;
    }

    public String run(String filepath) throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xd", "justNameItXD.png",
                        RequestBody.create(MEDIA_TYPE_PNG, new File(filepath)))
                .build();

        Request request = new Request.Builder()
                .url(uploadURL)
                .post(requestBody)
                .build();

        String responseUrl = "error XD";
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            responseUrl = response.body().string();
            return responseUrl;
        }
        return responseUrl;
    }

    @Override
    protected String doInBackground(String... params) {
        String responseFromServer = null;
        try {
            responseFromServer = run(params[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseFromServer;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Toast.makeText(context, "Copied: " + result, Toast.LENGTH_LONG).show();
        // copy the url to the clipboard
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("URL from nuuls server xd", result);
        clipboard.setPrimaryClip(clip);
        // reset the image to nuulsLogo
        image.setImageResource(R.drawable.nuulslogo);
        //display the url
        MainActivity.textView.setText(result);
        btnUpload.setBackgroundColor(Color.parseColor("#4caf50"));
        btnUpload.setText("UPLOAD");
        btnCamera.setEnabled(true);
        btnUpload.setEnabled(true);
        //reset the bitmap so you cant upload the same thing infinitely
        MainActivity.bitmap = null;
    }
}
