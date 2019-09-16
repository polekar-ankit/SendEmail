package com.gipl.sendemail;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.gipl.sendemail.sendemail.EmailSendResult;
import com.gipl.sendemail.sendemail.SendEmailManager;

public class MainActivity extends AppCompatActivity implements Observer<EmailSendResult> {
    private static final String TAG = "MainActivity";
    private static final String PREF_ACCOUNT_NAME = "account_name";
    ProgressBar progressBar;
    private EditText etEmail, etMessage;

    private SendEmailManager sendEmailManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.editText_email);
        etMessage = findViewById(R.id.editText_mesage);
        progressBar = findViewById(R.id.progressBar);

        sendEmailManager = new SendEmailManager(this);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                String email = settings.getString(PREF_ACCOUNT_NAME, null);
                sendEmailManager.startToSendEmail(email, etEmail.getText().toString(), etMessage.getText().toString());
            }
        });
        sendEmailManager.getResultMutableLiveData().observe(this, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        sendEmailManager.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onChanged(EmailSendResult message) {
        progressBar.setVisibility(View.GONE);
        switch (message.getStatus()) {
            case EMAIL_SEND_STARTED:
                progressBar.setVisibility(View.VISIBLE);
                break;
            case ACCOUNT_PICKUP:
                SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                settings.edit().putString(PREF_ACCOUNT_NAME, message.getExtraInfo()).apply();
                break;
            case EMAIL_SEND_SUCCESS:
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Your email Has been send successfully", Toast.LENGTH_SHORT).show();
                break;
            case EMAIL_SEND_FAILED:
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Your email not send", Toast.LENGTH_SHORT).show();
                break;
        }

    }
}
