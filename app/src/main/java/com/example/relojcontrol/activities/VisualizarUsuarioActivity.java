package com.example.relojcontrol.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import com.example.relojcontrol.R;
import com.example.relojcontrol.adapters.JustificacionesAdapter;
import com.example.relojcontrol.models.Usuario;
import com.example.relojcontrol.models.Asistencia;
import com.example.relojcontrol.models.Justificacion;
import com.example.relojcontrol.network.FirebaseRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.auth.FirebaseAuth;


public class VisualizarUsuarioActivity extends AppCompatActivity {

    private static final String TAG = "VisualizarUsuarioActivity";

    // Views - IDs exactos de tu XML
    private Toolbar toolbar;

    // Información básica
    private ImageView ivAvatar, ivCopyRut, ivCopyEmail;
    private TextView tvNombreCompleto, tvRolBadge, tvEstadoBadge;
    private TextView tvFechaRegistro, tvUltimoAcceso;
    private TextView tvRut, tvCorreo;

    // Botones de acción
    private MaterialButton btnEditar, btnResetearPassword, btnEliminar;

    // Historial de asistencia
    private TextView tvVerHistorialCompleto;
    private TextView tvDiasPresente, tvDiasAusente, tvAtrasos, tvPorcentajeAsistencia;
    private RecyclerView rvHistorialAsistencia;
    private TextView tvNoHistorialAsistencia;

    // Justificaciones y licencias
    private TextView tvVerJustificacionesCompleto;
    private TextView tvJustificacionesPendientes, tvJustificacionesAceptadas, tvJustificacionesRechazadas;
    private RecyclerView rvHistorialJustificaciones;
    private TextView tvNoJustificaciones;

    // Data
    private FirebaseRepository repository;
    private String usuarioId;
    private Usuario usuario;
    private JustificacionesAdapter justificacionesAdapter;  // Para justificaciones

    private List<Asistencia> asistenciasList;
    private List<Justificacion> justificacionesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualizar_usuario);

        Log.d(TAG, "=== VisualizarUsuarioActivity iniciada ===");

        initFirebase();
        initViews();
        setupToolbar();
        setupRecyclerViews();
        setupClickListeners();

        // Obtener ID del usuario desde Intent
        usuarioId = getIntent().getStringExtra("usuario_id");
        if (usuarioId != null && !usuarioId.isEmpty()) {
            loadUsuarioData();
        } else {
            Log.e(TAG, "No se recibió usuario_id");
            finish();
        }
    }

    private void initFirebase() {
        repository = FirebaseRepository.getInstance();
        asistenciasList = new ArrayList<>();
        justificacionesList = new ArrayList<>();
        Log.d(TAG, "Firebase repository inicializado");
    }

    private void initViews() {
        // IDs exactos de tu XML
        toolbar = findViewById(R.id.toolbar);

        // Información básica
        ivAvatar = findViewById(R.id.iv_avatar);
        ivCopyRut = findViewById(R.id.iv_copy_rut);
        ivCopyEmail = findViewById(R.id.iv_copy_email);
        tvNombreCompleto = findViewById(R.id.tv_nombre_completo);
        tvRolBadge = findViewById(R.id.tv_rol_badge);
        tvEstadoBadge = findViewById(R.id.tv_estado_badge);
        tvFechaRegistro = findViewById(R.id.tv_fecha_registro);
        tvUltimoAcceso = findViewById(R.id.tv_ultimo_acceso);
        tvRut = findViewById(R.id.tv_rut);
        tvCorreo = findViewById(R.id.tv_correo);

        // Botones de acción
        btnEditar = findViewById(R.id.btn_editar);
        btnResetearPassword = findViewById(R.id.btn_resetear_password);
        btnEliminar = findViewById(R.id.btn_eliminar);

        // Historial de asistencia
        tvVerHistorialCompleto = findViewById(R.id.tv_ver_historial_completo);
        tvDiasPresente = findViewById(R.id.tv_dias_presente);
        tvDiasAusente = findViewById(R.id.tv_dias_ausente);
        tvAtrasos = findViewById(R.id.tv_atrasos);
        tvPorcentajeAsistencia = findViewById(R.id.tv_porcentaje_asistencia);
        rvHistorialAsistencia = findViewById(R.id.rv_historial_asistencia);
        tvNoHistorialAsistencia = findViewById(R.id.tv_no_historial_asistencia);

        // Justificaciones y licencias
        tvVerJustificacionesCompleto = findViewById(R.id.tv_ver_justificaciones_completo);
        tvJustificacionesPendientes = findViewById(R.id.tv_justificaciones_pendientes);
        tvJustificacionesAceptadas = findViewById(R.id.tv_justificaciones_aceptadas);
        tvJustificacionesRechazadas = findViewById(R.id.tv_justificaciones_rechazadas);
        rvHistorialJustificaciones = findViewById(R.id.rv_historial_justificaciones);
        tvNoJustificaciones = findViewById(R.id.tv_no_justificaciones);

        Log.d(TAG, "Views inicializadas");
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Información del Usuario");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.visualizar_usuario_menu, menu); // o el archivo de menú que corresponda
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { // o el metodo que corresponda
        int id = item.getItemId();
    // Manejar eventos de clic en elementos del menú
        if (id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (id == R.id.menu_editar) {
            editarUsuario();
            return true;
        } else if (id == R.id.menu_cambiar_estado) {
            cambiarEstadoUsuario();
            return true;
        } else if (id == R.id.menu_reset_password) {
            resetearPassword();
            return true;
        } else if (id == R.id.menu_ver_asistencias) {
            verAsistenciasUsuario();
            return true;
        } else if (id == R.id.menu_ver_justificaciones) {
            verJustificacionesUsuario();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
    private void cambiarEstadoUsuario() {
        // Mostrar Dialog para cambiar estado activo/inactivo
        Toast.makeText(this, "Función cambiar estado - Por implementar", Toast.LENGTH_SHORT).show();
    }

    private void verAsistenciasUsuario() {
        // Navegar a vista de asistencias del usuario
        Toast.makeText(this, "Función ver asistencias - Por implementar", Toast.LENGTH_SHORT).show();
    }

    private void verJustificacionesUsuario() {
        // Navegar a vista de justificaciones del usuario
        Toast.makeText(this, "Función ver justificaciones - Por implementar", Toast.LENGTH_SHORT).show();
    }


    private void cerrarSesion() {
        // Limpiar SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Cerrar sesión en Firebase
        FirebaseAuth.getInstance().signOut();

        // Navegar a LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }



    private void setupRecyclerViews() {
        // RecyclerView para historial de asistencia
        rvHistorialAsistencia.setVisibility(View.GONE);
        tvNoHistorialAsistencia.setVisibility(View.VISIBLE);
        tvNoHistorialAsistencia.setText("Historial de asistencias próximamente"); //oculto hasta crear un AsistenciaAdapter

        // RecyclerView para justificaciones
        justificacionesAdapter = new JustificacionesAdapter(justificacionesList, this);
        rvHistorialJustificaciones.setLayoutManager(new LinearLayoutManager(this));
        rvHistorialJustificaciones.setAdapter(justificacionesAdapter);

        Log.d(TAG, "RecyclerViews configurados");
    }


    private void setupClickListeners() {
        // Copiar datos al clipboard
        ivCopyRut.setOnClickListener(v -> copiarAlPortapapeles("RUT", tvRut.getText().toString()));
        ivCopyEmail.setOnClickListener(v -> copiarAlPortapapeles("Correo", tvCorreo.getText().toString()));

        // Botones de acción
        btnEditar.setOnClickListener(v -> editarUsuario());
        btnResetearPassword.setOnClickListener(v -> confirmarResetPassword());
        btnEliminar.setOnClickListener(v -> confirmarEliminarUsuario());

        // Ver historial completo
        tvVerHistorialCompleto.setOnClickListener(v -> verHistorialCompleto());
        tvVerJustificacionesCompleto.setOnClickListener(v -> verJustificacionesCompleto());

        Log.d(TAG, "Click listeners configurados");
    }

    private void loadUsuarioData() {
        Log.d(TAG, "Cargando datos del usuario ID numérico: " + usuarioId);

        // 1. Buscar en userMappings para traducir ID (int) -> UID (string)
        repository.mDatabase.child("userMappings").child(usuarioId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String realFirebaseUid = snapshot.getValue(String.class);
                            // 2. Con el UID real, cargamos los datos
                            cargarDatosReales(realFirebaseUid);
                        } else {
                            Toast.makeText(VisualizarUsuarioActivity.this, "Error: Usuario no encontrado en índices", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error buscando mapping", error.toException());
                        Toast.makeText(VisualizarUsuarioActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void cargarDatosReales(String firebaseUid) {
        // Guardamos el UID real para usarlo en otras funciones (como eliminar)
        // Nota: Define 'private String currentFirebaseUid;' al inicio de la clase
        // currentFirebaseUid = firebaseUid;

        repository.mDatabase.child("usuarios").child(firebaseUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            usuario = dataSnapshot.getValue(Usuario.class);
                            if (usuario != null) {
                                mostrarInformacionUsuario();
                                // Ahora cargamos historiales usando el UID correcto si es necesario
                                loadHistorialAsistencia();
                                loadJustificaciones();
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void mostrarInformacionUsuario() {
        Log.d(TAG, "Mostrando información del usuario");

        // Información básica
        String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
        tvNombreCompleto.setText(nombreCompleto);
        tvRut.setText(usuario.getRut());
        tvCorreo.setText(usuario.getCorreo());

        // Rol badge
        if (usuario.getIdRol() == 1) {
            tvRolBadge.setText("Administrador");
            tvRolBadge.setBackgroundResource(R.drawable.badge_rol_admin);
        } else {
            tvRolBadge.setText("Empleado");
            tvRolBadge.setBackgroundResource(R.drawable.badge_rol_empleado);
        }

        // Estado badge
        String estado = usuario.getEstadoUsuario();
        tvEstadoBadge.setText(estado.substring(0, 1).toUpperCase() + estado.substring(1));

        if ("activo".equals(estado)) {
            tvEstadoBadge.setBackgroundResource(R.drawable.badge_estado_activo);
        } else {
            tvEstadoBadge.setBackgroundResource(R.drawable.badge_estado_inactivo);
        }

        // Fechas (mockup - en implementación real vendrían de Firebase)
        tvFechaRegistro.setText("Registrado: " + getCurrentDate());
        tvUltimoAcceso.setText("Último acceso: " + getCurrentDate());

        // Actualizar título del toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Usuario: " + usuario.getNombre());
        }

        Log.d(TAG, "Información del usuario mostrada correctamente");
    }

    private void loadHistorialAsistencia() {
        Log.d(TAG, "Cargando historial de asistencia");

        repository.obtenerHistorialAsistencia(usuarioId, new FirebaseRepository.DataCallback<List<Asistencia>>() {
            @Override
            public void onSuccess(List<Asistencia> asistencias) {
                Log.d(TAG, "✓ Historial de asistencia obtenido: " + asistencias.size() + " registros");

                asistenciasList.clear();

                // Filtrar últimos 30 días y mostrar solo los primeros 5
                List<Asistencia> asistenciasRecientes = filtrarUltimos30Dias(asistencias);
                List<Asistencia> asistenciasParaMostrar = asistenciasRecientes.size() > 5 ?
                        asistenciasRecientes.subList(0, 5) : asistenciasRecientes;

                asistenciasList.addAll(asistenciasParaMostrar);

                // Calcular estadísticas
                calcularEstadisticasAsistencia(asistenciasRecientes);

                // Actualizar vista
                if (asistenciasList.isEmpty()) {
                    rvHistorialAsistencia.setVisibility(View.GONE);
                    tvNoHistorialAsistencia.setVisibility(View.VISIBLE);
                } else {
                    rvHistorialAsistencia.setVisibility(View.VISIBLE);
                    tvNoHistorialAsistencia.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "✗ Error cargando historial de asistencia", error);
                rvHistorialAsistencia.setVisibility(View.GONE);
                tvNoHistorialAsistencia.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadJustificaciones() {
        Log.d(TAG, "Cargando justificaciones del usuario");

        repository.obtenerJustificaciones(new FirebaseRepository.DataCallback<List<Justificacion>>() {
            @Override
            public void onSuccess(List<Justificacion> todasJustificaciones) {
                // Filtrar solo las justificaciones del usuario actual
                List<Justificacion> justificacionesUsuario = new ArrayList<>();
                for (Justificacion j : todasJustificaciones) {
                    if (usuarioId.equals(String.valueOf(j.getIdUsuario()))) {
                        justificacionesUsuario.add(j);
                    }
                }

                Log.d(TAG, "✓ Justificaciones del usuario: " + justificacionesUsuario.size());

                justificacionesList.clear();

                // Mostrar solo las más recientes (máximo 5)
                List<Justificacion> justificacionesParaMostrar = justificacionesUsuario.size() > 5 ?
                        justificacionesUsuario.subList(0, 5) : justificacionesUsuario;

                justificacionesList.addAll(justificacionesParaMostrar);

                // Calcular estadísticas por estado
                calcularEstadisticasJustificaciones(justificacionesUsuario);

                // Actualizar vista
                if (justificacionesList.isEmpty()) {
                    rvHistorialJustificaciones.setVisibility(View.GONE);
                    tvNoJustificaciones.setVisibility(View.VISIBLE);
                } else {
                    rvHistorialJustificaciones.setVisibility(View.VISIBLE);
                    tvNoJustificaciones.setVisibility(View.GONE);
                    justificacionesAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "✗ Error cargando justificaciones", error);
                rvHistorialJustificaciones.setVisibility(View.GONE);
                tvNoJustificaciones.setVisibility(View.VISIBLE);
            }
        });
    }

    private List<Asistencia> filtrarUltimos30Dias(List<Asistencia> asistencias) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        String fechaLimite = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        List<Asistencia> asistenciasFiltradas = new ArrayList<>();
        for (Asistencia asistencia : asistencias) {
            if (asistencia.getFecha().compareTo(fechaLimite) >= 0) { // USAR FECHA
                asistenciasFiltradas.add(asistencia);
            }
        }
        return asistenciasFiltradas;
    }

    private void calcularEstadisticasAsistencia(List<Asistencia> asistencias) {
        int diasPresente = 0;
        int atrasos = 0;

        // Contar entradas para calcular días presente
        for (Asistencia asistencia : asistencias) {

            if (asistencia.getIdTipoAccion() == 1) { // 1 = entrada
                diasPresente++;

                // Simulamos atrasos ocasionalmente
                if (Math.random() < 0.1) {
                    atrasos++;
                }
            }
        }

        int diasTotales = 30; // Últimos 30 días
        int diasAusente = diasTotales - diasPresente;
        int porcentajeAsistencia = diasTotales > 0 ? (diasPresente * 100) / diasTotales : 0;

        // Actualizar TextViews
        tvDiasPresente.setText(String.valueOf(diasPresente));
        tvDiasAusente.setText(String.valueOf(Math.max(0, diasAusente)));
        tvAtrasos.setText(String.valueOf(atrasos));
        tvPorcentajeAsistencia.setText(porcentajeAsistencia + "%");

        Log.d(TAG, "Estadísticas calculadas - Presente: " + diasPresente +
                ", Ausente: " + diasAusente + ", Atrasos: " + atrasos +
                ", Porcentaje: " + porcentajeAsistencia + "%");
    }


    private void calcularEstadisticasJustificaciones(List<Justificacion> justificaciones) {
        int pendientes = 0, aceptadas = 0, rechazadas = 0;

        for (Justificacion justificacion : justificaciones) {

            switch (justificacion.getIdEstado()) {
                case 2: // pendiente
                    pendientes++;
                    break;
                case 3: // aprobado/aceptado
                    aceptadas++;
                    break;
                case 4: // rechazado
                    rechazadas++;
                    break;
            }
        }

        // Actualizar TextViews
        tvJustificacionesPendientes.setText(String.valueOf(pendientes));
        tvJustificacionesAceptadas.setText(String.valueOf(aceptadas));
        tvJustificacionesRechazadas.setText(String.valueOf(rechazadas));

        Log.d(TAG, "Estadísticas de justificaciones - Pendientes: " + pendientes +
                ", Aceptadas: " + aceptadas + ", Rechazadas: " + rechazadas);
    }


    private void copiarAlPortapapeles(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, label + " copiado al portapapeles", Toast.LENGTH_SHORT).show();
        Log.d(TAG, label + " copiado: " + text);
    }

    private void editarUsuario() {
        Log.d(TAG, "Navegando a editar usuario: " + usuarioId);

        Intent intent = new Intent(this, AnadirUsuarioActivity.class);
        intent.putExtra("usuario_id", usuarioId);
        intent.putExtra("modo_edicion", true);
        startActivity(intent);
    }

    private void confirmarResetPassword() {
        new AlertDialog.Builder(this)
                .setTitle("Resetear Contraseña")
                .setMessage("¿Estás seguro de que quieres resetear la contraseña de " +
                        usuario.getNombre() + " " + usuario.getApellido() + "?")
                .setPositiveButton("Resetear", (dialog, which) -> resetearPassword())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void resetearPassword() {
        Log.d(TAG, "Reseteando contraseña para usuario: " + usuarioId);

        // Implementar reset de contraseña en Firebase Auth
        Toast.makeText(this, "Funcionalidad de reset de contraseña en desarrollo", Toast.LENGTH_LONG).show();
    }

    private void confirmarEliminarUsuario() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Usuario")
                .setMessage("¿Estás seguro de que quieres eliminar a " +
                        usuario.getNombre() + " " + usuario.getApellido() + "?\n\n" +
                        "Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarUsuario())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarUsuario() {
        Log.d(TAG, "Eliminando usuario: " + usuarioId);

        repository.eliminarUsuario(usuarioId, new FirebaseRepository.CrudCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✓ Usuario eliminado exitosamente");
                Toast.makeText(VisualizarUsuarioActivity.this,
                        "Usuario eliminado correctamente", Toast.LENGTH_SHORT).show();
                finish(); // Cerrar activity
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "✗ Error eliminando usuario", error);
                Toast.makeText(VisualizarUsuarioActivity.this,
                        "Error eliminando usuario: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void verHistorialCompleto() {
        // Navegar a activity de historial completo de asistencia
        Toast.makeText(this, "Ver historial completo (funcionalidad pendiente)", Toast.LENGTH_SHORT).show();
    }

    private void verJustificacionesCompleto() {
        // Navegar a activity de justificaciones completas
        Toast.makeText(this, "Ver justificaciones completas (funcionalidad pendiente)", Toast.LENGTH_SHORT).show();
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new java.util.Date());
    }
}
