package com.example.relojcontrol.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.relojcontrol.R;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity"; // Para logs

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

        Log.d(TAG, "=== LoginActivity iniciada ===");

        initViews();
        initSharedPreferences();
        setupValidation();
        setupClickListeners();

        // Verificar si ya está logueado
        checkExistingSession();

        // TEST: Probar conectividad al crear la actividad
        testEmulatorConnectivity();
        testConnectivity();
    }

    // METODO NUEVO: Probar conectividad del emulador
    private void testEmulatorConnectivity() {
        Log.d(TAG, "=== TESTING EMULATOR CONNECTIVITY ===");

        // Test 1: Conexión a internet general
        String internetTestUrl = "http://www.google.com";

        // Test 2: Conexión a tu servidor local
        String localTestUrl = "http://10.0.2.2/asistencia-api/api/auth/simple_test.php";

        StringRequest internetTest = new StringRequest(
                Request.Method.GET,
                internetTestUrl,
                response -> Log.d(TAG, "✓ INTERNET: Conectado a internet"),
                error -> Log.e(TAG, "✗ INTERNET: No hay conexión a internet")
        );

        StringRequest localTest = new StringRequest(
                Request.Method.GET,
                localTestUrl,
                response -> Log.d(TAG, "✓ LOCAL: Conectado al servidor local"),
                error -> Log.e(TAG, "✗ LOCAL: No se puede acceder al servidor local")
        );

        ApiClient.getInstance(this).addToRequestQueue(internetTest);
        ApiClient.getInstance(this).addToRequestQueue(localTest);
    }

    // METODO MEJORADO: Probar conectividad con la API
    private void testConnectivity() {
        Log.d(TAG, "=== PROBANDO CONECTIVIDAD CON API ===");
        Log.d(TAG, "URL de LOGIN: " + ApiEndpoints.LOGIN);
        Log.d(TAG, "URL de TEST: " + ApiEndpoints.SIMPLE_TEST);

        // Usa la constante corregida
        JsonObjectRequest testRequest = new JsonObjectRequest(
                Request.Method.GET,
                ApiEndpoints.SIMPLE_TEST, // ¡Usa la constante directa!
                null,
                response -> {
                    Log.d(TAG, "✓ API CONECTADA: " + response.toString());
                    Toast.makeText(this, "Servidor conectado ✓", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    Log.e(TAG, "✗ ERROR DE CONEXIÓN A LA API");
                    analyzeConnectionError(error);
                    Toast.makeText(this, "Error de conexión ✗", Toast.LENGTH_LONG).show();
                }
        );

        ApiClient.getInstance(this).addToRequestQueue(testRequest);
    }

    // METODO NUEVO: Análisis detallado de errores de conexión
    private void analyzeConnectionError(VolleyError error) {
        Log.e(TAG, "=== ANÁLISIS DETALLADO DE ERROR ===");

        if (error instanceof TimeoutError) {
            Log.e(TAG, "Timeout - El servidor no responde en el tiempo esperado");
        } else if (error instanceof NoConnectionError) {
            Log.e(TAG, "No hay conexión de red disponible");
        } else if (error instanceof AuthFailureError) {
            Log.e(TAG, "Error de autenticación en el servidor");
        } else if (error instanceof ServerError) {
            Log.e(TAG, "Error interno del servidor (500)");
        } else if (error instanceof NetworkError) {
            Log.e(TAG, "Error general de red");
        } else if (error instanceof ParseError) {
            Log.e(TAG, "Error parseando la respuesta del servidor");
        } else {
            Log.e(TAG, "Error desconocido: " + error.getClass().getSimpleName());
        }

        if (error.networkResponse != null) {
            Log.e(TAG, "Status Code: " + error.networkResponse.statusCode);
            Log.e(TAG, "Headers: " + error.networkResponse.headers);
            try {
                String responseBody = new String(error.networkResponse.data, "utf-8");
                Log.e(TAG, "Response Body: " + responseBody);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Error leyendo response body", e);
            }
        } else {
            Log.e(TAG, "No hay networkResponse - Error de conexión");
            if (error.getCause() != null) {
                Log.e(TAG, "Causa del error: " + error.getCause().toString());
            }
        }

        Log.e(TAG, "Mensaje de error: " + error.getMessage());
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

        Log.d(TAG, "Views inicializadas correctamente");
    }

    private void initSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Log.d(TAG, "SharedPreferences inicializado");
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
            Log.d(TAG, "Botón login presionado");
            if (validateAllFields()) {
                performLogin();
            } else {
                Log.w(TAG, "Validación de campos falló");
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
                password.length() >= 2 &&
                Patterns.EMAIL_ADDRESS.matcher(usuario).matches();

        btnLogin.setEnabled(isValidForm);
        hideErrorMessage();
    }

    private boolean validateAllFields() {
        boolean isValid = true;
        String usuario = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        Log.d(TAG, "Validando campos - Usuario: " + usuario + ", Password length: " + password.length());

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
        } else if (password.length() < 4) {
            tilPassword.setError("Mínimo 4 caracteres");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        Log.d(TAG, "Validación completada - isValid: " + isValid);
        return isValid;
    }

    private void performLogin() {
        Log.d(TAG, "=== INICIANDO PROCESO DE LOGIN ===");

        showLoading(true);
        hideErrorMessage();

        String correo = etUsuario.getText().toString().trim();
        String contrasena = etPassword.getText().toString().trim();

        Log.d(TAG, "Credenciales - Email: " + correo + ", Password: [OCULTA]");
        Log.d(TAG, "URL destino: " + ApiEndpoints.LOGIN);

        // Crear parametros segun tu API PHP
        Map<String, String> params = new HashMap<>();
        params.put("correo", correo);
        params.put("contrasena", contrasena);

        Log.d(TAG, "Parámetros preparados: " + params.keySet());

        try {
            JSONObject jsonParams = new JSONObject(params);
            Log.d(TAG, "JSON a enviar: " + jsonParams.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    ApiEndpoints.LOGIN,
                    jsonParams,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d(TAG, "=== RESPUESTA RECIBIDA ===");
                            Log.d(TAG, "Response completa: " + response.toString());

                            showLoading(false);
                            try {
                                // Estructura según tu API PHP corregida
                                if (response.getBoolean("success")) {
                                    Log.d(TAG, "✓ Login exitoso según respuesta");
                                    // Login exitoso
                                    JSONObject userData = response.getJSONObject("data").getJSONObject("usuario");
                                    Log.d(TAG, "Datos usuario: " + userData.toString());
                                    handleLoginSuccess(userData);
                                } else {
                                    Log.w(TAG, "✗ Login falló según respuesta");
                                    // Error de credenciales
                                    String errorMessage = response.getString("message");
                                    Log.w(TAG, "Mensaje de error: " + errorMessage);
                                    showErrorMessage(errorMessage != null ? errorMessage : "Credenciales incorrectas");
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parseando JSON de respuesta", e);
                                e.printStackTrace();
                                showErrorMessage("Error al procesar la respuesta del servidor");
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "=== ERROR EN PETICIÓN DE LOGIN ===");
                            analyzeConnectionError(error); // Usar el metodo mejorado

                            showLoading(false);
                            String errorMessage = "Error de conexión";

                            // Manejo específico de errores
                            if (error instanceof NoConnectionError || error instanceof TimeoutError) {
                                errorMessage = "Verifica tu conexión a internet";
                            } else if (error.networkResponse != null) {
                                switch (error.networkResponse.statusCode) {
                                    case 401:
                                        errorMessage = "Credenciales incorrectas";
                                        break;
                                    case 500:
                                        errorMessage = "Error interno del servidor";
                                        break;
                                    case 404:
                                        errorMessage = "Servicio no encontrado";
                                        break;
                                    default:
                                        errorMessage = "Error: " + error.networkResponse.statusCode;
                                }
                            }

                            Log.e(TAG, "Mensaje final de error: " + errorMessage);
                            showErrorMessage(errorMessage);
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept", "application/json");
                    return headers;
                }
            };

            Log.d(TAG, "Request creado, agregando a cola...");
            // Agregar a la cola de Volley
            ApiClient.getInstance(this).addToRequestQueue(request);
            Log.d(TAG, "Request agregado a cola de Volley");

        } catch (Exception e) {
            Log.e(TAG, "Error creando request", e);
            showLoading(false);
            showErrorMessage("Error preparando la petición");
        }
    }

    private void handleLoginSuccess(JSONObject userData) throws JSONException {
        Log.d(TAG, "=== MANEJANDO LOGIN EXITOSO ===");

        // Guardar datos de sesión según tu estructura de BD
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userData.getInt("id_usuario"));
        editor.putString(KEY_USER_NAME, userData.getString("nombre") + " " + userData.getString("apellido"));
        editor.putString(KEY_USER_EMAIL, userData.getString("correo"));
        editor.putInt(KEY_USER_ROL, userData.getInt("id_rol"));
        editor.apply();

        Log.d(TAG, "Datos guardados en SharedPreferences");
        Log.d(TAG, "ID Usuario: " + userData.getInt("id_usuario"));
        Log.d(TAG, "Rol: " + userData.getInt("id_rol"));

        // Navegar a la actividad principal según el rol
        navigateBasedOnRole(userData.getInt("id_rol"));
    }

    private void navigateBasedOnRole(int idRol) {
        Log.d(TAG, "Navegando según rol: " + idRol);

        Intent intent;

        if (idRol == 1) { // Administrador (según tu BD)
            Log.d(TAG, "Navegando a MainAdminActivity");
            intent = new Intent(this, MainAdminActivity.class);
        } else { // Empleado
            Log.d(TAG, "Navegando a MainEmpleadoActivity");
            intent = new Intent(this, MainEmpleadoActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        Log.d(TAG, "ShowLoading: " + show);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnLogin.setText(show ? "Ingresando..." : "Ingresar");

        if (show) {
            hideErrorMessage();
        }
    }

    private void showErrorMessage(String message) {
        Log.d(TAG, "Mostrando error: " + message);
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void hideErrorMessage() {
        tvErrorMessage.setVisibility(View.GONE);
    }

    private void checkExistingSession() {
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        Log.d(TAG, "Verificando sesión existente: " + isLoggedIn);

        if (isLoggedIn) {
            int userRole = sharedPreferences.getInt(KEY_USER_ROL, -1);
            Log.d(TAG, "Sesión existente encontrada, rol: " + userRole);
            navigateBasedOnRole(userRole);
        }
    }

    public void logout() {
        Log.d(TAG, "Cerrando sesión");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}