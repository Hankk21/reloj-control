package com.example.relojcontrol.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.relojcontrol.R;
import com.example.relojcontrol.adapters.UsuarioAdapter;
import com.example.relojcontrol.models.Usuario;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsuariosActivity extends AppCompatActivity implements UsuarioAdapter.OnUsuarioClickListener {

    private Toolbar toolbar;
    private TextInputEditText etBuscar;
    private AutoCompleteTextView spinnerRol, spinnerEstado;
    private MaterialButton btnAnadirUsuario, btnExportar;
    private RecyclerView rvUsuarios;
    private LinearLayout layoutNoUsuarios, layoutPaginacion;
    private TextView tvPaginaActual;
    private ProgressBar progressBar;

    private UsuarioAdapter usuarioAdapter;
    private List<Usuario> listaUsuarios = new ArrayList<>();
    private List<Usuario> listaUsuariosFiltrada = new ArrayList<>();

    private String filtroTexto = "";
    private String filtroRol = "Todos";
    private String filtroEstado = "Todos";

    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    private String[] rolesDisponibles = {"Todos", "Empleado", "Administrador"};
    private String[] estadosDisponibles = {"Todos", "Activo", "Inactivo"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuarios);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSpinners();
        setupSearchListener();
        setupClickListeners();

        cargarUsuarios();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarUsuarios();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etBuscar = findViewById(R.id.et_buscar);
        spinnerRol = findViewById(R.id.spinner_rol);
        spinnerEstado = findViewById(R.id.spinner_estado);
        btnAnadirUsuario = findViewById(R.id.btn_añadir_usuario);
        btnExportar = findViewById(R.id.btn_exportar);
        rvUsuarios = findViewById(R.id.rv_usuarios);
        layoutNoUsuarios = findViewById(R.id.layout_no_usuarios);
        layoutPaginacion = findViewById(R.id.layout_paginacion);
        tvPaginaActual = findViewById(R.id.tv_pagina_actual);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        usuarioAdapter = new UsuarioAdapter(listaUsuariosFiltrada, this);
        rvUsuarios.setLayoutManager(new LinearLayoutManager(this));
        rvUsuarios.setAdapter(usuarioAdapter);
    }

    private void setupSpinners() {
        ArrayAdapter<String> rolAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, rolesDisponibles);
        spinnerRol.setAdapter(rolAdapter);
        spinnerRol.setText("Todos", false);

        ArrayAdapter<String> estadoAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, estadosDisponibles);
        spinnerEstado.setAdapter(estadoAdapter);
        spinnerEstado.setText("Todos", false);

        spinnerRol.setOnItemClickListener((parent, view, position, id) -> {
            filtroRol = rolesDisponibles[position];
            aplicarFiltros();
        });

        spinnerEstado.setOnItemClickListener((parent, view, position, id) -> {
            filtroEstado = estadosDisponibles[position];
            aplicarFiltros();
        });
    }

    private void setupSearchListener() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filtroTexto = s.toString().trim();

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> aplicarFiltros();
                searchHandler.postDelayed(searchRunnable, 500);
            }
        });
    }

    private void setupClickListeners() {
        btnAnadirUsuario.setOnClickListener(v -> {
            startActivity(new Intent(this, AnadirUsuarioActivity.class));
        });

        btnExportar.setOnClickListener(v -> {
            Toast.makeText(this, "Exportar funcionalidad en desarrollo", Toast.LENGTH_SHORT).show();
        });
    }

    private void cargarUsuarios() {
        progressBar.setVisibility(View.VISIBLE);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                ApiEndpoints.USUARIOS_LIST,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressBar.setVisibility(View.GONE);
                        try {
                            if (response.getBoolean("success")) {
                                JSONArray usuariosArray = response.getJSONObject("data").getJSONArray("usuarios");
                                listaUsuarios = parseUsuariosFromJson(usuariosArray);
                                aplicarFiltros();
                            } else {
                                mostrarError("Error al cargar usuarios");
                            }
                        } catch (JSONException e) {
                            mostrarError("Error en formato de respuesta");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        mostrarError("Error de conexión");
                    }
                }
        );

        ApiClient.getInstance(this).addToRequestQueue(request);
    }

    private List<Usuario> parseUsuariosFromJson(JSONArray usuariosArray) throws JSONException {
        List<Usuario> usuarios = new ArrayList<>();
        for (int i = 0; i < usuariosArray.length(); i++) {
            JSONObject usuarioJson = usuariosArray.getJSONObject(i);
            Usuario usuario = new Usuario();
            usuario.setIdUsuario(usuarioJson.getInt("id_usuario"));
            usuario.setRut(usuarioJson.getString("rut"));
            usuario.setNombre(usuarioJson.getString("nombre"));
            usuario.setApellido(usuarioJson.getString("apellido"));
            usuario.setCorreo(usuarioJson.getString("correo"));
            usuario.setEstadoUsuario(usuarioJson.getString("estado_usuario"));
            usuario.setIdRol(usuarioJson.getInt("id_rol"));
            usuarios.add(usuario);
        }
        return usuarios;
    }

    private void aplicarFiltros() {
        listaUsuariosFiltrada.clear();

        for (Usuario usuario : listaUsuarios) {
            boolean cumpleFiltros = true;

            // Filtro de texto
            if (!filtroTexto.isEmpty()) {
                String textoBusqueda = (usuario.getNombre() + " " + usuario.getApellido() + " " +
                        usuario.getRut() + " " + usuario.getCorreo()).toLowerCase();
                if (!textoBusqueda.contains(filtroTexto.toLowerCase())) {
                    cumpleFiltros = false;
                }
            }

            // Filtro de rol
            if (!"Todos".equals(filtroRol)) {
                String rolUsuario = usuario.getRolTexto();
                if (!filtroRol.equals(rolUsuario)) {
                    cumpleFiltros = false;
                }
            }

            // Filtro de estado
            if (!"Todos".equals(filtroEstado)) {
                String estadoUsuario = usuario.isActivo() ? "Activo" : "Inactivo";
                if (!filtroEstado.equals(estadoUsuario)) {
                    cumpleFiltros = false;
                }
            }

            if (cumpleFiltros) {
                listaUsuariosFiltrada.add(usuario);
            }
        }

        actualizarVistaUsuarios();
    }

    private void actualizarVistaUsuarios() {
        if (listaUsuariosFiltrada.isEmpty()) {
            layoutNoUsuarios.setVisibility(View.VISIBLE);
            rvUsuarios.setVisibility(View.GONE);
            layoutPaginacion.setVisibility(View.GONE);
        } else {
            layoutNoUsuarios.setVisibility(View.GONE);
            rvUsuarios.setVisibility(View.VISIBLE);
            layoutPaginacion.setVisibility(View.GONE); // Por ahora sin paginación

            usuarioAdapter.updateData(listaUsuariosFiltrada);
        }
    }

    private void mostrarError(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
        layoutNoUsuarios.setVisibility(View.VISIBLE);
        rvUsuarios.setVisibility(View.GONE);
    }

    // Implementación de UsuarioAdapter.OnUsuarioClickListener
    @Override
    public void onUsuarioClick(Usuario usuario) {
        Intent intent = new Intent(this, VisualizarUsuarioActivity.class);
        intent.putExtra("usuario", usuario);
        startActivity(intent);
    }

    @Override
    public void onEditUsuario(Usuario usuario) {
        Intent intent = new Intent(this, AnadirUsuarioActivity.class);
        intent.putExtra("modo_edicion", true);
        intent.putExtra("usuario", usuario);
        startActivity(intent);
    }

    @Override
    public void onDeleteUsuario(Usuario usuario) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar Usuario")
                .setMessage("¿Está seguro de que desea eliminar a " + usuario.getNombreCompleto() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarUsuario(usuario))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onToggleUsuarioStatus(Usuario usuario) {
        String nuevoEstado = usuario.isActivo() ? "Inactivo" : "Activo";
        String mensaje = "¿Cambiar estado a " + nuevoEstado.toLowerCase() + " para " +
                usuario.getNombreCompleto() + "?";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cambiar Estado")
                .setMessage(mensaje)
                .setPositiveButton("Confirmar", (dialog, which) -> cambiarEstadoUsuario(usuario))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarUsuario(Usuario usuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("id_usuario", usuario.getIdUsuario());

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiEndpoints.USUARIOS_DELETE,
                new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("success")) {
                                Toast.makeText(UsuariosActivity.this,
                                        "Usuario eliminado correctamente", Toast.LENGTH_SHORT).show();
                                cargarUsuarios();
                            } else {
                                Toast.makeText(UsuariosActivity.this,
                                        response.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(UsuariosActivity.this,
                                    "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(UsuariosActivity.this,
                                "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        ApiClient.getInstance(this).addToRequestQueue(request);
    }

    private void cambiarEstadoUsuario(Usuario usuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("id_usuario", usuario.getIdUsuario());
        params.put("estado_usuario", usuario.isActivo() ? "inactivo" : "activo");

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiEndpoints.USUARIOS_UPDATE,
                new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("success")) {
                                String estado = usuario.isActivo() ? "desactivado" : "activado";
                                Toast.makeText(UsuariosActivity.this,
                                        "Usuario " + estado + " correctamente", Toast.LENGTH_SHORT).show();
                                cargarUsuarios();
                            } else {
                                Toast.makeText(UsuariosActivity.this,
                                        response.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(UsuariosActivity.this,
                                    "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(UsuariosActivity.this,
                                "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        ApiClient.getInstance(this).addToRequestQueue(request);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.usuarios_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            cargarUsuarios();
            return true;
        } else if (item.getItemId() == R.id.action_clear_filters) {
            limpiarFiltros();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void limpiarFiltros() {
        etBuscar.setText("");
        spinnerRol.setText("Todos", false);
        spinnerEstado.setText("Todos", false);
        filtroTexto = "";
        filtroRol = "Todos";
        filtroEstado = "Todos";
        aplicarFiltros();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}