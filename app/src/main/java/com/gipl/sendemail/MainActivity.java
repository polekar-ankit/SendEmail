package com.gipl.sendemail;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MainActivity extends AppCompatActivity implements Observer<Message> {
    private static final String TAG = "MainActivity";

    private static final String APPLICATION_NAME = "Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final int REQUEST_ACCOUNT_PICKER = 765;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    //    private Tasks service;
    private final MutableLiveData<Message> result = new MutableLiveData<>();
    ProgressBar progressBar;
    private NetHttpTransport HTTP_TRANSPORT;
    private GoogleAccountCredential credential;
    private EditText etEmail, etMessage;

    private static MimeMessage createEmail(String to,
                                           String from,
                                           String subject,
                                           String bodyText)
            throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO,
                new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    private static Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private static Message sendMessage(Gmail service,
                                       String userId,
                                       MimeMessage emailContent)
            throws MessagingException, IOException {
        Message message = createMessageWithEmail(emailContent);
        message = service.users().messages().send(userId, message).execute();
        return message;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.editText_email);
        etMessage = findViewById(R.id.editText_mesage);
        progressBar = findViewById(R.id.progressBar);

        HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
        String[] googleSCOPES = {GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_READONLY, GmailScopes.MAIL_GOOGLE_COM};
        credential =
                GoogleAccountCredential.usingOAuth2(this,
                        Arrays.asList(googleSCOPES)).setBackOff(new ExponentialBackOff());

        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        credential.setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                String email = settings.getString(PREF_ACCOUNT_NAME, null);
                if (email == null) {
                    startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
                } else {
                    getEmailService(email, etEmail.getText().toString(), etMessage.getText().toString());
                }
            }
        });
        result.observe(this, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ACCOUNT_PICKER) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null) {
                    SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(PREF_ACCOUNT_NAME, accountName);
                    editor.apply();
                    getEmailService(accountName, etEmail.getText().toString(), etMessage.getText().toString());
                }
            }
        }
    }

    private void getEmailService(final String senderEmail, final String receiverEmail, final String message) {
        if (receiverEmail.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please enter email id and messaage", Toast.LENGTH_SHORT).show();
            return;
        }
        credential.setSelectedAccount(new Account(senderEmail, getPackageName()));
        final Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        progressBar.setVisibility(View.VISIBLE);
        // Print the labels in the user's account.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MimeMessage mimeMessage = createEmail(receiverEmail,
                            senderEmail,
                            "[Email Send] from Test App",
                            message);

                    Message message = sendMessage(service, senderEmail, mimeMessage);
                    System.out.println("Message id: " + message.getId());
                    System.out.println(message.toPrettyString());
                    result.postValue(message);
                } catch (MessagingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (e instanceof UserRecoverableAuthIOException)
                        startActivityForResult(((UserRecoverableAuthIOException) e).getIntent(), 1234);
                }
            }
        }).start();

    }

    @Override
    public void onChanged(Message message) {
        progressBar.setVisibility(View.GONE);
        if (message.getLabelIds().contains("SENT")) {
            etMessage.setText("");
            etEmail.setText("");
            Toast.makeText(this, "Email has been successfully sent", Toast.LENGTH_SHORT).show();
        }
    }
}
