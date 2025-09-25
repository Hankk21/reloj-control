package com.example.relojcontrol.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.relojcontrol.R;

public class MainAdminActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_admin);

        initViews();
        setupToolbar();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvWelcome = findViewById(R.id.tv_welcome);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Dashboard Admin");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_dashboard) {
            // Ya estamos en dashboard
            return true;
        } else if (id == R.id.menu_usuarios) {
            startActivity(new Intent(this, UsuariosActivity.class));
            return true;
        } else if (id == R.id.menu_justificaciones) {
            startActivity(new Intent(this, JustificadoresActivity.class));
            return true;
        } else if (id == R.id.menu_licencias) {
            // startActivity(new Intent(this, LicenciasActivity.class));
            return true;
        } else if (id == R.id.menu_reportes) {
            startActivity(new Intent(this, ReportesActivity.class));
            return true;
        } else if (id == R.id.menu_cerrar_sesion) {
            cerrarSesion();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void cerrarSesion() {
        // Limpiar preferencias y volver a login
        // SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        // preferences.edit().clear().apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}