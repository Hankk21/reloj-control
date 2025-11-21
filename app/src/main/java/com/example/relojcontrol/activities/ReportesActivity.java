package com.example.relojcontrol.activities;

import android.app.DatePickerDialog;
import android.graphics.Color;
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

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Asistencia;
import com.example.relojcontrol.models.Usuario;
import com.example.relojcontrol.network.FirebaseRepository;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportesActivity extends AppCompatActivity {

    private static final String TAG = "ReportesActivity";
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

    // GRÁFICO REAL
    private BarChart barChart;

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

    // Mappings para saber qué usuario seleccionó el admin
    private Map<String, String> nombreToUidMap;

    private String tipoReporteSeleccionado = "";
    private String fechaDesde = "";
    private String fechaHasta = "";
    private String uidUsuarioSeleccionado = null; // Null = Todos

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
        nombreToUidMap = new HashMap<>();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
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

        cardReporteAsistencia = findViewById(R.id.card_reporte_asistencia);
        cardReporteAtrasos = findViewById(R.id.card_reporte_atrasos);
        cardReporteAusencias = findViewById(R.id.card_reporte_ausencias);

        cardVistaPrevia = findViewById(R.id.card_vista_previa);
        tvTituloVistaPrevia = findViewById(R.id.tv_titulo_vista_previa);
        ivRefreshPreview = findViewById(R.id.iv_refresh_preview);
        layoutChartPlaceholder = findViewById(R.id.layout_chart_placeholder);
        layoutResumenDatos = findViewById(R.id.layout_resumen_datos);

        // AQUI CAMBIAMOS EL VIEW GENÉRICO POR EL BARCHART
        barChart = findViewById(R.id.barChart);

        tvStat1Value = findViewById(R.id.tv_stat1_value);
        tvStat1Label = findViewById(R.id.tv_stat1_label);
        tvStat2Value = findViewById(R.id.tv_stat2_value);
        tvStat2Label = findViewById(R.id.tv_stat2_label);
        tvStat3Value = findViewById(R.id.tv_stat3_value);
        tvStat3Label = findViewById(R.id.tv_stat3_label);

        layoutBotonesExportacion = findViewById(R.id.layout_botones_exportacion);
        btnGenerarPdf = findViewById(R.id.btn_generar_pdf);
        btnExportarExcel = findViewById(R.id.btn_exportar_excel);

        layoutLoadingReporte = findViewById(R.id.layout_loading_reporte);
        tvLoadingMessage = findViewById(R.id.tv_loading_message);
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
        String[] rangosFechas = {"Esta semana", "Este mes", "Últimos 30 días", "Personalizado"};
        ArrayAdapter<String> adapterRangos = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, rangosFechas);
        spinnerRangoFechas.setAdapter(adapterRangos);
        spinnerRangoFechas.setText("Esta semana", false);

        spinnerRangoFechas.setOnItemClickListener((parent, view, position, id) -> {
            String rango = (String) parent.getItemAtPosition(position);
            manejarSeleccionRango(rango);
        });

        // Listener para usuario seleccionado
        spinnerUsuario.setOnItemClickListener((parent, view, position, id) -> {
            String seleccion = (String) parent.getItemAtPosition(position);
            if (seleccion.equals("Todos los usuarios")) {
                uidUsuarioSeleccionado = null;
            } else {
                // Buscamos el UID usando el mapa que llenaremos al cargar usuarios
                uidUsuarioSeleccionado = nombreToUidMap.get(seleccion);
            }
        });
    }

    private void setupDatePickers() {
        etFechaDesde.setOnClickListener(v -> mostrarDatePicker(etFechaDesde));
        etFechaHasta.setOnClickListener(v -> mostrarDatePicker(etFechaHasta));
    }

    private void setupClickListeners() {
        btnAplicarFiltros.setOnClickListener(v -> aplicarFiltros());

        cardReporteAsistencia.setOnClickListener(v -> seleccionarTipoReporte("asistencia"));
        cardReporteAtrasos.setOnClickListener(v -> seleccionarTipoReporte("atrasos"));
        cardReporteAusencias.setOnClickListener(v -> seleccionarTipoReporte("ausencias"));

        ivRefreshPreview.setOnClickListener(v -> actualizarVistaPrevia());

        btnGenerarPdf.setOnClickListener(v -> Toast.makeText(this, "Función PDF pendiente", Toast.LENGTH_SHORT).show());
        btnExportarExcel.setOnClickListener(v -> Toast.makeText(this, "Función Excel pendiente", Toast.LENGTH_SHORT).show());
    }

    private void loadUsuarios() {
        repository.obtenerUsuarios(new FirebaseRepository.DataCallback<List<Usuario>>() {
            @Override
            public void onSuccess(List<Usuario> usuarios) {
                usuariosList.clear();
                usuariosList.addAll(usuarios);
                nombreToUidMap.clear();

                List<String> nombres = new ArrayList<>();
                nombres.add("Todos los usuarios");

                // OJO: Aquí necesitamos saber el UID String de Firebase, no el ID int.
                // Asumo que tu modelo Usuario tiene un campo para el UID de Firebase,
                // o que lo inyectaste al cargar.
                // Si no, tendremos que buscar en userMappings.
                // Por simplicidad y para que funcione YA, usaremos el ID numérico si es lo único que tienes,
                // pero lo ideal sería mapear al UID real.

                for (Usuario u : usuarios) {
                    String nombre = u.getNombre() + " " + u.getApellido();
                    nombres.add(nombre);
                    // Guardamos mapeo Nombre -> ID numérico (String)
                    nombreToUidMap.put(nombre, String.valueOf(u.getIdUsuario()));
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(ReportesActivity.this, android.R.layout.simple_dropdown_item_1line, nombres);
                spinnerUsuario.setAdapter(adapter);
            }
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error loading users", error);
            }
        });
    }

    private void configurarFechasIniciales() {
        Calendar calendar = Calendar.getInstance();
        fechaHasta = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        fechaDesde = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
    }

    private void manejarSeleccionRango(String rango) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        if ("Personalizado".equals(rango)) {
            layoutFechasPersonalizadas.setVisibility(View.VISIBLE);
            return;
        } else {
            layoutFechasPersonalizadas.setVisibility(View.GONE);
        }

        fechaHasta = sdf.format(calendar.getTime());
        switch (rango) {
            case "Esta semana": calendar.add(Calendar.DAY_OF_YEAR, -7); break;
            case "Este mes": calendar.add(Calendar.MONTH, -1); break;
            case "Últimos 30 días": calendar.add(Calendar.DAY_OF_YEAR, -30); break;
        }
        fechaDesde = sdf.format(calendar.getTime());
    }

    private void mostrarDatePicker(TextInputEditText editText) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day);
            editText.setText(date);
            if (editText == etFechaDesde) fechaDesde = date;
            else fechaHasta = date;
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void aplicarFiltros() {
        if ("Personalizado".equals(spinnerRangoFechas.getText().toString())) {
            fechaDesde = etFechaDesde.getText().toString();
            fechaHasta = etFechaHasta.getText().toString();
            if (fechaDesde.isEmpty() || fechaHasta.isEmpty()) {
                Toast.makeText(this, "Seleccione fechas", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Toast.makeText(this, "Filtros aplicados", Toast.LENGTH_SHORT).show();
        if (!tipoReporteSeleccionado.isEmpty()) actualizarVistaPrevia();
    }

    private void seleccionarTipoReporte(String tipo) {
        tipoReporteSeleccionado = tipo;
        cardVistaPrevia.setVisibility(View.VISIBLE);
        layoutBotonesExportacion.setVisibility(View.VISIBLE);

        switch (tipo) {
            case "asistencia": tvTituloVistaPrevia.setText("Reporte de Asistencia"); break;
            case "atrasos": tvTituloVistaPrevia.setText("Reporte de Atrasos"); break;
            case "ausencias": tvTituloVistaPrevia.setText("Reporte de Ausencias"); break;
        }
        actualizarVistaPrevia();
    }

    // === EL CORAZÓN DE LOS REPORTES: LÓGICA REAL ===
    private void actualizarVistaPrevia() {
        showLoading(true);

        repository.obtenerUsuarios(new FirebaseRepository.DataCallback<List<Usuario>>() {
            @Override
            public void onSuccess(List<Usuario> usuarios) {
                // Variables para controlar el progreso de carga asíncrona
                List<Asistencia> todasLasAsistencias = new ArrayList<>();

                // Usamos un array de 2 posiciones: [0] = procesados, [1] = total
                // Usamos array final para poder accederlo desde dentro del loop
                final int[] contadores = {0, usuarios.size()};

                if (contadores[1] == 0) {
                    procesarDatosYMostrar(new ArrayList<>());
                    return;
                }

                for (Usuario u : usuarios) {
                    // Filtro de usuario único: si no es el seleccionado, lo contamos como "listo" y saltamos
                    if (uidUsuarioSeleccionado != null && !String.valueOf(u.getIdUsuario()).equals(uidUsuarioSeleccionado)) {
                        checkFinProceso(contadores, todasLasAsistencias);
                        continue;
                    }

                    // Buscar mapping y luego historial
                    repository.mDatabase.child("userMappings").child(String.valueOf(u.getIdUsuario()))
                            .get().addOnSuccessListener(snapshot -> {
                                String firebaseUid = snapshot.getValue(String.class);
                                if (firebaseUid != null) {
                                    repository.obtenerHistorialAsistencia(firebaseUid, new FirebaseRepository.DataCallback<List<Asistencia>>() {
                                        @Override
                                        public void onSuccess(List<Asistencia> asistencias) {
                                            todasLasAsistencias.addAll(asistencias);
                                            checkFinProceso(contadores, todasLasAsistencias);
                                        }
                                        @Override
                                        public void onError(Exception e) {
                                            checkFinProceso(contadores, todasLasAsistencias);
                                        }
                                    });
                                } else {
                                    checkFinProceso(contadores, todasLasAsistencias);
                                }
                            }).addOnFailureListener(e -> checkFinProceso(contadores, todasLasAsistencias));
                }
            }

            @Override
            public void onError(Exception error) {
                showLoading(false);
                Toast.makeText(ReportesActivity.this, "Error cargando usuarios", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Metodo auxiliar para contar el progreso
    private synchronized void checkFinProceso(int[] contadores, List<Asistencia> todasLasAsistencias) {
        contadores[0]++; // Incrementamos usuarios procesados

        // Si procesados == total, terminamos
        if (contadores[0] >= contadores[1]) {
            procesarDatosYMostrar(todasLasAsistencias);
        }
    }

    //Metodo para filtrar, calcular y mostrar
    private void procesarDatosYMostrar(List<Asistencia> datosBrutos) {
        // Este proceso debe correr en el hilo principal porque toca la UI
        runOnUiThread(() -> {
            try {
                List<Asistencia> filtradas = filtrarPorFecha(datosBrutos, fechaDesde, fechaHasta);
                calcularEstadisticasYGraficar(filtradas);
                showLoading(false);
                mostrarVistaPreviaUI();
            } catch (Exception e) {
                Log.e(TAG, "Error procesando datos", e);
                showLoading(false);
            }
        });
    }

    private List<Asistencia> filtrarPorFecha(List<Asistencia> input, String desde, String hasta) {
        List<Asistencia> output = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date dDesde = sdf.parse(desde);
            Date dHasta = sdf.parse(hasta);

            for (Asistencia a : input) {
                Date dAsistencia = sdf.parse(a.getFecha());
                if (dAsistencia != null && !dAsistencia.before(dDesde) && !dAsistencia.after(dHasta)) {
                    output.add(a);
                }
            }
        } catch (ParseException e) { e.printStackTrace(); }
        return output;
    }

    private void calcularEstadisticasYGraficar(List<Asistencia> datos) {
        int totalAsistencias = 0;
        int totalAtrasos = 0;
        // Map para agrupar por fecha (para el gráfico)
        Map<String, Integer> conteoPorFecha = new HashMap<>();

        for (Asistencia a : datos) {
            if (a.getIdTipoAccion() == 1) { // Solo entradas cuentan como asistencia
                totalAsistencias++;

                // Lógica de Atraso: Si hora > 09:00:00 (Ajustar según regla de negocio)
                if (a.getHora().compareTo("09:00:00") > 0) {
                    totalAtrasos++;
                }

                // Agrupar para gráfico
                String fecha = a.getFecha(); // "2023-10-20"
                conteoPorFecha.put(fecha, conteoPorFecha.getOrDefault(fecha, 0) + 1);
            }
        }

        // Actualizar Textos de Estadísticas
        tvStat1Label.setText("Total Asistencias");
        tvStat1Value.setText(String.valueOf(totalAsistencias));

        tvStat2Label.setText("Atrasos");
        tvStat2Value.setText(String.valueOf(totalAtrasos));

        tvStat3Label.setText("Puntualidad");
        int porcentaje = totalAsistencias > 0 ? ((totalAsistencias - totalAtrasos) * 100 / totalAsistencias) : 0;
        tvStat3Value.setText(porcentaje + "%");

        // Configurar Gráfico
        configurarGrafico(conteoPorFecha);
    }

    private void configurarGrafico(Map<String, Integer> datosMapa) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        int i = 0;
        // Ordenar fechas (TreeMap ordena claves automáticamente)
        Map<String, Integer> sortedMap = new java.util.TreeMap<>(datosMapa);

        for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
            entries.add(new BarEntry(i, entry.getValue()));
            labels.add(entry.getKey().substring(5)); // Mostrar solo MM-dd
            i++;
        }

        if (entries.isEmpty()) {
            barChart.clear();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Asistencias por Día");
        dataSet.setColor(getColor(R.color.primary_color));
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);

        barChart.setData(data);
        barChart.setFitBars(true);
        barChart.getDescription().setEnabled(false);

        // Configurar Eje X
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        barChart.animateY(1000);
        barChart.invalidate(); // Refrescar
    }

    private void showLoading(boolean show) {
        layoutLoadingReporte.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            layoutChartPlaceholder.setVisibility(View.GONE);
            layoutResumenDatos.setVisibility(View.GONE);
            barChart.setVisibility(View.GONE);
        }
    }

    private void mostrarVistaPreviaUI() {
        layoutChartPlaceholder.setVisibility(View.GONE);
        layoutResumenDatos.setVisibility(View.VISIBLE);
        barChart.setVisibility(View.VISIBLE);
    }
}