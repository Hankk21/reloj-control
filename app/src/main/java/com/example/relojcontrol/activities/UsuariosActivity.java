package com.example.relojcontrol.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;


import com.example.relojcontrol.R;
import com.example.relojcontrol.adapters.UsuarioAdapter;
import com.example.relojcontrol.models.Usuario;
import com.example.relojcontrol.network.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;

public class UsuariosActivity extends AppCompatActivity {

    private static final String TAG = "UsuariosActivity";

    // Views del XML
    private Toolbar toolbar;
    private TextInputLayout tilBuscar, tilFiltroRol, tilFiltroEstado;
    private TextInputEditText etBuscar;
    private AutoCompleteTextView spinnerRol, spinnerEstado;
    private MaterialButton btnAnadirUsuario, btnExportar;
    private RecyclerView rvUsuarios;
    private LinearLayout layoutNoUsuarios, layoutPaginacion;
    private Button btnPaginaAnterior, btnPaginaSiguiente;
    private TextView tvPaginaActual;

    // Data
    private FirebaseRepository repository;
    private UsuarioAdapter usuarioAdapter;
    private List<Usuario> usuariosList;
    private List<Usuario> usuariosFilteredList;

    // Filtros
    private String filtroRol = "Todos";
    private String filtroEstado = "Todos";
    private String textosBusqueda = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuarios);

        Log.d(TAG, "=== UsuariosActivity iniciada ===");

        initFirebase();
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSpinners();
        setupSearchAndFilters();
        setupClickListeners();
        loadUsuarios();
    }

    private void initFirebase() {
        repository = FirebaseRepository.getInstance();
        Log.d(TAG, "Firebase repository inicializado");
    }

    private void initViews() {
        // IDs exactos de tu XML
        toolbar = findViewById(R.id.toolbar);
        tilBuscar = findViewById(R.id.til_buscar);
        tilFiltroRol = findViewById(R.id.til_filtro_rol);
        tilFiltroEstado = findViewById(R.id.til_filtro_estado);
        etBuscar = findViewById(R.id.et_buscar);
        spinnerRol = findViewById(R.id.spinner_rol);
        spinnerEstado = findViewById(R.id.spinner_estado);
        btnAnadirUsuario = findViewById(R.id.btn_anadir_usuario);
        btnExportar = findViewById(R.id.btn_exportar);
        rvUsuarios = findViewById(R.id.rv_usuarios);
        layoutNoUsuarios = findViewById(R.id.layout_no_usuarios);
        layoutPaginacion = findViewById(R.id.layout_paginacion);
        btnPaginaAnterior = findViewById(R.id.btn_pagina_anterior);
        btnPaginaSiguiente = findViewById(R.id.btn_pagina_siguiente);
        tvPaginaActual = findViewById(R.id.tv_pagina_actual);

        Log.d(TAG, "Views inicializadas");
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Gestión de Usuarios");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        usuariosList = new ArrayList<>();
        usuariosFilteredList = new ArrayList<>();

        usuarioAdapter = new UsuarioAdapter(usuariosFilteredList, new UsuarioAdapter.OnUsuarioClickListener() {

            @Override
            public void onUsuarioClick(Usuario usuario) {
                // Acción por defecto al hacer click en el usuario
                Intent intent = new Intent(UsuariosActivity.this, VisualizarUsuarioActivity.class);
                intent.putExtra("usuario_id", String.valueOf(usuario.getIdUsuario()));
                startActivity(intent);
            }
            @Override
            public void onEditClick(Usuario usuario) {
                // Navegar a editar usuario
                Intent intent = new Intent(UsuariosActivity.this, AnadirUsuarioActivity.class);
                intent.putExtra("usuario_id", String.valueOf(usuario.getIdUsuario()));
                intent.putExtra("modo_edicion", true);
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(Usuario usuario) {
                confirmarEliminarUsuario(usuario);
            }

            @Override
            public void onToggleUsuarioStatus(Usuario usuario) {
                // Implementar lógica para cambiar el estado del usuario
            }
        });

        rvUsuarios.setLayoutManager(new LinearLayoutManager(this));
        rvUsuarios.setAdapter(usuarioAdapter);

        Log.d(TAG, "RecyclerView configurado");
    }

    private void setupSpinners() {
        // Configurar spinner de roles
        String[] roles = {"Todos", "Administrador", "Empleado"};
        ArrayAdapter<String> adapterRoles = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, roles);
        spinnerRol.setAdapter(adapterRoles);
        spinnerRol.setText("Todos", false);

        // Configurar spinner de estados
        String[] estados = {"Todos", "Activo", "Inactivo"};
        ArrayAdapter<String> adapterEstados = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, estados);
        spinnerEstado.setAdapter(adapterEstados);
        spinnerEstado.setText("Todos", false);

        Log.d(TAG, "Spinners configurados");
    }

    private void setupSearchAndFilters() {
        // Configurar búsqueda en tiempo real
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textosBusqueda = s.toString().trim();
                aplicarFiltros();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Configurar filtro por rol
        spinnerRol.setOnItemClickListener((parent, view, position, id) -> {
            filtroRol = (String) parent.getItemAtPosition(position);
            aplicarFiltros();
        });

        // Configurar filtro por estado
        spinnerEstado.setOnItemClickListener((parent, view, position, id) -> {
            filtroEstado = (String) parent.getItemAtPosition(position);
            aplicarFiltros();
        });

        Log.d(TAG, "Búsqueda y filtros configurados");
    }

    private void setupClickListeners() {
        btnAnadirUsuario.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnadirUsuarioActivity.class);
            intent.putExtra("modo_edicion", false);
            startActivity(intent);
        });

        btnExportar.setOnClickListener(v -> {
            Toast.makeText(this, "Funcionalidad de exportar en desarrollo", Toast.LENGTH_SHORT).show();
        });

        btnPaginaAnterior.setOnClickListener(v -> {
            // Implementar paginación anterior
            Toast.makeText(this, "Página anterior", Toast.LENGTH_SHORT).show();
        });

        btnPaginaSiguiente.setOnClickListener(v -> {
            // Implementar paginación siguiente
            Toast.makeText(this, "Página siguiente", Toast.LENGTH_SHORT).show();
        });

        Log.d(TAG, "Click listeners configurados");
    }

    private void loadUsuarios() {
        Log.d(TAG, "Cargando usuarios desde Firebase");

        repository.obtenerUsuarios(new FirebaseRepository.DataCallback<List<Usuario>>() {
            @Override
            public void onSuccess(List<Usuario> usuarios) {
                Log.d(TAG, "✓ Usuarios cargados: " + usuarios.size());

                usuariosList.clear();
                usuariosList.addAll(usuarios);

                aplicarFiltros();
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "✗ Error cargando usuarios", error);
                Toast.makeText(UsuariosActivity.this,
                        "Error cargando usuarios: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();

                mostrarEstadoSinUsuarios();
            }
        });
    }

    private void aplicarFiltros() {
        usuariosFilteredList.clear();

        for (Usuario usuario : usuariosList) {
            boolean cumpleFiltros = true;

            // Filtro por texto de búsqueda (mantener igual)
            if (!textosBusqueda.isEmpty()) {
                String textoCompleto = (usuario.getNombre() + " " + usuario.getApellido() + " " +
                        usuario.getRut() + " " + usuario.getCorreo()).toLowerCase();
                if (!textoCompleto.contains(textosBusqueda.toLowerCase())) {
                    cumpleFiltros = false;
                }
            }

            // Filtro por rol
            if (!filtroRol.equals("Todos")) {
                if (filtroRol.equals("Administrador") && usuario.getIdRol() != 1) {
                    cumpleFiltros = false;
                } else if (filtroRol.equals("Empleado") && usuario.getIdRol() != 2) {
                    cumpleFiltros = false;
                }
            }

            // Filtro por estado
            if (!filtroEstado.equals("Todos")) {
                String estadoUsuario = usuario.getEstadoUsuario();
                if (filtroEstado.equals("Activo") && !"activo".equals(estadoUsuario.toLowerCase())) {
                    cumpleFiltros = false;
                } else if (filtroEstado.equals("Inactivo") && !"inactivo".equals(estadoUsuario.toLowerCase())) {
                    cumpleFiltros = false;
                }
            }

            if (cumpleFiltros) {
                usuariosFilteredList.add(usuario);
            }
        }

        actualizarVista();
        Log.d(TAG, "Filtros aplicados - Usuarios mostrados: " + usuariosFilteredList.size());
    }


    private void actualizarVista() {
        if (usuariosFilteredList.isEmpty()) {
            mostrarEstadoSinUsuarios();
        } else {
            mostrarListaUsuarios();
        }

        usuarioAdapter.notifyDataSetChanged();
    }

    private void mostrarEstadoSinUsuarios() {
        rvUsuarios.setVisibility(View.GONE);
        layoutNoUsuarios.setVisibility(View.VISIBLE);
        layoutPaginacion.setVisibility(View.GONE);
    }

    private void mostrarListaUsuarios() {
        rvUsuarios.setVisibility(View.VISIBLE);
        layoutNoUsuarios.setVisibility(View.GONE);

        // Mostrar paginación si hay más de 10 usuarios (ejemplo)
        if (usuariosFilteredList.size() > 10) {
            layoutPaginacion.setVisibility(View.VISIBLE);
            tvPaginaActual.setText("Página 1 de " + ((usuariosFilteredList.size() / 10) + 1));
        } else {
            layoutPaginacion.setVisibility(View.GONE);
        }
    }

    private void confirmarEliminarUsuario(Usuario usuario) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar Usuario")
                .setMessage("¿Estás seguro de que quieres eliminar a " + usuario.getNombre() + " " + usuario.getApellido() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    eliminarUsuario(usuario);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarUsuario(Usuario usuario) {
        Log.d(TAG, "Eliminando usuario: " + usuario.getIdUsuario());

        // Buscar Firebase UID por ID numérico
        buscarFirebaseUidPorId(usuario.getIdUsuario(), firebaseUid -> {
            if (firebaseUid != null) {
                // Ahora sí eliminar con el UID correcto
                repository.eliminarUsuario(firebaseUid, new FirebaseRepository.CrudCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "✓ Usuario eliminado exitosamente");
                        Toast.makeText(UsuariosActivity.this, "Usuario eliminado", Toast.LENGTH_SHORT).show();

                        // Recargar lista
                        loadUsuarios();
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "✗ Error eliminando usuario", error);
                        Toast.makeText(UsuariosActivity.this,
                                "Error eliminando usuario: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                // Caso cuando no se encuentra el Firebase UID
                Log.e(TAG, "No se pudo encontrar Firebase UID para usuario ID: " + usuario.getIdUsuario());
                Toast.makeText(UsuariosActivity.this,
                        "Error: No se pudo localizar el usuario en el sistema",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void buscarFirebaseUidPorId(int userId, UidCallback callback) {
        Log.d(TAG, "Buscando Firebase UID para usuario ID: " + userId);

        repository.mDatabase.child("userMappings").child(String.valueOf(userId))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String firebaseUid = dataSnapshot.getValue(String.class);
                            Log.d(TAG, "✓ Firebase UID encontrado: " + firebaseUid);
                            callback.onUid(firebaseUid);
                        } else {
                            Log.w(TAG, "✗ No se encontró mapping para usuario ID: " + userId);
                            callback.onUid(null);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error buscando Firebase UID", error.toException());
                        callback.onUid(null);
                    }
                });
    }

    // Interfaz para devolver el Firebase UID
    private interface UidCallback {
        void onUid(String firebaseUid);
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadUsuarios(); // Recargar datos cuando vuelva a la activity
    }
}
