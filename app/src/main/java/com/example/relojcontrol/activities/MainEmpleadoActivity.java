package com.example.relojcontrol.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.button.MaterialButton;
import com.example.relojcontrol.R;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainEmpleadoActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvWelcome, tvCurrentTime, tvCurrentDate, tvConfirmationMessage;
    private TextView tvEntradaTime, tvSalidaTime, tvEntradaStatus, tvSalidaStatus;
    private MaterialButton btnEntrada, btnSalida;
    private ProgressBar progressBar;

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
        loadTodayAttendance();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimeUpdater();
    }

    private void initViews() {
        sharedPreferences = getSharedPreferences("RelojControl", MODE_PRIVATE);
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);

        tvWelcome = findViewById(R.id.tv_welcome);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvCurrentDate = findViewById(R.id.tv_current_date);
        tvConfirmationMessage = findViewById(R.id.tv_confirmation_message);

        tvEntradaTime = findViewById(R.id.tv_entrada_time);
        tvSalidaTime = findViewById(R.id.tv_salida_time);
        tvEntradaStatus = findViewById(R.id.tv_entrada_status);
        tvSalidaStatus = findViewById(R.id.tv_salida_status);

        btnEntrada = findViewById(R.id.btn_entrada);
        btnSalida = findViewById(R.id.btn_salida);

        userId = sharedPreferences.getInt("user_id", -1);

        // Configurar bienvenida
        String userName = sharedPreferences.getString("user_name", "Empleado");
        tvWelcome.setText("Bienvenido, " + userName);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void setupDateTimeFormats() {
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
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
                timeHandler.postDelayed(this, 1000);
            }
        };
    }

    private void startTimeUpdater() {
        timeHandler.post(timeRunnable);
    }

    private void stopTimeUpdater() {
        timeHandler.removeCallbacks(timeRunnable);
    }

    private void updateDateTime() {
        Date currentTime = new Date();
        tvCurrentTime.setText(timeFormat.format(currentTime));
        tvCurrentDate.setText(dateFormat.format(currentTime));
    }

    private void registrarEntrada() {
        if (hasEntrada) {
            Toast.makeText(this, "Ya registraste tu entrada hoy", Toast.LENGTH_SHORT).show();
            return;
        }

        btnEntrada.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> params = new HashMap<>();
        params.put("id_usuario", userId);
        params.put("id_tipo_accion", 1); // 1 = entrada
        params.put("observaciones", "Registro desde app móvil");

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiEndpoints.ASISTENCIA_REGISTRAR,
                new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressBar.setVisibility(View.GONE);
                        btnEntrada.setEnabled(true);

                        try {
                            if (response.getBoolean("success")) {
                                hasEntrada = true;
                                String horaEntrada = response.getJSONObject("data").getString("hora");
                                updateEntradaUI(horaEntrada);
                                showConfirmationMessage("Entrada registrada: " + horaEntrada);
                                updateButtonStates();
                            } else {
                                Toast.makeText(MainEmpleadoActivity.this,
                                        response.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(MainEmpleadoActivity.this,
                                    "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        btnEntrada.setEnabled(true);
                        Toast.makeText(MainEmpleadoActivity.this,
                                "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        ApiClient.getInstance(this).addToRequestQueue(request);
    }

    private void registrarSalida() {
        if (!hasEntrada) {
            Toast.makeText(this, "Primero debes registrar entrada", Toast.LENGTH_SHORT).show();
            return;
        }
        if (hasSalida) {
            Toast.makeText(this, "Ya registraste tu salida hoy", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSalida.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> params = new HashMap<>();
        params.put("id_usuario", userId);
        params.put("id_tipo_accion", 2); // 2 = salida
        params.put("observaciones", "Registro desde app móvil");

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiEndpoints.ASISTENCIA_REGISTRAR,
                new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressBar.setVisibility(View.GONE);
                        btnSalida.setEnabled(true);

                        try {
                            if (response.getBoolean("success")) {
                                hasSalida = true;
                                String horaSalida = response.getJSONObject("data").getString("hora");
                                updateSalidaUI(horaSalida);
                                showConfirmationMessage("Salida registrada: " + horaSalida);
                                updateButtonStates();
                            } else {
                                Toast.makeText(MainEmpleadoActivity.this,
                                        response.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(MainEmpleadoActivity.this,
                                    "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        btnSalida.setEnabled(true);
                        Toast.makeText(MainEmpleadoActivity.this,
                                "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        ApiClient.getInstance(this).addToRequestQueue(request);
    }

    private void loadTodayAttendance() {
        String url = ApiEndpoints.ASISTENCIA_HOY + "?id_usuario=" + userId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("success")) {
                                JSONObject data = response.getJSONObject("data");
                                updateUIWithAttendanceData(data);
                            }
                        } catch (JSONException e) {
                            // Error silencioso - puede ser que no haya registros hoy
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Error silencioso
                    }
                }
        );

        ApiClient.getInstance(this).addToRequestQueue(request);
    }

    private void updateUIWithAttendanceData(JSONObject data) throws JSONException {
        if (!data.isNull("entrada")) {
            JSONObject entrada = data.getJSONObject("entrada");
            String horaEntrada = entrada.getString("hora");
            hasEntrada = true;
            updateEntradaUI(horaEntrada);
        }

        if (!data.isNull("salida")) {
            JSONObject salida = data.getJSONObject("salida");
            String horaSalida = salida.getString("hora");
            hasSalida = true;
            updateSalidaUI(horaSalida);
        }

        updateButtonStates();
    }

    private void updateEntradaUI(String hora) {
        tvEntradaTime.setText(hora);
        tvEntradaStatus.setText("✓ Registrada");
        tvEntradaStatus.setTextColor(getResources().getColor(R.color.success_color));
    }

    private void updateSalidaUI(String hora) {
        tvSalidaTime.setText(hora);
        tvSalidaStatus.setText("✓ Registrada");
        tvSalidaStatus.setTextColor(getResources().getColor(R.color.success_color));
    }

    private void updateButtonStates() {
        btnEntrada.setEnabled(!hasEntrada);
        btnSalida.setEnabled(hasEntrada && !hasSalida);

        if (hasEntrada) {
            btnEntrada.setText("Entrada\nRegistrada");
        } else {
            btnEntrada.setText("Registrar\nEntrada");
        }

        if (hasSalida) {
            btnSalida.setText("Salida\nRegistrada");
        } else {
            btnSalida.setText("Registrar\nSalida");
        }
    }

    private void showConfirmationMessage(String message) {
        tvConfirmationMessage.setText(message);
        tvConfirmationMessage.setVisibility(View.VISIBLE);
        tvConfirmationMessage.postDelayed(() ->
                tvConfirmationMessage.setVisibility(View.GONE), 5000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.empleado_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_justificaciones) {
            startActivity(new Intent(this, JustificadoresActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        sharedPreferences.edit().clear().apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}