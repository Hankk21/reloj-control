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
    private RecyclerView recyclerView;
    private JustificacionesAdminAdapter adapter;
    private List<Justificacion> listaJustificaciones;
    private DatabaseReference justificacionesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_justificaciones_admin);

        // Configurar toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Gestión de Justificaciones");

        recyclerView = findViewById(R.id.recyclerViewJustificaciones);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        listaJustificaciones = new ArrayList<>();
        adapter = new JustificacionesAdminAdapter(listaJustificaciones, this);
        recyclerView.setAdapter(adapter);

        cargarJustificaciones();
    }

    private void cargarJustificaciones() {
        justificacionesRef = FirebaseDatabase.getInstance()
                .getReference("justificaciones");

        justificacionesRef.orderByChild("id_estado").equalTo(2) // Pendientes
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaJustificaciones.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Justificacion justificacion = data.getValue(Justificacion.class);
                            justificacion.setId(data.getKey());
                            listaJustificaciones.add(justificacion);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(JustificacionesAdminActivity.this,
                                "Error al cargar", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void aprobarJustificacion(String justificacionId) {
        justificacionesRef.child(justificacionId)
                .child("id_estado").setValue(3) // 3 = Aprobado
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Justificación aprobada", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al aprobar", Toast.LENGTH_SHORT).show());
    }

    public void rechazarJustificacion(String justificacionId) {
        justificacionesRef.child(justificacionId)
                .child("id_estado").setValue(4) // 4 = Rechazado
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Justificación rechazada", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al rechazar", Toast.LENGTH_SHORT).show());
    }
}

