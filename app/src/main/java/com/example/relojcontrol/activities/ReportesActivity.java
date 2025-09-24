package com.example.relojcontrol.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

import com.example.relojcontrol.R;

public class ReportesActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private MaterialButton btnGenerarReporte;
    private ProgressBar progressBar;
    private RecyclerView recyclerView; // NUEVO
    private ReportesAdapter adapter; // NUEVO
    private List<Reporte> reportesList; // NUEVO

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        initViews();
        setupToolbar();
        SetupRecyclerView(); // NUEVO
        setupClickListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        btnGenerarReporte = findViewById(R.id.btn_generar_reporte);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView); // NUEVO
        reportesList = new ArrayList<>(); // NUEVO
    }
    private void setupRecyclerView() {
        adapter = new ReportesAdapter(reportesList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Generar Reportes");
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnGenerarReporte.setOnClickListener(v -> {
            Toast.makeText(this, "Generar reporte PDF - En desarrollo", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_usuarios) {
            finish();
            return true;
        } else if (id == R.id.menu_justificaciones) {
            finish();
            return true;
        } else if (id == R.id.menu_reportes) {
            return true;
        } else if (id == R.id.menu_cerrar_sesion) {
            finishAffinity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}