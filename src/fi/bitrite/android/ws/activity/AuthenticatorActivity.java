package fi.bitrite.android.ws.activity;

import android.accounts.Account;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;

import org.json.JSONException;

import java.io.IOException;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.NoAccountException;
import fi.bitrite.android.ws.auth.http.HttpAuthenticator;

/**
 * The activity responsible for getting WarmShowers credentials from the user,
 * verifying them against the WarmShowers web service and storing and storing
 * them on the device using Android's custom account facilities.
 */
public class AuthenticatorActivity extends WSSupportAccountAuthenticatorActivity {

    public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";
    public static final int RESULT_AUTHENTICATION_FAILED = RESULT_FIRST_USER + 1;
    public static final int RESULT_NO_NETWORK = 101;
    public static final int REQUEST_TYPE_AUTHENTICATE = 201;

    View loggedInLayout;
    View notLoggedInLayout;
    EditText editUsername;
    EditText editPassword;
    TextView txtLoggedInStatus;

    private DialogHandler mDialogHandler;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_authentication);
        initView();

        loggedInLayout = findViewById(R.id.loggedInLayout);
        notLoggedInLayout = findViewById(R.id.notLoggedInLayout);
        txtLoggedInStatus = (TextView)findViewById(R.id.txtLoggedInStatus);
        editUsername = (EditText) findViewById(R.id.editUsername);
        editPassword = (EditText) findViewById(R.id.editPassword);

        mDialogHandler = new DialogHandler(this);

        updateView();
    }

    public void updateView() {
        notLoggedInLayout.setVisibility(View.VISIBLE);
        loggedInLayout.setVisibility(View.GONE);

        try {
            String loggedInUsername = AuthenticationHelper.getAccountUsername();
            notLoggedInLayout.setVisibility(View.GONE);
            loggedInLayout.setVisibility(View.VISIBLE);
            txtLoggedInStatus.setText(getString(R.string.current_login_status, loggedInUsername));
        } catch (NoAccountException e) {
            // No action
        }

    }


    public void logout(View unusedArg) {
        AuthenticationHelper.removeOldAccount();
        // TODO: Actually perform a logout operation
        updateView();
    }

    public void cancel(View view) {
        Intent resultIntent = new Intent();
        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        cancel(null);
    }

    public void applyCredentials(View view) {
        AuthenticationHelper.removeOldAccount();

        String username = editUsername.getText().toString();
        String password = editPassword.getText().toString();
        if (!username.isEmpty() && !password.isEmpty()) {
            mDialogHandler.showDialog(DialogHandler.AUTHENTICATE);
            Account account = AuthenticationHelper.createNewAccount(username, password);
            AuthenticationTask authTask = new AuthenticationTask();
            authTask.execute();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        return mDialogHandler.createDialog(id, getResources().getString(R.string.authenticating));
    }

    private class AuthenticationTask extends AsyncTask<Void, Void, Void> {
        int mUID = 0;
        boolean mNetworkError = false;

        protected Void doInBackground(Void... params) {

            HttpAuthenticator authenticator = new HttpAuthenticator();
            try {
                mUID = authenticator.authenticate();
            } catch (IOException e) {
                mNetworkError = true;
            } catch (JSONException e) {
                mNetworkError = true;
            } catch (Exception e) {
                // mUid value will capture anything else.
            }
            return null;
        }

        protected void onPostExecute(Void v) {
            mDialogHandler.dismiss();

            if (mUID < 1) {
                if (mNetworkError) {
                    Toast.makeText(AuthenticatorActivity.this, R.string.http_server_access_failure, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AuthenticatorActivity.this, R.string.authentication_failed, Toast.LENGTH_LONG).show();
                }
                AuthenticationHelper.removeOldAccount();
                // And just stay on the page auth screen.
            } else {
                Intent resultIntent = new Intent();
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        }
    }


    @Override
    protected void onStop() {
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

}
