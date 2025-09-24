package com.example.relojcontrol.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
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
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Usuario;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class AnadirUsuarioActivity extends AppCompatActivity {

    // Views
    private Toolbar toolbar;
    private TextInputLayout tilNombre, tilApellido, tilRut, tilCorreo, tilPassword, tilRol;
    private TextInputEditText etNombre, etApellido, etRut, etCorreo, etPassword;
    private AutoCompleteTextView spinnerRol;
    private MaterialButton btnCancelar, btnCrearUsuario;
    private LinearLayout layoutLoading, layoutRutValidation;
    private CardView cardSuccess;
    private TextView tvSuccessMessage, tvRutValidation;
    private ImageView ivRutValidation, ivPasswordLength, ivPasswordNumber, ivPasswordUppercase;
    private LinearLayout layoutPasswordLength, layoutPasswordNumber, layoutPasswordUppercase;

    // Variables para validación
    private boolean isRutValid = false;
    private boolean isPasswordValid = false;
    private boolean hasMinLength = false;
    private boolean hasNumber = false;
    private boolean hasUppercase = false;

    // Variables para modo edición
    private boolean isModoEdicion = false;
    private Usuario usuarioEdicion;

    // Patrones de validación
    private static final Pattern RUT_PATTERN = Pattern.compile("^[0-9]+-[0-9kK]$");
    private static final Pattern PASSWORD_NUMBER = Pattern.compile(".*[0-9].*");
    private static final Pattern PASSWORD_UPPERCASE = Pattern.compile(".*[A-Z].*");

    // Roles disponibles (adaptados a tu modelo)
    private String[] roles = {"Empleado", "Administrador"};
    private int[] rolesIds = {2, 1}; // 1: Administrador, 2: Empleado

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anadir_usuario);

        initViews();
        setupToolbar();
        setupRolSpinner();
        setupValidation();
        setupClickListeners();
        checkModoEdicion();
    }

    private void initViews() {
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

        // Buttons
        btnCancelar = findViewById(R.id.btn_cancelar);
        btnCrearUsuario = findViewById(R.id.btn_crear_usuario);

        // Other views
        layoutLoading = findViewById(R.id.layout_loading);
        layoutRutValidation = findViewById(R.id.layout_rut_validation);
        cardSuccess = findViewById(R.id.card_success);
        tvSuccessMessage = findViewById(R.id.tv_success_message);
        tvRutValidation = findViewById(R.id.tv_rut_validation);
        ivRutValidation = findViewById(R.id.iv_rut_validation);

        // Password validation views
        layoutPasswordLength = findViewById(R.id.layout_password_length);
        layoutPasswordNumber = findViewById(R.id.layout_password_number);
        layoutPasswordUppercase = findViewById(R.id.layout_password_uppercase);
        ivPasswordLength = findViewById(R.id.iv_password_length);
        ivPasswordNumber = findViewById(R.id.iv_password_number);
        ivPasswordUppercase = findViewById(R.id.iv_password_uppercase);
    }

    private void checkModoEdicion() {
        if (getIntent() != null && getIntent().hasExtra("modo_edicion")) {
            isModoEdicion = getIntent().getBooleanExtra("modo_edicion", false);
            usuarioEdicion = (Usuario) getIntent().getSerializableExtra("usuario");

            if (isModoEdicion && usuarioEdicion != null) {
                setupModoEdicion();
            }
        }
    }

    private void setupModoEdicion() {
        // Cambiar título de la toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Editar Usuario");
        }

        // Llenar campos con datos del usuario
        etNombre.setText(usuarioEdicion.getNombre());
        etApellido.setText(usuarioEdicion.getApellido());
        etRut.setText(usuarioEdicion.getRut());
        etCorreo.setText(usuarioEdicion.getCorreo());

        // Establecer rol basado en idRol
        String rolTexto = usuarioEdicion.getRolTexto();
        spinnerRol.setText(rolTexto, false);

        // Cambiar texto del botón
        btnCrearUsuario.setText("Actualizar Usuario");

        // En edición, la contraseña no es obligatoria
        tilPassword.setHint("Contraseña (dejar vacío para no cambiar)");
        isPasswordValid = true; // En edición, la contraseña es opcional
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isModoEdicion ? "Editar Usuario" : "Añadir Usuario");
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRolSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                roles
        );
        spinnerRol.setAdapter(adapter);
        if (!isModoEdicion) {
            spinnerRol.setText(roles[0], false); // Default to "Empleado"
        }
    }

    private void setupValidation() {
        // RUT validation
        etRut.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateRut(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Password validation (solo aplica en creación)
        if (!isModoEdicion) {
            etPassword.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validatePassword(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // General form validation
        TextWatcher formValidationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etNombre.addTextChangedListener(formValidationWatcher);
        etApellido.addTextChangedListener(formValidationWatcher);
        etCorreo.addTextChangedListener(formValidationWatcher);
    }

    private void setupClickListeners() {
        btnCancelar.setOnClickListener(v -> finish());

        btnCrearUsuario.setOnClickListener(v -> {
            if (validateAllFields()) {
                if (isModoEdicion) {
                    actualizarUsuario();
                } else {
                    crearUsuario();
                }
            }
        });
    }

    private void validateRut(String rut) {
        if (rut.isEmpty()) {
            layoutRutValidation.setVisibility(View.GONE);
            isRutValid = false;
            tilRut.setError(null);
            validateForm();
            return;
        }

        // Formatear RUT automáticamente
        String formattedRut = formatRut(rut);
        if (!formattedRut.equals(rut)) {
            etRut.removeTextChangedListener((TextWatcher) etRut.getTag());
            etRut.setText(formattedRut);
            etRut.setSelection(formattedRut.length());
            etRut.addTextChangedListener((TextWatcher) this);
        }

        // Validar formato
        if (RUT_PATTERN.matcher(formattedRut).matches()) {
            // Validar dígito verificador
            if (isValidRutDigit(formattedRut)) {
                isRutValid = true;
                layoutRutValidation.setVisibility(View.VISIBLE);
                tvRutValidation.setText("RUT válido");
                tvRutValidation.setTextColor(ContextCompat.getColor(this, R.color.success_color));
                ivRutValidation.setImageResource(R.drawable.ic_check);
                ivRutValidation.setColorFilter(ContextCompat.getColor(this, R.color.success_color));
                tilRut.setError(null);
            } else {
                isRutValid = false;
                layoutRutValidation.setVisibility(View.VISIBLE);
                tvRutValidation.setText("Dígito verificador incorrecto");
                tvRutValidation.setTextColor(ContextCompat.getColor(this, R.color.error_color));
                ivRutValidation.setImageResource(R.drawable.ic_close);
                ivRutValidation.setColorFilter(ContextCompat.getColor(this, R.color.error_color));
            }
        } else {
            isRutValid = false;
            layoutRutValidation.setVisibility(View.GONE);
            tilRut.setError("Formato inválido");
        }

        validateForm();
    }

    private String formatRut(String rut) {
        // Remover caracteres no numéricos excepto K
        rut = rut.replaceAll("[^0-9kK]", "");

        if (rut.length() < 2) return rut;

        // Separar número y dígito verificador
        String number = rut.substring(0, rut.length() - 1);
        String digit = rut.substring(rut.length() - 1);

        // Formatear con puntos y guión
        StringBuilder formatted = new StringBuilder();
        int count = 0;
        for (int i = number.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 3 == 0) {
                formatted.insert(0, ".");
            }
            formatted.insert(0, number.charAt(i));
            count++;
        }

        return formatted.toString() + "-" + digit.toUpperCase();
    }

    private boolean isValidRutDigit(String rut) {
        String[] parts = rut.split("-");
        if (parts.length != 2) return false;

        String number = parts[0].replaceAll("\\.", "");
        String digit = parts[1];

        try {
            int rutNumber = Integer.parseInt(number);
            int sum = 0;
            int multiplier = 2;

            while (rutNumber > 0) {
                sum += (rutNumber % 10) * multiplier;
                rutNumber /= 10;
                multiplier = multiplier == 7 ? 2 : multiplier + 1;
            }

            int remainder = sum % 11;
            String calculatedDigit = remainder == 0 ? "0" : remainder == 1 ? "K" : String.valueOf(11 - remainder);

            return calculatedDigit.equalsIgnoreCase(digit);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void validatePassword(String password) {
        // Validar longitud
        hasMinLength = password.length() >= 8;
        updatePasswordValidationIcon(ivPasswordLength, hasMinLength);

        // Validar número
        hasNumber = PASSWORD_NUMBER.matcher(password).matches();
        updatePasswordValidationIcon(ivPasswordNumber, hasNumber);

        // Validar mayúscula
        hasUppercase = PASSWORD_UPPERCASE.matcher(password).matches();
        updatePasswordValidationIcon(ivPasswordUppercase, hasUppercase);

        isPasswordValid = hasMinLength && hasNumber && hasUppercase;
        validateForm();
    }

    private void updatePasswordValidationIcon(ImageView imageView, boolean isValid) {
        if (isValid) {
            imageView.setImageResource(R.drawable.ic_check);
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.success_color));
        } else {
            imageView.setImageResource(R.drawable.ic_close);
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.error_color));
        }
    }

    private void validateForm() {
        boolean isFormValid = !etNombre.getText().toString().trim().isEmpty() &&
                !etApellido.getText().toString().trim().isEmpty() &&
                isRutValid &&
                isValidEmail(etCorreo.getText().toString().trim()) &&
                (isModoEdicion ? true : isPasswordValid); // En edición, password es opcional

        btnCrearUsuario.setEnabled(isFormValid);
    }

    private boolean isValidEmail(String email) {
        return !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean validateAllFields() {
        boolean isValid = true;

        // Validar nombre
        if (etNombre.getText().toString().trim().isEmpty()) {
            tilNombre.setError("El nombre es requerido");
            isValid = false;
        } else {
            tilNombre.setError(null);
        }

        // Validar apellido
        if (etApellido.getText().toString().trim().isEmpty()) {
            tilApellido.setError("El apellido es requerido");
            isValid = false;
        } else {
            tilApellido.setError(null);
        }

        // Validar email
        String email = etCorreo.getText().toString().trim();
        if (email.isEmpty()) {
            tilCorreo.setError("El correo es requerido");
            isValid = false;
        } else if (!isValidEmail(email)) {
            tilCorreo.setError("Correo inválido");
            isValid = false;
        } else {
            tilCorreo.setError(null);
        }

        // Validar password solo en creación
        if (!isModoEdicion && !isPasswordValid) {
            tilPassword.setError("La contraseña no cumple los requisitos");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        return isValid && isRutValid;
    }

    // 🎯 **MÉTODO ADAPTADO - CREAR USUARIO**
    private void crearUsuario() {
        layoutLoading.setVisibility(View.VISIBLE);
        btnCrearUsuario.setEnabled(false);
        cardSuccess.setVisibility(View.GONE);

        try {
            // Obtener idRol basado en la selección
            String rolSeleccionado = spinnerRol.getText().toString();
            int idRol = getRolId(rolSeleccionado);

            Map<String, Object> params = new HashMap<>();
            params.put("rut", etRut.getText().toString().trim());
            params.put("nombre", etNombre.getText().toString().trim());
            params.put("apellido", etApellido.getText().toString().trim());
            params.put("correo", etCorreo.getText().toString().trim());
            params.put("contrasena", etPassword.getText().toString());
            params.put("id_rol", idRol);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    ApiEndpoints.USUARIOS_CREATE,
                    new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            layoutLoading.setVisibility(View.GONE);
                            try {
                                if (response.getBoolean("success")) {
                                    mostrarMensajeExito("Usuario creado exitosamente");
                                    limpiarFormulario();
                                } else {
                                    btnCrearUsuario.setEnabled(true);
                                    String error = response.getString("message");
                                    Toast.makeText(AnadirUsuarioActivity.this,
                                            "Error: " + error,
                                            Toast.LENGTH_LONG).show();
                                }
                            } catch (JSONException e) {
                                btnCrearUsuario.setEnabled(true);
                                Toast.makeText(AnadirUsuarioActivity.this,
                                        "Error al procesar respuesta",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            layoutLoading.setVisibility(View.GONE);
                            btnCrearUsuario.setEnabled(true);
                            Toast.makeText(AnadirUsuarioActivity.this,
                                    "Error de conexión: " + error.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
            );

            ApiClient.getInstance(this).addToRequestQueue(request);

        } catch (JSONException e) {
            layoutLoading.setVisibility(View.GONE);
            btnCrearUsuario.setEnabled(true);
            Toast.makeText(this, "Error al crear petición", Toast.LENGTH_SHORT).show();
        }
    }

    // 🎯 **MÉTODO ADAPTADO - ACTUALIZAR USUARIO**
    private void actualizarUsuario() {
        layoutLoading.setVisibility(View.VISIBLE);
        btnCrearUsuario.setEnabled(false);
        cardSuccess.setVisibility(View.GONE);

        try {
            // Obtener idRol basado en la selección
            String rolSeleccionado = spinnerRol.getText().toString();
            int idRol = getRolId(rolSeleccionado);

            Map<String, Object> params = new HashMap<>();
            params.put("id_usuario", usuarioEdicion.getIdUsuario());
            params.put("rut", etRut.getText().toString().trim());
            params.put("nombre", etNombre.getText().toString().trim());
            params.put("apellido", etApellido.getText().toString().trim());
            params.put("correo", etCorreo.getText().toString().trim());
            params.put("id_rol", idRol);

            // Solo enviar password si no está vacío
            String password = etPassword.getText().toString();
            if (!password.isEmpty()) {
                params.put("contrasena", password);
            }

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    ApiEndpoints.USUARIOS_UPDATE,
                    new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            layoutLoading.setVisibility(View.GONE);
                            try {
                                if (response.getBoolean("success")) {
                                    mostrarMensajeExito("Usuario actualizado exitosamente");
                                    setResult(RESULT_OK);
                                    finish(); // Cerrar actividad después de actualizar
                                } else {
                                    btnCrearUsuario.setEnabled(true);
                                    String error = response.getString("message");
                                    Toast.makeText(AnadirUsuarioActivity.this,
                                            "Error: " + error,
                                            Toast.LENGTH_LONG).show();
                                }
                            } catch (JSONException e) {
                                btnCrearUsuario.setEnabled(true);
                                Toast.makeText(AnadirUsuarioActivity.this,
                                        "Error al procesar respuesta",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            layoutLoading.setVisibility(View.GONE);
                            btnCrearUsuario.setEnabled(true);
                            Toast.makeText(AnadirUsuarioActivity.this,
                                    "Error de conexión: " + error.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
            );

            ApiClient.getInstance(this).addToRequestQueue(request);

        } catch (JSONException e) {
            layoutLoading.setVisibility(View.GONE);
            btnCrearUsuario.setEnabled(true);
            Toast.makeText(this, "Error al crear petición", Toast.LENGTH_SHORT).show();
        }
    }

    // 🎯 **MÉTODO PARA OBTENER ID DEL ROL**
    private int getRolId(String rolTexto) {
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equals(rolTexto)) {
                return rolesIds[i];
            }
        }
        return 2; // Default: Empleado
    }

    // 🏗️ **MENÚ DE NAVEGACIÓN**
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_usuarios) {
            // Ya estamos en usuarios
            return true;
        } else if (id == R.id.menu_justificadores) {
            // Ir a JustificadoresActivity
            // Intent intent = new Intent(this, JustificadoresActivity.class);
            // startActivity(intent);
            return true;
        } else if (id == R.id.menu_reportes) {
            // Ir a ReportesActivity
            // Intent intent = new Intent(this, ReportesActivity.class);
            // startActivity(intent);
            return true;
        } else if (id == R.id.menu_cerrar_sesion) {
            // Cerrar sesión
            finishAffinity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void mostrarMensajeExito(String mensaje) {
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        tvSuccessMessage.setText(mensaje);
        cardSuccess.setVisibility(View.VISIBLE);
    }

    private void limpiarFormulario() {
        if (!isModoEdicion) {
            etNombre.setText("");
            etApellido.setText("");
            etRut.setText("");
            etCorreo.setText("");
            etPassword.setText("");
            spinnerRol.setText(roles[0], false);

            // Limpiar errores
            tilNombre.setError(null);
            tilApellido.setError(null);
            tilRut.setError(null);
            tilCorreo.setError(null);
            tilPassword.setError(null);

            // Resetear validaciones
            layoutRutValidation.setVisibility(View.GONE);
            isRutValid = false;
            isPasswordValid = false;
            hasMinLength = false;
            hasNumber = false;
            hasUppercase = false;

            updatePasswordValidationIcon(ivPasswordLength, false);
            updatePasswordValidationIcon(ivPasswordNumber, false);
            updatePasswordValidationIcon(ivPasswordUppercase, false);

            btnCrearUsuario.setEnabled(false);

            // Ocultar mensaje de éxito después de 5 segundos
            cardSuccess.postDelayed(() -> cardSuccess.setVisibility(View.GONE), 5000);
        }
    }
}