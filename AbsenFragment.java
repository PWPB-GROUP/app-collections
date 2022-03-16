package com.menusantara.megawe.ui.main.fragment;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.menusantara.library.model.Attendance;
import com.menusantara.library.model.User;
import com.menusantara.megawe.BaseApplication;
import com.menusantara.megawe.R;
import com.menusantara.megawe.model.TimeModel;
import com.menusantara.megawe.ui.main.MainViewModel;
import com.menusantara.megawe.utils.DateTools;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

public class AbsenFragment extends Fragment {

    private static final String TAG = "AbsenFragment";

    private MainViewModel mainViewModel;
    private BaseApplication application;

    private FirebaseFirestore firestore;
    private StorageReference storage;

    private Bitmap photoBitmap = null;

    private ImageView imgPhoto;
    private MaterialCardView cardImg;
    private MaterialTextView tvTitle, tvTime, tvLocation;
    private TextInputLayout inputLayout;
    private CircularProgressIndicator loading;

    private TextInputEditText edtNote;

    private MaterialButton btnAbsen;

    private boolean isAbsen;

    public AbsenFragment(){}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_absensi, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        application = mainViewModel.getApplication();

        firestore = application.getFirestore();
        storage = application.getStorage();

        tvTitle = view.findViewById(R.id.absensi_tv_time_title);
        tvTime = view.findViewById(R.id.absensi_tv_time);
        cardImg = view.findViewById(R.id.absensi_cardview_sec);
        tvLocation = view.findViewById(R.id.absensi_tv_location);
        imgPhoto = view.findViewById(R.id.absensi_img_photo);
        edtNote = view.findViewById(R.id.absensi_input_note);
        inputLayout = view.findViewById(R.id.absensi_layout_note);
        btnAbsen = view.findViewById(R.id.absensi_btn_absen);
        loading = view.findViewById(R.id.absensi_loading);

        isAbsen = application.checkTimeIn();

        TimeModel timeModel = DateTools.convertTime(System.currentTimeMillis());
        tvTime.setText(String.format(Locale.getDefault(), "%d.%d", timeModel.getJam(), timeModel.getMenit()));

        if (isAbsen){
            tvTitle.setText("Absensi Keluar");
            btnAbsen.setText("Absen Keluar");

            cardImg.setVisibility(View.GONE);
            inputLayout.setVisibility(View.GONE);
            edtNote.setVisibility(View.GONE);
        }

        btnAbsen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loading.setVisibility(View.VISIBLE);

                User user = application.getUserProfile();
                if (!isAbsen){
                    if (photoBitmap == null){
                        Toast.makeText(application, "Silakan masukan foto bukti absen", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (edtNote.getText() == null || edtNote.getText().toString().trim().isEmpty()){
                        Toast.makeText(application, "Silakan masukan note (eg: hari ini hadir..)", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onClick: edt note null/empty");
                        return;
                    }

                    long timeIn = System.currentTimeMillis();

                    String photoPath = user.getCompanyId() + "_" + timeIn + "_IMG.jpg";

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    photoBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] imgBytes = stream.toByteArray();
                    photoBitmap.recycle();

                    StorageReference imgRef = storage.child("Megawe/attendance_selfie/" + photoPath);

                    imgRef.putBytes(imgBytes)
                            .continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                @Override
                                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                    if (!task.isSuccessful()){
                                        Log.d(TAG, "then: img url fail: " + task.getException());
                                        return null;
                                    }
                                    return imgRef.getDownloadUrl();
                                }
                            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            loading.setVisibility(View.GONE);

                            if (task.isSuccessful()){
                                String imgUrl = task.getResult() == null ? "-" : task.getResult().toString();

                                String attendanceId = "attendance_" + timeIn;

                                String note = edtNote.getText().toString();

                                Attendance attendance = new Attendance(
                                        attendanceId,
                                        user.getUserName(),
                                        user.getUserUID(),
                                        timeIn,
                                        null,
                                        "coordinate-xxx-xxx-xx",
                                        note,
                                        imgUrl,
                                        1
                                );

                                firestore.collection("Companies").document(user.getCompanyId()).collection("attendances").document(attendanceId).set(attendance).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            mainViewModel.getTimeInLiveData().setValue(timeIn);
                                            application.saveUserIn(timeIn, imgUrl);

                                            Log.d(TAG, "onSuccess: absen time in success");
                                            FragmentTransaction ft = getParentFragmentManager().beginTransaction().replace(R.id.main_frame, new AbsenInfoFragment("Selamat! Absen berhasil", "Anda telah melakukan absen jam masuk"));
                                            ft.commit();
                                            
                                            return;
                                        }

                                        Log.d(TAG, "onComplete: save data attendances gagal: ");
                                        if (task.getException() != null){
                                            Log.d(TAG, "onComplete: ex: " + task.getException().getMessage());
                                        }
                                    }
                                });
                                return;
                            }

                            FragmentTransaction ft = getParentFragmentManager().beginTransaction().replace(R.id.main_frame, new AbsenInfoFragment("Gagal! Terjadi kesalahan saat absen", "Silahkan coba kembali atau hubungi developer"));
                            ft.commit();
                            Log.d(TAG, "onSuccess: absen time in gagal");
                            if (task.getException() != null){
                                Log.d(TAG, "onSuccess: ex: " + task.getException().getMessage());
                            }
                        }
                    });
                    return;
                }

                long timeOut = System.currentTimeMillis(), timeIn = application.getTimeIn();
                String attendancesId = "attendance_" + timeIn;

                firestore.collection("Companies").document(user.getCompanyId()).collection("attendances").document(attendancesId).update("timeOut", timeOut).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            loading.setVisibility(View.GONE);
                            Log.d(TAG, "onComplete: absen time out success");

                            mainViewModel.getTimeOutLiveData().setValue(timeOut);
                            application.saveUserOut(timeOut);
//                            application.clearUserIn();

                            // fragment success /toast/dialog
                            FragmentTransaction ft = getParentFragmentManager().beginTransaction().replace(R.id.main_frame, new AbsenInfoFragment("Selamat! Absen berhasil", "Anda telah melakukan absen jam keluar"));
                            ft.commit();
                            return;
                        }

                        Log.d(TAG, "onComplete: gagal: check time out");
                    }
                });
            }
        });
        
        mainViewModel.getSelfieLiveData().observe(getViewLifecycleOwner(), new Observer<Bitmap>() {
            @Override
            public void onChanged(Bitmap bitmap) {
                photoBitmap = bitmap;

                imgPhoto.setPadding(0, 0, 0, 0);
                Glide.with(application).load(bitmap).into(imgPhoto);
            }
        });

        imgPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getOpenIntent().openImage();
            }
        });

    }
}
