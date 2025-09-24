package com.example.relojcontrol.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.button.MaterialButton;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Usuario;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class VisualizarUsuarioActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ProgressBar progressBar;

    // Views de información
    private TextView tvNombre, tvRut, tvCorreo, tvRol, tvEstado;
    private CardView cardInfo, cardEstadisticas;
    private MaterialButton btnEditar, btnResetPassword, btnEliminar;

    // Estadísticas
    private TextView tvAsistencias, tvAusencias, tvAtrasos, tvPorcentajeAsistencia;

    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualizar_usuario);

        // Obtener usuario del intent
        usuario = (Usuario) getIntent().getSerializableExtra("usuario");
        if (usuario == null) {
            Toast.makeText(this, "Error: Usuario no disponible", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupDatosUsuario();
        setupClickListeners();
        cargarEstadisticas(); // Opcional, si tienes datos
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);

        // Información básica
        tvNombre = findViewById(R.id.tv_nombre_usuario);
        tvRut = findViewById(R.id.tv_rut);
        tvCorreo = findViewById(R.id.tv_correo);
        tvRol = findViewById(R.id.tv_rol);
        tvEstado = findViewById(R.id.tv_estado);

        // Botones
        btnEditar = findViewById(R.id.btn_editar);
        btnResetPassword = findViewById(R.id.btn_reset_password); //revisar
        btnEliminar = findViewById(R.id.btn_eliminar);

        // Estadísticas
        cardInfo = findViewById(R.id.card_info);
        cardEstadisticas = findViewById(R.id.card_estadisticas);
        tvAsistencias = findViewById(R.id.tv_asistencias);
        tvAusencias = findViewById(R.id.tv_ausencias);
        tvAtrasos = findViewById(R.id.tv_atrasos);
        tvPorcentajeAsistencia = findViewById(R.id.tv_porcentaje_asistencia);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detalles del Usuario");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupDatosUsuario() {
        tvNombre.setText(usuario.getNombreCompleto());
        tvRut.setText(usuario.getRut());
        tvCorreo.setText(usuario.getCorreo());
        tvRol.setText(usuario.getRolTexto());
        tvEstado.setText(usuario.getEstadoUsuario());

        // Color según estado
        int colorEstado = usuario.isActivo() ?
                getResources().getColor(R.color.success_color) :
                getResources().getColor(R.color.error_color);
        tvEstado.setTextColor(colorEstado);
    }

    private void setupClickListeners() {
        btnEditar.setOnClickListener(v -> editarUsuario());
        btnResetPassword.setOnClickListener(v -> resetPassword());
        btnEliminar.setOnClickListener(v -> eliminarUsuario());
    }

    private void editarUsuario() {
        Intent intent = new Intent(this, AnadirUsuarioActivity.class);
        intent.putExtra("modo_edicion", true);
        intent.putExtra("usuario", usuario);
        startActivity(intent);
    }

    private void resetPassword() {
        progressBar.setVisibility(View.VISIBLE);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("id_usuario", usuario.getIdUsuario());
            params.put("nueva_contrasena", "123456"); // Password por defecto

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    ApiEndpoints.USUARIOS_UPDATE, // Usar update para resetear password
                    new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            progressBar.setVisibility(View.GONE);
                            try {
                                if (response.getBoolean("success")) {
                                    Toast.makeText(VisualizarUsuarioActivity.this,
                                            "Contraseña reseteada exitosamente",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(VisualizarUsuarioActivity.this,
                                            response.getString("message"),
                                            Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                Toast.makeText(VisualizarUsuarioActivity.this,
                                        "Error al procesar respuesta",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(VisualizarUsuarioActivity.this,
                                    "Error de conexión",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            ApiClient.getInstance(this).addToRequestQueue(request);

        } catch (JSONException e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error al crear petición", Toast.LENGTH_SHORT).show();
        }
    }

    private void eliminarUsuario() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar Usuario")
                .setMessage("¿Está seguro de eliminar a " + usuario.getNombreCompleto() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> confirmarEliminacion())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarEliminacion() {
        progressBar.setVisibility(View.VISIBLE);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("id_usuario", usuario.getIdUsuario());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    ApiEndpoints.USUARIOS_DELETE,
                    new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            progressBar.setVisibility(View.GONE);
                            try {
                                if (response.getBoolean("success")) {
                                    Toast.makeText(VisualizarUsuarioActivity.this,
                                            "Usuario eliminado exitosamente",
                                            Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                } else {
                                    Toast.makeText(VisualizarUsuarioActivity.this,
                                            response.getString("message"),
                                            Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                Toast.makeText(VisualizarUsuarioActivity.this,
                                        "Error al procesar respuesta",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(VisualizarUsuarioActivity.this,
                                    "Error de conexión",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            ApiClient.getInstance(this).addToRequestQueue(request);

        } catch (JSONException e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error al crear petición", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarEstadisticas() {
        // Opcional: Si tienes endpoint para estadísticas del usuario
        // Por ahora mostrar datos de ejemplo o ocultar la sección
        cardEstadisticas.setVisibility(View.GONE); // Ocultar si no hay datos
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.visualizar_usuario_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_cambiar_estado) {
            cambiarEstadoUsuario();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void cambiarEstadoUsuario() {
        String nuevoEstado = usuario.isActivo() ? "inactivo" : "activo";
        String mensaje = "¿Cambiar estado a " + nuevoEstado + " para " + usuario.getNombreCompleto() + "?";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cambiar Estado")
                .setMessage(mensaje)
                .setPositiveButton("Confirmar", (dialog, which) -> actualizarEstadoUsuario(nuevoEstado))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void actualizarEstadoUsuario(String nuevoEstado) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("id_usuario", usuario.getIdUsuario());
            params.put("estado_usuario", nuevoEstado);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    ApiEndpoints.USUARIOS_UPDATE,
                    new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            progressBar.setVisibility(View.GONE);
                            try {
                                if (response.getBoolean("success")) {
                                    usuario.setEstadoUsuario(nuevoEstado);
                                    setupDatosUsuario(); // Actualizar vista
                                    Toast.makeText(VisualizarUsuarioActivity.this,
                                            "Estado actualizado exitosamente",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(VisualizarUsuarioActivity.this,
                                            response.getString("message"),
                                            Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                Toast.makeText(VisualizarUsuarioActivity.this,
                                        "Error al procesar respuesta",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(VisualizarUsuarioActivity.this,
                                    "Error de conexión",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            ApiClient.getInstance(this).addToRequestQueue(request);

        } catch (JSONException e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error al crear petición", Toast.LENGTH_SHORT).show();
        }
    }
}