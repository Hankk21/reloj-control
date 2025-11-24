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
import com.example.relojcontrol.adapters.JustificacionesAdminAdapter;
import com.example.relojcontrol.models.Justificacion;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class JustificacionesAdminActivity extends AppCompatActivity {
    private static final String TAG = "JustificacionesAdminActivity";

    private RecyclerView recyclerView;
    private JustificacionesAdminAdapter adapter;
    private List<Justificacion> listaJustificaciones;
    private DatabaseReference justificacionesRef;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_justificaciones_admin);

        initViews();
        setupToolbar();
        setupRecyclerView();
        cargarJustificaciones();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewJustificaciones);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Gestión de Justificaciones");
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        listaJustificaciones = new ArrayList<>();
        adapter = new JustificacionesAdminAdapter(listaJustificaciones, this);
        recyclerView.setAdapter(adapter);
    }

    private void cargarJustificaciones() {
        showLoading(true);
        justificacionesRef = FirebaseDatabase.getInstance().getReference("justificaciones");

        // Cargar solo justificaciones pendientes
        justificacionesRef.orderByChild("id_estado").equalTo(2)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaJustificaciones.clear();

                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                Justificacion justificacion = data.getValue(Justificacion.class);
                                if (justificacion != null) {
                                    justificacion.setId(data.getKey());
                                    listaJustificaciones.add(justificacion);
                                }
                            }

                            showEmptyState(false);
                            adapter.notifyDataSetChanged();
                            Log.d(TAG, "Justificaciones cargadas: " + listaJustificaciones.size());
                        } else {
                            showEmptyState(true);
                            Log.d(TAG, "No hay justificaciones pendientes");
                        }

                        showLoading(false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        Log.e(TAG, "Error al cargar justificaciones: " + error.getMessage());
                        Toast.makeText(JustificacionesAdminActivity.this,
                                "Error al cargar justificaciones", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void aprobarJustificacion(String justificacionId, int position) {
        if (justificacionId == null) return;

        // VALIDACIÓN DE SEGURIDAD ANTI-CRASH
        if (position < 0 || position >= listaJustificaciones.size()) {
            Log.e(TAG, "Intento de borrar índice inválido: " + position);
            //recargamostodo por si la lista se desincronizo
            adapter.notifyDataSetChanged();
            return;
        }

        // Actualizar en Firebase
        justificacionesRef.child(justificacionId).child("id_estado").setValue(3) // 3 = Aprobado
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Justificación aprobada", Toast.LENGTH_SHORT).show();

                    // Solo borramos si el índice sigue siendo válido
                    if (position < listaJustificaciones.size()) {
                        listaJustificaciones.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, listaJustificaciones.size());
                    }

                    if (listaJustificaciones.isEmpty()) {
                        showEmptyState(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al aprobar justificación", e);
                    Toast.makeText(this, "Error al aprobar", Toast.LENGTH_SHORT).show();
                });
    }

    public void rechazarJustificacion(String justificacionId, int position) {
        if (justificacionId == null) return;

        if (position < 0 || position >= listaJustificaciones.size()) {
            adapter.notifyDataSetChanged();
            return;
        }

        justificacionesRef.child(justificacionId).child("id_estado").setValue(4) // 4 = Rechazado
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Justificación rechazada", Toast.LENGTH_SHORT).show();

                    if (position < listaJustificaciones.size()) {
                        listaJustificaciones.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, listaJustificaciones.size());
                    }

                    if (listaJustificaciones.isEmpty()) {
                        showEmptyState(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al rechazar justificación", e);
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
        // Limpiar listeners si es necesario
        if (justificacionesRef != null) {
            justificacionesRef.removeEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {}
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }
}
