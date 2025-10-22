package com.example.relojcontrol.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.relojcontrol.R;
import com.example.relojcontrol.adapters.ArchivoAdapter;
import com.example.relojcontrol.adapters.HistorialAdapter;
import com.example.relojcontrol.models.Justificacion;
import com.example.relojcontrol.models.Licencia;
import com.example.relojcontrol.network.FirebaseRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JustificadoresActivity extends AppCompatActivity {

    private static final String TAG = "JustificadoresActivity";
    private static final String PREFS_NAME = "RelojControl";

    // Views - IDs exactos de tu XML
    private Toolbar toolbar;
    private TextInputLayout tilTipoSelector;
    private AutoCompleteTextView spinnerTipo;
    private LinearLayout layoutFormulario;

    // Campos del formulario
    private TextInputLayout tilMotivo, tilDescripcion, tilFechaJustificar;
    private TextInputLayout tilFechaInicio, tilFechaFin;
    private TextInputEditText etMotivo, etDescripcion, etFechaJustificar;
    private TextInputEditText etFechaInicio, etFechaFin;
    private MaterialButton btnAdjuntar, btnEnviar;
    private RecyclerView rvArchivosAdjuntos;

    // Historial
    private TextView tvHistorialTitle, tvNoHistorial;
    private ImageView ivRefreshHistorial;
    private RecyclerView rvHistorial;

    // Data
    private FirebaseRepository repository;
    private SharedPreferences sharedPreferences;
    private String userId, userName;
    private String tipoSeleccionado = "Justificaciones";

    // Adapters
    private ArchivoAdapter archivoAdapter;
    private HistorialAdapter historialAdapter;
    private List<Uri> archivosSeleccionados;
    private List<Object> historialList; // Object porque puede ser Justificacion o Licencia

    // File picker
    private ActivityResultLauncher<String[]> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_justificadores);

        Log.d(TAG, "=== JustificadoresActivity iniciada ===");

        initFirebase();
        initViews();
        setupToolbar();
        setupSpinner();
        setupFormulario();
        setupFilePickerLauncher();
        setupClickListeners();
        setupRecyclerViews();
        loadUserData();
        loadHistorial();
    }

    private void initFirebase() {
        repository = FirebaseRepository.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Log.d(TAG, "Firebase repository inicializado");
    }

    private void initViews() {
        // IDs exactos de tu XML
        toolbar = findViewById(R.id.toolbar);
        tilTipoSelector = findViewById(R.id.til_tipo_selector);
        spinnerTipo = findViewById(R.id.spinner_tipo);
        layoutFormulario = findViewById(R.id.layout_formulario);

        // Campos del formulario
        tilMotivo = findViewById(R.id.til_motivo);
        tilDescripcion = findViewById(R.id.til_descripcion);
        tilFechaJustificar = findViewById(R.id.til_fecha_justificar);
        tilFechaInicio = findViewById(R.id.til_fecha_inicio);
        tilFechaFin = findViewById(R.id.til_fecha_fin);

        etMotivo = findViewById(R.id.et_motivo);
        etDescripcion = findViewById(R.id.et_descripcion);
        etFechaJustificar = findViewById(R.id.et_fecha_justificar);
        etFechaInicio = findViewById(R.id.et_fecha_inicio);
        etFechaFin = findViewById(R.id.et_fecha_fin);

        btnAdjuntar = findViewById(R.id.btn_adjuntar);
        btnEnviar = findViewById(R.id.btn_enviar);
        rvArchivosAdjuntos = findViewById(R.id.rv_archivos_adjuntos);

        // Historial
        tvHistorialTitle = findViewById(R.id.tv_historial_title);
        tvNoHistorial = findViewById(R.id.tv_no_historial);
        ivRefreshHistorial = findViewById(R.id.iv_refresh_historial);
        rvHistorial = findViewById(R.id.rv_historial);

        Log.d(TAG, "Views inicializadas");
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Justificaciones y Licencias");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupSpinner() {
        String[] tipos = {"Justificaciones", "Licencias"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, tipos);
        spinnerTipo.setAdapter(adapter);
        spinnerTipo.setText("Justificaciones", false);

        spinnerTipo.setOnItemClickListener((parent, view, position, id) -> {
            tipoSeleccionado = (String) parent.getItemAtPosition(position);
            actualizarFormulario();
            actualizarTituloHistorial();
        });

        Log.d(TAG, "Spinner configurado");
    }

    private void setupFormulario() {
        // Configurar campos de fecha como no editables y clickeables
        etFechaJustificar.setOnClickListener(v -> mostrarDatePicker(etFechaJustificar));
        etFechaInicio.setOnClickListener(v -> mostrarDatePicker(etFechaInicio));
        etFechaFin.setOnClickListener(v -> mostrarDatePicker(etFechaFin));

        // Configurar formulario inicial
        actualizarFormulario();

        Log.d(TAG, "Formulario configurado");
    }

    //btnAdjuntar.setOnClickListener(v -> {
        //filePickerLauncher.launch(new String[]{"application/pdf"});
    //});

    // Y el launcher corregido:
    private void setupFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                result -> {
                    if (result != null && !result.isEmpty()) {
                        archivosSeleccionados.addAll(result); // ← Agregar todos los URIs
                        archivoAdapter.notifyDataSetChanged();

                        if (!archivosSeleccionados.isEmpty()) {
                            rvArchivosAdjuntos.setVisibility(View.VISIBLE);
                        }

                        Log.d(TAG, "Archivos seleccionados: " + archivosSeleccionados.size());
                    }
                }
        );
    }


    private void setupRecyclerViews() {
        // RecyclerView para archivos adjuntos
        archivosSeleccionados = new ArrayList<>();
        archivoAdapter = new ArchivoAdapter(archivosSeleccionados, new ArchivoAdapter.OnArchivoClickListener() {
            @Override
            public void onArchivoClick(Uri archivo) {
                // Manejar click para ver/previsualizar archivo
                Toast.makeText(JustificadoresActivity.this, "Archivo: " + archivo.getLastPathSegment(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEliminarArchivoClick(int position) {
                // Eliminar por posición
                if (position >= 0 && position < archivosSeleccionados.size()) {
                    archivosSeleccionados.remove(position);
                    archivoAdapter.notifyItemRemoved(position);
                    archivoAdapter.notifyItemRangeChanged(position, archivosSeleccionados.size());

                    if (archivosSeleccionados.isEmpty()) {
                        rvArchivosAdjuntos.setVisibility(View.GONE);
                    }
                }
            }
        });

        rvArchivosAdjuntos.setLayoutManager(new LinearLayoutManager(this));
        rvArchivosAdjuntos.setAdapter(archivoAdapter);

        // RecyclerView para historial
        historialList = new ArrayList<>();
        historialAdapter = new HistorialAdapter(historialList, new HistorialAdapter.OnHistorialClickListener() {
            @Override
            public void onHistorialClick(Object item) {
                // Manejar click en item del historial
                if (item instanceof Justificacion) {
                    Justificacion just = (Justificacion) item;
                    Toast.makeText(JustificadoresActivity.this, "Justificación: " + just.getMotivo(), Toast.LENGTH_SHORT).show();
                } else if (item instanceof Licencia) {
                    Licencia lic = (Licencia) item;
                    Toast.makeText(JustificadoresActivity.this, "Licencia del " + lic.getFechaInicio(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDescargarArchivoClick(Object item) {
                // Manejar descarga de archivo
                Toast.makeText(JustificadoresActivity.this, "Descargando archivo...", Toast.LENGTH_SHORT).show();
            }
        });

        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        rvHistorial.setAdapter(historialAdapter);

        Log.d(TAG, "RecyclerViews configurados");
    }



    private void setupClickListeners() {
        btnAdjuntar.setOnClickListener(v -> {
            filePickerLauncher.launch(new String[]{"application/pdf"});
        });

        btnEnviar.setOnClickListener(v -> {
            if (validarFormulario()) {
                if (tipoSeleccionado.equals("Justificaciones")) {
                    enviarJustificacion();
                } else {
                    enviarLicencia();
                }
            }
        });

        ivRefreshHistorial.setOnClickListener(v -> loadHistorial());

        Log.d(TAG, "Click listeners configurados");
    }

    private void loadUserData() {
        userId = sharedPreferences.getString("user_id", "");
        userName = sharedPreferences.getString("user_name", "Usuario");
        Log.d(TAG, "Datos de usuario cargados - ID: " + userId);
    }

    private void actualizarFormulario() {
        if (tipoSeleccionado.equals("Justificaciones")) {
            // Mostrar campos para justificaciones
            tilDescripcion.setVisibility(View.VISIBLE);
            tilFechaJustificar.setVisibility(View.VISIBLE);
            tilFechaInicio.setVisibility(View.GONE);
            tilFechaFin.setVisibility(View.GONE);

            etMotivo.setHint("Motivo de la justificación");
        } else {
            // Mostrar campos para licencias
            tilDescripcion.setVisibility(View.GONE);
            tilFechaJustificar.setVisibility(View.GONE);
            tilFechaInicio.setVisibility(View.VISIBLE);
            tilFechaFin.setVisibility(View.VISIBLE);

            etMotivo.setHint("Tipo de licencia");
        }

        // Limpiar campos
        etMotivo.setText("");
        etDescripcion.setText("");
        etFechaJustificar.setText("");
        etFechaInicio.setText("");
        etFechaFin.setText("");

        Log.d(TAG, "Formulario actualizado para: " + tipoSeleccionado);
    }

    private void actualizarTituloHistorial() {
        String titulo = tipoSeleccionado.equals("Justificaciones") ?
                "Historial de Justificaciones" : "Historial de Licencias";
        tvHistorialTitle.setText(titulo);

        loadHistorial(); // Recargar historial según el tipo
    }

    private void mostrarDatePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(selectedDate.getTime());
                    editText.setText(dateString);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private boolean validarFormulario() {
        boolean esValido = true;

        // Validar motivo
        if (etMotivo.getText().toString().trim().isEmpty()) {
            tilMotivo.setError("Este campo es requerido");
            esValido = false;
        } else {
            tilMotivo.setError(null);
        }

        if (tipoSeleccionado.equals("Justificaciones")) {
            // Validar descripción para justificaciones
            if (etDescripcion.getText().toString().trim().isEmpty()) {
                tilDescripcion.setError("Este campo es requerido");
                esValido = false;
            } else {
                tilDescripcion.setError(null);
            }

            // Validar fecha a justificar
            if (etFechaJustificar.getText().toString().trim().isEmpty()) {
                tilFechaJustificar.setError("Seleccione una fecha");
                esValido = false;
            } else {
                tilFechaJustificar.setError(null);
            }
        } else {
            // Validar fechas para licencias
            if (etFechaInicio.getText().toString().trim().isEmpty()) {
                tilFechaInicio.setError("Seleccione fecha de inicio");
                esValido = false;
            } else {
                tilFechaInicio.setError(null);
            }

            if (etFechaFin.getText().toString().trim().isEmpty()) {
                tilFechaFin.setError("Seleccione fecha de fin");
                esValido = false;
            } else {
                tilFechaFin.setError(null);
            }
        }

        return esValido;
    }

    private void enviarJustificacion() {
        Log.d(TAG, "Enviando justificación");

        Justificacion justificacion = new Justificacion();
        justificacion.setIdUsuario(Integer.parseInt(userId));
        justificacion.setMotivo(etMotivo.getText().toString().trim());
        justificacion.setFecha(etFechaJustificar.getText().toString().trim());
        justificacion.setEvidencia(""); // Si no hay archivo
        justificacion.setIdEstado(2); // id_estado 2 = pendiente


        // subir archivos adjuntos a Firebase Storage

        repository.crearJustificacion(justificacion, new FirebaseRepository.JustificacionCallback() {
            @Override
            public void onSuccess(Justificacion justificacion) {
                Log.d(TAG, "✓ Justificación enviada exitosamente");
                Toast.makeText(JustificadoresActivity.this,
                        "Justificación enviada correctamente", Toast.LENGTH_LONG).show();

                limpiarFormulario();
                loadHistorial();
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "✗ Error enviando justificación", error);
                Toast.makeText(JustificadoresActivity.this,
                        "Error enviando justificación: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void enviarLicencia() {
        Log.d(TAG, "Enviando licencia");

        Licencia licencia = new Licencia();
        licencia.setIdUsuario(Integer.parseInt(userId));
        licencia.setFechaInicio(etFechaInicio.getText().toString().trim());
        licencia.setFechaFin(etFechaFin.getText().toString().trim());
        licencia.setDocumento(""); // Si no hay documento
        licencia.setIdEstado(2); // id_estado 2 = pendiente

        // Subir archivos adjuntos a Firebase Storage

        repository.crearLicencia(licencia, new FirebaseRepository.LicenciaCallback() {
            @Override
            public void onSuccess(Licencia licencia) {
                Log.d(TAG, "✓ Licencia enviada exitosamente");
                Toast.makeText(JustificadoresActivity.this,
                        "Licencia enviada correctamente", Toast.LENGTH_LONG).show();

                limpiarFormulario();
                loadHistorial();
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "✗ Error enviando licencia", error);
                Toast.makeText(JustificadoresActivity.this,
                        "Error enviando licencia: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void limpiarFormulario() {
        etMotivo.setText("");
        etDescripcion.setText("");
        etFechaJustificar.setText("");
        etFechaInicio.setText("");
        etFechaFin.setText("");

        archivosSeleccionados.clear();
        archivoAdapter.notifyDataSetChanged();
        rvArchivosAdjuntos.setVisibility(View.GONE);
    }

    private void loadHistorial() {
        Log.d(TAG, "Cargando historial de " + tipoSeleccionado);

        if (tipoSeleccionado.equals("Justificaciones")) {
            loadHistorialJustificaciones();
        } else {
            loadHistorialLicencias();
        }
    }

    private void loadHistorialJustificaciones() {
        repository.obtenerJustificaciones(new FirebaseRepository.DataCallback<List<Justificacion>>() {
            @Override
            public void onSuccess(List<Justificacion> justificaciones) {
                // Filtrar solo las justificaciones del usuario actual
                List<Justificacion> misJustificaciones = new ArrayList<>();
                for (Justificacion j : justificaciones) {

                    if (userId.equals(String.valueOf(j.getIdUsuario()))) {
                    }
                }

                historialList.clear();
                historialList.addAll(misJustificaciones);

                actualizarVistaHistorial();
                Log.d(TAG, "✓ Historial de justificaciones cargado: " + misJustificaciones.size());
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "✗ Error cargando historial de justificaciones", error);
                mostrarEstadoSinHistorial();
            }
        });
    }

    private void loadHistorialLicencias() {
        repository.obtenerLicencias(new FirebaseRepository.DataCallback<List<Licencia>>() {
            @Override
            public void onSuccess(List<Licencia> licencias) {
                // Filtrar solo las licencias del usuario actual
                List<Licencia> misLicencias = new ArrayList<>();
                for (Licencia l : licencias) {

                    if (userId.equals(String.valueOf(l.getIdUsuario()))) {
                        misLicencias.add(l);
                    }
                }

                historialList.clear();
                historialList.addAll(misLicencias);

                actualizarVistaHistorial();
                Log.d(TAG, "✓ Historial de licencias cargado: " + misLicencias.size());
            }


            @Override
            public void onError(Exception error) {
                Log.e(TAG, "✗ Error cargando historial de licencias", error);
                mostrarEstadoSinHistorial();
            }
        });
    }



    private void actualizarVistaHistorial() {
        if (historialList.isEmpty()) {
            mostrarEstadoSinHistorial();
        } else {
            rvHistorial.setVisibility(View.VISIBLE);
            tvNoHistorial.setVisibility(View.GONE);
            historialAdapter.notifyDataSetChanged();
        }
    }

    private void mostrarEstadoSinHistorial() {
        rvHistorial.setVisibility(View.GONE);
        tvNoHistorial.setVisibility(View.VISIBLE);
    }
}
