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

public class SignInDriverActivity extends AppCompatActivity {

    private TextInputLayout textInputEmail;
    private TextInputLayout textInputName;
    private TextInputLayout textInputPassword;
    private TextInputLayout textInputConfirmedPassword;
    private Button loginSignUpBtnDriver;
    private TextView toggleLoginSignUpTextView;

    private static final String TAG = "MyTag";
    private boolean loginModeActive;

    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference usersDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_driver);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        usersDatabaseReference = database.getReference().child("drivers");

        textInputEmail = findViewById(R.id.textInputEmail);
        textInputName = findViewById(R.id.textInputName);
        textInputPassword = findViewById(R.id.textInputPassword);
        textInputConfirmedPassword = findViewById(R.id.textInputConfirmedPassword);
        loginSignUpBtnDriver = findViewById(R.id.loginSignUpBtnDriver);
        toggleLoginSignUpTextView = findViewById(R.id.toggleLoginSignUpTextView);

        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(SignInDriverActivity.this, DriverMapsActivity.class));
        }
    }

    public void loginSignUpUser(View view) {
        String email = textInputEmail.getEditText().getText().toString().trim();
        String password =  textInputPassword.getEditText().getText().toString().trim();
        String confirmedPassword = textInputConfirmedPassword.getEditText().getText().toString().trim();

        if (!loginModeActive) {
            if (!password.equals(confirmedPassword)) {
                textInputPassword.setError("Passwords do not match");
            } else if(validateName() || validateEmail() || validatePassword()) {
                Log.d(TAG, "Email: " + email + "/n" + "Password: " + password);
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "createUserWithEmail:success");
                                    FirebaseUser driver = mAuth.getCurrentUser();
                                    createDriver(driver);

                                    Intent intent = new Intent(SignInDriverActivity.this,
                                            DriverMapsActivity.class);
                                    //intent.putExtra("user_name", textInputName.getEditText().getText()
                                    //        .toString().trim());
                                    startActivity(intent);
                                } else {
                                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                    Toast.makeText(SignInDriverActivity.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        } else {
            if (validateEmail() || validatePassword()) {
                Log.d(TAG, "Email: " + email + "/n" + "Password: " + password);
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "signInWithEmail:success");
                                    FirebaseUser driver = mAuth.getCurrentUser();

                                    Intent intent = new Intent(SignInDriverActivity.this,
                                            DriverMapsActivity.class);
                                    //intent.putExtra("user_name", textInputName.getEditText().getText()
                                    //        .toString().trim());
                                    startActivity(intent);
                                } else {
                                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                                    Toast.makeText(SignInDriverActivity.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        }
    }

    private void createDriver(FirebaseUser firebaseDriver) {
        CabDriver driver = new CabDriver();
        driver.setId(firebaseDriver.getUid());
        driver.setEmail(firebaseDriver.getEmail());
        driver.setName(textInputName.getEditText().getText().toString().trim());

        usersDatabaseReference.push().setValue(driver);
    }

    public void toggleLoginSignUpUser(View view) {
        if (loginModeActive) {
            loginModeActive = false;
            textInputConfirmedPassword.setVisibility(View.VISIBLE);
            loginSignUpBtnDriver.setText("Sign Up");
            toggleLoginSignUpTextView.setText("Or, login in");
        } else {
            loginModeActive = true;
            textInputConfirmedPassword.setVisibility(View.GONE);
            loginSignUpBtnDriver.setText("Login In");
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

        if (password.isEmpty()) {
            textInputPassword.setError("Please, input your password");
            return false;
        } else if (password.length() < 7) {
            textInputPassword.setError("Password have to be longer than 6");
            return false;
        } else {
            textInputPassword.setError("");
            return true;
        }
    }
}
