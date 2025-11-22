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
    private String urlDocumentoFinal = "";
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

        // Inicializar Firebase y Preferencias
        databaseRef = FirebaseDatabase.getInstance().getReference();
        sharedPreferences = getSharedPreferences("RelojControl", MODE_PRIVATE);

        // Cargar ID del Usuario
        loadUserData();

        initViews();
        setupToolbar();
        setupSpinner();
        setupDatePickers();
        setupFileSelector();
        setupListeners();


        // Verificar Modo (Historial Unificado o Individual)
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

        if ("Licencias".equals(tipoIntent)) {
            tipoSeleccionado = "Licencias";
            spinnerTipo.setText("Licencias", false);
        } else {
            // Por defecto o si dice "Justificaciones"
            tipoSeleccionado = "Justificaciones";
            spinnerTipo.setText("Justificaciones", false);
        }

        actualizarFormulario();
        cargarHistorial();
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
        new DatePickerDialog(this, (view, year, month, day) -> {
            String fecha = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
            editText.setText(fecha);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setupFileSelector() {
        seleccionarArchivoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        archivoSeleccionado = result.getData().getData();
                        if (archivoSeleccionado != null) {
                            String nombreArchivo = obtenerNombreArchivo(archivoSeleccionado);
                            btnAdjuntar.setText(nombreArchivo); // Muestra el nombre en el botón
                            btnAdjuntar.setIconResource(R.drawable.ic_check);
                            // Por ahora, usaremos el nombre como "URL" si no hay Storage
                            urlDocumentoFinal = nombreArchivo;
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
        limpiarFormulario();
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

        // Deshabilitar botón para evitar doble envío
        btnEnviar.setEnabled(false);

        Justificacion justificacion = new Justificacion();
        justificacion.setMotivo(etMotivo.getText().toString().trim());
        justificacion.setDescripcion(etDescripcion.getText().toString().trim());
        justificacion.setFechaJustificar(etFechaJustificar.getText().toString().trim());
        justificacion.setFechaCreacion(obtenerFechaActual());
        justificacion.setIdUsuario(idUsuarioActual);
        justificacion.setIdEstado(2); //pendiente

        justificacion.setUrlDocumento(urlDocumentoFinal);

        justificacion.setIdEstado(2); // Pendiente
        justificacion.setUrlDocumento(archivoSeleccionado != null ? archivoSeleccionado.toString() : "");

        DatabaseReference ref = databaseRef.child("justificaciones");
        String key = ref.push().getKey();

        if (key != null) {
            ref.child(key).setValue(justificacion)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Solicitud enviada con éxito", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarHistorial();
                        // Reactivar botón
                        btnEnviar.setEnabled(true);
                        btnEnviar.setText("Enviar");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnEnviar.setEnabled(true);
                        btnEnviar.setText("Enviar");
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
        licencia.setUrlDocumento(urlDocumentoFinal);

        licencia.setIdEstado(2); // Pendiente
        licencia.setUrlDocumento(archivoSeleccionado != null ? archivoSeleccionado.toString() : "");

        DatabaseReference ref = databaseRef.child("licencias");
        String key = ref.push().getKey();

        if (key != null) {
            ref.child(key).setValue(licencia)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Solicitud enviada con éxito", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarHistorial();
                        // Reactivar botón
                        btnEnviar.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnEnviar.setEnabled(true);
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
        databaseRef.child("justificaciones").orderByChild("id_usuario").equalTo(idUsuarioActual)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaJustificaciones.clear();
                        for (DataSnapshot d : snapshot.getChildren()) {
                            Justificacion j = d.getValue(Justificacion.class);
                            if (j != null) { j.setId(d.getKey()); listaJustificaciones.add(j); }
                        }
                        Collections.reverse(listaJustificaciones);
                        actualizarLista(listaJustificaciones.isEmpty());
                        if (!listaJustificaciones.isEmpty()) {
                            justificacionesAdapter = new JustificacionesAdapter(listaJustificaciones, JustificadoresActivity.this);
                            rvHistorial.setAdapter(justificacionesAdapter);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void cargarHistorialLicencias() {
        tvHistorialTitle.setText("Historial de Licencias");
        databaseRef.child("licencias").orderByChild("id_usuario").equalTo(idUsuarioActual)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaLicencias.clear();
                        for (DataSnapshot d : snapshot.getChildren()) {
                            Licencia l = d.getValue(Licencia.class);
                            if (l != null) { l.setId(d.getKey()); listaLicencias.add(l); }
                        }
                        Collections.reverse(listaLicencias);
                        actualizarLista(listaLicencias.isEmpty());
                        if (!listaLicencias.isEmpty()) {
                            licenciasAdapter = new LicenciasAdapter(listaLicencias, JustificadoresActivity.this);
                            rvHistorial.setAdapter(licenciasAdapter);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void actualizarLista(boolean vacia) {
        rvHistorial.setVisibility(vacia ? View.GONE : View.VISIBLE);
        tvNoHistorial.setVisibility(vacia ? View.VISIBLE : View.GONE);
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