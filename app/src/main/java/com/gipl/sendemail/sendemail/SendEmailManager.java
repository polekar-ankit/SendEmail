package com.gipl.sendemail.sendemail;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import java.io.IOException;
import java.util.Arrays;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static com.gipl.sendemail.sendemail.EmailSendResult.STATUS.ACCOUNT_PICK_UP_ERROR;
import static com.gipl.sendemail.sendemail.EmailSendResult.STATUS.EMAIL_SEND_FAILED;
import static com.gipl.sendemail.sendemail.EmailSendResult.STATUS.EMAIL_SEND_SUCCESS;

/**
 * Created by Ankit on 16-Sep-19.
 */
public class SendEmailManager {
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "";
    private static final int REQUEST_ACCOUNT_PICKER = 765;
    private final MutableLiveData<EmailSendResult> resultMutableLiveData = new MutableLiveData<>();
    private NetHttpTransport HTTP_TRANSPORT;
    private GoogleAccountCredential credential;
    private Context context;
    private String receiverEmail, message;

    public SendEmailManager(Context context) {
        this.context = context;
        HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
        String[] googleSCOPES = {GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_READONLY, GmailScopes.MAIL_GOOGLE_COM};
        credential =
                GoogleAccountCredential.usingOAuth2(context,
                        Arrays.asList(googleSCOPES)).setBackOff(new ExponentialBackOff());
    }

    public MutableLiveData<EmailSendResult> getResultMutableLiveData() {
        return resultMutableLiveData;
    }

    public void startToSendEmail(String senderEmail, String receiverEmail, String message) {
        this.receiverEmail = receiverEmail;
        this.message = message;
        if (!senderEmail.isEmpty()) {
            ((Activity) context).startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
        } else {
            getEmailService(senderEmail);
        }
    }

    private void getEmailService(final String senderEmail) {
        if (receiverEmail.isEmpty() || message.isEmpty()) {
            resultMutableLiveData.postValue(new EmailSendResult(EMAIL_SEND_FAILED, new Exception("Please enter Message and email")));
            return;
        }
        credential.setSelectedAccount(new Account(senderEmail, context.getPackageName()));
        final Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Print the labels in the user's account.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MimeMessage mimeMessage = EmailUtility.createEmail(receiverEmail,
                            senderEmail,
                            "[Email Send] from Test App",
                            message);

                    Message message = EmailUtility.sendMessage(service, senderEmail, mimeMessage);
                    System.out.println("Message id: " + message.getId());
                    System.out.println(message.toPrettyString());
                    if (message.getLabelIds().contains("SENT")) {
                        resultMutableLiveData.postValue(new EmailSendResult(EMAIL_SEND_SUCCESS));
                    }
                } catch (MessagingException e) {
                    e.printStackTrace();
                    resultMutableLiveData.postValue(new EmailSendResult(EMAIL_SEND_FAILED, e));
                } catch (IOException e) {
                    e.printStackTrace();
                    if (e instanceof UserRecoverableAuthIOException) {
                        ((Activity) context).startActivityForResult(((UserRecoverableAuthIOException) e).getIntent(), 1234);
                    } else {
                        resultMutableLiveData.postValue(new EmailSendResult(EMAIL_SEND_FAILED, e));
                    }
                }
            }
        }).start();

    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ACCOUNT_PICKER) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null) {
                    resultMutableLiveData.postValue(new EmailSendResult(EmailSendResult.STATUS.ACCOUNT_PICKUP, accountName));
                    getEmailService(accountName);
                }
            } else {

                resultMutableLiveData.postValue(new EmailSendResult(ACCOUNT_PICK_UP_ERROR));
            }
        }
    }

}
