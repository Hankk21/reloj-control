package com.example.relojcontrol.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Asistencia;
import com.example.relojcontrol.network.FirebaseRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainEmpleadoActivity extends AppCompatActivity {

    private static final String TAG = "MainEmpleadoActivity";
    private static final String PREFS_NAME = "RelojControl";

    // Views del XML
    private Toolbar toolbar;
    private TextView tvCurrentTime, tvCurrentDate;
    private MaterialButton btnEntrada, btnSalida;
    private TextView tvConfirmationMessage;
    private TextView tvEntradaTime, tvSalidaTime, tvEntradaStatus, tvSalidaStatus;

    // Data
    private FirebaseRepository repository;
    private SharedPreferences sharedPreferences;
    private String userId;
    private String userName;

    // Handler para actualizar la hora
    private Handler timeHandler = new Handler();
    private Runnable timeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_empleado);

        Log.d(TAG, "=== MainEmpleadoActivity iniciada ===");

        initFirebase();
        initViews();
        setupToolbar();
        loadUserData();
        setupClickListeners();
        startTimeUpdater();
        loadTodayAttendance();
    }

    private void initFirebase() {
        repository = FirebaseRepository.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Log.d(TAG, "Firebase repository inicializado");
    }

    private void initViews() {
        // IDs exactos XML
        toolbar = findViewById(R.id.toolbar);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvCurrentDate = findViewById(R.id.tv_current_date);
        btnEntrada = findViewById(R.id.btn_entrada);
        btnSalida = findViewById(R.id.btn_salida);
        tvConfirmationMessage = findViewById(R.id.tv_confirmation_message);

        // Historial del día
        tvEntradaTime = findViewById(R.id.tv_entrada_time);
        tvSalidaTime = findViewById(R.id.tv_salida_time);
        tvEntradaStatus = findViewById(R.id.tv_entrada_status);
        tvSalidaStatus = findViewById(R.id.tv_salida_status);

        Log.d(TAG, "Views inicializadas");
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Registro de Asistencia");
        }
    }

    private void loadUserData() {
        userId = sharedPreferences.getString("user_id", "");
        userName = sharedPreferences.getString("user_name", "Usuario");
        Log.d(TAG, "Datos de usuario cargados - ID: " + userId + ", Nombre: " + userName);
    }

    private void setupClickListeners() {
        btnEntrada.setOnClickListener(v -> registrarAsistencia("entrada"));
        btnSalida.setOnClickListener(v -> registrarAsistencia("salida"));

        Log.d(TAG, "Click listeners configurados");
    }

    private void startTimeUpdater() {
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                updateDateTime();
                timeHandler.postDelayed(this, 1000); // Actualizar cada segundo
            }
        };
        timeHandler.post(timeRunnable);
    }

    private void updateDateTime() {
        Date now = new Date();

        // Formato para la hora (08:32 AM)
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now);
        tvCurrentTime.setText(time);

        // Formato para la fecha (Lunes, 18 de Septiembre 2025)
        String date = new SimpleDateFormat("EEEE, d 'de' MMMM yyyy",
                new Locale("es", "CL")).format(now);
        tvCurrentDate.setText(date);
    }

    private void loadTodayAttendance() {
        Log.d(TAG, "Cargando asistencia del día");

        repository.obtenerAsistenciaHoy(userId, new FirebaseRepository.DataCallback<List<Asistencia>>() {
            @Override
            public void onSuccess(List<Asistencia> asistencias) {
                Log.d(TAG, "✓ Asistencia de hoy obtenida: " + asistencias.size() + " registros");

                // Resetear valores por defecto
                tvEntradaTime.setText("Pendiente");
                tvSalidaTime.setText("Pendiente");
                tvEntradaStatus.setText("—");
                tvSalidaStatus.setText("—");

                // Procesar asistencias del día
                boolean hasEntrada = false, hasSalida = false;

                for (Asistencia asistencia : asistencias) {
                    int tipoAccionId = asistencia.getIdTipoAccion();

                    if (tipoAccionId == 1 && !hasEntrada) { // 1 = entrada
                        tvEntradaTime.setText(formatTime(asistencia.getHora()));
                        tvEntradaStatus.setText("✓");
                        hasEntrada = true;
                    } else if (tipoAccionId == 2 && !hasSalida) { // 2 = salida
                        tvSalidaTime.setText(formatTime(asistencia.getHora()));
                        tvSalidaStatus.setText("✓");
                        hasSalida = true;
                    }
                }

                // Actualizar estado de botones
                updateButtonStates(hasEntrada, hasSalida);
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "✗ Error cargando asistencia de hoy", error);
                Toast.makeText(MainEmpleadoActivity.this,
                        "Error cargando datos de hoy", Toast.LENGTH_SHORT).show();

                // Habilitar solo entrada por defecto en caso de error
                btnEntrada.setEnabled(true);
                btnSalida.setEnabled(false);
            }
        });
    }

    private String formatTime(String time24) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date date = input.parse(time24);
            return output.format(date);
        } catch (Exception e) {
            return time24; // Devolver original si hay error
        }
    }

    private void updateButtonStates(boolean hasEntrada, boolean hasSalida) {
        if (!hasEntrada) {
            // Sin entrada: habilitar solo entrada
            btnEntrada.setEnabled(true);
            btnSalida.setEnabled(false);
        } else if (!hasSalida) {
            // Con entrada pero sin salida: habilitar solo salida
            btnEntrada.setEnabled(false);
            btnSalida.setEnabled(true);
        } else {
            // Con entrada y salida: deshabilitar ambos
            btnEntrada.setEnabled(false);
            btnSalida.setEnabled(false);
        }

        Log.d(TAG, "Botones actualizados - Entrada: " + hasEntrada + ", Salida: " + hasSalida);
    }

    private void registrarAsistencia(String tipoAccion) {
        Log.d(TAG, "=== REGISTRANDO ASISTENCIA ===");
        Log.d(TAG, "Tipo: " + tipoAccion + ", Usuario: " + userId);

        // Deshabilitar botones mientras se procesa
        btnEntrada.setEnabled(false);
        btnSalida.setEnabled(false);
        hideConfirmationMessage();

        // Determinar ID del tipo de acción
        int tipoAccionId = "entrada".equals(tipoAccion) ? 1 : 2;

        repository.registrarAsistencia(userId, tipoAccionId, new FirebaseRepository.AsistenciaCallback() {
            @Override
            public void onSuccess(Asistencia asistencia) {
                Log.d(TAG, "✓ Asistencia registrada exitosamente");

                runOnUiThread(() -> {
                    String timeFormatted = formatTime(asistencia.getHora());
                    String mensaje = "entrada".equals(tipoAccion) ?
                            "Entrada registrada correctamente a las " + timeFormatted :
                            "Salida registrada correctamente a las " + timeFormatted;

                    showConfirmationMessage(mensaje, true);

                    // Recargar datos del día
                    loadTodayAttendance();
                });
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "✗ Error registrando asistencia", error);

                runOnUiThread(() -> {
                    String mensaje = "Error registrando " + tipoAccion + ": " + error.getMessage();
                    showConfirmationMessage(mensaje, false);

                    // Rehabilitar botones
                    loadTodayAttendance(); // Esto actualizará el estado correcto de los botones
                });
            }
        });
    }

    private void showConfirmationMessage(String message, boolean isSuccess) {
        tvConfirmationMessage.setText(message);
        tvConfirmationMessage.setVisibility(View.VISIBLE);

        // Ocultar el mensaje después de 5 segundos
        timeHandler.postDelayed(() -> hideConfirmationMessage(), 5000);
    }

    private void hideConfirmationMessage() {
        tvConfirmationMessage.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodayAttendance(); // Recargar datos cuando vuelva a la activity
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }
}
