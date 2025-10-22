package com.example.relojcontrol.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Usuario;
import com.example.relojcontrol.models.Asistencia;
import com.example.relojcontrol.network.FirebaseRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportesActivity extends AppCompatActivity {

    private static final String TAG = "ReportesActivity";

    // Views - IDs exactos de tu XML
    private Toolbar toolbar;

    // Filtros
    private TextInputLayout tilRangoFechas, tilUsuario;
    private AutoCompleteTextView spinnerRangoFechas, spinnerUsuario;
    private LinearLayout layoutFechasPersonalizadas;
    private TextInputLayout tilFechaDesde, tilFechaHasta;
    private TextInputEditText etFechaDesde, etFechaHasta;
    private MaterialButton btnAplicarFiltros;

    // Tipos de reportes
    private CardView cardReporteAsistencia, cardReporteAtrasos, cardReporteAusencias;

    // Vista previa
    private CardView cardVistaPrevia;
    private TextView tvTituloVistaPrevia;
    private ImageView ivRefreshPreview;
    private LinearLayout layoutChartPlaceholder, layoutResumenDatos;
    private View chartContainer;

    // Stats
    private TextView tvStat1Value, tvStat1Label;
    private TextView tvStat2Value, tvStat2Label;
    private TextView tvStat3Value, tvStat3Label;

    // Exportación
    private LinearLayout layoutBotonesExportacion;
    private MaterialButton btnGenerarPdf, btnExportarExcel;

    // Loading
    private LinearLayout layoutLoadingReporte;
    private TextView tvLoadingMessage;

    // Data
    private FirebaseRepository repository;
    private List<Usuario> usuariosList;
    private String tipoReporteSeleccionado = "";
    private String fechaDesde = "";
    private String fechaHasta = "";
    private String usuarioSeleccionado = "Todos los usuarios";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        Log.d(TAG, "=== ReportesActivity iniciada ===");

        initFirebase();
        initViews();
        setupToolbar();
        setupSpinners();
        setupDatePickers();
        setupClickListeners();
        loadUsuarios();
        configurarFechasIniciales();
    }

    private void initFirebase() {
        repository = FirebaseRepository.getInstance();
        usuariosList = new ArrayList<>();
        Log.d(TAG, "Firebase repository inicializado");
    }

    private void initViews() {
        // IDs exactos de tu XML
        toolbar = findViewById(R.id.toolbar);

        // Filtros
        tilRangoFechas = findViewById(R.id.til_rango_fechas);
        tilUsuario = findViewById(R.id.til_usuario);
        spinnerRangoFechas = findViewById(R.id.spinner_rango_fechas);
        spinnerUsuario = findViewById(R.id.spinner_usuario);
        layoutFechasPersonalizadas = findViewById(R.id.layout_fechas_personalizadas);
        tilFechaDesde = findViewById(R.id.til_fecha_desde);
        tilFechaHasta = findViewById(R.id.til_fecha_hasta);
        etFechaDesde = findViewById(R.id.et_fecha_desde);
        etFechaHasta = findViewById(R.id.et_fecha_hasta);
        btnAplicarFiltros = findViewById(R.id.btn_aplicar_filtros);

        // Tipos de reportes
        cardReporteAsistencia = findViewById(R.id.card_reporte_asistencia);
        cardReporteAtrasos = findViewById(R.id.card_reporte_atrasos);
        cardReporteAusencias = findViewById(R.id.card_reporte_ausencias);

        // Vista previa
        cardVistaPrevia = findViewById(R.id.card_vista_previa);
        tvTituloVistaPrevia = findViewById(R.id.tv_titulo_vista_previa);
        ivRefreshPreview = findViewById(R.id.iv_refresh_preview);
        layoutChartPlaceholder = findViewById(R.id.layout_chart_placeholder);
        layoutResumenDatos = findViewById(R.id.layout_resumen_datos);
        chartContainer = findViewById(R.id.chart_container);

        // Stats
        tvStat1Value = findViewById(R.id.tv_stat1_value);
        tvStat1Label = findViewById(R.id.tv_stat1_label);
        tvStat2Value = findViewById(R.id.tv_stat2_value);
        tvStat2Label = findViewById(R.id.tv_stat2_label);
        tvStat3Value = findViewById(R.id.tv_stat3_value);
        tvStat3Label = findViewById(R.id.tv_stat3_label);

        // Exportación
        layoutBotonesExportacion = findViewById(R.id.layout_botones_exportacion);
        btnGenerarPdf = findViewById(R.id.btn_generar_pdf);
        btnExportarExcel = findViewById(R.id.btn_exportar_excel);

        // Loading
        layoutLoadingReporte = findViewById(R.id.layout_loading_reporte);
        tvLoadingMessage = findViewById(R.id.tv_loading_message);

        Log.d(TAG, "Views inicializadas");
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Reportes de Asistencia");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupSpinners() {
        // Configurar spinner de rango de fechas
        String[] rangosFechas = {"Esta semana", "Este mes", "Últimos 30 días", "Personalizado"};
        ArrayAdapter<String> adapterRangos = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, rangosFechas);
        spinnerRangoFechas.setAdapter(adapterRangos);
        spinnerRangoFechas.setText("Esta semana", false);

        spinnerRangoFechas.setOnItemClickListener((parent, view, position, id) -> {
            String rangoSeleccionado = (String) parent.getItemAtPosition(position);
            manejarSeleccionRango(rangoSeleccionado);
        });

        Log.d(TAG, "Spinners configurados");
    }

    private void setupDatePickers() {
        etFechaDesde.setOnClickListener(v -> mostrarDatePicker(etFechaDesde, "Seleccionar fecha desde"));
        etFechaHasta.setOnClickListener(v -> mostrarDatePicker(etFechaHasta, "Seleccionar fecha hasta"));

        Log.d(TAG, "Date pickers configurados");
    }

    private void setupClickListeners() {
        btnAplicarFiltros.setOnClickListener(v -> aplicarFiltros());

        // Tipos de reportes
        cardReporteAsistencia.setOnClickListener(v -> seleccionarTipoReporte("asistencia"));
        cardReporteAtrasos.setOnClickListener(v -> seleccionarTipoReporte("atrasos"));
        cardReporteAusencias.setOnClickListener(v -> seleccionarTipoReporte("ausencias"));

        // Vista previa
        ivRefreshPreview.setOnClickListener(v -> actualizarVistaPrevia());

        // Exportación
        btnGenerarPdf.setOnClickListener(v -> generarPDF());
        btnExportarExcel.setOnClickListener(v -> exportarExcel());

        Log.d(TAG, "Click listeners configurados");
    }

    private void loadUsuarios() {
        Log.d(TAG, "Cargando usuarios para filtro");

        repository.obtenerUsuarios(new FirebaseRepository.DataCallback<List<Usuario>>() {
            @Override
            public void onSuccess(List<Usuario> usuarios) {
                usuariosList.clear();
                usuariosList.addAll(usuarios);

                // Configurar spinner de usuarios
                List<String> nombresUsuarios = new ArrayList<>();
                nombresUsuarios.add("Todos los usuarios");

                for (Usuario usuario : usuarios) {
                    String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
                    nombresUsuarios.add(nombreCompleto);
                }

                ArrayAdapter<String> adapterUsuarios = new ArrayAdapter<>(ReportesActivity.this,
                        android.R.layout.simple_dropdown_item_1line, nombresUsuarios);
                spinnerUsuario.setAdapter(adapterUsuarios);
                spinnerUsuario.setText("Todos los usuarios", false);

                Log.d(TAG, "✓ Usuarios cargados para filtro: " + usuarios.size());
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "✗ Error cargando usuarios", error);
                Toast.makeText(ReportesActivity.this,
                        "Error cargando usuarios", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarFechasIniciales() {
        // Configurar fechas para "Esta semana" por defecto
        Calendar calendar = Calendar.getInstance();

        // Fecha hasta = hoy
        fechaHasta = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        // Fecha desde = hace 7 días
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        fechaDesde = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        Log.d(TAG, "Fechas configuradas - Desde: " + fechaDesde + ", Hasta: " + fechaHasta);
    }

    private void manejarSeleccionRango(String rango) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        if ("Personalizado".equals(rango)) {
            layoutFechasPersonalizadas.setVisibility(View.VISIBLE);
            return;
        } else {
            layoutFechasPersonalizadas.setVisibility(View.GONE);
        }

        // Fecha hasta = hoy
        fechaHasta = dateFormat.format(calendar.getTime());

        switch (rango) {
            case "Esta semana":
                calendar.add(Calendar.DAY_OF_YEAR, -7);
                break;
            case "Este mes":
                calendar.add(Calendar.MONTH, -1);
                break;
            case "Últimos 30 días":
                calendar.add(Calendar.DAY_OF_YEAR, -30);
                break;
        }

        fechaDesde = dateFormat.format(calendar.getTime());
        Log.d(TAG, "Rango actualizado - " + rango + ": " + fechaDesde + " a " + fechaHasta);
    }

    private void mostrarDatePicker(TextInputEditText editText, String titulo) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(selectedDate.getTime());
                    editText.setText(dateString);

                    // Actualizar variables según el campo
                    if (editText == etFechaDesde) {
                        fechaDesde = dateString;
                    } else if (editText == etFechaHasta) {
                        fechaHasta = dateString;
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.setTitle(titulo);
        datePickerDialog.show();
    }

    private void aplicarFiltros() {
        Log.d(TAG, "Aplicando filtros de reporte");

        // Si es rango personalizado, usar las fechas de los campos
        if ("Personalizado".equals(spinnerRangoFechas.getText().toString())) {
            fechaDesde = etFechaDesde.getText().toString().trim();
            fechaHasta = etFechaHasta.getText().toString().trim();

            if (fechaDesde.isEmpty() || fechaHasta.isEmpty()) {
                Toast.makeText(this, "Seleccione ambas fechas", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        usuarioSeleccionado = spinnerUsuario.getText().toString();

        Toast.makeText(this, "Filtros aplicados correctamente", Toast.LENGTH_SHORT).show();

        // Si hay un tipo de reporte seleccionado, actualizar la vista previa
        if (!tipoReporteSeleccionado.isEmpty()) {
            actualizarVistaPrevia();
        }

        Log.d(TAG, "Filtros aplicados - Período: " + fechaDesde + " a " + fechaHasta +
                ", Usuario: " + usuarioSeleccionado);
    }

    private void seleccionarTipoReporte(String tipo) {
        Log.d(TAG, "Tipo de reporte seleccionado: " + tipo);

        tipoReporteSeleccionado = tipo;

        // Mostrar vista previa
        cardVistaPrevia.setVisibility(View.VISIBLE);
        layoutBotonesExportacion.setVisibility(View.VISIBLE);

        // Actualizar título según el tipo
        switch (tipo) {
            case "asistencia":
                tvTituloVistaPrevia.setText("Reporte de Asistencia");
                break;
            case "atrasos":
                tvTituloVistaPrevia.setText("Reporte de Atrasos");
                break;
            case "ausencias":
                tvTituloVistaPrevia.setText("Reporte de Ausencias");
                break;
        }

        // Generar vista previa
        actualizarVistaPrevia();
    }

    private void actualizarVistaPrevia() {
        Log.d(TAG, "Actualizando vista previa del reporte: " + tipoReporteSeleccionado);

        showLoading(true);

        // Simular carga de datos
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simular procesamiento

                runOnUiThread(() -> {
                    generarDatosMockup();
                    showLoading(false);
                    mostrarVistaPrevia();
                });

            } catch (InterruptedException e) {
                Log.e(TAG, "Error en simulación", e);
            }
        }).start();
    }

    private void generarDatosMockup() {
        // Generar datos de ejemplo según el tipo de reporte
        switch (tipoReporteSeleccionado) {
            case "asistencia":
                tvStat1Value.setText("87%");
                tvStat1Label.setText("Asistencia");
                tvStat2Value.setText("24");
                tvStat2Label.setText("Días trabajados");
                tvStat3Value.setText("3");
                tvStat3Label.setText("Días faltantes");
                break;

            case "atrasos":
                tvStat1Value.setText("12");
                tvStat1Label.setText("Total atrasos");
                tvStat2Value.setText("8 min");
                tvStat2Label.setText("Promedio");
                tvStat3Value.setText("35 min");
                tvStat3Label.setText("Mayor atraso");
                break;

            case "ausencias":
                tvStat1Value.setText("5");
                tvStat1Label.setText("Ausencias");
                tvStat2Value.setText("3");
                tvStat2Label.setText("Justificadas");
                tvStat3Value.setText("2");
                tvStat3Label.setText("Sin justificar");
                break;
        }

        Log.d(TAG, "Datos mockup generados para: " + tipoReporteSeleccionado);
    }

    private void mostrarVistaPrevia() {
        // Ocultar placeholder y mostrar contenido
        layoutChartPlaceholder.setVisibility(View.GONE);
        layoutResumenDatos.setVisibility(View.VISIBLE);
        chartContainer.setVisibility(View.VISIBLE);

        // Aquí se implementaría la lógica real para generar gráficos
        // Por ejemplo, usando una librería como MPAndroidChart

        Log.d(TAG, "Vista previa mostrada");
    }

    private void showLoading(boolean show) {
        tvLoadingMessage.setText("Generando reporte de " + tipoReporteSeleccionado + "...");
        layoutLoadingReporte.setVisibility(show ? View.VISIBLE : View.GONE);

        if (show) {
            layoutChartPlaceholder.setVisibility(View.VISIBLE);
            layoutResumenDatos.setVisibility(View.GONE);
            chartContainer.setVisibility(View.GONE);
        }
    }

    private void generarPDF() {
        Log.d(TAG, "Generando PDF del reporte: " + tipoReporteSeleccionado);

        Toast.makeText(this, "Generando PDF... (Funcionalidad en desarrollo)", Toast.LENGTH_LONG).show();

        // Implementar generación de PDF
        // Usar librerías como iText o PDFDocument
    }

    private void exportarExcel() {
        Log.d(TAG, "Exportando a Excel el reporte: " + tipoReporteSeleccionado);

        Toast.makeText(this, "Exportando a Excel... (Funcionalidad en desarrollo)", Toast.LENGTH_LONG).show();

        // Implementar exportación a Excel
        // Usar librerías como Apache POI
    }
}
