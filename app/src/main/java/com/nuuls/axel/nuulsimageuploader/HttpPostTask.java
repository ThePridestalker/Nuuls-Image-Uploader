package com.nuuls.axel.nuulsimageuploader;

import android.os.AsyncTask;

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
    private final onUploadedCallback callback;

    interface onUploadedCallback {
        void onUpload(String result);
    }

    HttpPostTask(onUploadedCallback callback) {
        this.callback = callback;
    }

    private String run(String filepath) throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xd", "justNameItXD.png",
                        RequestBody.create(new File(filepath), MEDIA_TYPE_PNG))
                .build();

        Request request = new Request.Builder()
                .url(uploadURL)
                .post(requestBody)
                .build();

        String responseUrl = "error XD";
        Response response = client.newCall(request).execute();
        if (response.isSuccessful() && response.body() != null) {
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
        callback.onUpload(result);
    }
}
