package com.example.relojcontrol.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import com.example.relojcontrol.R;
import com.example.relojcontrol.adapters.HistorialAsistenciaAdapter;
import com.example.relojcontrol.adapters.HistorialJustificacionesAdapter;
import com.example.relojcontrol.models.Usuario;
import com.example.relojcontrol.models.HistorialAsistencia;
import com.example.relojcontrol.models.HistorialJustificaciones;
import com.example.relojcontrol.models.EstadisticasUsuario;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VisualizarUsuarioActivity extends AppCompatActivity {

    // Views - Header
    private Toolbar toolbar;
    private ImageView ivAvatar, ivCopyRut, ivCopyEmail;
    private TextView tvNombreCompleto, tvRolBadge, tvEstadoBadge;
    private TextView tvFechaRegistro, tvUltimoAcceso, tvRut, tvCorreo;
    private MaterialButton btnEditar, btnResetearPassword, btnEliminar;

    // Views - Estadísticas de asistencia
    private TextView tvDiasPresente, tvDiasAusente, tvAtrasos, tvPorcentajeAsistencia;
    private TextView tvVerHistorialCompleto, tvNoHistorialAsistencia;
    private RecyclerView rvHistorialAsistencia;

    // Views - Justificaciones
    private TextView tvJustificacionesPendientes, tvJustificacionesAceptadas, tvJustificacionesRechazadas;
    private TextView tvVerJustificacionesCompleto, tvNoJustificaciones;
    private RecyclerView rvHistorialJustificaciones;

    // Variables
    private Usuario usuario;
    private int usuarioId;
    private EstadisticasUsuario estadisticas;
    private List<HistorialAsistencia> listaHistorialAsistencia = new ArrayList<>();
    private List<HistorialJustificaciones> listaJustificaciones = new ArrayList<>();

    // Adapters
    private HistorialAsistenciaAdapter asistenciaAdapter;
    private HistorialJustificacionesAdapter justificacionesAdapter;

    // Format
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualizar_usuario);

        obtenerParametros();
        initViews();
        setupToolbar();
        setupRecyclerViews();
        setupClickListeners();

        cargarDatosUsuario();
    }

    private void obtenerParametros() {
        Intent intent = getIntent();
        usuarioId = intent.getIntExtra("usuario_id", -1);

        if (usuarioId == -1) {
            Toast.makeText(this, "Error: ID de usuario no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        // Header views
        ivAvatar = findViewById(R.id.iv_avatar);
        tvNombreCompleto = findViewById(R.id.tv_nombre_completo);
        tvRolBadge = findViewById(R.id.tv_rol_badge);
        tvEstadoBadge = findViewById(R.id.tv_estado_badge);
        tvFechaRegistro = findViewById(R.id.tv_fecha_registro);
        tvUltimoAcceso = findViewById(R.id.tv_ultimo_acceso);

        // Info details
        tvRut = findViewById(R.id.tv_rut);
        tvCorreo = findViewById(R.id.tv_correo);
        ivCopyRut = findViewById(R.id.iv_copy_rut);
        ivCopyEmail = findViewById(R.id.iv_copy_email);

        // Action buttons
        btnEditar = findViewById(R.id.btn_editar);
        btnResetearPassword = findViewById(R.id.btn_resetear_password);
        btnEliminar = findViewById(R.id.btn_eliminar);

        // Asistencia stats
        tvDiasPresente = findViewById(R.id.tv_dias_presente);
        tvDiasAusente = findViewById(R.id.tv_dias_ausente);
        tvAtrasos = findViewById(R.id.tv_atrasos);
        tvPorcentajeAsistencia = findViewById(R.id.tv_porcentaje_asistencia);
        tvVerHistorialCompleto = findViewById(R.id.tv_ver_historial_completo);
        tvNoHistorialAsistencia = findViewById(R.id.tv_no_historial_asistencia);
        rvHistorialAsistencia = findViewById(R.id.rv_historial_asistencia);

        // Justificaciones stats
        tvJustificacionesPendientes = findViewById(R.id.tv_justificaciones_pendientes);
        tvJustificacionesAceptadas = findViewById(R.id.tv_justificaciones_aceptadas);
        tvJustificacionesRechazadas = findViewById(R.id.tv_justificaciones_rechazadas);
        tvVerJustificacionesCompleto = findViewById(R.id.tv_ver_justificaciones_completo);
        tvNoJustificaciones = findViewById(R.id.tv_no_justificaciones);
        rvHistorialJustificaciones = findViewById(R.id.rv_historial_justificaciones);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        // RecyclerView para historial de asistencia
        asistenciaAdapter = new HistorialAsistenciaAdapter(listaHistorialAsistencia);
        rvHistorialAsistencia.setLayoutManager(new LinearLayoutManager(this));
        rvHistorialAsistencia.setAdapter(asistenciaAdapter);
        rvHistorialAsistencia.setNestedScrollingEnabled(false);

        // RecyclerView para historial de justificaciones
        justificacionesAdapter = new HistorialJustificacionesAdapter(listaJustificaciones);
        rvHistorialJustificaciones.setLayoutManager(new LinearLayoutManager(this));
        rvHistorialJustificaciones.setAdapter(justificacionesAdapter);
        rvHistorialJustificaciones.setNestedScrollingEnabled(false);
    }

    private void setupClickListeners() {
        // Copy buttons
        ivCopyRut.setOnClickListener(v -> copyToClipboard("RUT", tvRut.getText().toString()));
        ivCopyEmail.setOnClickListener(v -> copyToClipboard("Correo", tvCorreo.getText().toString()));

        // Action buttons
        btnEditar.setOnClickListener(v -> editarUsuario());
        btnResetearPassword.setOnClickListener(v -> mostrarDialogoResetPassword());
        btnEliminar.setOnClickListener(v -> mostrarDialogoEliminar());

        // Ver más links
        tvVerHistorialCompleto.setOnClickListener(v -> verHistorialCompleto());
        tvVerJustificacionesCompleto.setOnClickListener(v -> verJustificacionesCompletas());
    }

    private void cargarDatosUsuario() {
        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);

        // Cargar información básica del usuario
        Call<Usuario> callUsuario = apiService.getUsuario(usuarioId);
        callUsuario.enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                if (response.isSuccessful() && response.body() != null) {
                    usuario = response.body();
                    mostrarDatosUsuario();
                } else {
                    Toast.makeText(VisualizarUsuarioActivity.this,
                            "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                Toast.makeText(VisualizarUsuarioActivity.this,
                        "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // Cargar estadísticas
        cargarEstadisticas();

        // Cargar historial de asistencia
        cargarHistorialAsistencia();

        // Cargar historial de justificaciones
        cargarHistorialJustificaciones();
    }

    private void mostrarDatosUsuario() {
        if (usuario == null) return;

        // Nombre completo
        tvNombreCompleto.setText(usuario.getNombre() + " " + usuario.getApellido());

        // RUT y correo
        tvRut.setText(usuario.getRut());
        tvCorreo.setText(usuario.getCorreo());

        // Badge de rol
        tvRolBadge.setText(usuario.getRol());
        configurarBadgeRol();

        // Badge de estado
        String estado = usuario.isActivo() ? "Activo" : "Inactivo";
        tvEstadoBadge.setText(estado);
        configurarBadgeEstado();

        // Fechas
        if (usuario.getFechaRegistro() != null) {
            tvFechaRegistro.setText("Registrado: " + dateFormat.format(usuario.getFechaRegistro()));
        }
        if (usuario.getUltimoAcceso() != null) {
            tvUltimoAcceso.setText("Último acceso: " + dateFormat.format(usuario.getUltimoAcceso()));
        }

        // TODO: Cargar avatar del usuario si está disponible
        // Glide.with(this).load(usuario.getUrlAvatar()).into(ivAvatar);
    }

    private void configurarBadgeRol() {
        if ("Administrador".equals(usuario.getRol())) {
            tvRolBadge.setBackgroundResource(R.drawable.badge_rol_admin);
        } else {
            tvRolBadge.setBackgroundResource(R.drawable.badge_rol_empleado);
        }
    }

    private void configurarBadgeEstado() {
        if (usuario.isActivo()) {
            tvEstadoBadge.setBackgroundResource(R.drawable.badge_estado_activo);
        } else {
            tvEstadoBadge.setBackgroundResource(R.drawable.badge_estado_inactivo);
        }
    }

    private void cargarEstadisticas() {
        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<EstadisticasUsuario> call = apiService.getEstadisticasUsuario(usuarioId);

        call.enqueue(new Callback<EstadisticasUsuario>() {
            @Override
            public void onResponse(Call<EstadisticasUsuario> call, Response<EstadisticasUsuario> response) {
                if (response.isSuccessful() && response.body() != null) {
                    estadisticas = response.body();
                    mostrarEstadisticas();
                }
            }

            @Override
            public void onFailure(Call<EstadisticasUsuario> call, Throwable t) {
                // Mostrar estadísticas por defecto en caso de error
                mostrarEstadisticasDefault();
            }
        });
    }

    private void mostrarEstadisticas() {
        if (estadisticas == null) return;

        // Estadísticas de asistencia
        tvDiasPresente.setText(String.valueOf(estadisticas.getDiasPresente()));
        tvDiasAusente.setText(String.valueOf(estadisticas.getDiasAusente()));
        tvAtrasos.setText(String.valueOf(estadisticas.getAtrasos()));
        tvPorcentajeAsistencia.setText(estadisticas.getPorcentajeAsistencia() + "%");

        // Estadísticas de justificaciones
        tvJustificacionesPendientes.setText(String.valueOf(estadisticas.getJustificacionesPendientes()));
        tvJustificacionesAceptadas.setText(String.valueOf(estadisticas.getJustificacionesAceptadas()));
        tvJustificacionesRechazadas.setText(String.valueOf(estadisticas.getJustificacionesRechazadas()));
    }

    private void mostrarEstadisticasDefault() {
        tvDiasPresente.setText("--");
        tvDiasAusente.setText("--");
        tvAtrasos.setText("--");
        tvPorcentajeAsistencia.setText("--");
        tvJustificacionesPendientes.setText("--");
        tvJustificacionesAceptadas.setText("--");
        tvJustificacionesRechazadas.setText("--");
    }

    private void cargarHistorialAsistencia() {
        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<List<HistorialAsistencia>> call = apiService.getHistorialAsistenciaUsuario(usuarioId, 10); // Últimos 10 registros

        call.enqueue(new Callback<List<HistorialAsistencia>>() {
            @Override
            public void onResponse(Call<List<HistorialAsistencia>> call, Response<List<HistorialAsistencia>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listaHistorialAsistencia.clear();
                    listaHistorialAsistencia.addAll(response.body());
                    asistenciaAdapter.notifyDataSetChanged();

                    if (listaHistorialAsistencia.isEmpty()) {
                        mostrarEstadoVacioAsistencia();
                    } else {
                        rvHistorialAsistencia.setVisibility(View.VISIBLE);
                        tvNoHistorialAsistencia.setVisibility(View.GONE);
                    }
                } else {
                    mostrarEstadoVacioAsistencia();
                }
            }

            @Override
            public void onFailure(Call<List<HistorialAsistencia>> call, Throwable t) {
                mostrarEstadoVacioAsistencia();
            }
        });
    }

    private void cargarHistorialJustificaciones() {
        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<List<HistorialJustificaciones>> call = apiService.getHistorialJustificacionesUsuario(usuarioId, 5); // Últimas 5

        call.enqueue(new Callback<List<HistorialJustificaciones>>() {
            @Override
            public void onResponse(Call<List<HistorialJustificaciones>> call, Response<List<HistorialJustificaciones>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listaJustificaciones.clear();
                    listaJustificaciones.addAll(response.body());
                    justificacionesAdapter.notifyDataSetChanged();

                    if (listaJustificaciones.isEmpty()) {
                        mostrarEstadoVacioJustificaciones();
                    } else {
                        rvHistorialJustificaciones.setVisibility(View.VISIBLE);
                        tvNoJustificaciones.setVisibility(View.GONE);
                    }
                } else {
                    mostrarEstadoVacioJustificaciones();
                }
            }

            @Override
            public void onFailure(Call<List<HistorialJustificaciones>> call, Throwable t) {
                mostrarEstadoVacioJustificaciones();
            }
        });
    }

    private void mostrarEstadoVacioAsistencia() {
        rvHistorialAsistencia.setVisibility(View.GONE);
        tvNoHistorialAsistencia.setVisibility(View.VISIBLE);
    }

    private void mostrarEstadoVacioJustificaciones() {
        rvHistorialJustificaciones.setVisibility(View.GONE);
        tvNoJustificaciones.setVisibility(View.VISIBLE);
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, label + " copiado al portapapeles", Toast.LENGTH_SHORT).show();
    }

    private void editarUsuario() {
        Intent intent = new Intent(this, AnadirUsuarioActivity.class);
        intent.putExtra("modo_edicion", true);
        intent.putExtra("usuario_id", usuarioId);
        startActivity(intent);
    }

    private void verHistorialCompleto() {
        // TODO: Implementar vista de historial completo
        Toast.makeText(this, "Ver historial completo de asistencia", Toast.LENGTH_SHORT).show();
    }

    private void verJustificacionesCompletas() {
        Intent intent = new Intent(this, JustificadoresActivity.class);
        intent.putExtra("filtrar_usuario", usuarioId);
        startActivity(intent);
    }

    private void mostrarDialogoResetPassword() {
        new AlertDialog.Builder(this)
                .setTitle("Resetear Contraseña")
                .setMessage("¿Está seguro de que desea resetear la contraseña de " +
                        usuario.getNombre() + " " + usuario.getApellido() + "?\n\n" +
                        "Se generará una nueva contraseña temporal y se enviará por correo.")
                .setPositiveButton("Resetear", (dialog, which) -> resetearPassword())
                .setNegativeButton("Cancelar", null)
                .setIcon(R.drawable.ic_key)
                .show();
    }

    private void mostrarDialogoEliminar() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Usuario")
                .setMessage("¿Está seguro de que desea eliminar a " + usuario.getNombre() + " " + usuario.getApellido() + "?\n\n" +
                        "Esta acción eliminará:\n" +
                        "• Todos sus registros de asistencia\n" +
                        "• Sus justificaciones y licencias\n" +
                        "• Su acceso al sistema\n\n" +
                        "Esta acción NO se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarUsuario())
                .setNegativeButton("Cancelar", null)
                .setIcon(R.drawable.ic_warning)
                .show();
    }

    private void resetearPassword() {
        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<Void> call = apiService.resetearPasswordUsuario(usuarioId);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(VisualizarUsuarioActivity.this,
                            "Nueva contraseña enviada al correo del usuario", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(VisualizarUsuarioActivity.this,
                            "Error al resetear contraseña", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(VisualizarUsuarioActivity.this,
                        "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void eliminarUsuario() {
        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<Void> call = apiService.eliminarUsuario(usuarioId);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(VisualizarUsuarioActivity.this,
                            "Usuario eliminado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(VisualizarUsuarioActivity.this,
                            "Error al eliminar usuario", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(VisualizarUsuarioActivity.this,
                        "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.visualizar_usuario_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_toggle_status) {
            cambiarEstadoUsuario();
            return true;
        } else if (id == R.id.action_generate_report) {
            generarReporteUsuario();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void cambiarEstadoUsuario() {
        boolean nuevoEstado = !usuario.isActivo();
        String accion = nuevoEstado ? "activar" : "desactivar";
        String mensaje = "¿Está seguro de que desea " + accion + " a " +
                usuario.getNombre() + " " + usuario.getApellido() + "?";

        new AlertDialog.Builder(this)
                .setTitle("Cambiar Estado")
                .setMessage(mensaje)
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
                    Call<Usuario> call = apiService.cambiarEstadoUsuario(usuarioId, nuevoEstado);

                    call.enqueue(new Callback<Usuario>() {
                        @Override
                        public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                usuario = response.body();
                                configurarBadgeEstado();
                                String estadoTexto = nuevoEstado ? "activado" : "desactivado";
                                Toast.makeText(VisualizarUsuarioActivity.this,
                                        "Usuario " + estadoTexto + " correctamente", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(VisualizarUsuarioActivity.this,
                                        "Error al cambiar estado del usuario", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Usuario> call, Throwable t) {
                            Toast.makeText(VisualizarUsuarioActivity.this,
                                    "Error de conexión", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void generarReporteUsuario() {
        // TODO: Implementar generación de reporte individual del usuario
        Toast.makeText(this, "Generando reporte individual...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar datos cuando se regrese de edición
        if (usuario != null) {
            cargarDatosUsuario();
        }
    }
}