package com.example.relojcontrol.activities;

import android.app.DownloadManager;
import androidx.core.content.ContextCompat;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.button.MaterialButton;

import com.example.relojcontrol.R;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReportesActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private MaterialButton btnGenerarPdf;
    private ProgressBar progressBar;
    private long downloadId;
    private BroadcastReceiver downloadReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        initViews();
        setupToolbar();
        setupClickListeners();
        setupDownloadReceiver();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        btnGenerarPdf = findViewById(R.id.btn_generar_pdf);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Reportes de Asistencia");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnGenerarPdf.setOnClickListener(v -> {
            generarReportePdf();
        });
    }

    private void setupDownloadReceiver() {
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long receivedDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (receivedDownloadId == downloadId) {
                    Toast.makeText(ReportesActivity.this,
                            "PDF descargado exitosamente",
                            Toast.LENGTH_SHORT).show();

                    abrirPdfDescargado();
                }
            }
        };

        // CORRECCIÓN PARA ANDROID 14+
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
// Con targetSdk 36, usar siempre ContextCompat
        ContextCompat.registerReceiver(this, downloadReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void generarReportePdf() {
        progressBar.setVisibility(View.VISIBLE);
        btnGenerarPdf.setEnabled(false);

        try {
            Map<String, Object> params = obtenerParametrosFiltros();

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    ApiEndpoints.REPORTES_GENERAR,
                    new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            progressBar.setVisibility(View.GONE);
                            btnGenerarPdf.setEnabled(true);

                            try {
                                if (response.getBoolean("success")) {
                                    String urlPdf = response.getString("url_pdf");
                                    Toast.makeText(ReportesActivity.this,
                                            "Generando PDF...",
                                            Toast.LENGTH_SHORT).show();

                                    descargarPdf(urlPdf);

                                } else {
                                    String error = response.getString("message");
                                    Toast.makeText(ReportesActivity.this,
                                            "Error: " + error,
                                            Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                Toast.makeText(ReportesActivity.this,
                                        "Error en formato de respuesta",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            progressBar.setVisibility(View.GONE);
                            btnGenerarPdf.setEnabled(true);
                            Toast.makeText(ReportesActivity.this,
                                    "Error de conexión",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            ApiClient.getInstance(this).addToRequestQueue(request);

        } catch (JSONException e) {
            progressBar.setVisibility(View.GONE);
            btnGenerarPdf.setEnabled(true);
            Toast.makeText(this, "Error en parámetros", Toast.LENGTH_SHORT).show();
        }
    }

    private Map<String, Object> obtenerParametrosFiltros() {
        Map<String, Object> params = new HashMap<>();

        String tipoReporte = obtenerTipoReporteSeleccionado();
        String fechaInicio = obtenerFechaInicio();
        String fechaFin = obtenerFechaFin();
        int idUsuario = obtenerUsuarioSeleccionado();

        params.put("tipo_reporte", tipoReporte);
        params.put("fecha_inicio", fechaInicio);
        params.put("fecha_fin", fechaFin);
        params.put("id_usuario", idUsuario);

        return params;
    }

    private void descargarPdf(String urlPdf) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String fileName = "reporte_asistencia_" + timeStamp + ".pdf";

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(urlPdf))
                    .setTitle("Reporte de Asistencia")
                    .setDescription("Descargando reporte PDF")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadId = downloadManager.enqueue(request);
                Toast.makeText(this, "Descargando PDF...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error al iniciar descarga", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error al descargar PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirPdfDescargado() {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File[] files = downloadsDir.listFiles((dir, name) -> name.startsWith("reporte_asistencia_") && name.endsWith(".pdf"));

            if (files != null && files.length > 0) {
                File latestFile = files[0];
                for (File file : files) {
                    if (file.lastModified() > latestFile.lastModified()) {
                        latestFile = file;
                    }
                }

                // Usar FileProvider para Android 7+ (API 24+)
                Uri pdfUri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    pdfUri = androidx.core.content.FileProvider.getUriForFile(
                            this,
                            getPackageName() + ".provider",
                            latestFile
                    );
                } else {
                    pdfUri = Uri.fromFile(latestFile);
                }

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(pdfUri, "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(this,
                            "Instala una app para ver PDFs",
                            Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this,
                    "PDF descargado en la carpeta Descargas",
                    Toast.LENGTH_LONG).show();
        }
    }

    // MÉTODOS AUXILIARES (implementar según filtros)
    private String obtenerTipoReporteSeleccionado() {
        return "asistencias";
    }

    private String obtenerFechaInicio() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String obtenerFechaFin() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private int obtenerUsuarioSeleccionado() {
        return 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloadReceiver != null) {
            unregisterReceiver(downloadReceiver);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_usuarios) {
            finish();
            return true;
        } else if (id == R.id.menu_justificaciones) {
            finish();
            return true;
        } else if (id == R.id.menu_reportes) {
            return true;
        } else if (id == R.id.menu_cerrar_sesion) {
            finishAffinity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}