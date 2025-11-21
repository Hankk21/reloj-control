package com.example.relojcontrol.activities.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.relojcontrol.R;
import com.example.relojcontrol.adapters.LicenciasAdminAdapter;
import com.example.relojcontrol.models.Licencia;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LicenciasAdminActivity extends AppCompatActivity {
    private static final String TAG = "LicenciasAdminActivity";

    private RecyclerView recyclerView;
    private LicenciasAdminAdapter adapter;
    private List<Licencia> listaLicencias;
    private DatabaseReference licenciasRef;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licencias_admin);

        initViews();
        setupToolbar();
        setupRecyclerView();
        cargarLicencias();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewLicencias);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Gestión de Licencias");
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        listaLicencias = new ArrayList<>();
        adapter = new LicenciasAdminAdapter(listaLicencias, this);
        recyclerView.setAdapter(adapter);
    }

    private void cargarLicencias() {
        showLoading(true);
        licenciasRef = FirebaseDatabase.getInstance().getReference("licencias");

        // Cargar solo licencias pendientes (estado = 2)
        licenciasRef.orderByChild("id_estado").equalTo(2)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaLicencias.clear();

                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                Licencia licencia = data.getValue(Licencia.class);
                                if (licencia != null) {
                                    licencia.setId(data.getKey());
                                    listaLicencias.add(licencia);
                                }
                            }

                            showEmptyState(false);
                            adapter.notifyDataSetChanged();
                            Log.d(TAG, "Licencias cargadas: " + listaLicencias.size());
                        } else {
                            showEmptyState(true);
                            Log.d(TAG, "No hay licencias pendientes");
                        }

                        showLoading(false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        Log.e(TAG, "Error al cargar licencias: " + error.getMessage());
                        Toast.makeText(LicenciasAdminActivity.this,
                                "Error al cargar licencias", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void aprobarLicencia(String licenciaId, int position) {
        if (licenciaId == null) return;

        //Validación de seguridad PREVIA
        if (position < 0 || position >= listaLicencias.size()) {
            Log.e(TAG, "Intento de borrar índice inválido en Licencias: " + position);
            adapter.notifyDataSetChanged(); // Sincronizar visualmente por si acaso
            return;
        }

        //Llamada a Firebase
        licenciasRef.child(licenciaId).child("id_estado").setValue(3) // 3 = Aprobado
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Licencia aprobada", Toast.LENGTH_SHORT).show();

                    //Validación de seguridad
                    if (position < listaLicencias.size()) {
                        listaLicencias.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, listaLicencias.size());
                    } else {
                        // Si la posición ya no es válida, refrescamos para evitar errores visuales
                        adapter.notifyDataSetChanged();
                    }

                    if (listaLicencias.isEmpty()) {
                        showEmptyState(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al aprobar licencia", e);
                    Toast.makeText(this, "Error al aprobar", Toast.LENGTH_SHORT).show();
                });
    }

    public void rechazarLicencia(String licenciaId, int position) {
        if (licenciaId == null) return;

        // Validación de seguridad PREVIA
        if (position < 0 || position >= listaLicencias.size()) {
            adapter.notifyDataSetChanged();
            return;
        }

        // Llamada a Firebase
        licenciasRef.child(licenciaId).child("id_estado").setValue(4) // 4 = Rechazado
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Licencia rechazada", Toast.LENGTH_SHORT).show();

                    // Validación de seguridad
                    if (position < listaLicencias.size()) {
                        listaLicencias.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, listaLicencias.size());
                    } else {
                        adapter.notifyDataSetChanged();
                    }

                    if (listaLicencias.isEmpty()) {
                        showEmptyState(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al rechazar licencia", e);
                    Toast.makeText(this, "Error al rechazar", Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean show) {
        tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpiar listeners de Firebase
        if (licenciasRef != null) {
            licenciasRef.removeEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {}
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }
}
