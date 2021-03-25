/*************************************************************************
 *
 * ATOS CONFIDENTIAL
 * __________________
 *
 *  Copyright (2020) Atos Spain SA
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Atos Spain SA and other companies of the Atos group.
 * The intellectual and technical concepts contained
 * herein are proprietary to Atos Spain SA
 * and other companies of the Atos group and may be covered by Spanish regulations
 * and are protected by copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Atos Spain SA.
 */
package eu.h2020.helios_social.modules.filetransfer;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;

import java.net.URL;

import io.tus.java.client.TusClient;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;

public class UploadTask extends AsyncTask<Void, Long, URL> {
    private FileTransferActivity activity;
    private TusClient client;
    private TusUpload upload;
    private Exception exception;

    public UploadTask(FileTransferActivity activity, TusClient client, TusUpload upload) {
        this.activity = activity;
        this.client = client;
        this.upload = upload;
    }

    @Override
    protected void onPreExecute() {
        activity.setStatus("Upload selected...");
        activity.setPauseButtonEnabled(true);
        activity.setUploadProgress(0);
    }

    @Override
    protected void onPostExecute(URL uploadURL) {
        activity.setStatus("Upload finished!\n" + uploadURL.toString());
        activity.setPauseButtonEnabled(false);

        Intent returnIntent = new Intent();
        returnIntent.putExtra("uploadURL", uploadURL.toString());
        activity.setResult(Activity.RESULT_OK,returnIntent);
        activity.finish();
    }

    @Override
    protected void onCancelled() {
        if(exception != null) {
            activity.showError(exception);
        }

        activity.setPauseButtonEnabled(false);
    }

    @Override
    protected void onProgressUpdate(Long... updates) {
        long uploadedBytes = updates[0];
        long totalBytes = updates[1];
        activity.setStatus(String.format("Uploaded %d/%d.", uploadedBytes, totalBytes));
        activity.setUploadProgress((int) ((double) uploadedBytes / totalBytes * 100));
    }

    @Override
    protected URL doInBackground(Void... params) {
        try {
            TusUploader uploader = client.resumeOrCreateUpload(upload);
            long totalBytes = upload.getSize();
            long uploadedBytes = uploader.getOffset();

            // Upload file in 1MiB chunks
            uploader.setChunkSize(1024 * 1024);

            while(!isCancelled() && uploader.uploadChunk() > 0) {
                uploadedBytes = uploader.getOffset();
                publishProgress(uploadedBytes, totalBytes);
            }

            uploader.finish();
            return uploader.getUploadURL();

        } catch(Exception e) {
            exception = e;
            cancel(true);
        }
        return null;
    }
}
