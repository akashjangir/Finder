package com.black.ak.finder;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Password;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;

public class LoginActivity extends AppCompatActivity implements Validator.ValidationListener{

    @NotEmpty
    @Email private EditText editTextEmail;
    @Password(min = 6, scheme = Password.Scheme.ALPHA_NUMERIC, message = "Password should be Alpha Numeric")
    private EditText editTextPassword;
    private Button buttonRegister;
    private Button buttonReset;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Register validator for this activity
        final Validator validator = new Validator(this);
        validator.setValidationListener(this);

        editTextEmail = (EditText) findViewById(R.id.editText_email_login);
        editTextPassword = (EditText) findViewById(R.id.editText_password_login);
        buttonLogin = (Button) findViewById(R.id.button_login);
        buttonRegister = (Button) findViewById(R.id.button_register);
        buttonReset = (Button) findViewById(R.id.button_reset);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validator.validate();
            }
        });

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this,RegistrationActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onValidationSucceeded() {
        String email = editTextEmail.getText().toString();
        String password = editTextPassword.getText().toString();

        //background login task
        LoginTask task = new LoginTask();
        task.execute(email, password);
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
        for (ValidationError error : errors) {
            View view = error.getView();
            String message = error.getCollatedErrorMessage(this);

            // Display error messages ;)
            if (view instanceof EditText) {
                ((EditText) view).setError(message);
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class LoginTask extends AsyncTask<String, String, String> {

        ProgressDialog dialog ;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(LoginActivity.this);
            dialog.setTitle("Checking");
            dialog.setMessage("Please Wait...");
            dialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String email = params[0];
            String password = params[1];

            MongoClient mongoClient = null;
            try {
                mongoClient = new MongoClient(new MongoClientURI(getString(R.string.database_finder_uri)));
                MongoDatabase mongoDatabase = mongoClient.getDatabase(getString(R.string.db_name));
                MongoCollection<Document> collection =  mongoDatabase.getCollection(getString(R.string.registration_collection));
                Bson filterEmail = Filters.eq("email", email);
                Document searchedDocument = collection.find(filterEmail).first();
                mongoClient.close();

                if(searchedDocument != null) {
                    if(searchedDocument.get("password").equals(password)) {

                        SharedPreferences.Editor editor = getSharedPreferences("com.finder.ak", MODE_PRIVATE).edit();
                        editor.putString("name", searchedDocument.getString("name"));
                        editor.putString("phone", searchedDocument.getString("phone"));
                        editor.putString("email", searchedDocument.getString("email"));
                        editor.commit();
                        return "success";

                    } else {
                        return "error_password";
                    }
                } else {
                    return "error_email";
                }

            } catch (Exception e) {
                Log.e("EXCEPTION", e.getMessage());
                if(mongoClient!=null) {
                    mongoClient.close();
                }
                return "failed";
            }

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            dialog.dismiss();

            if(s.equals("success")) {
                Toast.makeText(LoginActivity.this, "Login Success", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            } else if(s.equals("error_password")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setTitle("Failed");
                builder.setMessage("Wrong Password");
                builder.show();
            } else if(s.equals("error_email")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setTitle("Failed");
                builder.setMessage("Wrong Email Address");
                builder.show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setTitle("Failed");
                builder.setMessage("Something went wrong...try again");
                builder.show();
            }
        }
    }
}
