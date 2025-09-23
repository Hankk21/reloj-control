package com.example.relojcontrol.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.relojcontrol.R;
import com.example.relojcontrol.adapters.HistorialAdapter;
import com.example.relojcontrol.adapters.ArchivoAdapter;
import com.example.relojcontrol.models.Justificacion;
import com.example.relojcontrol.models.Licencia;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JustificadoresActivity extends AppCompatActivity {

    // Constants
    private static final String TIPO_JUSTIFICACION = "Justificaciones";
    private static final String TIPO_LICENCIA = "Licencias";
    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    // Views
    private Toolbar toolbar;
    private AutoCompleteTextView spinnerTipo;
    private TextInputLayout tilTipoSelector, tilMotivo, tilDescripcion, tilFechaJustificar;
    private TextInputLayout tilFechaInicio, tilFechaFin;
    private TextInputEditText etMotivo, etDescripcion, etFechaJustificar;
    private TextInputEditText etFechaInicio, etFechaFin;
    private MaterialButton btnAdjuntar, btnEnviar;
    private RecyclerView rvArchivosAdjuntos, rvHistorial;
    private TextView tvHistorialTitle, tvNoHistorial;
    private ImageView ivRefreshHistorial;
    private LinearLayout layoutFormulario;

    // Variables
    private String tipoSeleccionado = TIPO_JUSTIFICACION;
    private Calendar fechaSeleccionada = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private List<Uri> archivosAdjuntos = new ArrayList<>();
    private List<Object> historialItems = new ArrayList<>(); // Puede ser Justificacion o Licencia

    // Adapters
    private ArchivoAdapter archivoAdapter;
    private HistorialAdapter historialAdapter;

    // Launcher para seleccionar archivos
    private ActivityResultLauncher<String> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_justificadores);

        initViews();
        setupToolbar();
        setupSpinner();
        setupRecyclerViews();
        setupDatePickers();
        setupFileSelector();
        setupClickListeners();
        setupFormValidation();

        // Cargar historial inicial
        cargarHistorial();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        // Tipo selector
        tilTipoSelector = findViewById(R.id.til_tipo_selector);
        spinnerTipo = findViewById(R.id.spinner_tipo);

        // Form fields
        layoutFormulario = findViewById(R.id.layout_formulario);
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

        // Buttons
        btnAdjuntar = findViewById(R.id.btn_adjuntar);
        btnEnviar = findViewById(R.id.btn_enviar);

        // RecyclerViews
        rvArchivosAdjuntos = findViewById(R.id.rv_archivos_adjuntos);
        rvHistorial = findViewById(R.id.rv_historial);

        // Other views
        tvHistorialTitle = findViewById(R.id.tv_historial_title);
        tvNoHistorial = findViewById(R.id.tv_no_historial);
        ivRefreshHistorial = findViewById(R.id.iv_refresh_historial);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinner() {
        String[] tipos = {TIPO_JUSTIFICACION, TIPO_LICENCIA};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                tipos
        );
        spinnerTipo.setAdapter(adapter);
        spinnerTipo.setText(TIPO_JUSTIFICACION, false);

        spinnerTipo.setOnItemClickListener((parent, view, position, id) -> {
            tipoSeleccionado = tipos[position];
            updateFormForType();
            updateHistorialTitle();
        });
    }

    private void setupRecyclerViews() {
        // RecyclerView para archivos adjuntos
        rvArchivosAdjuntos.setLayoutManager(new LinearLayoutManager(this));
        archivoAdapter = new ArchivoAdapter(archivosAdjuntos, this::removeAttachment);
        rvArchivosAdjuntos.setAdapter(archivoAdapter);

        // RecyclerView para historial
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        historialAdapter = new HistorialAdapter(historialItems);
        rvHistorial.setAdapter(historialAdapter);
    }

    private void setupDatePickers() {
        // DatePicker para fecha a justificar
        etFechaJustificar.setOnClickListener(v -> showDatePicker((date) -> {
            etFechaJustificar.setText(dateFormat.format(date));
            validateForm();
        }));

        // DatePicker para fecha de inicio
        etFechaInicio.setOnClickListener(v -> showDatePicker((date) -> {
            etFechaInicio.setText(dateFormat.format(date));
            // Limpiar fecha fin si es anterior a fecha inicio
            if (!etFechaFin.getText().toString().isEmpty()) {
                try {
                    Date fechaFin = dateFormat.parse(etFechaFin.getText().toString());
                    if (fechaFin != null && fechaFin.before(date)) {
                        etFechaFin.setText("");
                    }
                } catch (Exception e) {
                    // Ignore parsing error
                }
            }
            validateForm();
        }));

        // DatePicker para fecha de fin
        etFechaFin.setOnClickListener(v -> showDatePicker((date) -> {
            // Validar que no sea anterior a fecha inicio
            if (!etFechaInicio.getText().toString().isEmpty()) {
                try {
                    Date fechaInicio = dateFormat.parse(etFechaInicio.getText().toString());
                    if (fechaInicio != null && date.before(fechaInicio)) {
                        Toast.makeText(this, "La fecha de fin no puede ser anterior a la fecha de inicio", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (Exception e) {
                    // Ignore parsing error
                }
            }
            etFechaFin.setText(dateFormat.format(date));
            validateForm();
        }));
    }

    private void setupFileSelector() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        addAttachment(uri);
                    }
                }
        );
    }

    private void setupClickListeners() {
        btnAdjuntar.setOnClickListener(v -> filePickerLauncher.launch("application/pdf"));

        btnEnviar.setOnClickListener(v -> {
            if (validateAllFields()) {
                enviarFormulario();
            }
        });

        ivRefreshHistorial.setOnClickListener(v -> cargarHistorial());
    }

    private void setupFormValidation() {
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etMotivo.addTextChangedListener(validationWatcher);
        etDescripcion.addTextChangedListener(validationWatcher);
    }

    private void updateFormForType() {
        if (TIPO_JUSTIFICACION.equals(tipoSeleccionado)) {
            // Mostrar campos para justificaciones
            tilDescripcion.setVisibility(View.VISIBLE);
            tilFechaJustificar.setVisibility(View.VISIBLE);
            tilFechaInicio.setVisibility(View.GONE);
            tilFechaFin.setVisibility(View.GONE);

            // Limpiar campos no usados
            etFechaInicio.setText("");
            etFechaFin.setText("");
        } else {
            // Mostrar campos para licencias
            tilDescripcion.setVisibility(View.GONE);
            tilFechaJustificar.setVisibility(View.GONE);
            tilFechaInicio.setVisibility(View.VISIBLE);
            tilFechaFin.setVisibility(View.VISIBLE);

            // Limpiar campos no usados
            etDescripcion.setText("");
            etFechaJustificar.setText("");
        }

        validateForm();
    }

    private void updateHistorialTitle() {
        if (TIPO_JUSTIFICACION.equals(tipoSeleccionado)) {
            tvHistorialTitle.setText("Historial de Justificaciones");
        } else {
            tvHistorialTitle.setText("Historial de Licencias");
        }
    }

    private void showDatePicker(DateCallback callback) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    callback.onDateSelected(selectedDate.getTime());
                },
                fechaSeleccionada.get(Calendar.YEAR),
                fechaSeleccionada.get(Calendar.MONTH),
                fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void addAttachment(Uri uri) {
        // Validar tamaño del archivo (esto es una simulación, necesitarías implementar la validación real)
        archivosAdjuntos.add(uri);
        archivoAdapter.notifyItemInserted(archivosAdjuntos.size() - 1);

        if (rvArchivosAdjuntos.getVisibility() == View.GONE) {
            rvArchivosAdjuntos.setVisibility(View.VISIBLE);
        }

        validateForm();
    }

    private void removeAttachment(int position) {
        archivosAdjuntos.remove(position);
        archivoAdapter.notifyItemRemoved(position);

        if (archivosAdjuntos.isEmpty()) {
            rvArchivosAdjuntos.setVisibility(View.GONE);
        }

        validateForm();
    }

    private void validateForm() {
        boolean isValid = false;

        if (TIPO_JUSTIFICACION.equals(tipoSeleccionado)) {
            isValid = !etMotivo.getText().toString().trim().isEmpty() &&
                    !etDescripcion.getText().toString().trim().isEmpty() &&
                    !etFechaJustificar.getText().toString().trim().isEmpty();
        } else {
            isValid = !etMotivo.getText().toString().trim().isEmpty() &&
                    !etFechaInicio.getText().toString().trim().isEmpty() &&
                    !etFechaFin.getText().toString().trim().isEmpty();
        }

        btnEnviar.setEnabled(isValid);
    }

    private boolean validateAllFields() {
        boolean isValid = true;

        // Validar motivo
        if (etMotivo.getText().toString().trim().isEmpty()) {
            tilMotivo.setError("El motivo es requerido");
            isValid = false;
        } else {
            tilMotivo.setError(null);
        }

        if (TIPO_JUSTIFICACION.equals(tipoSeleccionado)) {
            // Validar campos específicos de justificación
            if (etDescripcion.getText().toString().trim().isEmpty()) {
                tilDescripcion.setError("La descripción es requerida");
                isValid = false;
            } else {
                tilDescripcion.setError(null);
            }

            if (etFechaJustificar.getText().toString().trim().isEmpty()) {
                tilFechaJustificar.setError("La fecha es requerida");
                isValid = false;
            } else {
                tilFechaJustificar.setError(null);
            }
        } else {
            // Validar campos específicos de licencia
            if (etFechaInicio.getText().toString().trim().isEmpty()) {
                tilFechaInicio.setError("La fecha de inicio es requerida");
                isValid = false;
            } else {
                tilFechaInicio.setError(null);
            }

            if (etFechaFin.getText().toString().trim().isEmpty()) {
                tilFechaFin.setError("La fecha de fin es requerida");
                isValid = false;
            } else {
                tilFechaFin.setError(null);
            }
        }

        return isValid;
    }

    private void enviarFormulario() {
        btnEnviar.setEnabled(false);
        btnEnviar.setText("Enviando...");

        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<?> call;

        if (TIPO_JUSTIFICACION.equals(tipoSeleccionado)) {
            Justificacion justificacion = new Justificacion();
            justificacion.setMotivo(etMotivo.getText().toString().trim());
            justificacion.setDescripcion(etDescripcion.getText().toString().trim());
            justificacion.setFecha(etFechaJustificar.getText().toString().trim());
            // Agregar archivos adjuntos si los hay

            call = apiService.enviarJustificacion(justificacion);
        } else {
            Licencia licencia = new Licencia();
            licencia.setMotivo(etMotivo.getText().toString().trim());
            licencia.setFechaInicio(etFechaInicio.getText().toString().trim());
            licencia.setFechaFin(etFechaFin.getText().toString().trim());
            // Agregar archivos adjuntos si los hay

            call = apiService.enviarLicencia(licencia);
        }

        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                btnEnviar.setEnabled(true);
                btnEnviar.setText("Enviar");

                if (response.isSuccessful()) {
                    Toast.makeText(JustificadoresActivity.this,
                            "Enviado exitosamente", Toast.LENGTH_SHORT).show();
                    limpiarFormulario();
                    cargarHistorial();
                } else {
                    Toast.makeText(JustificadoresActivity.this,
                            "Error al enviar: " + response.message(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                btnEnviar.setEnabled(true);
                btnEnviar.setText("Enviar");
                Toast.makeText(JustificadoresActivity.this,
                        "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void cargarHistorial() {
        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<List<Object>> call;

        if (TIPO_JUSTIFICACION.equals(tipoSeleccionado)) {
            // call = apiService.getJustificaciones();
        } else {
            // call = apiService.getLicencias();
        }

        // Simulación de carga de historial
        // En una implementación real, aquí cargarías los datos desde la API
        historialItems.clear();
        historialAdapter.notifyDataSetChanged();

        if (historialItems.isEmpty()) {
            tvNoHistorial.setVisibility(View.VISIBLE);
            rvHistorial.setVisibility(View.GONE);
        } else {
            tvNoHistorial.setVisibility(View.GONE);
            rvHistorial.setVisibility(View.VISIBLE);
        }
    }

    private void limpiarFormulario() {
        etMotivo.setText("");
        etDescripcion.setText("");
        etFechaJustificar.setText("");
        etFechaInicio.setText("");
        etFechaFin.setText("");

        // Limpiar archivos adjuntos
        archivosAdjuntos.clear();
        archivoAdapter.notifyDataSetChanged();
        rvArchivosAdjuntos.setVisibility(View.GONE);

        // Limpiar errores
        tilMotivo.setError(null);
        tilDescripcion.setError(null);
        tilFechaJustificar.setError(null);
        tilFechaInicio.setError(null);
        tilFechaFin.setError(null);

        validateForm();
    }

    // Interfaz para callback de fecha
    private interface DateCallback {
        void onDateSelected(Date date);
    }
}