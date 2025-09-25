package com.example.relojcontrol.activities;

import android.content.ClipboardManager;
import androidx.core.content.ContextCompat;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

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

    // Views de información (CORREGIDOS según XML)
    private TextView tvNombre, tvRut, tvCorreo, tvRol, tvEstado;
    private CardView cardInfo, cardEstadisticas;
    private MaterialButton btnEditar, btnResetPassword, btnEliminar;

    // Estadísticas (NUEVOS IDs según XML)
    private TextView tvAsistencias, tvAusencias, tvAtrasos, tvPorcentajeAsistencia;

    // NUEVOS COMPONENTES DEL XML
    private RecyclerView rvHistorialAsistencia, rvHistorialJustificaciones;
    private ImageView ivCopyRut, ivCopyEmail;
    private TextView tvVerHistorialCompleto, tvVerJustificacionesCompleto;
    private TextView tvNoHistorial, tvNoJustificaciones;

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
        cargarEstadisticas();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);

        // INFORMACIÓN BÁSICA (IDs CORREGIDOS)
        tvNombre = findViewById(R.id.tv_nombre_completo); // ← CORREGIDO
        tvRut = findViewById(R.id.tv_rut);
        tvCorreo = findViewById(R.id.tv_correo);
        tvRol = findViewById(R.id.tv_rol_badge); // ← NUEVO según XML
        tvEstado = findViewById(R.id.tv_estado_badge); // ← NUEVO según XML

        // Botones (IDs CORREGIDOS)
        btnEditar = findViewById(R.id.btn_editar);
        btnResetPassword = findViewById(R.id.btn_resetear_password); // ← CORREGIDO
        btnEliminar = findViewById(R.id.btn_eliminar);

        // ESTADÍSTICAS (NUEVOS según XML)
        tvAsistencias = findViewById(R.id.tv_dias_presente); // ← CORREGIDO
        tvAusencias = findViewById(R.id.tv_dias_ausente); // ← CORREGIDO
        tvAtrasos = findViewById(R.id.tv_atrasos); // ← CORREGIDO
        tvPorcentajeAsistencia = findViewById(R.id.tv_porcentaje_asistencia); // ← CORREGIDO

        // NUEVOS COMPONENTES DEL XML
        ivCopyRut = findViewById(R.id.iv_copy_rut);
        ivCopyEmail = findViewById(R.id.iv_copy_email);
        tvVerHistorialCompleto = findViewById(R.id.tv_ver_historial_completo);
        tvVerJustificacionesCompleto = findViewById(R.id.tv_ver_justificaciones_completo);

        // RecyclerViews
        rvHistorialAsistencia = findViewById(R.id.rv_historial_asistencia);
        rvHistorialJustificaciones = findViewById(R.id.rv_historial_justificaciones);

        // Mensajes de vacío
        tvNoHistorial = findViewById(R.id.tv_no_historial_asistencia);
        tvNoJustificaciones = findViewById(R.id.tv_no_justificaciones);
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
        // DATOS BÁSICOS
        tvNombre.setText(usuario.getNombreCompleto());
        tvRut.setText(usuario.getRut());
        tvCorreo.setText(usuario.getCorreo());

        // ROL Y ESTADO (actualizar badges)
        tvRol.setText(usuario.getRolTexto());
        tvEstado.setText(usuario.isActivo() ? "Activo" : "Inactivo");

        // Color según estado
        int colorEstado = usuario.isActivo() ?
                getResources().getColor(R.color.success_color) :
                getResources().getColor(R.color.error_color);
        tvEstado.setTextColor(colorEstado);

        // Color según rol
        int colorRol = usuario.getRolTexto().equals("admin") ?
                ContextCompat.getColor(this,R.color.primary_color) :
                ContextCompat.getColor(this,R.color.secondary_color);
        tvRol.setTextColor(colorRol);
    }

    private void setupClickListeners() {
        // BOTONES PRINCIPALES
        btnEditar.setOnClickListener(v -> editarUsuario());
        btnResetPassword.setOnClickListener(v -> resetPassword());
        btnEliminar.setOnClickListener(v -> eliminarUsuario());

        // FUNCIONALIDAD DE COPIAR
        ivCopyRut.setOnClickListener(v -> copiarAlPortapapeles(usuario.getRut(), "RUT"));
        ivCopyEmail.setOnClickListener(v -> copiarAlPortapapeles(usuario.getCorreo(), "correo"));

        // NAVEGACIÓN
        tvVerHistorialCompleto.setOnClickListener(v -> verAsistenciasCompletas());
        tvVerJustificacionesCompleto.setOnClickListener(v -> verJustificacionesCompletas());
    }

    private void copiarAlPortapapeles(String texto, String tipo) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(tipo, texto);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, tipo + " copiado al portapapeles", Toast.LENGTH_SHORT).show();
    }

    // MÉTODOS DE NAVEGACIÓN (para implementar después)
    private void verAsistenciasCompletas() {
        Toast.makeText(this, "Navegar a historial completo de asistencias", Toast.LENGTH_SHORT).show();
        // Intent para Activity de historial completo
    }

    private void verJustificacionesCompletas() {
        Toast.makeText(this, "Navegar a justificaciones completas", Toast.LENGTH_SHORT).show();
        // Intent para Activity de justificaciones del usuario
    }

    // MÉTODOS EXISTENTES (sin cambios)
    private void editarUsuario() {
        Intent intent = new Intent(this, AnadirUsuarioActivity.class);
        intent.putExtra("modo_edicion", true);
        intent.putExtra("usuario", usuario);
        startActivity(intent);
    }

    private void resetPassword() {
        progressBar.setVisibility(View.VISIBLE);


            Map<String, Object> params = new HashMap<>();
            params.put("id_usuario", usuario.getIdUsuario());
            params.put("nueva_contrasena", "123456"); // Password por defecto

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


    }

    private void cargarEstadisticas() {
        // Por ahora ocultar secciones sin datos
        cardEstadisticas.setVisibility(View.GONE);
        rvHistorialAsistencia.setVisibility(View.GONE);
        rvHistorialJustificaciones.setVisibility(View.GONE);

        // Mostrar mensajes de vacío
        tvNoHistorial.setVisibility(View.VISIBLE);
        tvNoJustificaciones.setVisibility(View.VISIBLE);

        // TODO: Implementar carga real de estadísticas desde API
    }

    // MENÚ MEJORADO
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.visualizar_usuario_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_editar) {
            editarUsuario();
            return true;
        } else if (id == R.id.menu_cambiar_estado) {
            cambiarEstadoUsuario();
            return true;
        } else if (id == R.id.menu_reset_password) {
            resetPassword();
            return true;
        } else if (id == R.id.menu_ver_asistencias) {
            verAsistenciasCompletas();
            return true;
        } else if (id == R.id.menu_ver_justificaciones) {
            verJustificacionesCompletas();
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


    }
}