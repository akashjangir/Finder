package com.black.ak.finder;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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

import com.black.ak.finder.model.Registration;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.ConfirmPassword;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Order;
import com.mobsandgeeks.saripaar.annotation.Password;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Date;
import java.util.List;

public class RegistrationActivity extends AppCompatActivity implements Validator.ValidationListener {

    private EditText editTextName;
    private EditText editTextPhone;
    @NotEmpty
    @Email private EditText editTextEmail;
    @Order(1)
    @Password(min = 6, scheme = Password.Scheme.ALPHA_NUMERIC, message = "Password should be Alpha Numeric")
    private EditText editTextPassword;
    @Order(2)
    @ConfirmPassword(message = "Password do not match")
    private EditText editTextConfirmPassword;
    private Button buttonRegister;
    private Button buttonReset;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        //Register validator for this activity
        final Validator validator = new Validator(this);
        validator.setValidationListener(this);

        //Get the Ids of fields
        editTextName = (EditText) findViewById(R.id.editText_name);
        editTextPhone = (EditText) findViewById(R.id.editText_phone);
        editTextEmail = (EditText) findViewById(R.id.editText_email);
        editTextPassword = (EditText) findViewById(R.id.editText_password);
        editTextConfirmPassword= (EditText) findViewById(R.id.editText_confirmPassword);
        buttonRegister = (Button) findViewById(R.id.button_register);
        buttonReset = (Button) findViewById(R.id.button_reset);

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validator.validate();
            }
        });

        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextName.setText("");
                editTextPhone.setText("");
                editTextEmail.setText("");
                editTextPassword.setText("");
                editTextConfirmPassword.setText("");
            }
        });

    }

    @Override
    public void onValidationSucceeded() {

        Registration registration = new Registration();
        registration.setName(editTextName.getText().toString());
        registration.setPhone(editTextPhone.getText().toString());
        registration.setEmail(editTextEmail.getText().toString());
        registration.setPassword(editTextPassword.getText().toString());

        //starting background task for registration
        RegistrationTask task = new RegistrationTask();
        task.execute(registration);
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

    private class RegistrationTask extends AsyncTask<Object, String, String> {

        ProgressDialog dialog;



        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(RegistrationActivity.this);
            dialog.setTitle("Saving");
            dialog.setMessage("Please Wait...");
            dialog.setCancelable(true);
            dialog.show();
        }

        @Override
        protected String doInBackground(Object... params) {
            Registration registration = (Registration) params[0];

            //getting database connections...
            try {
                MongoClient mongoClient = new MongoClient(new MongoClientURI(getString(R.string.database_finder_uri)));
                MongoDatabase mongoDatabase = mongoClient.getDatabase(getString(R.string.db_name));
                MongoCollection<Document> collection =  mongoDatabase.getCollection(getString(R.string.registration_collection));

                Bson filterToFind = Filters.eq("email", registration.getEmail());
                Document searchedDocument = collection.find(filterToFind).first();
                if(searchedDocument == null) {
                    Document document = new Document();
                    document.put("name", registration.getName().trim().toUpperCase());
                    document.put("phone", registration.getPhone().trim());
                    document.put("email", registration.getEmail().trim().toLowerCase());
                    document.put("password", registration.getPassword().trim());
                    document.put("registration_date", new Date());
                    collection.insertOne(document);
                    mongoClient.close();

                    SharedPreferences.Editor editor = getSharedPreferences("com.finder.ak", MODE_PRIVATE).edit();
                    editor.putString("name", registration.getName());
                    editor.putString("phone", registration.getPhone());
                    editor.putString("email", registration.getEmail());
                    editor.commit();
                } else {
                    return "USER_EXISTS";
                }
            } catch (Exception e) {
                Log.e("EXCEPTION ", e.getMessage());
                return "";
            }

            return "USER_SAVED";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            dialog.dismiss();
            if(s.equals("USER_SAVED")) {
                Toast.makeText(RegistrationActivity.this, "Register Successfully", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(RegistrationActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            } else if(s.equals("USER_EXISTS")){
                final AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationActivity.this);
                builder.setTitle("User Already Registered");
                builder.setMessage("Please provide different Email or Login with existing Email");
                builder.setPositiveButton("Login", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.show();
            } else {
                Toast.makeText(RegistrationActivity.this, "Error in Registration", Toast.LENGTH_LONG).show();
            }
        }
    }
}
