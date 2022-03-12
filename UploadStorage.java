package com.menusantara.megawe;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

public class UploadStorage extends AppCompatActivity {

    private static final String TAG = "UploadStorage";

    private StorageReference storageReference;

    private Button btnPilihFile; // untuk tombol pilih file

    private CircularProgressIndicator loading; // untuk tampilan loading

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(btnPilihFile);

        btnPilihFile = findViewById(R.id.uploadfile_pilihfile);
        loading = findViewById(R.id.uploadfile_loading);

        // inisasi firebase storage agar disa digunakan
        storageReference = FirebaseStorage.getInstance().getReference();

        btnPilihFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // saat btn diklik akan membuka file manager menggunakan {launchPilihFile}
                launchPilihFile.launch("*");
            }
        });
    }

    // ini untuk membuka file selector atau file manager atau aplikasi untuk memilih file
    private ActivityResultLauncher<String> launchPilihFile = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri result) {
            // mengecek file apa yang dipilih (foto, atau file txt, atau .docx, atau .zip dll)
            ContentResolver cR = getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String type = mime.getExtensionFromMimeType(cR.getType(result));

            // membuat nama file
            String namaFile = "iniFile_" + UUID.randomUUID().toString().substring(0, 4) + "." + type;

            // mengupload file menggunaka firestore storage
            storageReference.child("folderFileNya/" + namaFile).putFile(result).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    // mengclose tampilan loading
                    loading.setVisibility(View.GONE);

                    // cek task berhasil atau tidak
                    if (task.isSuccessful()){
                        Toast.makeText(UploadStorage.this, "Upload file berhasil", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(UploadStorage.this, "Upload file gagal", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onComplete: gagal: " + task.getException().getMessage());

                }
            });
        }
    });

}
