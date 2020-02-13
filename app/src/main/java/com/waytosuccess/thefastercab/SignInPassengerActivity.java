package com.waytosuccess.thefastercab;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignInPassengerActivity extends AppCompatActivity {

    private TextInputLayout textInputEmail;
    private TextInputLayout textInputName;
    private TextInputLayout textInputPassword;
    private TextInputLayout textInputConfirmedPassword;
    private Button loginSignUpPasBtn;
    private TextView toggleLoginSignUpTextView;

    private static final String TAG = "MyTag";
    private boolean loginModeActive;

    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference passengersDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_passenger);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        passengersDatabaseReference = database.getReference().child("passengers");

        textInputEmail = findViewById(R.id.textInputEmail);
        textInputName = findViewById(R.id.textInputName);
        textInputPassword = findViewById(R.id.textInputPassword);
        textInputConfirmedPassword = findViewById(R.id.textInputConfirmedPassword);
        loginSignUpPasBtn = findViewById(R.id.loginSignUpPasBtn);
        toggleLoginSignUpTextView = findViewById(R.id.toggleLoginSignUpTextView);

//        if (mAuth.getCurrentUser() != null) {
//            startActivity(new Intent(SignInPassengerActivity.this, PassengerMapsActivity.class));
//        }
    }

    public void loginSignUpUser(View view) {
        String email = textInputEmail.getEditText().getText().toString().trim();
        String password =  textInputPassword.getEditText().getText().toString().trim();
        String confirmedPassword = textInputConfirmedPassword.getEditText().getText().toString().trim();

        if (!loginModeActive) {
            if (!password.equals(confirmedPassword)) {
                textInputPassword.setError("Passwords do not match");
            } else if(validateName() || validateEmail() || validatePassword())
                Log.d(TAG, "Created: Email: " + email + "/n" + "Password: " + password);
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "createUserWithEmail:success");
                                    FirebaseUser passenger = mAuth.getCurrentUser();
                                    createPassenger(passenger);

                                    Intent intent = new Intent(SignInPassengerActivity.this,
                                            PassengerMapsActivity.class);
                                    intent.putExtra("user_name", textInputName.getEditText().getText()
                                            .toString().trim());
                                    startActivity(intent);
                                } else {
                                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                    Toast.makeText(SignInPassengerActivity.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
        }
        else {
            if (validateEmail() || validatePassword()) {
                Log.d(TAG, "Email: " + email + "/n" + "Password: " + password);
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "signInWithEmail:success");
                                    FirebaseUser passenger = mAuth.getCurrentUser();

                                    Intent intent = new Intent(SignInPassengerActivity.this,
                                            PassengerMapsActivity.class);
                                    intent.putExtra("user_name", textInputName.getEditText().getText()
                                            .toString().trim());
                                    startActivity(intent);
                                } else {
                                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                                    Toast.makeText(SignInPassengerActivity.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        }
    }

    private void createPassenger(FirebaseUser firebasePassenger) {
        Passenger passenger = new Passenger();
        passenger.setId(firebasePassenger.getUid());
        passenger.setEmail(firebasePassenger.getEmail());
        passenger.setName(textInputName.getEditText().getText().toString().trim());

        passengersDatabaseReference.push().setValue(passenger);
    }

    public void toggleLoginSignUpUser(View view) {
        if (loginModeActive) {
            loginModeActive = false;
            textInputConfirmedPassword.setVisibility(View.VISIBLE);
            loginSignUpPasBtn.setText("Sign Up");
            toggleLoginSignUpTextView.setText("Or, login in");
        } else {
            loginModeActive = true;
            textInputConfirmedPassword.setVisibility(View.GONE);
            loginSignUpPasBtn.setText("Login In");
            toggleLoginSignUpTextView.setText("Or, sign up");
        }
    }

    public boolean validateEmail() {
        String email = textInputEmail.getEditText().getText().toString().trim();

        if (email.isEmpty()) {
            textInputEmail.setError("Please, input your email");
            return false;
        } else {
            textInputEmail.setError("");
            return true;
        }
    }

    public boolean validateName() {
        String name = textInputName.getEditText().getText().toString().trim();

        if (name.isEmpty()) {
            textInputName.setError("Please, input your name");
            return false;
        } else if (name.length() > 15) {
            textInputName.setError("Please, input short name");
            return false;
        } else {
            textInputName.setError("");
            return true;
        }
    }

    public boolean validatePassword() {
        String password = textInputPassword.getEditText().getText().toString().trim();
        String confirmedPassword = textInputConfirmedPassword.getEditText().getText().toString().trim();

        if (password.isEmpty()) {
            textInputPassword.setError("Please, input your password");
            return false;
        } else if (password.length() < 7) {
            textInputPassword.setError("Password have to be longer than 6");
            return false;
        } else if (!password.equals(confirmedPassword)) {
            textInputPassword.setError("Passwords do not match");
            return false;
        } else {
            textInputPassword.setError("");
            return true;
        }
    }
}