package com.example.relojcontrol.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.relojcontrol.R;
import com.example.relojcontrol.adapters.AsistenciaAdapter; // ¡Nuevo!
import com.example.relojcontrol.adapters.JustificacionesAdapter;
import com.example.relojcontrol.models.Asistencia;
import com.example.relojcontrol.models.Justificacion;
import com.example.relojcontrol.models.Usuario;
import com.example.relojcontrol.network.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class VisualizarUsuarioActivity extends AppCompatActivity {

    private static final String TAG = "VisualizarUsuarioActivity";

    // Views
    private Toolbar toolbar;
    private ImageView ivAvatar, ivCopyRut, ivCopyEmail;
    private TextView tvNombreCompleto, tvRolBadge, tvEstadoBadge;
    private TextView tvFechaRegistro, tvUltimoAcceso;
    private TextView tvRut, tvCorreo;

    private MaterialButton btnEditar, btnResetearPassword, btnEliminar;

    // Historial Asistencia
    private TextView tvVerHistorialCompleto;
    private TextView tvDiasPresente, tvDiasAusente, tvAtrasos, tvPorcentajeAsistencia;
    private RecyclerView rvHistorialAsistencia;
    private TextView tvNoHistorialAsistencia;

    // Historial Justificaciones
    private TextView tvVerJustificacionesCompleto;
    private TextView tvJustificacionesPendientes, tvJustificacionesAceptadas, tvJustificacionesRechazadas;
    private RecyclerView rvHistorialJustificaciones;
    private TextView tvNoJustificaciones;

    // Data
    private FirebaseRepository repository;
    private String usuarioIdNumerico; // El ID "5" que viene del Intent
    private String currentFirebaseUid; // El UID "XyZ..." real de Firebase
    private Usuario usuario;

    private JustificacionesAdapter justificacionesAdapter;
    private AsistenciaAdapter asistenciaAdapter; // ¡Nuevo!

    private List<Asistencia> asistenciasList;
    private List<Justificacion> justificacionesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualizar_usuario);
        Log.d(TAG, "=== VisualizarUsuarioActivity iniciada ===");

        initFirebase();
        initViews();
        setupToolbar();
        setupRecyclerViews();
        setupClickListeners();

        // Obtener ID numérico del Intent
        usuarioIdNumerico = getIntent().getStringExtra("usuario_id");

        if (usuarioIdNumerico != null && !usuarioIdNumerico.isEmpty()) {
            loadUsuarioData(); // Aquí inicia la magia corregida
        } else {
            Log.e(TAG, "No se recibió usuario_id");
            Toast.makeText(this, "Error: No se seleccionó usuario", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initFirebase() {
        repository = FirebaseRepository.getInstance();
        asistenciasList = new ArrayList<>();
        justificacionesList = new ArrayList<>();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        ivAvatar = findViewById(R.id.iv_avatar);
        ivCopyRut = findViewById(R.id.iv_copy_rut);
        ivCopyEmail = findViewById(R.id.iv_copy_email);
        tvNombreCompleto = findViewById(R.id.tv_nombre_completo);
        tvRolBadge = findViewById(R.id.tv_rol_badge);
        tvEstadoBadge = findViewById(R.id.tv_estado_badge);
        tvFechaRegistro = findViewById(R.id.tv_fecha_registro);
        tvUltimoAcceso = findViewById(R.id.tv_ultimo_acceso);
        tvRut = findViewById(R.id.tv_rut);
        tvCorreo = findViewById(R.id.tv_correo);

        btnEditar = findViewById(R.id.btn_editar);
        btnResetearPassword = findViewById(R.id.btn_resetear_password);
        btnEliminar = findViewById(R.id.btn_eliminar);

        tvVerHistorialCompleto = findViewById(R.id.tv_ver_historial_completo);
        tvDiasPresente = findViewById(R.id.tv_dias_presente);
        tvDiasAusente = findViewById(R.id.tv_dias_ausente);
        tvAtrasos = findViewById(R.id.tv_atrasos);
        tvPorcentajeAsistencia = findViewById(R.id.tv_porcentaje_asistencia);
        rvHistorialAsistencia = findViewById(R.id.rv_historial_asistencia);
        tvNoHistorialAsistencia = findViewById(R.id.tv_no_historial_asistencia);

        tvVerJustificacionesCompleto = findViewById(R.id.tv_ver_justificaciones_completo);
        tvJustificacionesPendientes = findViewById(R.id.tv_justificaciones_pendientes);
        tvJustificacionesAceptadas = findViewById(R.id.tv_justificaciones_aceptadas);
        tvJustificacionesRechazadas = findViewById(R.id.tv_justificaciones_rechazadas);
        rvHistorialJustificaciones = findViewById(R.id.rv_historial_justificaciones);
        tvNoJustificaciones = findViewById(R.id.tv_no_justificaciones);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Información del Usuario");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    // === CARGA DE DATOS EN DOS PASOS ===
    private void loadUsuarioData() {
        Log.d(TAG, "1. Buscando UID para ID numérico: " + usuarioIdNumerico);

        //Traducir ID Numérico -> UID Firebase usando 'userMappings'
        repository.mDatabase.child("userMappings").child(usuarioIdNumerico)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            currentFirebaseUid = snapshot.getValue(String.class);
                            Log.d(TAG, "✓ Mapping encontrado. UID Real: " + currentFirebaseUid);

                            //Cargar datos reales con el UID
                            cargarDatosReales(currentFirebaseUid);
                        } else {
                            Log.e(TAG, "✗ No existe mapping para usuario " + usuarioIdNumerico);
                            Toast.makeText(VisualizarUsuarioActivity.this,
                                    "Error: Usuario no sincronizado", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error DB", error.toException());
                        Toast.makeText(VisualizarUsuarioActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void cargarDatosReales(String firebaseUid) {
        repository.mDatabase.child("usuarios").child(firebaseUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            usuario = dataSnapshot.getValue(Usuario.class);
                            if (usuario != null) {
                                mostrarInformacionUsuario();
                                // Ahora sí cargamos historiales usando el UID correcto
                                loadHistorialAsistencia(firebaseUid);
                                loadJustificaciones(usuarioIdNumerico); // Justificaciones suelen usar ID numérico en tu modelo
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void mostrarInformacionUsuario() {
        tvNombreCompleto.setText(usuario.getNombre() + " " + usuario.getApellido());
        tvRut.setText(usuario.getRut());
        tvCorreo.setText(usuario.getCorreo());

        if (usuario.getIdRol() == 1) {
            tvRolBadge.setText("Administrador");
            tvRolBadge.setBackgroundResource(R.drawable.badge_rol_admin);
        } else {
            tvRolBadge.setText("Empleado");
            tvRolBadge.setBackgroundResource(R.drawable.badge_rol_empleado);
        }

        String estado = usuario.getEstadoUsuario();
        tvEstadoBadge.setText(estado != null ? estado.toUpperCase() : "Desconocido");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Usuario: " + usuario.getNombre());
        }
    }

    private void setupRecyclerViews() {
        // Adapter Asistencia
        asistenciaAdapter = new AsistenciaAdapter(asistenciasList);
        rvHistorialAsistencia.setLayoutManager(new LinearLayoutManager(this));
        rvHistorialAsistencia.setAdapter(asistenciaAdapter);

        // Adapter Justificaciones
        justificacionesAdapter = new JustificacionesAdapter(justificacionesList, this);
        rvHistorialJustificaciones.setLayoutManager(new LinearLayoutManager(this));
        rvHistorialJustificaciones.setAdapter(justificacionesAdapter);
    }

    private void loadHistorialAsistencia(String firebaseUid) {
        repository.obtenerHistorialAsistencia(firebaseUid, new FirebaseRepository.DataCallback<List<Asistencia>>() {
            @Override
            public void onSuccess(List<Asistencia> asistencias) {
                asistenciasList.clear();

                // Mostrar solo las últimas 5
                if (asistencias.size() > 5) {
                    asistenciasList.addAll(asistencias.subList(0, 5));
                } else {
                    asistenciasList.addAll(asistencias);
                }

                calcularEstadisticasAsistencia(asistencias); // Estadísticas sobre el total, no el recorte

                if (asistenciasList.isEmpty()) {
                    rvHistorialAsistencia.setVisibility(View.GONE);
                    tvNoHistorialAsistencia.setVisibility(View.VISIBLE);
                    tvNoHistorialAsistencia.setText("Sin historial reciente");
                } else {
                    rvHistorialAsistencia.setVisibility(View.VISIBLE);
                    tvNoHistorialAsistencia.setVisibility(View.GONE);
                    asistenciaAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error historial", error);
            }
        });
    }

    private void loadJustificaciones(String idNumerico) {
        repository.obtenerJustificaciones(new FirebaseRepository.DataCallback<List<Justificacion>>() {
            @Override
            public void onSuccess(List<Justificacion> todas) {
                justificacionesList.clear();
                List<Justificacion> filtradas = new ArrayList<>();

                for (Justificacion j : todas) {
                    // Comparamos como String por seguridad
                    if (String.valueOf(j.getIdUsuario()).equals(idNumerico)) {
                        filtradas.add(j);
                    }
                }

                if (filtradas.size() > 5) justificacionesList.addAll(filtradas.subList(0, 5));
                else justificacionesList.addAll(filtradas);

                if (justificacionesList.isEmpty()) {
                    rvHistorialJustificaciones.setVisibility(View.GONE);
                    tvNoJustificaciones.setVisibility(View.VISIBLE);
                } else {
                    rvHistorialJustificaciones.setVisibility(View.VISIBLE);
                    tvNoJustificaciones.setVisibility(View.GONE);
                    justificacionesAdapter.notifyDataSetChanged();
                }

                // Calcular stats (opcional)
                int pendientes = 0;
                for(Justificacion j : filtradas) if(j.getIdEstado() == 2) pendientes++;
                tvJustificacionesPendientes.setText(String.valueOf(pendientes));
            }
            @Override public void onError(Exception e) {}
        });
    }

    private void calcularEstadisticasAsistencia(List<Asistencia> total) {
        int presentes = 0;
        for (Asistencia a : total) {
            if (a.getIdTipoAccion() == 1) presentes++;
        }
        tvDiasPresente.setText(String.valueOf(presentes));
        // Lógica simplificada
        tvDiasAusente.setText("0");
        tvAtrasos.setText("0");
    }

    private void setupClickListeners() {
        ivCopyRut.setOnClickListener(v -> copiar("RUT", tvRut.getText().toString()));
        ivCopyEmail.setOnClickListener(v -> copiar("Correo", tvCorreo.getText().toString()));
        btnEliminar.setOnClickListener(v -> confirmarEliminar());
        // Agregar listeners restantes...
    }

    private void copiar(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, label + " copiado", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmarEliminar() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Usuario")
                .setMessage("¿Seguro? Esta acción no se deshace.")
                .setPositiveButton("Eliminar", (d, w) -> {
                    if (currentFirebaseUid != null) {
                        repository.eliminarUsuario(currentFirebaseUid, new FirebaseRepository.CrudCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(VisualizarUsuarioActivity.this, "Eliminado", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(VisualizarUsuarioActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Métodos de menú y override obligatorios
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.visualizar_usuario_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}