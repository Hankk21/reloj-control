package com.example.relojcontrol.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.relojcontrol.R;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

public class MainAdminActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private TextView tvWelcome, tvEmpleadosTotales, tvEmpleadosPresentes, tvEmpleadosAusentes, tvAtrasosHoy, tvJustificacionesPendientes;
    private CardView cardUsuarios, cardJustificaciones, cardReportes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_admin);

        initViews();
        setupToolbar();
        setupClickListeners();
        loadDashboardData();
    }

    private void initViews() {
        sharedPreferences = getSharedPreferences("RelojControl", MODE_PRIVATE);
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);

        tvWelcome = findViewById(R.id.tv_welcome);
        tvEmpleadosTotales = findViewById(R.id.tv_empleados_totales);
        tvEmpleadosPresentes = findViewById(R.id.tv_empleados_presentes);
        tvEmpleadosAusentes = findViewById(R.id.tv_empleados_ausentes);
        tvAtrasosHoy = findViewById(R.id.tv_atrasos_hoy);
        tvJustificacionesPendientes = findViewById(R.id.tv_justificaciones_pendientes);

        cardUsuarios = findViewById(R.id.card_usuarios);
        cardJustificaciones = findViewById(R.id.card_justificaciones);
        cardReportes = findViewById(R.id.card_reportes);

        String userName = sharedPreferences.getString("user_name", "Administrador");
        tvWelcome.setText("Bienvenido, " + userName);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void setupClickListeners() {
        cardUsuarios.setOnClickListener(v -> {
            startActivity(new Intent(this, UsuariosActivity.class));
        });

        cardJustificaciones.setOnClickListener(v -> {
            startActivity(new Intent(this, JustificadoresActivity.class));
        });

        cardReportes.setOnClickListener(v -> {
            startActivity(new Intent(this, ReportesActivity.class));
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Verificar si el archivo de menú existe antes de inflarlo
        try {
            getMenuInflater().inflate(R.menu.admin_menu, menu);
            return true;
        } catch (Exception e) {
            // Si no existe el menú, no mostrar errores
            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        // Usar IDs de recursos de manera segura
        if (itemId == R.id.action_refresh) {
            loadDashboardData();
            return true;
        } else if (itemId == R.id.action_logout) {
            logout();
            return true;
        } else if (itemId == R.id.action_profile) {
            showUserProfile();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadDashboardData() {
        progressBar.setVisibility(View.VISIBLE);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                ApiEndpoints.DASHBOARD_ADMIN,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressBar.setVisibility(View.GONE);
                        try {
                            if (response.getBoolean("success")) {
                                JSONObject data = response.getJSONObject("data");
                                updateDashboardUI(data);
                            } else {
                                showError("Error al cargar datos");
                            }
                        } catch (JSONException e) {
                            showError("Error en formato de respuesta");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        showError("Error de conexión");
                    }
                }
        );

        ApiClient.getInstance(this).addToRequestQueue(request);
    }

    private void updateDashboardUI(JSONObject data) throws JSONException {
        tvEmpleadosTotales.setText(String.valueOf(data.getInt("empleados_totales")));
        tvEmpleadosPresentes.setText(String.valueOf(data.getInt("empleados_presentes")));
        tvEmpleadosAusentes.setText(String.valueOf(data.getInt("empleados_ausentes")));
        tvAtrasosHoy.setText(String.valueOf(data.getInt("atrasos_hoy")));
        tvJustificacionesPendientes.setText(String.valueOf(data.getInt("justificaciones_pendientes")));
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        tvEmpleadosTotales.setText("0");
        tvEmpleadosPresentes.setText("0");
        tvEmpleadosAusentes.setText("0");
        tvAtrasosHoy.setText("0");
        tvJustificacionesPendientes.setText("0");
    }

    private void showUserProfile() {
        Toast.makeText(this, "Perfil de usuario", Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        sharedPreferences.edit().clear().apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }
}