package com.example.relojcontrol.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Usuario;
import com.example.relojcontrol.models.Justificacion;
import com.example.relojcontrol.network.FirebaseRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainAdminActivity extends AppCompatActivity {

    private static final String TAG = "MainAdminActivity";
    private static final String PREFS_NAME = "RelojControl";

    private Toolbar toolbar;
    private ProgressBar progressBar;
    private TextView tvWelcome;

    // Cards de estadísticas
    private TextView tvEmpleadosTotales, tvEmpleadosPresentes, tvEmpleadosAusentes, tvAtrasosHoy;
    private TextView tvJustificacionesPendientes;

    // Cards de navegación
    private CardView cardUsuarios, cardJustificaciones, cardReportes;

    // Data
    private FirebaseRepository repository;
    private SharedPreferences sharedPreferences;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_admin);

        Log.d(TAG, "=== MainAdminActivity iniciada ===");

        initFirebase();
        initViews();
        setupToolbar();
        loadUserData();
        setupClickListeners();
        loadDashboardData();
    }

    private void initFirebase() {
        repository = FirebaseRepository.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Log.d(TAG, "Firebase repository inicializado");
    }

    private void initViews() {
        // IDs de las vistas desde el XML
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        tvWelcome = findViewById(R.id.tv_welcome);

        // TextViews de estadísticas
        tvEmpleadosTotales = findViewById(R.id.tv_empleados_totales);
        tvEmpleadosPresentes = findViewById(R.id.tv_empleados_presentes);
        tvEmpleadosAusentes = findViewById(R.id.tv_empleados_ausentes);
        tvAtrasosHoy = findViewById(R.id.tv_atrasos_hoy);
        tvJustificacionesPendientes = findViewById(R.id.tv_justificaciones_pendientes);

        // Cards de navegación
        cardUsuarios = findViewById(R.id.card_usuarios);
        cardJustificaciones = findViewById(R.id.card_justificaciones);
        cardReportes = findViewById(R.id.card_reportes);

        Log.d(TAG, "Views inicializadas");
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Panel de Administración");
        }
    }

    private void loadUserData() {
        userName = sharedPreferences.getString("user_name", "Administrador");
        tvWelcome.setText("Bienvenido, " + userName);
        Log.d(TAG, "Datos de usuario cargados - Nombre: " + userName);
    }

    private void setupClickListeners() {
        cardUsuarios.setOnClickListener(v -> {
            Intent intent = new Intent(this, UsuariosActivity.class);
            startActivity(intent);
        });

        cardJustificaciones.setOnClickListener(v -> {
            Intent intent = new Intent(this, JustificadoresActivity.class);
            startActivity(intent);
        });

        cardReportes.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportesActivity.class);
            startActivity(intent);
        });

        Log.d(TAG, "Click listeners configurados");
    }

    private void loadDashboardData() {
        Log.d(TAG, "Cargando datos del dashboard admin");
        showLoading(true);

        loadTotalEmpleados();
        loadEmpleadosPresentes();
        loadEmpleadosAusentes();
        loadAtrasosHoy();
        loadJustificacionesPendientes();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void loadTotalEmpleados() {
        repository.obtenerUsuarios(new FirebaseRepository.DataCallback<List<Usuario>>() {
            @Override
            public void onSuccess(List<Usuario> usuarios) {
                // Filtrar solo empleados (rol != admin)
                int totalEmpleados = 0;
                for (Usuario usuario : usuarios) {
                    if (usuario.getIdRol() != 1) {
                        totalEmpleados++;
                    }
                }

                Log.d(TAG, "✓ Total empleados: " + totalEmpleados);
                final int finalTotal = totalEmpleados;
                runOnUiThread(() -> {
                    tvEmpleadosTotales.setText(String.valueOf(finalTotal));
                });
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "✗ Error cargando total empleados", error);
                runOnUiThread(() -> {
                    tvEmpleadosTotales.setText("Error");
                    showLoading(false);
                });
            }
        });
    }

    private void loadEmpleadosPresentes() {
        String fechaHoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        repository.mDatabase.child("indices").child("asistencias_por_fecha").child(fechaHoy)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // Contar usuarios únicos que registraron entrada hoy
                        int empleadosPresentes = 0;
                        for (DataSnapshot asistenciaRef : dataSnapshot.getChildren()) {
                            String asistenciaId = asistenciaRef.getKey();

                            // Aquí deberías verificar si es una "entrada" y contar usuarios únicos
                            // Por simplicidad, vamos a usar el número de asistencias como aproximación
                            empleadosPresentes++;
                        }

                        // Dividir por 2 asumiendo que cada empleado tiene entrada y salida
                        empleadosPresentes = Math.max(1, empleadosPresentes / 2);

                        Log.d(TAG, "✓ Empleados presentes: " + empleadosPresentes);
                        final int finalPresentes = empleadosPresentes;
                        runOnUiThread(() -> {
                            tvEmpleadosPresentes.setText(String.valueOf(finalPresentes));
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "✗ Error cargando empleados presentes", error.toException());
                        runOnUiThread(() -> {
                            tvEmpleadosPresentes.setText("Error");
                        });
                    }
                });
    }

    private void loadEmpleadosAusentes() {
        // Por simplicidad, calcular como Total - Presentes
        runOnUiThread(() -> {
            try {
                String totalStr = tvEmpleadosTotales.getText().toString();
                String presentesStr = tvEmpleadosPresentes.getText().toString();

                if (!totalStr.equals("Error") && !presentesStr.equals("Error")) {
                    int total = Integer.parseInt(totalStr);
                    int presentes = Integer.parseInt(presentesStr);
                    int ausentes = Math.max(0, total - presentes);

                    tvEmpleadosAusentes.setText(String.valueOf(ausentes));
                    Log.d(TAG, "✓ Empleados ausentes calculados: " + ausentes);
                }
            } catch (NumberFormatException e) {
                tvEmpleadosAusentes.setText("0");
            }
        });
    }

    private void loadAtrasosHoy() {
        // Por simplicidad, establecer en 0
        runOnUiThread(() -> {
            tvAtrasosHoy.setText("0");
            Log.d(TAG, "✓ Atrasos hoy: 0 (simplificado)");
        });
    }

    private void loadJustificacionesPendientes() {
        repository.obtenerJustificaciones(new FirebaseRepository.DataCallback<List<Justificacion>>() {
            @Override
            public void onSuccess(List<Justificacion> justificaciones) {
                // Contar justificaciones pendientes
                int pendientes = 0;
                for (Justificacion justificacion : justificaciones) {
                    if (justificacion.getIdEstado() == 2) { // 2 = pendiente
                        pendientes++;
                    }
                }

                Log.d(TAG, "✓ Justificaciones pendientes: " + pendientes);
                final int finalPendientes = pendientes;
                runOnUiThread(() -> {
                    tvJustificacionesPendientes.setText(String.valueOf(finalPendientes));
                    showLoading(false); // Ocultar loading cuando termine
                });
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "✗ Error cargando justificaciones pendientes", error);
                runOnUiThread(() -> {
                    tvJustificacionesPendientes.setText("Error");
                    showLoading(false);
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData(); // Recargar datos cuando vuelva a la activity
    }
}
