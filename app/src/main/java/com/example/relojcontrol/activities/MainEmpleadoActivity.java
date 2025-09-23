package com.example.relojcontrol.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.RegistroAsistencia;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainEmpleadoActivity extends AppCompatActivity {

    // Constants
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_USER_ID = "user_id";

    // Views
    private Toolbar toolbar;
    private TextView tvCurrentTime, tvCurrentDate, tvConfirmationMessage;
    private TextView tvEntradaTime, tvSalidaTime, tvEntradaStatus, tvSalidaStatus;
    private MaterialButton btnEntrada, btnSalida;

    // Variables
    private Handler timeHandler;
    private Runnable timeRunnable;
    private SimpleDateFormat timeFormat, dateFormat;
    private SharedPreferences sharedPreferences;
    private int userId;
    private boolean hasEntrada = false;
    private boolean hasSalida = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_empleado);

        initViews();
        setupToolbar();
        setupDateTimeFormats();
        setupClickListeners();
        initTimeUpdater();
        loadTodayAttendance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startTimeUpdater();
        loadTodayAttendance(); // Recargar datos por si hubo cambios
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimeUpdater();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimeUpdater();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        // Time and date views
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvCurrentDate = findViewById(R.id.tv_current_date);
        tvConfirmationMessage = findViewById(R.id.tv_confirmation_message);

        // History views
        tvEntradaTime = findViewById(R.id.tv_entrada_time);
        tvSalidaTime = findViewById(R.id.tv_salida_time);
        tvEntradaStatus = findViewById(R.id.tv_entrada_status);
        tvSalidaStatus = findViewById(R.id.tv_salida_status);

        // Buttons
        btnEntrada = findViewById(R.id.btn_entrada);
        btnSalida = findViewById(R.id.btn_salida);

        // SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userId = sharedPreferences.getInt(KEY_USER_ID, -1);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void setupDateTimeFormats() {
        timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        dateFormat = new SimpleDateFormat("EEEE, dd 'de' MMMM yyyy", new Locale("es", "ES"));
    }

    private void setupClickListeners() {
        btnEntrada.setOnClickListener(v -> registrarEntrada());
        btnSalida.setOnClickListener(v -> registrarSalida());
    }

    private void initTimeUpdater() {
        timeHandler = new Handler();
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                updateDateTime();
                timeHandler.postDelayed(this, 1000); // Actualizar cada segundo
            }
        };
    }

    private void startTimeUpdater() {
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.post(timeRunnable);
        }
    }

    private void stopTimeUpdater() {
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }

    private void updateDateTime() {
        Date currentTime = new Date();
        tvCurrentTime.setText(timeFormat.format(currentTime));
        tvCurrentDate.setText(dateFormat.format(currentTime));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_justificaciones) {
            Intent intent = new Intent(this, JustificadoresActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_profile) {
            // TODO: Abrir perfil de usuario
            Toast.makeText(this, "Perfil de usuario", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void registrarEntrada() {
        if (hasEntrada) {
            Toast.makeText(this, "Ya has registrado tu entrada hoy", Toast.LENGTH_SHORT).show();
            return;
        }

        btnEntrada.setEnabled(false);
        btnEntrada.setText("Registrando...");

        Date currentTime = new Date();
        RegistroAsistencia registro = new RegistroAsistencia();
        registro.setUsuarioId(userId);
        registro.setTipoRegistro("ENTRADA");
        registro.setFechaHora(currentTime);

        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<RegistroAsistencia> call = apiService.registrarAsistencia(registro);

        call.enqueue(new Callback<RegistroAsistencia>() {
            @Override
            public void onResponse(Call<RegistroAsistencia> call, Response<RegistroAsistencia> response) {
                btnEntrada.setEnabled(true);
                btnEntrada.setText("Registrar\nEntrada");

                if (response.isSuccessful() && response.body() != null) {
                    hasEntrada = true;
                    updateEntradaUI(currentTime);
                    showConfirmationMessage("Entrada registrada correctamente a las " +
                            timeFormat.format(currentTime));
                    updateButtonStates();
                } else {
                    Toast.makeText(MainEmpleadoActivity.this,
                            "Error al registrar entrada", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegistroAsistencia> call, Throwable t) {
                btnEntrada.setEnabled(true);
                btnEntrada.setText("Registrar\nEntrada");
                Toast.makeText(MainEmpleadoActivity.this,
                        "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registrarSalida() {
        if (!hasEntrada) {
            Toast.makeText(this, "Debes registrar primero tu entrada", Toast.LENGTH_SHORT).show();
            return;
        }

        if (hasSalida) {
            Toast.makeText(this, "Ya has registrado tu salida hoy", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSalida.setEnabled(false);
        btnSalida.setText("Registrando...");

        Date currentTime = new Date();
        RegistroAsistencia registro = new RegistroAsistencia();
        registro.setUsuarioId(userId);
        registro.setTipoRegistro("SALIDA");
        registro.setFechaHora(currentTime);

        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<RegistroAsistencia> call = apiService.registrarAsistencia(registro);

        call.enqueue(new Callback<RegistroAsistencia>() {
            @Override
            public void onResponse(Call<RegistroAsistencia> call, Response<RegistroAsistencia> response) {
                btnSalida.setEnabled(true);
                btnSalida.setText("Registrar\nSalida");

                if (response.isSuccessful() && response.body() != null) {
                    hasSalida = true;
                    updateSalidaUI(currentTime);
                    showConfirmationMessage("Salida registrada correctamente a las " +
                            timeFormat.format(currentTime));
                    updateButtonStates();
                } else {
                    Toast.makeText(MainEmpleadoActivity.this,
                            "Error al registrar salida", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegistroAsistencia> call, Throwable t) {
                btnSalida.setEnabled(true);
                btnSalida.setText("Registrar\nSalida");
                Toast.makeText(MainEmpleadoActivity.this,
                        "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTodayAttendance() {
        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<TodayAttendanceResponse> call = apiService.getTodayAttendance(userId);

        call.enqueue(new Callback<TodayAttendanceResponse>() {
            @Override
            public void onResponse(Call<TodayAttendanceResponse> call, Response<TodayAttendanceResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUIWithAttendanceData(response.body());
                } else {
                    // Si no hay registros para hoy, mantener estado inicial
                    resetAttendanceUI();
                }
            }

            @Override
            public void onFailure(Call<TodayAttendanceResponse> call, Throwable t) {
                // En caso de error, mantener estado actual
                Toast.makeText(MainEmpleadoActivity.this,
                        "Error al cargar datos de asistencia", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIWithAttendanceData(TodayAttendanceResponse data) {
        hasEntrada = data.getEntrada() != null;
        hasSalida = data.getSalida() != null;

        if (hasEntrada) {
            updateEntradaUI(data.getEntrada());
        }

        if (hasSalida) {
            updateSalidaUI(data.getSalida());
        }

        updateButtonStates();
    }

    private void updateEntradaUI(Date entradaTime) {
        tvEntradaTime.setText(timeFormat.format(entradaTime));
        tvEntradaStatus.setText("✓");
        tvEntradaStatus.setTextColor(getResources().getColor(R.color.success_color));
    }

    private void updateSalidaUI(Date salidaTime) {
        tvSalidaTime.setText(timeFormat.format(salidaTime));
        tvSalidaStatus.setText("✓");
        tvSalidaStatus.setTextColor(getResources().getColor(R.color.success_color));
    }

    private void resetAttendanceUI() {
        hasEntrada = false;
        hasSalida = false;

        tvEntradaTime.setText("Pendiente");
        tvSalidaTime.setText("Pendiente");
        tvEntradaStatus.setText("—");
        tvSalidaStatus.setText("—");
        tvEntradaStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        tvSalidaStatus.setTextColor(getResources().getColor(R.color.text_secondary));

        updateButtonStates();
    }

    private void updateButtonStates() {
        btnEntrada.setEnabled(!hasEntrada);
        btnSalida.setEnabled(hasEntrada && !hasSalida);
    }

    private void showConfirmationMessage(String message) {
        tvConfirmationMessage.setText(message);
        tvConfirmationMessage.setVisibility(View.VISIBLE);

        // Ocultar mensaje después de 5 segundos
        tvConfirmationMessage.postDelayed(() ->
                tvConfirmationMessage.setVisibility(View.GONE), 5000);
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

    @Override
    public void onBackPressed() {
        // En la activity principal del empleado, el botón back cierra la app
        moveTaskToBack(true);
    }

    // Clase auxiliar para la respuesta de asistencia del día
    private static class TodayAttendanceResponse {
        private Date entrada;
        private Date salida;

        public Date getEntrada() { return entrada; }
        public void setEntrada(Date entrada) { this.entrada = entrada; }
        public Date getSalida() { return salida; }
        public void setSalida(Date salida) { this.salida = salida; }
    }
}