public class MenyimpanDataKeFirestore extends AppCompatActivity {

    private static final String TAG = "MenyimpanDataKeFirestor";
    
    private FirebaseFirestore firestore;
    
    private Button btnKirim;
    private EditText edtNama, edtAlamat, edtNomor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menyimpandata);
        
        btnKirim = findViewById(R.id.menyimpan_tombol);
        edtNama = findViewById(R.id.menyimpan_nama);
        edtAlamat = findViewById(R.id.menyimpan_alamat);
        edtNomor = findViewById(R.id.menyimpan_nomor);
        
        // inisiasi firestore agar bisa digunakan
        firestore = FirebaseFirestore.getInstance();
        
        // saat btnKirim diklik maka
        btnKirim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // mendapat text dari edittext yang diinputkan
                String nama = edtNama.getText().toString();
                String alamat = edtAlamat.getText().toString();
                String nomor = edtNomor.getText().toString();
                
                Map<String, String> dataIdentitas = new HashMap<>();
                dataIdentitas.put("nama", nama);
                dataIdentitas.put("alamat", alamat);
                dataIdentitas.put("nomor", nomor);
                
                // menyimpan data ke firestore
                firestore.collection("dataPengguna").add(dataIdentitas).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()){
                            // data berhasil disimpan
                            Toast.makeText(MenyimpanDataKeFirestore.this, "Berhasil menyimpan data", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        Log.d(TAG, "onComplete: gagal menyimpan data");
                    }
                });
            }
        });
        
    }
}
