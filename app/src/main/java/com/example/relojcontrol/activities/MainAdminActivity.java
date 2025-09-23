package com.example.relojcontrol.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.DashboardData;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainAdminActivity extends AppCompatActivity {

    // Constants
    private static final String PREFS_NAME = "LoginPrefs";

    // Views
    private Toolbar toolbar;
    private TextView tvEmpleadosPresentes, tvEmpleadosAusentes;
    private CardView cardUsuarios, cardJustificaciones, cardReportes;
    private View chartAtrasos;

    // Variables
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_admin);

        initViews();
        setupToolbar();
        setupClickListeners();
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar datos del dashboard cuando se regrese a esta activity
        loadDashboardData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        // Dashboard counters
        tvEmpleadosPresentes = findViewById(R.id.tv_empleados_presentes);
        tvEmpleadosAusentes = findViewById(R.id.tv_empleados_ausentes);

        // Action cards
        cardUsuarios = findViewById(R.id.card_usuarios);
        cardJustificaciones = findViewById(R.id.card_justificaciones);
        cardReportes = findViewById(R.id.card_reportes);

        // Chart
        chartAtrasos = findViewById(R.id.chart_atrasos);

        // SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void setupClickListeners() {
        // Card Usuarios - navegar a UsuariosActivity
        cardUsuarios.setOnClickListener(v -> {
            Intent intent = new Intent(this, UsuariosActivity.class);
            startActivity(intent);
        });

        // Card Justificaciones - navegar a JustificadoresActivity
        cardJustificaciones.setOnClickListener(v -> {
            Intent intent = new Intent(this, JustificadoresActivity.class);
            startActivity(intent);
        });

        // Card Reportes - navegar a ReportesActivity
        cardReportes.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportesActivity.class);
            startActivity(intent);
        });

        // Chart click para ver detalles
        chartAtrasos.setOnClickListener(v -> {
            // TODO: Implementar navegación a vista detallada de atrasos
            Toast.makeText(this, "Ver detalles de atrasos", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_profile) {
            // TODO: Abrir perfil de usuario
            Toast.makeText(this, "Perfil de usuario", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_settings) {
            // TODO: Abrir configuraciones
            Toast.makeText(this, "Configuraciones", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadDashboardData() {
        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<DashboardData> call = apiService.getDashboardData();

        call.enqueue(new Callback<DashboardData>() {
            @Override
            public void onResponse(Call<DashboardData> call, Response<DashboardData> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateDashboardUI(response.body());
                } else {
                    // En caso de error, mostrar datos por defecto o mensaje de error
                    Toast.makeText(MainAdminActivity.this,
                            "Error al cargar datos del dashboard", Toast.LENGTH_SHORT).show();
                    setDefaultDashboardData();
                }
            }

            @Override
            public void onFailure(Call<DashboardData> call, Throwable t) {
                Toast.makeText(MainAdminActivity.this,
                        "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                setDefaultDashboardData();
            }
        });
    }

    private void updateDashboardUI(DashboardData data) {
        // Actualizar contadores
        tvEmpleadosPresentes.setText(String.valueOf(data.getEmpleadosPresentes()));
        tvEmpleadosAusentes.setText(String.valueOf(data.getEmpleadosAusentes()));

        // TODO: Actualizar gráfico de atrasos cuando se implemente
        // updateChartAtrasos(data.getAtrasosSemanales());
    }

    private void setDefaultDashboardData() {
        // Datos por defecto en caso de error
        tvEmpleadosPresentes.setText("--");
        tvEmpleadosAusentes.setText("--");
    }

    private void logout() {
        // Limpiar datos de sesión
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Navegar a LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Método para refrescar datos (puede ser llamado desde otras activities)
    public void refreshDashboard() {
        loadDashboardData();
    }

    // Método para manejar el botón de navegación del menú hamburguesa
    private void toggleDrawer() {
        // TODO: Implementar drawer navigation si se decide usar
        Toast.makeText(this, "Menú de navegación", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        // En la activity principal del admin, el botón back cierra la app
        moveTaskToBack(true);
    }
}