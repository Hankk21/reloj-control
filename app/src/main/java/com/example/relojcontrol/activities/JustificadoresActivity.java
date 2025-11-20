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
    private int idUsuarioActual;

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

        initViews();
        setupToolbar();
        setupSpinner();
        setupDatePickers();
        setupFileSelector();
        setupListeners();
        actualizarFormulario();
        cargarHistorial();
    }

    private void initViews() {
        // Spinner de tipo
        spinnerTipo = findViewById(R.id.spinner_tipo);

        // TextInputLayouts
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

        // Botones
        btnAdjuntar = findViewById(R.id.btn_adjuntar);
        btnEnviar = findViewById(R.id.btn_enviar);

        // RecyclerViews
        rvArchivosAdjuntos = findViewById(R.id.rv_archivos_adjuntos);
        rvHistorial = findViewById(R.id.rv_historial);

        // TextViews
        tvHistorialTitle = findViewById(R.id.tv_historial_title);
        tvNoHistorial = findViewById(R.id.tv_no_historial);

        // ImageView
        ivRefreshHistorial = findViewById(R.id.iv_refresh_historial);

        // Inicializar Firebase y SharedPreferences
        databaseRef = FirebaseDatabase.getInstance().getReference();
        sharedPreferences = getSharedPreferences("RelojControl", MODE_PRIVATE);
        idUsuarioActual = sharedPreferences.getInt("USER_ID", 0);

        // Inicializar listas
        listaJustificaciones = new ArrayList<>();
        listaLicencias = new ArrayList<>();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Justificaciones y Licencias");
        }
    }

    private void setupSpinner() {
        String[] tipos = {"Justificaciones", "Licencias"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, tipos);
        spinnerTipo.setAdapter(adapter);
        spinnerTipo.setText("Justificaciones", false);

        spinnerTipo.setOnItemClickListener((parent, view, position, id) -> {
            tipoSeleccionado = tipos[position];
            actualizarFormulario();
            cargarHistorial();
        });
    }

    private void setupDatePickers() {
        // DatePicker para fecha a justificar
        etFechaJustificar.setOnClickListener(v -> mostrarDatePicker(etFechaJustificar));

        // DatePicker para fecha inicio
        etFechaInicio.setOnClickListener(v -> mostrarDatePicker(etFechaInicio));

        // DatePicker para fecha fin
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
        // Botón adjuntar archivo
        btnAdjuntar.setOnClickListener(v -> seleccionarArchivo());

        // Botón enviar
        btnEnviar.setOnClickListener(v -> {
            if (tipoSeleccionado.equals("Justificaciones")) {
                enviarJustificacion();
            } else {
                enviarLicencia();
            }
        });

        // Refresh historial
        ivRefreshHistorial.setOnClickListener(v -> cargarHistorial());
    }

    private void actualizarFormulario() {
        if (tipoSeleccionado.equals("Justificaciones")) {
            // Mostrar campos de justificación
            tilDescripcion.setVisibility(View.VISIBLE);
            tilFechaJustificar.setVisibility(View.VISIBLE);

            // Ocultar campos de licencia
            tilFechaInicio.setVisibility(View.GONE);
            tilFechaFin.setVisibility(View.GONE);
        } else {
            // Ocultar campos de justificación
            tilDescripcion.setVisibility(View.GONE);
            tilFechaJustificar.setVisibility(View.GONE);

            // Mostrar campos de licencia
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
            seleccionarArchivoLauncher.launch(
                    Intent.createChooser(intent, "Seleccionar archivo PDF")
            );
        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir selector de archivos", Toast.LENGTH_SHORT).show();
        }
    }

    private String obtenerNombreArchivo(Uri uri) {
        String nombre = "archivo.pdf";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1) {
                nombre = cursor.getString(nameIndex);
            }
            cursor.close();
        }

        return nombre;
    }

    private void enviarJustificacion() {
        if (!validarFormulario()) {
            return;
        }

        // Crear objeto Justificacion
        Justificacion justificacion = new Justificacion();
        justificacion.setMotivo(etMotivo.getText().toString().trim());
        justificacion.setDescripcion(etDescripcion.getText().toString().trim());
        justificacion.setFechaJustificar(etFechaJustificar.getText().toString().trim());
        justificacion.setFechaCreacion(obtenerFechaActual());
        justificacion.setIdUsuario(idUsuarioActual);
        justificacion.setIdEstado(2); // 2 = Pendiente
        justificacion.setUrlDocumento(archivoSeleccionado != null ? archivoSeleccionado.toString() : "");

        // Guardar en Firebase
        DatabaseReference justificacionesRef = databaseRef.child("justificaciones");
        String key = justificacionesRef.push().getKey();

        if (key != null) {
            justificacionesRef.child(key).setValue(justificacion)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Justificación enviada correctamente", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarHistorial();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al enviar justificación", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error", e);
                    });
        }
    }

    private void enviarLicencia() {
        if (!validarFormulario()) {
            return;
        }

        // Crear objeto Licencia
        Licencia licencia = new Licencia();
        licencia.setMotivo(etMotivo.getText().toString().trim());
        licencia.setFechaInicio(etFechaInicio.getText().toString().trim());
        licencia.setFechaFin(etFechaFin.getText().toString().trim());
        licencia.setFechaCreacion(obtenerFechaActual());
        licencia.setIdUsuario(idUsuarioActual);
        licencia.setIdEstado(2); // 2 = Pendiente
        licencia.setUrlDocumento(archivoSeleccionado != null ? archivoSeleccionado.toString() : "");

        // Guardar en Firebase
        DatabaseReference licenciasRef = databaseRef.child("licencias");
        String key = licenciasRef.push().getKey();

        if (key != null) {
            licenciasRef.child(key).setValue(licencia)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Licencia enviada correctamente", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarHistorial();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al enviar licencia", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error", e);
                    });
        }
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

    private void cargarHistorial() {
        if (tipoSeleccionado.equals("Justificaciones")) {
            cargarHistorialJustificaciones();
        } else {
            cargarHistorialLicencias();
        }
    }

    private void cargarHistorialJustificaciones() {
        tvHistorialTitle.setText("Historial de Justificaciones");

        DatabaseReference justificacionesRef = databaseRef.child("justificaciones");

        justificacionesRef.orderByChild("id_usuario").equalTo(idUsuarioActual)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaJustificaciones.clear();

                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                Justificacion justificacion = data.getValue(Justificacion.class);
                                if (justificacion != null) {
                                    justificacion.setId(data.getKey());
                                    listaJustificaciones.add(justificacion);
                                }
                            }

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
                        Toast.makeText(JustificadoresActivity.this,
                                "Error al cargar historial", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error: " + error.getMessage());
                    }
                });
    }

    private void cargarHistorialLicencias() {
        tvHistorialTitle.setText("Historial de Licencias");

        DatabaseReference licenciasRef = databaseRef.child("licencias");

        licenciasRef.orderByChild("id_usuario").equalTo(idUsuarioActual)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaLicencias.clear();

                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                Licencia licencia = data.getValue(Licencia.class);
                                if (licencia != null) {
                                    licencia.setId(data.getKey());
                                    listaLicencias.add(licencia);
                                }
                            }

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
                        Toast.makeText(JustificadoresActivity.this,
                                "Error al cargar historial", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error: " + error.getMessage());
                    }
                });
    }

    private void mostrarHistorialJustificaciones() {
        if (justificacionesAdapter == null) {
            justificacionesAdapter = new JustificacionesAdapter(listaJustificaciones, this);
            rvHistorial.setLayoutManager(new LinearLayoutManager(this));
            rvHistorial.setAdapter(justificacionesAdapter);
        } else {
            justificacionesAdapter.notifyDataSetChanged();
        }
    }

    private void mostrarHistorialLicencias() {
        if (licenciasAdapter == null) {
            licenciasAdapter = new LicenciasAdapter(listaLicencias, this);
            rvHistorial.setLayoutManager(new LinearLayoutManager(this));
            rvHistorial.setAdapter(licenciasAdapter);
        } else {
            licenciasAdapter.notifyDataSetChanged();
        }
    }

    private String obtenerFechaActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
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

        // Resetear errores
        tilMotivo.setError(null);
        tilDescripcion.setError(null);
        tilFechaJustificar.setError(null);
        tilFechaInicio.setError(null);
        tilFechaFin.setError(null);
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
