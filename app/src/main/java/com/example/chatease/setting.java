package com.example.chatease;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class setting extends AppCompatActivity {

    ImageView setprofile;
    EditText setname, setstatus;
    Button donebut;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    Uri setImageUri;
    String email, password;
    String existingProfileUri = "";

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setting);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        setprofile = findViewById(R.id.settingprofile);
        setname = findViewById(R.id.settingname);
        setstatus = findViewById(R.id.settingstatus);
        donebut = findViewById(R.id.donebutt);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving...");

        DatabaseReference reference = database.getReference().child("user").child(auth.getUid());
        StorageReference storageReference = storage.getReference().child("upload").child(auth.getUid());

        // Load existing user data
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    email = getSafeString(snapshot.child("mail").getValue());
                    password = getSafeString(snapshot.child("password").getValue());
                    String name = getSafeString(snapshot.child("username").getValue());
                    String profile = getSafeString(snapshot.child("profilepic").getValue());
                    String status = getSafeString(snapshot.child("status").getValue());

                    // Set global variable with current profile image URI
                    existingProfileUri = profile;

                    setname.setText(name);
                    setstatus.setText(status);

                    if (!profile.isEmpty()) {
                        Picasso.get().load(profile).into(setprofile);
                    }
                }
            }


            private String getSafeString(Object obj) {
                return obj != null ? obj.toString() : "";
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(setting.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });

        // Select new profile image
        setprofile.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 10);
        });

        // Save updated data
        donebut.setOnClickListener(v -> {
            String name = setname.getText().toString().trim();
            String status = setstatus.getText().toString().trim();

            if (name.isEmpty() || status.isEmpty()) {
                Toast.makeText(setting.this, "Name and status cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            progressDialog.show();

            if (setImageUri != null) {
                // User picked a new image
                storageReference.putFile(setImageUri).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            String finalImageUri = uri.toString();
                            saveUserData(reference, name, status, finalImageUri);
                        });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(setting.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Use existing profile image from Realtime Database
                if (!existingProfileUri.isEmpty()) {
                    saveUserData(reference, name, status, existingProfileUri);
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(setting.this, "No profile image found", Toast.LENGTH_SHORT).show();
                }
            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void saveUserData(DatabaseReference reference, String name, String status, String profileUri) {
        Users users = new Users(auth.getUid(), name, email, password, profileUri, status);
        reference.setValue(users).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(setting.this, "Profile updated", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(setting.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(setting.this, "Failed to save data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && resultCode == RESULT_OK && data != null) {
            setImageUri = data.getData();
            setprofile.setImageURI(setImageUri);
        }
    }
}
