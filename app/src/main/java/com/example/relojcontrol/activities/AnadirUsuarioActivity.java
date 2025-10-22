package com.example.relojcontrol.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Usuario;
import com.example.relojcontrol.network.FirebaseRepository;

import java.util.regex.Pattern;

public class AnadirUsuarioActivity extends AppCompatActivity {

    private static final String TAG = "AñadirUsuarioActivity";

    private Toolbar toolbar;
    private TextInputLayout tilNombre, tilApellido, tilRut, tilCorreo, tilPassword, tilRol;
    private TextInputEditText etNombre, etApellido, etRut, etCorreo, etPassword;
    private AutoCompleteTextView spinnerRol;

    // Validación RUT
    private LinearLayout layoutRutValidation;
    private ImageView ivRutValidation;
    private TextView tvRutValidation;

    // Validación contraseña
    private LinearLayout layoutPasswordLength, layoutPasswordNumber, layoutPasswordUppercase;
    private ImageView ivPasswordLength, ivPasswordNumber, ivPasswordUppercase;

    // Botones y estados
    private MaterialButton btnCancelar, btnCrearUsuario;
    private LinearLayout layoutLoading;
    private CardView cardSuccess;
    private TextView tvSuccessMessage;

    // Data
    private FirebaseRepository repository;
    private boolean modoEdicion = false;
    private String usuarioId = "";

    // Validaciones
    private boolean nombreValido = false;
    private boolean apellidoValido = false;
    private boolean rutValido = false;
    private boolean correoValido = false;
    private boolean passwordValido = false;

    // Regex patterns
    private static final Pattern RUT_PATTERN = Pattern.compile("^[0-9]{1,2}\\.[0-9]{3}\\.[0-9]{3}-[0-9kK]{1}$");
    private static final Pattern PASSWORD_LENGTH_PATTERN = Pattern.compile(".{8,}");
    private static final Pattern PASSWORD_NUMBER_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern PASSWORD_UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anadir_usuario);

        Log.d(TAG, "=== AñadirUsuarioActivity iniciada ===");

        initFirebase();
        initViews();
        setupToolbar();
        setupSpinner();
        setupValidation();
        setupClickListeners();

        // Verificar si es modo edición
        checkModoEdicion();
    }

    private void initFirebase() {
        repository = FirebaseRepository.getInstance();
        Log.d(TAG, "Firebase repository inicializado");
    }

    private void initViews() {
        // IDs del XML
        toolbar = findViewById(R.id.toolbar);

        // TextInputLayouts
        tilNombre = findViewById(R.id.til_nombre);
        tilApellido = findViewById(R.id.til_apellido);
        tilRut = findViewById(R.id.til_rut);
        tilCorreo = findViewById(R.id.til_correo);
        tilPassword = findViewById(R.id.til_password);
        tilRol = findViewById(R.id.til_rol);

        // EditTexts
        etNombre = findViewById(R.id.et_nombre);
        etApellido = findViewById(R.id.et_apellido);
        etRut = findViewById(R.id.et_rut);
        etCorreo = findViewById(R.id.et_correo);
        etPassword = findViewById(R.id.et_password);
        spinnerRol = findViewById(R.id.spinner_rol);

        // Validar RUT
        layoutRutValidation = findViewById(R.id.layout_rut_validation);
        ivRutValidation = findViewById(R.id.iv_rut_validation);
        tvRutValidation = findViewById(R.id.tv_rut_validation);

        // Validar contraseña
        layoutPasswordLength = findViewById(R.id.layout_password_length);
        layoutPasswordNumber = findViewById(R.id.layout_password_number);
        layoutPasswordUppercase = findViewById(R.id.layout_password_uppercase);

        ivPasswordLength = findViewById(R.id.iv_password_length);
        ivPasswordNumber = findViewById(R.id.iv_password_number);
        ivPasswordUppercase = findViewById(R.id.iv_password_uppercase);

        // Botones y estados
        btnCancelar = findViewById(R.id.btn_cancelar);
        btnCrearUsuario = findViewById(R.id.btn_crear_usuario);
        layoutLoading = findViewById(R.id.layout_loading);
        cardSuccess = findViewById(R.id.card_success);
        tvSuccessMessage = findViewById(R.id.tv_success_message);

        Log.d(TAG, "Views inicializadas");
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupSpinner() {
        String[] roles = {"Empleado", "Administrador"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, roles);
        spinnerRol.setAdapter(adapter);
        spinnerRol.setText("Empleado", false);

        Log.d(TAG, "Spinner de roles configurado");
    }

    private void setupValidation() {
        // Validar nombre
        etNombre.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validarNombre(s.toString().trim());
                updateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Validar apellido
        etApellido.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validarApellido(s.toString().trim());
                updateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Validar RUT
        etRut.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validarRut(s.toString().trim());
                updateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Validar correo
        etCorreo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validarCorreo(s.toString().trim());
                updateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Validar contraseña
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validarPassword(s.toString());
                updateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        Log.d(TAG, "Validaciones configuradas");
    }

    private void setupClickListeners() {
        btnCancelar.setOnClickListener(v -> onBackPressed());

        btnCrearUsuario.setOnClickListener(v -> {
            if (modoEdicion) {
                actualizarUsuario();
            } else {
                crearUsuario();
            }
        });

        Log.d(TAG, "Click listeners configurados");
    }

    private void checkModoEdicion() {
        modoEdicion = getIntent().getBooleanExtra("modo_edicion", false);
        usuarioId = getIntent().getStringExtra("usuario_id");

        if (modoEdicion) {
            // Configurar para modo edicion
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Editar Usuario");
            }
            btnCrearUsuario.setText("Actualizar Usuario");

            // En modo edición, la contraseña es opcional
            tilPassword.setHint("Nueva contraseña (opcional)");
            passwordValido = true; // Permitir formulario sin cambiar contraseña

            // Cargar datos del usuario
            if (usuarioId != null && !usuarioId.isEmpty()) {
                // TODO: Cargar datos del usuario desde Firebase
                Log.d(TAG, "Cargando usuario para edición: " + usuarioId);
            }
        } else {
            // Configurar para modo creación
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Añadir Nuevo Usuario");
            }
            btnCrearUsuario.setText("Crear Usuario");
        }

        Log.d(TAG, "Modo configurado - Edición: " + modoEdicion);
    }

    private void validarNombre(String nombre) {
        if (nombre.isEmpty()) {
            tilNombre.setError("El nombre es requerido");
            nombreValido = false;
        } else if (nombre.length() < 2) {
            tilNombre.setError("El nombre debe tener al menos 2 caracteres");
            nombreValido = false;
        } else {
            tilNombre.setError(null);
            nombreValido = true;
        }
    }

    private void validarApellido(String apellido) {
        if (apellido.isEmpty()) {
            tilApellido.setError("El apellido es requerido");
            apellidoValido = false;
        } else if (apellido.length() < 2) {
            tilApellido.setError("El apellido debe tener al menos 2 caracteres");
            apellidoValido = false;
        } else {
            tilApellido.setError(null);
            apellidoValido = true;
        }
    }

    private void validarRut(String rut) {
        if (rut.isEmpty()) {
            tilRut.setError("El RUT es requerido");
            layoutRutValidation.setVisibility(View.GONE);
            rutValido = false;
        } else if (isValidRut(rut)) {
            tilRut.setError(null);
            mostrarValidacionRut(true, "RUT válido");
            rutValido = true;
        } else {
            tilRut.setError("Formato de RUT inválido");
            mostrarValidacionRut(false, "RUT inválido");
            rutValido = false;
        }
    }

    private void validarCorreo(String correo) {
        if (correo.isEmpty()) {
            tilCorreo.setError("El correo es requerido");
            correoValido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            tilCorreo.setError("Formato de correo inválido");
            correoValido = false;
        } else {
            tilCorreo.setError(null);
            correoValido = true;
        }
    }

    private void validarPassword(String password) {
        if (!modoEdicion && password.isEmpty()) {
            tilPassword.setError("La contraseña es requerida");
            passwordValido = false;
            return;
        }

        if (password.isEmpty() && modoEdicion) {
            // En modo edición, contraseña vacía es válida (no se cambia)
            tilPassword.setError(null);
            passwordValido = true;
            return;
        }

        tilPassword.setError(null);

        // Validar longitud
        boolean lengthValid = PASSWORD_LENGTH_PATTERN.matcher(password).matches();
        updatePasswordValidation(ivPasswordLength, lengthValid);

        // Validar número
        boolean numberValid = PASSWORD_NUMBER_PATTERN.matcher(password).matches();
        updatePasswordValidation(ivPasswordNumber, numberValid);

        // Validar mayúscula
        boolean uppercaseValid = PASSWORD_UPPERCASE_PATTERN.matcher(password).matches();
        updatePasswordValidation(ivPasswordUppercase, uppercaseValid);

        passwordValido = lengthValid && numberValid && uppercaseValid;
    }

    private boolean isValidRut(String rut) {
        // Simplificada: solo verificar formato básico
        return RUT_PATTERN.matcher(rut).matches();
    }

    private void mostrarValidacionRut(boolean isValid, String message) {
        layoutRutValidation.setVisibility(View.VISIBLE);
        tvRutValidation.setText(message);

        if (isValid) {
            ivRutValidation.setImageResource(R.drawable.ic_check);
            ivRutValidation.setImageTintList(getColorStateList(R.color.success_color));
            tvRutValidation.setTextColor(getColor(R.color.success_color));
        } else {
            ivRutValidation.setImageResource(R.drawable.ic_close);
            ivRutValidation.setImageTintList(getColorStateList(R.color.error_color));
            tvRutValidation.setTextColor(getColor(R.color.error_color));
        }
    }

    private void updatePasswordValidation(ImageView imageView, boolean isValid) {
        if (isValid) {
            imageView.setImageResource(R.drawable.ic_check);
            imageView.setImageTintList(getColorStateList(R.color.success_color));
        } else {
            imageView.setImageResource(R.drawable.ic_close);
            imageView.setImageTintList(getColorStateList(R.color.error_color));
        }
    }

    private void updateButtonState() {
        boolean formValid = nombreValido && apellidoValido && rutValido && correoValido && passwordValido;
        btnCrearUsuario.setEnabled(formValid);

        Log.d(TAG, "Estado del formulario - Válido: " + formValid);
    }

    private void crearUsuario() {
        Log.d(TAG, "=== CREANDO USUARIO ===");

        showLoading(true);

        // Crear objeto Usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(etNombre.getText().toString().trim());
        usuario.setApellido(etApellido.getText().toString().trim());
        usuario.setRut(etRut.getText().toString().trim());
        usuario.setCorreo(etCorreo.getText().toString().trim());
        usuario.setEstadoUsuario("activo");

        // Configurar rol
        String rolSeleccionado = spinnerRol.getText().toString();
        if ("Administrador".equals(rolSeleccionado)) {
            usuario.setIdRol(1);
        } else {
            usuario.setIdRol(2);
        }

        String password = etPassword.getText().toString();

        repository.crearUsuario(usuario, password, new FirebaseRepository.CrudCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✓ Usuario creado exitosamente");

                runOnUiThread(() -> {
                    showLoading(false);
                    mostrarExito("Usuario creado exitosamente",
                            "El usuario " + usuario.getNombre() + " " + usuario.getApellido() +
                                    " ha sido registrado en el sistema.");

                    limpiarFormulario();
                });
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "✗ Error creando usuario", error);

                runOnUiThread(() -> {
                    showLoading(false);

                    String errorMessage = "Error creando usuario";
                    if (error.getMessage() != null) {
                        if (error.getMessage().contains("email-already-in-use")) {
                            errorMessage = "Ya existe un usuario con este correo";
                        } else if (error.getMessage().contains("weak-password")) {
                            errorMessage = "La contraseña es muy débil";
                        }
                    }

                    Toast.makeText(AnadirUsuarioActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void actualizarUsuario() {
        Log.d(TAG, "=== ACTUALIZANDO USUARIO ===");

        //Implementar actualizacion de usuario
        Toast.makeText(this, "Funcionalidad de actualizar en desarrollo", Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean show) {
        layoutLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        btnCrearUsuario.setEnabled(!show);
        btnCancelar.setEnabled(!show);
    }

    private void mostrarExito(String titulo, String mensaje) {
        tvSuccessMessage.setText(mensaje);
        cardSuccess.setVisibility(View.VISIBLE);

        // Ocultar mensaje después de 5 segundos
        cardSuccess.postDelayed(() -> {
            cardSuccess.setVisibility(View.GONE);
        }, 5000);
    }

    private void limpiarFormulario() {
        etNombre.setText("");
        etApellido.setText("");
        etRut.setText("");
        etCorreo.setText("");
        etPassword.setText("");
        spinnerRol.setText("Empleado", false);

        layoutRutValidation.setVisibility(View.GONE);

        // Reset validaciones
        nombreValido = false;
        apellidoValido = false;
        rutValido = false;
        correoValido = false;
        passwordValido = false;

        updateButtonState();

        Log.d(TAG, "Formulario limpiado");
    }
}
