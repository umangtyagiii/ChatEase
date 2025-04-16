package com.example.chatease;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import de.hdodenhof.circleimageview.CircleImageView;

public class Registration extends AppCompatActivity {
    TextView loginbut;
    EditText rg_username, rg_email, rg_password, rgre_password;
    Button rg_signup;
    CircleImageView rg_profile;
    FirebaseAuth auth;
    Uri imageURI;
    String imageuri;
    String emailpattern = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";
    FirebaseDatabase database;
    FirebaseStorage storage;
    android.app.ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Just a moment!");
        progressDialog.setCancelable(false);
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
        loginbut = findViewById(R.id.loginbut);
        rg_username = findViewById(R.id.rgusername);
        rg_email = findViewById(R.id.rgemail);
        rg_password = findViewById(R.id.rgpassword);
        rgre_password = findViewById(R.id.rgrepassword);
        rg_signup = findViewById(R.id.signupbutton);
        rg_profile = findViewById(R.id.profiler0);

        loginbut.setOnClickListener(v -> {
            Intent intent = new Intent(Registration.this, Login.class);
            startActivity(intent);
            finish();
        });

        rg_signup.setOnClickListener(v -> {
            String namee = rg_username.getText().toString();
            String emaill = rg_email.getText().toString();
            String passwordd = rg_password.getText().toString();
            String repasswordd = rgre_password.getText().toString();
            String status = "Hey, I'm using this application!";

            if (TextUtils.isEmpty(namee) || TextUtils.isEmpty(emaill) || TextUtils.isEmpty(passwordd) || TextUtils.isEmpty(repasswordd)) {
                progressDialog.dismiss();
                Toast.makeText(Registration.this, "Please enter all credentials!", Toast.LENGTH_SHORT).show();
            } else if (!emaill.matches(emailpattern)) {
                progressDialog.dismiss();
                rg_email.setError("Enter valid email!");
            } else if (passwordd.length() < 6) {
                progressDialog.dismiss();
                rg_password.setError("Enter password of at least 6 characters!");
            } else if (!passwordd.equals(repasswordd)) {
                progressDialog.dismiss();
                rgre_password.setError("Password doesn't match!");
            } else {
                progressDialog.show(); // Show before starting registration
                auth.createUserWithEmailAndPassword(emaill, passwordd).addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        progressDialog.dismiss();
                        String id = task.getResult().getUser().getUid();
                        DatabaseReference reference = database.getReference().child("user").child(id);
                        StorageReference storagereference = storage.getReference().child("Upload").child(id);
                        if (imageURI != null) {
                            storagereference.putFile(imageURI).addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    storagereference.getDownloadUrl().addOnSuccessListener(uri -> {
                                        imageuri = uri.toString();
                                        Users users = new Users(id, namee, emaill, passwordd, imageuri, status);
                                        reference.setValue(users).addOnCompleteListener(task2 -> {
                                            if (task2.isSuccessful()) {
                                                progressDialog.show();
                                                Intent intent = new Intent(Registration.this, MainActivity.class);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Toast.makeText(Registration.this, "Error in creating the user!", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    });
                                }
                            });
                        }
                        else {
                            imageuri = "https://firebasestorage.googleapis.com/v0/b/chatease-ca96b.firebasestorage.app/o/man.png?alt=media&token=7281c72a-fcc9-477d-a379-8805f2de6e44";
                            Users users = new Users(id, namee, emaill, passwordd, imageuri, status);
                            reference.setValue(users).addOnCompleteListener(task2 -> {
                                if (task2.isSuccessful()) {
                                    Intent intent = new Intent(Registration.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(Registration.this, "Error in creating the user!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    else {
                        progressDialog.dismiss();
                        Toast.makeText(Registration.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        rg_profile.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select picture!"), 10);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    protected void onActivityResult(int requestCode, int resultcode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultcode, data);
        if (requestCode == 10 && data != null) {
            imageURI = data.getData();
            rg_profile.setImageURI(imageURI);
        }
    }
}

