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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.relojcontrol.R;
import com.example.relojcontrol.adapters.UsuarioAdapter;
import com.example.relojcontrol.models.Usuario;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsuariosActivity extends AppCompatActivity implements UsuarioAdapter.OnUsuarioClickListener {

    // Constants
    private static final int USUARIOS_POR_PAGINA = 10;
    private static final int SEARCH_DELAY = 500; // milliseconds

    // Views
    private Toolbar toolbar;
    private TextInputLayout tilBuscar, tilFiltroRol, tilFiltroEstado;
    private TextInputEditText etBuscar;
    private AutoCompleteTextView spinnerRol, spinnerEstado;
    private MaterialButton btnAnadirUsuario, btnExportar;
    private MaterialButton btnPaginaAnterior, btnPaginaSiguiente;
    private RecyclerView rvUsuarios;
    private LinearLayout layoutNoUsuarios, layoutPaginacion;
    private TextView tvPaginaActual;

    // Variables
    private UsuarioAdapter usuarioAdapter;
    private List<Usuario> listaUsuarios = new ArrayList<>();
    private List<Usuario> listaUsuariosFiltrada = new ArrayList<>();

    // Filtros
    private String filtroTexto = "";
    private String filtroRol = "Todos";
    private String filtroEstado = "Todos";

    // Paginación
    private int paginaActual = 1;
    private int totalPaginas = 1;
    private boolean isLoading = false;

    // Handler para búsqueda con delay
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    // Opciones de filtros
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
        // Recargar usuarios al volver (por si se agregó/editó alguno)
        cargarUsuarios();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        // Search and filters
        tilBuscar = findViewById(R.id.til_buscar);
        tilFiltroRol = findViewById(R.id.til_filtro_rol);
        tilFiltroEstado = findViewById(R.id.til_filtro_estado);
        etBuscar = findViewById(R.id.et_buscar);
        spinnerRol = findViewById(R.id.spinner_rol);
        spinnerEstado = findViewById(R.id.spinner_estado);

        // Action buttons
        btnAnadirUsuario = findViewById(R.id.btn_añadir_usuario);
        btnExportar = findViewById(R.id.btn_exportar);

        // RecyclerView and related
        rvUsuarios = findViewById(R.id.rv_usuarios);
        layoutNoUsuarios = findViewById(R.id.layout_no_usuarios);

        // Pagination
        layoutPaginacion = findViewById(R.id.layout_paginacion);
        btnPaginaAnterior = findViewById(R.id.btn_pagina_anterior);
        btnPaginaSiguiente = findViewById(R.id.btn_pagina_siguiente);
        tvPaginaActual = findViewById(R.id.tv_pagina_actual);
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
        // Spinner de rol
        ArrayAdapter<String> rolAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                rolesDisponibles
        );
        spinnerRol.setAdapter(rolAdapter);
        spinnerRol.setText(filtroRol, false);

        spinnerRol.setOnItemClickListener((parent, view, position, id) -> {
            filtroRol = rolesDisponibles[position];
            aplicarFiltros();
        });

        // Spinner de estado
        ArrayAdapter<String> estadoAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                estadosDisponibles
        );
        spinnerEstado.setAdapter(estadoAdapter);
        spinnerEstado.setText(filtroEstado, false);

        spinnerEstado.setOnItemClickListener((parent, view, position, id) -> {
            filtroEstado = estadosDisponibles[position];
            aplicarFiltros();
        });
    }

    private void setupSearchListener() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtroTexto = s.toString().trim();

                // Cancelar búsqueda anterior si existe
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                // Crear nueva búsqueda con delay
                searchRunnable = () -> aplicarFiltros();
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        // Añadir usuario
        btnAnadirUsuario.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnadirUsuarioActivity.class);
            startActivity(intent);
        });

        // Exportar usuarios
        btnExportar.setOnClickListener(v -> exportarUsuarios());

        // Paginación
        btnPaginaAnterior.setOnClickListener(v -> {
            if (paginaActual > 1) {
                paginaActual--;
                actualizarVistaUsuarios();
            }
        });

        btnPaginaSiguiente.setOnClickListener(v -> {
            if (paginaActual < totalPaginas) {
                paginaActual++;
                actualizarVistaUsuarios();
            }
        });
    }

    private void cargarUsuarios() {
        if (isLoading) return;

        isLoading = true;
        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<List<Usuario>> call = apiService.getUsuarios();

        call.enqueue(new Callback<List<Usuario>>() {
            @Override
            public void onResponse(Call<List<Usuario>> call, Response<List<Usuario>> response) {
                isLoading = false;

                if (response.isSuccessful() && response.body() != null) {
                    listaUsuarios = response.body();
                    aplicarFiltros();
                } else {
                    Toast.makeText(UsuariosActivity.this,
                            "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                    mostrarEstadoVacio();
                }
            }

            @Override
            public void onFailure(Call<List<Usuario>> call, Throwable t) {
                isLoading = false;
                Toast.makeText(UsuariosActivity.this,
                        "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                mostrarEstadoVacio();
            }
        });
    }

    private void aplicarFiltros() {
        listaUsuariosFiltrada.clear();

        for (Usuario usuario : listaUsuarios) {
            boolean cumpleFiltros = true;

            // Filtro de texto (nombre, RUT, correo)
            if (!filtroTexto.isEmpty()) {
                String textoCompleto = (usuario.getNombre() + " " + usuario.getApellido() + " " +
                        usuario.getRut() + " " + usuario.getCorreo()).toLowerCase();
                if (!textoCompleto.contains(filtroTexto.toLowerCase())) {
                    cumpleFiltros = false;
                }
            }

            // Filtro de rol
            if (!"Todos".equals(filtroRol)) {
                if (!filtroRol.equals(usuario.getRol())) {
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

        // Resetear paginación
        paginaActual = 1;
        calcularPaginacion();
        actualizarVistaUsuarios();
    }

    private void calcularPaginacion() {
        int totalUsuarios = listaUsuariosFiltrada.size();
        totalPaginas = (int) Math.ceil((double) totalUsuarios / USUARIOS_POR_PAGINA);

        if (totalPaginas <= 1) {
            layoutPaginacion.setVisibility(View.GONE);
        } else {
            layoutPaginacion.setVisibility(View.VISIBLE);
            actualizarControlesPaginacion();
        }
    }

    private void actualizarVistaUsuarios() {
        if (listaUsuariosFiltrada.isEmpty()) {
            mostrarEstadoVacio();
            return;
        }

        // Calcular elementos de la página actual
        int inicioIndice = (paginaActual - 1) * USUARIOS_POR_PAGINA;
        int finIndice = Math.min(inicioIndice + USUARIOS_POR_PAGINA, listaUsuariosFiltrada.size());

        List<Usuario> usuariosPagina = listaUsuariosFiltrada.subList(inicioIndice, finIndice);

        // Actualizar adapter con usuarios de la página actual
        usuarioAdapter.actualizarUsuarios(usuariosPagina);

        // Mostrar/ocultar vistas
        layoutNoUsuarios.setVisibility(View.GONE);
        rvUsuarios.setVisibility(View.VISIBLE);

        // Actualizar controles de paginación
        actualizarControlesPaginacion();
    }

    private void actualizarControlesPaginacion() {
        if (totalPaginas <= 1) {
            layoutPaginacion.setVisibility(View.GONE);
            return;
        }

        layoutPaginacion.setVisibility(View.VISIBLE);
        tvPaginaActual.setText("Página " + paginaActual + " de " + totalPaginas);

        btnPaginaAnterior.setEnabled(paginaActual > 1);
        btnPaginaSiguiente.setEnabled(paginaActual < totalPaginas);
    }

    private void mostrarEstadoVacio() {
        layoutNoUsuarios.setVisibility(View.VISIBLE);
        rvUsuarios.setVisibility(View.GONE);
        layoutPaginacion.setVisibility(View.GONE);
    }

    private void exportarUsuarios() {
        // Preparar datos filtrados para exportar
        ExportRequest request = new ExportRequest();
        request.setUsuarios(listaUsuariosFiltrada);
        request.setFiltros(crearResumenFiltros());

        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<ExportResponse> call = apiService.exportarUsuarios(request);

        call.enqueue(new Callback<ExportResponse>() {
            @Override
            public void onResponse(Call<ExportResponse> call, Response<ExportResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(UsuariosActivity.this,
                            "Exportación iniciada. Recibirá el archivo por correo.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(UsuariosActivity.this,
                            "Error al exportar usuarios", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ExportResponse> call, Throwable t) {
                Toast.makeText(UsuariosActivity.this,
                        "Error de conexión al exportar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String crearResumenFiltros() {
        StringBuilder resumen = new StringBuilder();
        if (!filtroTexto.isEmpty()) {
            resumen.append("Búsqueda: ").append(filtroTexto).append("; ");
        }
        if (!"Todos".equals(filtroRol)) {
            resumen.append("Rol: ").append(filtroRol).append("; ");
        }
        if (!"Todos".equals(filtroEstado)) {
            resumen.append("Estado: ").append(filtroEstado).append("; ");
        }
        return resumen.length() > 0 ? resumen.toString() : "Sin filtros";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.usuarios_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            cargarUsuarios();
            return true;
        } else if (id == R.id.action_clear_filters) {
            limpiarFiltros();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void limpiarFiltros() {
        etBuscar.setText("");
        spinnerRol.setText(rolesDisponibles[0], false);
        spinnerEstado.setText(estadosDisponibles[0], false);

        filtroTexto = "";
        filtroRol = "Todos";
        filtroEstado = "Todos";

        aplicarFiltros();
    }

    // Implementación de UsuarioAdapter.OnUsuarioClickListener
    @Override
    public void onUsuarioClick(Usuario usuario) {
        Intent intent = new Intent(this, VisualizarUsuarioActivity.class);
        intent.putExtra("usuario_id", usuario.getId());
        startActivity(intent);
    }

    @Override
    public void onEditUsuario(Usuario usuario) {
        Intent intent = new Intent(this, AnadirUsuarioActivity.class);
        intent.putExtra("modo_edicion", true);
        intent.putExtra("usuario_id", usuario.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteUsuario(Usuario usuario) {
        // Mostrar diálogo de confirmación
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar Usuario")
                .setMessage("¿Está seguro de que desea eliminar a " + usuario.getNombre() + " " + usuario.getApellido() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarUsuario(usuario.getId()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onToggleUsuarioStatus(Usuario usuario) {
        String nuevoEstado = usuario.isActivo() ? "Inactivo" : "Activo";
        String mensaje = "¿Marcar como " + nuevoEstado.toLowerCase() + " a " +
                usuario.getNombre() + " " + usuario.getApellido() + "?";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cambiar Estado")
                .setMessage(mensaje)
                .setPositiveButton("Confirmar", (dialog, which) -> cambiarEstadoUsuario(usuario.getId(), !usuario.isActivo()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarUsuario(int usuarioId) {
        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<Void> call = apiService.eliminarUsuario(usuarioId);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(UsuariosActivity.this,
                            "Usuario eliminado correctamente", Toast.LENGTH_SHORT).show();
                    cargarUsuarios(); // Recargar lista
                } else {
                    Toast.makeText(UsuariosActivity.this,
                            "Error al eliminar usuario", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(UsuariosActivity.this,
                        "Error de conexión al eliminar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cambiarEstadoUsuario(int usuarioId, boolean nuevoEstado) {
        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<Usuario> call = apiService.cambiarEstadoUsuario(usuarioId, nuevoEstado);

        call.enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                if (response.isSuccessful()) {
                    String estadoTexto = nuevoEstado ? "activado" : "desactivado";
                    Toast.makeText(UsuariosActivity.this,
                            "Usuario " + estadoTexto + " correctamente", Toast.LENGTH_SHORT).show();
                    cargarUsuarios(); // Recargar lista
                } else {
                    Toast.makeText(UsuariosActivity.this,
                            "Error al cambiar estado del usuario", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                Toast.makeText(UsuariosActivity.this,
                        "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpiar handler para evitar memory leaks
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    // Clases auxiliares para exportación
    private static class ExportRequest {
        private List<Usuario> usuarios;
        private String filtros;

        public List<Usuario> getUsuarios() { return usuarios; }
        public void setUsuarios(List<Usuario> usuarios) { this.usuarios = usuarios; }
        public String getFiltros() { return filtros; }
        public void setFiltros(String filtros) { this.filtros = filtros; }
    }

    private static class ExportResponse {
        private String mensaje;
        private String archivoUrl;

        public String getMensaje() { return mensaje; }
        public void setMensaje(String mensaje) { this.mensaje = mensaje; }
        public String getArchivoUrl() { return archivoUrl; }
        public void setArchivoUrl(String archivoUrl) { this.archivoUrl = archivoUrl; }
    }
}