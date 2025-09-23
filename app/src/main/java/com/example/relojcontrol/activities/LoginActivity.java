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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Usuario;
import com.example.relojcontrol.network.ApiClient;
import com.example.relojcontrol.network.ApiEndpoints;

import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    // Constants
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_ROL = "user_rol";
    private static final String KEY_USER_NOMBRE = "user_nombre";
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

    // Patrón para validar RUT chileno
    private static final Pattern RUT_PATTERN = Pattern.compile("^[0-9]+-[0-9kK]$");

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
            // TODO: Implementar recuperación de contraseña
            Toast.makeText(this, "Funcionalidad en desarrollo", Toast.LENGTH_SHORT).show();
        });
    }

    private void validateForm() {
        String usuario = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        isValidForm = !usuario.isEmpty() &&
                password.length() >= 6 &&
                (isValidEmail(usuario) || isValidRut(usuario));

        btnLogin.setEnabled(isValidForm);
        hideErrorMessage();
    }

    private boolean validateAllFields() {
        boolean isValid = true;
        String usuario = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validar usuario
        if (usuario.isEmpty()) {
            tilUsuario.setError("El usuario es requerido");
            isValid = false;
        } else if (!isValidEmail(usuario) && !isValidRut(usuario)) {
            tilUsuario.setError("Ingrese un correo válido o RUT (ej: 12345678-9)");
            isValid = false;
        } else {
            tilUsuario.setError(null);
        }

        // Validar contraseña
        if (password.isEmpty()) {
            tilPassword.setError("La contraseña es requerida");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("La contraseña debe tener al menos 6 caracteres");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        return isValid;
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidRut(String rut) {
        if (!RUT_PATTERN.matcher(rut).matches()) {
            return false;
        }

        // Validar dígito verificador
        return isValidRutDigit(rut);
    }

    private boolean isValidRutDigit(String rut) {
        try {
            String[] parts = rut.split("-");
            if (parts.length != 2) return false;

            String number = parts[0].replaceAll("\\.", "");
            String digit = parts[1].toUpperCase();

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

            return calculatedDigit.equals(digit);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void performLogin() {
        showLoading(true);
        hideErrorMessage();

        String usuario = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Crear objeto de login request
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsuario(usuario);
        loginRequest.setPassword(password);

        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<LoginResponse> call = apiService.login(loginRequest);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.isExitoso()) {
                        // Login exitoso
                        saveUserSession(loginResponse.getUsuario());
                        navigateToMainActivity(loginResponse.getUsuario());
                    } else {
                        // Credenciales incorrectas
                        showErrorMessage(loginResponse.getMensaje() != null ?
                                loginResponse.getMensaje() : "Credenciales incorrectas");
                    }
                } else {
                    // Error del servidor
                    if (response.code() == 401) {
                        showErrorMessage("Usuario o contraseña incorrectos");
                    } else {
                        showErrorMessage("Error del servidor. Intente nuevamente.");
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showLoading(false);
                showErrorMessage("Error de conexión. Verifique su conexión a internet.");
            }
        });
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            btnLogin.setText("Ingresando...");
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(isValidForm);
            btnLogin.setText("Ingresar");
        }
    }

    private void showErrorMessage(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void hideErrorMessage() {
        tvErrorMessage.setVisibility(View.GONE);
    }

    private void saveUserSession(Usuario usuario) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, usuario.getId());
        editor.putString(KEY_USER_ROL, usuario.getRol());
        editor.putString(KEY_USER_NOMBRE, usuario.getNombre() + " " + usuario.getApellido());
        editor.apply();
    }

    private void navigateToMainActivity(Usuario usuario) {
        Intent intent;

        if ("Administrador".equals(usuario.getRol())) {
            intent = new Intent(this, MainAdminActivity.class);
        } else {
            intent = new Intent(this, MainEmpleadoActivity.class);
        }

        intent.putExtra("usuario", usuario);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void checkExistingSession() {
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);

        if (isLoggedIn) {
            String rol = sharedPreferences.getString(KEY_USER_ROL, "");
            Intent intent;

            if ("Administrador".equals(rol)) {
                intent = new Intent(this, MainAdminActivity.class);
            } else {
                intent = new Intent(this, MainEmpleadoActivity.class);
            }

            startActivity(intent);
            finish();
        }
    }

    public void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // Clases auxiliares para las requests/responses
    private static class LoginRequest {
        private String usuario;
        private String password;

        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    private static class LoginResponse {
        private boolean exitoso;
        private String mensaje;
        private Usuario usuario;
        private String token;

        public boolean isExitoso() { return exitoso; }
        public void setExitoso(boolean exitoso) { this.exitoso = exitoso; }
        public String getMensaje() { return mensaje; }
        public void setMensaje(String mensaje) { this.mensaje = mensaje; }
        public Usuario getUsuario() { return usuario; }
        public void setUsuario(Usuario usuario) { this.usuario = usuario; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}