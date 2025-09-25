package com.example.relojcontrol.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.relojcontrol.R;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    // Constants
    private static final String PREFS_NAME = "RelojControl";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_ROL = "user_role";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    // Views
    private ImageView ivLogo;
    private TextInputLayout tilUsuario, tilPassword;
    private TextInputEditText etUsuario, etPassword;
    private TextView tvForgotPassword, tvErrorMessage;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;

    // Variables
    private SharedPreferences sharedPreferences;
    private boolean isValidForm = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        initSharedPreferences();
        setupValidation();
        setupClickListeners();

        // Verificar si ya está logueado
        checkExistingSession();
    }

    private void initViews() {
        ivLogo = findViewById(R.id.iv_logo);
        tilUsuario = findViewById(R.id.til_usuario);
        tilPassword = findViewById(R.id.til_password);
        etUsuario = findViewById(R.id.et_usuario);
        etPassword = findViewById(R.id.et_password);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private void setupValidation() {
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etUsuario.addTextChangedListener(validationWatcher);
        etPassword.addTextChangedListener(validationWatcher);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            if (validateAllFields()) {
                performLogin();
            }
        });

        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Funcionalidad en desarrollo", Toast.LENGTH_SHORT).show();
        });
    }

    private void validateForm() {
        String usuario = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        isValidForm = !usuario.isEmpty() &&
                password.length() >= 6 &&
                Patterns.EMAIL_ADDRESS.matcher(usuario).matches();

        btnLogin.setEnabled(isValidForm);
        hideErrorMessage();
    }

    private boolean validateAllFields() {
        boolean isValid = true;
        String usuario = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validar usuario (solo email según tu API)
        if (usuario.isEmpty()) {
            tilUsuario.setError("El correo es requerido");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(usuario).matches()) {
            tilUsuario.setError("Ingrese un correo válido");
            isValid = false;
        } else {
            tilUsuario.setError(null);
        }

        // Validar contraseña
        if (password.isEmpty()) {
            tilPassword.setError("La contraseña es requerida");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Mínimo 6 caracteres");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        return isValid;
    }

    private void performLogin() {
        showLoading(true);
        hideErrorMessage();

        String correo = etUsuario.getText().toString().trim();
        String contrasena = etPassword.getText().toString().trim();

        // Crear parámetros según tu API PHP
        Map<String, String> params = new HashMap<>();
        params.put("correo", correo);
        params.put("contrasena", contrasena);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiEndpoints.LOGIN,
                new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        showLoading(false);
                        try {
                            // Estructura según tu API PHP corregida
                            if (response.getBoolean("success")) {
                                // Login exitoso
                                JSONObject userData = response.getJSONObject("data").getJSONObject("usuario");
                                handleLoginSuccess(userData);
                            } else {
                                // Error de credenciales
                                String errorMessage = response.getString("message");
                                showErrorMessage(errorMessage != null ? errorMessage : "Credenciales incorrectas");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            showErrorMessage("Error al procesar la respuesta del servidor");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showLoading(false);
                        String errorMessage = "Error de conexión";

                        if (error.networkResponse != null) {
                            switch (error.networkResponse.statusCode) {
                                case 401:
                                    errorMessage = "Credenciales incorrectas";
                                    break;
                                case 500:
                                    errorMessage = "Error interno del servidor";
                                    break;
                                default:
                                    errorMessage = "Error: " + error.networkResponse.statusCode;
                            }
                        }

                        showErrorMessage(errorMessage);
                    }
                }
        );

        // Agregar a la cola de Volley
        ApiClient.getInstance(this).addToRequestQueue(request);
    }

    private void handleLoginSuccess(JSONObject userData) throws JSONException {
        // Guardar datos de sesión según tu estructura de BD
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userData.getInt("id_usuario"));
        editor.putString(KEY_USER_NAME, userData.getString("nombre") + " " + userData.getString("apellido"));
        editor.putString(KEY_USER_EMAIL, userData.getString("correo"));
        editor.putInt(KEY_USER_ROL, userData.getInt("id_rol"));
        editor.apply();

        // Navegar a la actividad principal según el rol
        navigateBasedOnRole(userData.getInt("id_rol"));
    }

    private void navigateBasedOnRole(int idRol) {
        Intent intent;

        if (idRol == 1) { // Administrador (según tu BD)
            intent = new Intent(this, MainAdminActivity.class);
        } else { // Empleado
            intent = new Intent(this, MainEmpleadoActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnLogin.setText(show ? "Ingresando..." : "Ingresar");

        if (show) {
            hideErrorMessage();
        }
    }

    private void showErrorMessage(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void hideErrorMessage() {
        tvErrorMessage.setVisibility(View.GONE);
    }

    private void checkExistingSession() {
        if (sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            int userRole = sharedPreferences.getInt(KEY_USER_ROL, -1);
            navigateBasedOnRole(userRole);
        }
    }

    public void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}