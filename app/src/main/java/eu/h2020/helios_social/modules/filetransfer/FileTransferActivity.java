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

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.net.URL;

import io.tus.android.client.TusAndroidUpload;
import io.tus.android.client.TusPreferencesURLStore;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusUpload;


public class FileTransferActivity extends AppCompatActivity {
    private final int REQUEST_FILE_SELECT = 1;
    private TusClient client;
    private TextView status;
    private Button pauseButton;
    private Button resumeButton;
    private UploadTask uploadTask;
    private ProgressBar progressBar;
    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_transfer);

        try {
            SharedPreferences pref = getSharedPreferences("tus", 0);
            client = new TusClient();
            String url = getString(R.string.TUS_URL);
            client.setUploadCreationURL(new URL(url));
            client.enableResuming(new TusPreferencesURLStore(pref));
        } catch(Exception e) {
            showError(e);
        }

        status = findViewById(R.id.status);
        progressBar = findViewById(R.id.progressBar);

        Button button = findViewById(R.id.upload_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("*/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select file to upload"), REQUEST_FILE_SELECT);
            }
        });

        pauseButton = findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseUpload();
            }
        });

        resumeButton = findViewById(R.id.resume_button);
        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resumeUpload();
            }
        });
    }

    private void beginUpload(Uri uri) {
        fileUri = uri;
        resumeUpload();
    }

    public void setPauseButtonEnabled(boolean enabled) {
        pauseButton.setEnabled(enabled);
        resumeButton.setEnabled(!enabled);
    }

    public void pauseUpload() {
        uploadTask.cancel(false);
    }

    public void resumeUpload() {
        try {
            TusUpload upload = new TusAndroidUpload(fileUri, this);
            uploadTask = new UploadTask(this, client, upload);
            uploadTask.execute(new Void[0]);
        } catch (Exception e) {
            showError(e);
        }
    }

    public void setStatus(String text) {
        status.setText(text);
    }

    public void setUploadProgress(int progress) {
        progressBar.setProgress(progress);
    }

    public void showError(Exception e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Internal error");
        builder.setMessage(e.getMessage());
        AlertDialog dialog = builder.create();
        dialog.show();
        e.printStackTrace();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_FILE_SELECT) {
            Uri uri = data.getData();
            beginUpload(uri);
        }
    }
}