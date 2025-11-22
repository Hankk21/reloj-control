package com.example.relojcontrol.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.relojcontrol.R;
import com.example.relojcontrol.adapters.JustificacionesAdapter;
import com.example.relojcontrol.adapters.LicenciasAdapter;
import com.example.relojcontrol.models.Justificacion;
import com.example.relojcontrol.models.Licencia;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JustificadoresActivity extends AppCompatActivity {
    private static final String TAG = "JustificadoresActivity";

    // Vistas del formulario
    private AutoCompleteTextView spinnerTipo;
    private TextInputLayout tilMotivo, tilDescripcion, tilFechaJustificar;
    private TextInputLayout tilFechaInicio, tilFechaFin;
    private TextInputEditText etMotivo, etDescripcion, etFechaJustificar;
    private TextInputEditText etFechaInicio, etFechaFin;
    private MaterialButton btnAdjuntar, btnEnviar;
    private RecyclerView rvArchivosAdjuntos, rvHistorial;
    private TextView tvHistorialTitle, tvNoHistorial;
    private ImageView ivRefreshHistorial;

    // Variables de control
    private String tipoSeleccionado = "Justificaciones";
    private Uri archivoSeleccionado = null;

    // Firebase y datos
    private DatabaseReference databaseRef;
    private SharedPreferences sharedPreferences;
    private int idUsuarioActual; // El ID numérico (ej: 2, 3)

    // Adapters para historial
    private JustificacionesAdapter justificacionesAdapter;
    private LicenciasAdapter licenciasAdapter;
    private List<Justificacion> listaJustificaciones;
    private List<Licencia> listaLicencias;

    // Launcher para seleccionar archivos
    private ActivityResultLauncher<Intent> seleccionarArchivoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_justificadores);

        // 1. Inicializar Firebase y Preferencias
        databaseRef = FirebaseDatabase.getInstance().getReference();
        sharedPreferences = getSharedPreferences("RelojControl", MODE_PRIVATE);

        // 2. Cargar ID del Usuario
        loadUserData();

        initViews();
        setupToolbar();
        setupSpinner();
        setupDatePickers();
        setupFileSelector();
        setupListeners();

        // 3. Cargar UI inicial
        actualizarFormulario();

        // 4. Verificar Modo (Historial Unificado o Individual)
        checkIntentMode();
    }

    private void loadUserData() {
        // Recuperamos el ID numérico (1, 2, 3...) guardado en LoginActivity
        idUsuarioActual = sharedPreferences.getInt("user_id_num", -1);

        Log.d(TAG, "Usuario cargado ID: " + idUsuarioActual);

        if (idUsuarioActual == -1) {
            Toast.makeText(this, "Error de sesión: Usuario no identificado", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkIntentMode() {
        String tipoIntent = getIntent().getStringExtra("tipo");
        if ("historial".equals(tipoIntent)) {
            // Si viene de "Mi Historial", mostramos Justificaciones por defecto
            cargarHistorial();
        } else if (tipoIntent != null) {
            // Si viene con tipo específico, seteamos el spinner
            if ("Licencias".equals(tipoIntent)) {
                spinnerTipo.setText("Licencias", false);
                tipoSeleccionado = "Licencias";
            }
            actualizarFormulario();
            cargarHistorial();
        } else {
            cargarHistorial();
        }
    }

    private void initViews() {
        // Spinner
        spinnerTipo = findViewById(R.id.spinner_tipo);

        // Layouts
        tilMotivo = findViewById(R.id.til_motivo);
        tilDescripcion = findViewById(R.id.til_descripcion);
        tilFechaJustificar = findViewById(R.id.til_fecha_justificar);
        tilFechaInicio = findViewById(R.id.til_fecha_inicio);
        tilFechaFin = findViewById(R.id.til_fecha_fin);

        // EditTexts
        etMotivo = findViewById(R.id.et_motivo);
        etDescripcion = findViewById(R.id.et_descripcion);
        etFechaJustificar = findViewById(R.id.et_fecha_justificar);
        etFechaInicio = findViewById(R.id.et_fecha_inicio);
        etFechaFin = findViewById(R.id.et_fecha_fin);

        // Botones y Listas
        btnAdjuntar = findViewById(R.id.btn_adjuntar);
        btnEnviar = findViewById(R.id.btn_enviar);
        rvArchivosAdjuntos = findViewById(R.id.rv_archivos_adjuntos);
        rvHistorial = findViewById(R.id.rv_historial);
        rvHistorial.setLayoutManager(new LinearLayoutManager(this)); // Configurar LayoutManager aquí

        tvHistorialTitle = findViewById(R.id.tv_historial_title);
        tvNoHistorial = findViewById(R.id.tv_no_historial);
        ivRefreshHistorial = findViewById(R.id.iv_refresh_historial);

        // Listas
        listaJustificaciones = new ArrayList<>();
        listaLicencias = new ArrayList<>();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Solicitudes");
        }
    }

    private void setupSpinner() {
        String[] tipos = {"Justificaciones", "Licencias"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, tipos);
        spinnerTipo.setAdapter(adapter);
        // No setear texto aquí si ya lo hace checkIntentMode, o dejar default
        if (spinnerTipo.getText().toString().isEmpty()) {
            spinnerTipo.setText("Justificaciones", false);
        }

        spinnerTipo.setOnItemClickListener((parent, view, position, id) -> {
            tipoSeleccionado = tipos[position];
            actualizarFormulario();
            cargarHistorial();
        });
    }

    private void setupDatePickers() {
        etFechaJustificar.setOnClickListener(v -> mostrarDatePicker(etFechaJustificar));
        etFechaInicio.setOnClickListener(v -> mostrarDatePicker(etFechaInicio));
        etFechaFin.setOnClickListener(v -> mostrarDatePicker(etFechaFin));
    }

    private void mostrarDatePicker(final TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String fecha = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                            selectedYear, selectedMonth + 1, selectedDay);
                    editText.setText(fecha);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void setupFileSelector() {
        seleccionarArchivoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        archivoSeleccionado = result.getData().getData();
                        if (archivoSeleccionado != null) {
                            btnAdjuntar.setText("Archivo: " + obtenerNombreArchivo(archivoSeleccionado));
                            btnAdjuntar.setIconResource(R.drawable.ic_check);
                        }
                    }
                }
        );
    }

    private void setupListeners() {
        btnAdjuntar.setOnClickListener(v -> seleccionarArchivo());

        btnEnviar.setOnClickListener(v -> {
            if (tipoSeleccionado.equals("Justificaciones")) {
                enviarJustificacion();
            } else {
                enviarLicencia();
            }
        });

        ivRefreshHistorial.setOnClickListener(v -> cargarHistorial());
    }

    private void actualizarFormulario() {
        if (tipoSeleccionado.equals("Justificaciones")) {
            tilDescripcion.setVisibility(View.VISIBLE);
            tilFechaJustificar.setVisibility(View.VISIBLE);
            tilFechaInicio.setVisibility(View.GONE);
            tilFechaFin.setVisibility(View.GONE);
        } else {
            tilDescripcion.setVisibility(View.GONE);
            tilFechaJustificar.setVisibility(View.GONE);
            tilFechaInicio.setVisibility(View.VISIBLE);
            tilFechaFin.setVisibility(View.VISIBLE);
        }
        // Opcional: limpiarFormulario() si queremos borrar al cambiar de pestaña
    }

    private void seleccionarArchivo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            seleccionarArchivoLauncher.launch(Intent.createChooser(intent, "Seleccionar PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir selector", Toast.LENGTH_SHORT).show();
        }
    }

    private String obtenerNombreArchivo(Uri uri) {
        String nombre = "archivo.pdf";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1) nombre = cursor.getString(nameIndex);
            cursor.close();
        }
        return nombre;
    }

    private void enviarJustificacion() {
        if (!validarFormulario()) return;

        Justificacion justificacion = new Justificacion();
        justificacion.setMotivo(etMotivo.getText().toString().trim());
        justificacion.setDescripcion(etDescripcion.getText().toString().trim());
        justificacion.setFechaJustificar(etFechaJustificar.getText().toString().trim());
        justificacion.setFechaCreacion(obtenerFechaActual());

        // Asignar ID del usuario actual
        justificacion.setIdUsuario(idUsuarioActual);

        justificacion.setIdEstado(2); // Pendiente
        justificacion.setUrlDocumento(archivoSeleccionado != null ? archivoSeleccionado.toString() : "");

        DatabaseReference ref = databaseRef.child("justificaciones");
        String key = ref.push().getKey();

        if (key != null) {
            ref.child(key).setValue(justificacion)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Enviado correctamente", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarHistorial();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al enviar", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error envio", e);
                    });
        }
    }

    private void enviarLicencia() {
        if (!validarFormulario()) return;

        Licencia licencia = new Licencia();
        licencia.setMotivo(etMotivo.getText().toString().trim());
        licencia.setFechaInicio(etFechaInicio.getText().toString().trim());
        licencia.setFechaFin(etFechaFin.getText().toString().trim());
        licencia.setFechaCreacion(obtenerFechaActual());

        // Asignar ID del usuario actual
        licencia.setIdUsuario(idUsuarioActual);

        licencia.setIdEstado(2); // Pendiente
        licencia.setUrlDocumento(archivoSeleccionado != null ? archivoSeleccionado.toString() : "");

        DatabaseReference ref = databaseRef.child("licencias");
        String key = ref.push().getKey();

        if (key != null) {
            ref.child(key).setValue(licencia)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Enviado correctamente", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarHistorial();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al enviar", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error envio", e);
                    });
        }
    }

    private boolean validarFormulario() {
        boolean esValido = true;
        if (etMotivo.getText().toString().trim().isEmpty()) {
            tilMotivo.setError("Requerido");
            esValido = false;
        } else {
            tilMotivo.setError(null);
        }

        if (tipoSeleccionado.equals("Justificaciones")) {
            if (etDescripcion.getText().toString().trim().isEmpty()) {
                tilDescripcion.setError("Requerido");
                esValido = false;
            }
            if (etFechaJustificar.getText().toString().trim().isEmpty()) {
                tilFechaJustificar.setError("Requerido");
                esValido = false;
            }
        } else {
            if (etFechaInicio.getText().toString().trim().isEmpty()) {
                tilFechaInicio.setError("Requerido");
                esValido = false;
            }
            if (etFechaFin.getText().toString().trim().isEmpty()) {
                tilFechaFin.setError("Requerido");
                esValido = false;
            }
        }
        return esValido;
    }

    private void cargarHistorial() {
        if (tipoSeleccionado.equals("Justificaciones")) {
            cargarHistorialJustificaciones();
        } else {
            cargarHistorialLicencias();
        }
    }

    private void cargarHistorialJustificaciones() {
        tvHistorialTitle.setText("Historial de Justificaciones");
        DatabaseReference ref = databaseRef.child("justificaciones");

        // Filtrar por 'id_usuario' (usando el ID numérico)
        ref.orderByChild("id_usuario").equalTo(idUsuarioActual)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaJustificaciones.clear();
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                Justificacion j = data.getValue(Justificacion.class);
                                if (j != null) {
                                    j.setId(data.getKey());
                                    listaJustificaciones.add(j);
                                }
                            }
                            // Ordenar: más reciente primero
                            Collections.reverse(listaJustificaciones);

                            mostrarHistorialJustificaciones();
                            tvNoHistorial.setVisibility(View.GONE);
                            rvHistorial.setVisibility(View.VISIBLE);
                        } else {
                            tvNoHistorial.setVisibility(View.VISIBLE);
                            rvHistorial.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error cargar historial", error.toException());
                    }
                });
    }

    private void cargarHistorialLicencias() {
        tvHistorialTitle.setText("Historial de Licencias");
        DatabaseReference ref = databaseRef.child("licencias");

        // CORRECCIÓN: Filtrar por 'id_usuario'
        ref.orderByChild("id_usuario").equalTo(idUsuarioActual)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaLicencias.clear();
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                Licencia l = data.getValue(Licencia.class);
                                if (l != null) {
                                    l.setId(data.getKey());
                                    listaLicencias.add(l);
                                }
                            }
                            Collections.reverse(listaLicencias);

                            mostrarHistorialLicencias();
                            tvNoHistorial.setVisibility(View.GONE);
                            rvHistorial.setVisibility(View.VISIBLE);
                        } else {
                            tvNoHistorial.setVisibility(View.VISIBLE);
                            rvHistorial.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error cargar historial", error.toException());
                    }
                });
    }

    private void mostrarHistorialJustificaciones() {
        justificacionesAdapter = new JustificacionesAdapter(listaJustificaciones, this);
        rvHistorial.setAdapter(justificacionesAdapter);
    }

    private void mostrarHistorialLicencias() {
        licenciasAdapter = new LicenciasAdapter(listaLicencias, this);
        rvHistorial.setAdapter(licenciasAdapter);
    }

    private String obtenerFechaActual() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void limpiarFormulario() {
        etMotivo.setText("");
        etDescripcion.setText("");
        etFechaJustificar.setText("");
        etFechaInicio.setText("");
        etFechaFin.setText("");
        archivoSeleccionado = null;
        btnAdjuntar.setText("Adjuntar archivo PDF (máx. 5 MB)");
        btnAdjuntar.setIconResource(R.drawable.ic_attach_file);

        tilMotivo.setError(null);
        tilDescripcion.setError(null);
        tilFechaJustificar.setError(null);
        tilFechaInicio.setError(null);
        tilFechaFin.setError(null);

        // Quitar focus
        etMotivo.clearFocus();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}