package com.example.relojcontrol.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.example.relojcontrol.R;
import com.example.relojcontrol.adapters.JustificacionesAdapter;
import com.example.relojcontrol.models.Justificacion;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JustificadoresActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FloatingActionButton fabAgregar;
    private JustificacionesAdapter adapter;
    private List<Justificacion> justificacionesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_justificadores);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        cargarJustificaciones();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.rv_historial);
        progressBar = findViewById(R.id.progressBar);

        justificacionesList = new ArrayList<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Justificaciones");
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new JustificacionesAdapter(justificacionesList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        fabAgregar.setOnClickListener(v -> {
            Toast.makeText(this, "Agregar justificación - En desarrollo", Toast.LENGTH_SHORT).show();
        });
    }

    private void cargarJustificaciones() {
        progressBar.setVisibility(View.VISIBLE);

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                ApiEndpoints.JUSTIFICACIONES_LIST,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        progressBar.setVisibility(View.GONE);
                        try {
                            justificacionesList.clear();
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);
                                Justificacion justificacion = parseJustificacionFromJson(jsonObject);
                                justificacionesList.add(justificacion);
                            }
                            adapter.notifyDataSetChanged();

                            if (justificacionesList.isEmpty()) {
                                Toast.makeText(JustificadoresActivity.this,
                                        "No hay justificaciones",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(JustificadoresActivity.this,
                                    "Error al procesar datos",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(JustificadoresActivity.this,
                                "Error de conexión",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );

        ApiClient.getInstance(this).addToRequestQueue(request);
    }

    private Justificacion parseJustificacionFromJson(JSONObject jsonObject) throws JSONException {
        Justificacion justificacion = new Justificacion();

        justificacion.setIdJustificacion(jsonObject.getInt("id_justificacion"));
        justificacion.setIdUsuario(jsonObject.getInt("id_usuario"));
        justificacion.setFecha(jsonObject.getString("fecha"));
        justificacion.setMotivo(jsonObject.getString("motivo"));
        justificacion.setEvidencia(jsonObject.optString("evidencia", ""));
        justificacion.setIdEstado(jsonObject.getInt("id_estado"));

        // Campos opcionales del JOIN
        if (jsonObject.has("nombre_usuario")) {
            justificacion.setNombreUsuario(jsonObject.getString("nombre_usuario"));
        }
        if (jsonObject.has("rut_usuario")) {
            justificacion.setRutUsuario(jsonObject.getString("rut_usuario"));
        }

        return justificacion;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reportes_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_usuarios) {
            finish();
            return true;
        } else if (id == R.id.menu_justificaciones) {
            return true;
        } else if (id == R.id.menu_reportes) {
            finish();
            return true;
        } else if (id == R.id.menu_cerrar_sesion) {
            finishAffinity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarJustificaciones();
    }
}