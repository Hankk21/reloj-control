package com.example.relojcontrol.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Reporte;
import com.example.relojcontrol.models.Usuario;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportesActivity extends AppCompatActivity {

    // Constants
    private static final String TIPO_ASISTENCIA = "asistencia";
    private static final String TIPO_ATRASOS = "atrasos";
    private static final String TIPO_AUSENCIAS = "ausencias";

    // Views
    private Toolbar toolbar;
    private AutoCompleteTextView spinnerRangoFechas, spinnerUsuario;
    private TextInputLayout tilRangoFechas, tilUsuario, tilFechaDesde, tilFechaHasta;
    private TextInputEditText etFechaDesde, etFechaHasta;
    private LinearLayout layoutFechasPersonalizadas, layoutResumenDatos, layoutBotonesExportacion, layoutLoadingReporte;
    private CardView cardReporteAsistencia, cardReporteAtrasos, cardReporteAusencias, cardVistaPrevia;
    private TextView tvTituloVistaPrevia, tvLoadingMessage;
    private TextView tvStat1Value, tvStat1Label, tvStat2Value, tvStat2Label, tvStat3Value, tvStat3Label;
    private ImageView ivRefreshPreview;
    private MaterialButton btnAplicarFiltros, btnGenerarPdf, btnExportarExcel;
    private View chartContainer;
    private LinearLayout layoutChartPlaceholder;

    // Variables
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private String tipoReporteSeleccionado = "";
    private String rangoSeleccionado = "Esta semana";
    private String usuarioSeleccionado = "Todos los usuarios";
    private List<Usuario> listaUsuarios = new ArrayList<>();
    private Calendar fechaDesdeCalendar = Calendar.getInstance();
    private Calendar fechaHastaCalendar = Calendar.getInstance();

    // Opciones de rango
    private String[] rangosDisponibles = {
            "Hoy", "Ayer", "Esta semana", "Semana pasada",
            "Este mes", "Mes pasado", "Último trimestre", "Este año", "Personalizado"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        initViews();
        setupToolbar();
        setupSpinners();
        setupDatePickers();
        setupClickListeners();
        cargarUsuarios();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        // Spinners y filtros
        tilRangoFechas = findViewById(R.id.til_rango_fechas);
        tilUsuario = findViewById(R.id.til_usuario);
        tilFechaDesde = findViewById(R.id.til_fecha_desde);
        tilFechaHasta = findViewById(R.id.til_fecha_hasta);
        spinnerRangoFechas = findViewById(R.id.spinner_rango_fechas);
        spinnerUsuario = findViewById(R.id.spinner_usuario);
        etFechaDesde = findViewById(R.id.et_fecha_desde);
        etFechaHasta = findViewById(R.id.et_fecha_hasta);
        layoutFechasPersonalizadas = findViewById(R.id.layout_fechas_personalizadas);

        // Cards de reportes
        cardReporteAsistencia = findViewById(R.id.card_reporte_asistencia);
        cardReporteAtrasos = findViewById(R.id.card_reporte_atrasos);
        cardReporteAusencias = findViewById(R.id.card_reporte_ausencias);

        // Vista previa
        cardVistaPrevia = findViewById(R.id.card_vista_previa);
        tvTituloVistaPrevia = findViewById(R.id.tv_titulo_vista_previa);
        ivRefreshPreview = findViewById(R.id.iv_refresh_preview);
        chartContainer = findViewById(R.id.chart_container);
        layoutChartPlaceholder = findViewById(R.id.layout_chart_placeholder);

        // Resumen de datos
        layoutResumenDatos = findViewById(R.id.layout_resumen_datos);
        tvStat1Value = findViewById(R.id.tv_stat1_value);
        tvStat1Label = findViewById(R.id.tv_stat1_label);
        tvStat2Value = findViewById(R.id.tv_stat2_value);
        tvStat2Label = findViewById(R.id.tv_stat2_label);
        tvStat3Value = findViewById(R.id.tv_stat3_value);
        tvStat3Label = findViewById(R.id.tv_stat3_label);

        // Botones
        btnAplicarFiltros = findViewById(R.id.btn_aplicar_filtros);
        layoutBotonesExportacion = findViewById(R.id.layout_botones_exportacion);
        btnGenerarPdf = findViewById(R.id.btn_generar_pdf);
        btnExportarExcel = findViewById(R.id.btn_exportar_excel);

        // Loading
        layoutLoadingReporte = findViewById(R.id.layout_loading_reporte);
        tvLoadingMessage = findViewById(R.id.tv_loading_message);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        // Spinner de rango de fechas
        ArrayAdapter<String> rangoAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                rangosDisponibles
        );
        spinnerRangoFechas.setAdapter(rangoAdapter);
        spinnerRangoFechas.setText(rangoSeleccionado, false);

        spinnerRangoFechas.setOnItemClickListener((parent, view, position, id) -> {
            rangoSeleccionado = rangosDisponibles[position];
            if ("Personalizado".equals(rangoSeleccionado)) {
                layoutFechasPersonalizadas.setVisibility(View.VISIBLE);
            } else {
                layoutFechasPersonalizadas.setVisibility(View.GONE);
                calcularFechasAutomaticas();
            }
        });

        // Spinner de usuarios (se carga dinámicamente)
        String[] usuariosIniciales = {"Todos los usuarios"};
        ArrayAdapter<String> usuarioAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                usuariosIniciales
        );
        spinnerUsuario.setAdapter(usuarioAdapter);
        spinnerUsuario.setText(usuarioSeleccionado, false);

        spinnerUsuario.setOnItemClickListener((parent, view, position, id) -> {
            usuarioSeleccionado = (String) parent.getItemAtPosition(position);
        });
    }

    private void setupDatePickers() {
        etFechaDesde.setOnClickListener(v -> showDatePicker(true));
        etFechaHasta.setOnClickListener(v -> showDatePicker(false));
    }

    private void setupClickListeners() {
        // Filtros
        btnAplicarFiltros.setOnClickListener(v -> {
            if (validarFiltros()) {
                if (!tipoReporteSeleccionado.isEmpty()) {
                    generarVistaPrevia();
                } else {
                    Toast.makeText(this, "Seleccione un tipo de reporte", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Cards de reportes
        cardReporteAsistencia.setOnClickListener(v -> seleccionarTipoReporte(TIPO_ASISTENCIA, "Reporte de Asistencia"));
        cardReporteAtrasos.setOnClickListener(v -> seleccionarTipoReporte(TIPO_ATRASOS, "Reporte de Atrasos"));
        cardReporteAusencias.setOnClickListener(v -> seleccionarTipoReporte(TIPO_AUSENCIAS, "Reporte de Ausencias"));

        // Vista previa
        ivRefreshPreview.setOnClickListener(v -> {
            if (!tipoReporteSeleccionado.isEmpty()) {
                generarVistaPrevia();
            }
        });

        // Exportación
        btnGenerarPdf.setOnClickListener(v -> exportarReporte("PDF"));
        btnExportarExcel.setOnClickListener(v -> exportarReporte("EXCEL"));
    }

    private void showDatePicker(boolean isDesde) {
        Calendar calendar = isDesde ? fechaDesdeCalendar : fechaHastaCalendar;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    String fechaFormateada = dateFormat.format(calendar.getTime());

                    if (isDesde) {
                        etFechaDesde.setText(fechaFormateada);
                        // Si fecha desde es posterior a fecha hasta, limpiar fecha hasta
                        if (fechaHastaCalendar.before(fechaDesdeCalendar)) {
                            etFechaHasta.setText("");
                        }
                    } else {
                        // Validar que fecha hasta no sea anterior a fecha desde
                        if (!etFechaDesde.getText().toString().isEmpty() &&
                                calendar.before(fechaDesdeCalendar)) {
                            Toast.makeText(this, "La fecha hasta no puede ser anterior a la fecha desde",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        etFechaHasta.setText(fechaFormateada);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void calcularFechasAutomaticas() {
        Calendar hoy = Calendar.getInstance();
        Calendar desde = Calendar.getInstance();
        Calendar hasta = Calendar.getInstance();

        switch (rangoSeleccionado) {
            case "Hoy":
                desde.setTime(hoy.getTime());
                hasta.setTime(hoy.getTime());
                break;
            case "Ayer":
                desde.add(Calendar.DAY_OF_MONTH, -1);
                hasta.add(Calendar.DAY_OF_MONTH, -1);
                break;
            case "Esta semana":
                desde.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                hasta.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                break;
            case "Semana pasada":
                desde.add(Calendar.WEEK_OF_YEAR, -1);
                desde.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                hasta.add(Calendar.WEEK_OF_YEAR, -1);
                hasta.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                break;
            case "Este mes":
                desde.set(Calendar.DAY_OF_MONTH, 1);
                hasta.set(Calendar.DAY_OF_MONTH, hasta.getActualMaximum(Calendar.DAY_OF_MONTH));
                break;
            case "Mes pasado":
                desde.add(Calendar.MONTH, -1);
                desde.set(Calendar.DAY_OF_MONTH, 1);
                hasta.add(Calendar.MONTH, -1);
                hasta.set(Calendar.DAY_OF_MONTH, hasta.getActualMaximum(Calendar.DAY_OF_MONTH));
                break;
            case "Último trimestre":
                desde.add(Calendar.MONTH, -3);
                break;
            case "Este año":
                desde.set(Calendar.DAY_OF_YEAR, 1);
                hasta.set(Calendar.DAY_OF_YEAR, hasta.getActualMaximum(Calendar.DAY_OF_YEAR));
                break;
        }

        fechaDesdeCalendar.setTime(desde.getTime());
        fechaHastaCalendar.setTime(hasta.getTime());
    }

    private void cargarUsuarios() {
        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<List<Usuario>> call = apiService.getUsuarios();

        call.enqueue(new Callback<List<Usuario>>() {
            @Override
            public void onResponse(Call<List<Usuario>> call, Response<List<Usuario>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listaUsuarios = response.body();
                    actualizarSpinnerUsuarios();
                } else {
                    Toast.makeText(ReportesActivity.this,
                            "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Usuario>> call, Throwable t) {
                Toast.makeText(ReportesActivity.this,
                        "Error de conexión al cargar usuarios", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarSpinnerUsuarios() {
        List<String> nombresUsuarios = new ArrayList<>();
        nombresUsuarios.add("Todos los usuarios");

        for (Usuario usuario : listaUsuarios) {
            nombresUsuarios.add(usuario.getNombre() + " " + usuario.getApellido());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                nombresUsuarios
        );
        spinnerUsuario.setAdapter(adapter);
    }

    private void seleccionarTipoReporte(String tipo, String titulo) {
        tipoReporteSeleccionado = tipo;
        tvTituloVistaPrevia.setText(titulo);
        cardVistaPrevia.setVisibility(View.VISIBLE);

        // Limpiar vista previa anterior
        layoutChartPlaceholder.setVisibility(View.VISIBLE);
        chartContainer.setVisibility(View.GONE);
        layoutResumenDatos.setVisibility(View.GONE);
        layoutBotonesExportacion.setVisibility(View.GONE);

        Toast.makeText(this, "Tipo de reporte seleccionado: " + titulo, Toast.LENGTH_SHORT).show();
    }

    private boolean validarFiltros() {
        if ("Personalizado".equals(rangoSeleccionado)) {
            if (etFechaDesde.getText().toString().isEmpty()) {
                tilFechaDesde.setError("Seleccione fecha desde");
                return false;
            }
            if (etFechaHasta.getText().toString().isEmpty()) {
                tilFechaHasta.setError("Seleccione fecha hasta");
                return false;
            }
            tilFechaDesde.setError(null);
            tilFechaHasta.setError(null);
        }
        return true;
    }

    private void generarVistaPrevia() {
        mostrarLoading(true, "Generando vista previa...");

        // Crear objeto de solicitud de reporte
        ReportRequest request = new ReportRequest();
        request.setTipoReporte(tipoReporteSeleccionado);
        request.setRangoFechas(rangoSeleccionado);
        request.setUsuario(usuarioSeleccionado);

        if ("Personalizado".equals(rangoSeleccionado)) {
            request.setFechaDesde(etFechaDesde.getText().toString());
            request.setFechaHasta(etFechaHasta.getText().toString());
        } else {
            request.setFechaDesde(dateFormat.format(fechaDesdeCalendar.getTime()));
            request.setFechaHasta(dateFormat.format(fechaHastaCalendar.getTime()));
        }

        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<Reporte> call = apiService.generarReporte(request);

        call.enqueue(new Callback<Reporte>() {
            @Override
            public void onResponse(Call<Reporte> call, Response<Reporte> response) {
                mostrarLoading(false, "");

                if (response.isSuccessful() && response.body() != null) {
                    mostrarVistaPrevia(response.body());
                } else {
                    Toast.makeText(ReportesActivity.this,
                            "Error al generar reporte", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Reporte> call, Throwable t) {
                mostrarLoading(false, "");
                Toast.makeText(ReportesActivity.this,
                        "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarVistaPrevia(Reporte reporte) {
        layoutChartPlaceholder.setVisibility(View.GONE);
        chartContainer.setVisibility(View.VISIBLE);
        layoutResumenDatos.setVisibility(View.VISIBLE);
        layoutBotonesExportacion.setVisibility(View.VISIBLE);

        // Actualizar estadísticas según el tipo de reporte
        switch (tipoReporteSeleccionado) {
            case TIPO_ASISTENCIA:
                tvStat1Value.setText(reporte.getPorcentajeAsistencia() + "%");
                tvStat1Label.setText("Asistencia");
                tvStat2Value.setText(String.valueOf(reporte.getDiasPresentes()));
                tvStat2Label.setText("Días Presentes");
                tvStat3Value.setText(String.valueOf(reporte.getDiasAusentes()));
                tvStat3Label.setText("Días Ausentes");
                break;
            case TIPO_ATRASOS:
                tvStat1Value.setText(String.valueOf(reporte.getTotalAtrasos()));
                tvStat1Label.setText("Total Atrasos");
                tvStat2Value.setText(reporte.getPromedioAtraso() + " min");
                tvStat2Label.setText("Promedio");
                tvStat3Value.setText(String.valueOf(reporte.getEmpleadosConAtrasos()));
                tvStat3Label.setText("Empleados");
                break;
            case TIPO_AUSENCIAS:
                tvStat1Value.setText(String.valueOf(reporte.getAusenciasJustificadas()));
                tvStat1Label.setText("Justificadas");
                tvStat2Value.setText(String.valueOf(reporte.getAusenciasNoJustificadas()));
                tvStat2Label.setText("No Justificadas");
                tvStat3Value.setText(String.valueOf(reporte.getTotalAusencias()));
                tvStat3Label.setText("Total");
                break;
        }

        // TODO: Implementar visualización de gráficos según el tipo de reporte
        // createChart(reporte);
    }

    private void exportarReporte(String formato) {
        mostrarLoading(true, "Generando archivo " + formato + "...");

        ReportRequest request = createReportRequest();
        request.setFormato(formato);

        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<ExportResponse> call = apiService.exportarReporte(request);

        call.enqueue(new Callback<ExportResponse>() {
            @Override
            public void onResponse(Call<ExportResponse> call, Response<ExportResponse> response) {
                mostrarLoading(false, "");

                if (response.isSuccessful() && response.body() != null) {
                    descargarArchivo(response.body().getUrlDescarga(), formato);
                } else {
                    Toast.makeText(ReportesActivity.this,
                            "Error al exportar reporte", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ExportResponse> call, Throwable t) {
                mostrarLoading(false, "");
                Toast.makeText(ReportesActivity.this,
                        "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private ReportRequest createReportRequest() {
        ReportRequest request = new ReportRequest();
        request.setTipoReporte(tipoReporteSeleccionado);
        request.setRangoFechas(rangoSeleccionado);
        request.setUsuario(usuarioSeleccionado);

        if ("Personalizado".equals(rangoSeleccionado)) {
            request.setFechaDesde(etFechaDesde.getText().toString());
            request.setFechaHasta(etFechaHasta.getText().toString());
        } else {
            request.setFechaDesde(dateFormat.format(fechaDesdeCalendar.getTime()));
            request.setFechaHasta(dateFormat.format(fechaHastaCalendar.getTime()));
        }

        return request;
    }

    private void descargarArchivo(String url, String formato) {
        // Implementar descarga de archivo
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);

        Toast.makeText(this, "Descargando reporte en formato " + formato, Toast.LENGTH_LONG).show();
    }

    private void mostrarLoading(boolean mostrar, String mensaje) {
        if (mostrar) {
            layoutLoadingReporte.setVisibility(View.VISIBLE);
            tvLoadingMessage.setText(mensaje);
        } else {
            layoutLoadingReporte.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reportes_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_help) {
            // TODO: Mostrar ayuda sobre reportes
            Toast.makeText(this, "Ayuda de reportes", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Clases auxiliares para requests/responses
    private static class ReportRequest {
        private String tipoReporte;
        private String rangoFechas;
        private String usuario;
        private String fechaDesde;
        private String fechaHasta;
        private String formato;

        // Getters y setters
        public String getTipoReporte() { return tipoReporte; }
        public void setTipoReporte(String tipoReporte) { this.tipoReporte = tipoReporte; }
        public String getRangoFechas() { return rangoFechas; }
        public void setRangoFechas(String rangoFechas) { this.rangoFechas = rangoFechas; }
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public String getFechaDesde() { return fechaDesde; }
        public void setFechaDesde(String fechaDesde) { this.fechaDesde = fechaDesde; }
        public String getFechaHasta() { return fechaHasta; }
        public void setFechaHasta(String fechaHasta) { this.fechaHasta = fechaHasta; }
        public String getFormato() { return formato; }
        public void setFormato(String formato) { this.formato = formato; }
    }

    private static class ExportResponse {
        private String urlDescarga;
        private String nombreArchivo;

        public String getUrlDescarga() { return urlDescarga; }
        public void setUrlDescarga(String urlDescarga) { this.urlDescarga = urlDescarga; }
        public String getNombreArchivo() { return nombreArchivo; }
        public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    }
}