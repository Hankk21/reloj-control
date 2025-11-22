package com.example.relojcontrol.activities.empleado;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.relojcontrol.R;
import com.example.relojcontrol.activities.JustificadoresActivity;
import com.example.relojcontrol.activities.LoginActivity;
import com.example.relojcontrol.adapters.AsistenciaAdapter;
import com.example.relojcontrol.models.Asistencia;
import com.example.relojcontrol.network.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

public class MainEmpleadoActivity extends AppCompatActivity {

    private static final String TAG = "MainEmpleadoActivity";
    private static final String PREFS_NAME = "RelojControl";

    /// Views
    private Toolbar toolbar;
    private TextView tvCurrentTime, tvCurrentDate;
    private MaterialButton btnEntrada, btnSalida;
    private TextView tvConfirmationMessage;

    // Vistas "ocultas" (para evitar crash por referencias antiguas)
    private TextView tvEntradaTime, tvSalidaTime, tvEntradaStatus, tvSalidaStatus;

    // Vistas Nuevas (Lista)
    private RecyclerView rvHistorial;
    private TextView tvNoHistorial;
    private AsistenciaAdapter adapter;

    // Data
    private FirebaseRepository repository;
    private SharedPreferences sharedPreferences;
    private String userId; // ID Numérico (Visual)
    private String userFirebaseUid; // UID Real (Firebase)
    private String userName;

    // Reloj
    private Handler timeHandler = new Handler();
    private Runnable timeRunnable;
    private ValueEventListener estadoUsuarioListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_empleado);

        Log.d(TAG, "=== MainEmpleadoActivity iniciada ===");

        initFirebase();
        initViews();

        loadUserData();

        setupToolbar();
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
        toolbar = findViewById(R.id.toolbar);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvCurrentDate = findViewById(R.id.tv_current_date);
        btnEntrada = findViewById(R.id.btn_entrada);
        btnSalida = findViewById(R.id.btn_salida);
        tvConfirmationMessage = findViewById(R.id.tv_confirmation_message);

        // Variables de texto antiguas (Defensa contra null)
        tvEntradaTime = findViewById(R.id.tv_entrada_time);
        tvSalidaTime = findViewById(R.id.tv_salida_time);
        tvEntradaStatus = findViewById(R.id.tv_entrada_status);
        tvSalidaStatus = findViewById(R.id.tv_salida_status);

        // Vistas nuevas
        rvHistorial = findViewById(R.id.rv_historial_asistencia);
        tvNoHistorial = findViewById(R.id.tv_no_historial_asistencia);

        // Configuración segura del RecyclerView
        if (rvHistorial != null) {
            rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        } else {
            Log.e(TAG, "CRÍTICO: No se encontró rv_historial_asistencia en el XML");
        }

        Log.d(TAG, "Views inicializadas");
    }

    private void loadUserData() {
        //recuperacion de id numerico guardado en login
        int idNumerico = sharedPreferences.getInt("user_id_num", -1);
        userId = String.valueOf(idNumerico);

        //convertido en string
        userFirebaseUid = sharedPreferences.getString("user_uid", "");
        userName = sharedPreferences.getString("user_name","Usuario");

        Log.d(TAG, "Datos cargados -> ID Numerico para consultas: " + userId + "| UID Real: " + userFirebaseUid);
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

        // Formato para la hora (08:30 AM)
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now);
        if (tvCurrentTime != null) tvCurrentTime.setText(time);

        // Formato para la fecha (Lunes, 18 de Septiembre 2025)
        String date = new SimpleDateFormat("EEEE, d 'de' MMMM yyyy",
                new Locale("es", "CL")).format(now);
        if (tvCurrentDate != null) tvCurrentDate.setText(time);
    }

    private void setupClickListeners() {
        btnEntrada.setOnClickListener(v -> registrarAsistencia("entrada"));
        btnSalida.setOnClickListener(v -> registrarAsistencia("salida"));

        Log.d(TAG, "Click listeners configurados");
    }

    private void loadTodayAttendance() {
        Log.d(TAG, "Cargando asistencia del día");

        repository.obtenerAsistenciaHoy(userFirebaseUid, new FirebaseRepository.DataCallback<List<Asistencia>>() {
            @Override
            public void onSuccess(List<Asistencia> asistencias) {
                Log.d(TAG, "✓ Asistencia de hoy obtenida: " + asistencias.size() + " registros");

                // Procesar asistencias del día
                boolean hasEntrada = false;
                boolean hasSalida = false;

                for (Asistencia a : asistencias) {
                    if (a.getIdTipoAccion() == 1) hasEntrada = true;
                    if (a.getIdTipoAccion() == 2) hasSalida = true;
                }
                updateButtonStates(hasEntrada, hasSalida);

                // actualizar lista visual
                if (asistencias.isEmpty()) {
                    if (rvHistorial != null) rvHistorial.setVisibility(View.GONE);
                    if (tvNoHistorial != null) tvNoHistorial.setVisibility(View.VISIBLE);
                } else {
                    if (rvHistorial != null) rvHistorial.setVisibility(View.VISIBLE);
                    if (tvNoHistorial != null) tvNoHistorial.setVisibility(View.GONE);
                    Collections.reverse(asistencias);

                    adapter = new AsistenciaAdapter(asistencias);
                    if (rvHistorial != null) rvHistorial.setAdapter(adapter);
                }

                //Actualizar Textos Ocultos (Para evitar crashes si algo los busca)
                if (tvEntradaStatus != null) tvEntradaStatus.setText(hasEntrada ? "OK" : "-");
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error cargando lista", error);
                // En caso de error, habilitamos entrada por seguridad
                btnEntrada.setEnabled(true);
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
            btnSalida.setAlpha(0.5f); // Efecto visual deshabilitado
            btnEntrada.setAlpha(1.0f);
        } else if (!hasSalida) {
            // Con entrada pero sin salida: habilitar solo salida
            btnEntrada.setEnabled(false);
            btnSalida.setAlpha(0.5f);
            btnSalida.setEnabled(true);
            btnEntrada.setAlpha(1.0f);
        } else {
            // Con entrada y salida: deshabilitar ambos
            btnEntrada.setEnabled(false);
            btnSalida.setEnabled(false);
            btnEntrada.setAlpha(0.5f);
            btnSalida.setAlpha(0.5f);
        }

        Log.d(TAG, "Botones actualizados - Entrada: " + hasEntrada + ", Salida: " + hasSalida);
    }

    private void registrarAsistencia(String tipoAccion) {
        Log.d(TAG, "=== REGISTRANDO ASISTENCIA ===");
        Log.d(TAG, "Tipo: " + tipoAccion + ", Usuario: " + userId);

        // Deshabilitar botones para evitar doble clic
        btnEntrada.setEnabled(false);
        btnSalida.setEnabled(false);

        // 1 = Entrada, 2 = Salida
        int tipoAccionId = "entrada".equals(tipoAccion) ? 1 : 2;

        repository.registrarAsistencia(userFirebaseUid, tipoAccionId, new FirebaseRepository.AsistenciaCallback() {
            @Override
            public void onSuccess(Asistencia asistencia) {
                Log.d(TAG, "✓ Asistencia registrada exitosamente");

                runOnUiThread(() -> {
                    showConfirmationMessage("Marca registrada correctamente", true);
                    loadTodayAttendance(); // Recargar lista
                });
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error registro", error);

                runOnUiThread(() -> {
                    showConfirmationMessage("Error: " + error.getMessage(), false);
                    loadTodayAttendance(); // Reactivar botones
                });
            }
        });
    }

    private void showConfirmationMessage(String message, boolean isSuccess) {
        tvConfirmationMessage.setText(message);
        tvConfirmationMessage.setVisibility(View.VISIBLE);
        tvConfirmationMessage.setBackgroundColor(isSuccess ? getColor(R.color.success_light)
                : getColor(R.color.error_light));
        tvConfirmationMessage.setTextColor(isSuccess ? getColor(R.color.success_text)
                : getColor(R.color.error_text));

        // Ocultar el mensaje después de 4 segundos
        timeHandler.postDelayed(() -> tvConfirmationMessage.setVisibility(View.GONE), 4000);
    }
    // configuracion de toolbar
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mi Asistencia");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.empleado_menu, menu);
        return true;
    }

    //manejar eventos en menu

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_mis_justificaciones) {
            Intent intentJustificaciones = new Intent(this, JustificadoresActivity.class);
            intentJustificaciones.putExtra("tipo", "Justificaciones");
            startActivity(intentJustificaciones);
            return true;
        } else if (id == R.id.menu_mis_licencias) {
            Intent intentLicencias = new Intent(this, JustificadoresActivity.class);
            intentLicencias.putExtra("tipo", "Licencias");
            startActivity(intentLicencias);
            return true;

        } else if (id == R.id.menu_cerrar_sesion) {
            SharedPreferences preferences = getSharedPreferences("RelojControl", MODE_PRIVATE);
            preferences.edit().clear().apply();
            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }else{
            return super.onOptionsItemSelected(item);
        }
    }

    private void escucharEstadoUsuario() {
        if (userFirebaseUid == null || userFirebaseUid.isEmpty()) return;

        DatabaseReference userRef = repository.mDatabase.child("usuarios").child(userFirebaseUid);

        estadoUsuarioListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String estado = snapshot.child("estado_usuario").getValue(String.class);
                    // Si el estado NO es activo, cerrar sesión forzosa
                    if (estado != null && !"activo".equalsIgnoreCase(estado)) {
                        Toast.makeText(MainEmpleadoActivity.this, "Tu cuenta ha sido desactivada.", Toast.LENGTH_LONG).show();
                        forceLogout();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };

        userRef.addValueEventListener(estadoUsuarioListener);
    }

    // metodo para forzar salida
    private void forceLogout() {
        if (sharedPreferences != null) sharedPreferences.edit().clear().apply();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
        // Limpiar listener de estado para evitar crashes
        if (userFirebaseUid != null && estadoUsuarioListener != null) {
            repository.mDatabase.child("usuarios").child(userFirebaseUid).removeEventListener(estadoUsuarioListener);
        }
    }
}
