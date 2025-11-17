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
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

// Firebase imports
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Usuario;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // Constants
    private static final String PREFS_NAME = "RelojControl";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_ROL = "user_role";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    // Views - Coherentes con tu XML
    private ImageView ivLogo;
    private TextInputLayout tilUsuario, tilPassword;
    private TextInputEditText etUsuario, etPassword;
    private TextView tvForgotPassword, tvErrorMessage;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private SharedPreferences sharedPreferences;
    private boolean isValidForm = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // INICIALIZAR FIREBASE
        try {
            FirebaseApp.initializeApp(this);
            Log.d("Firebase", "✓ Firebase inicializado correctamente");
        } catch (Exception e) {
            Log.e("Firebase", "✗ Error inicializando Firebase", e);
            Toast.makeText(this, "Error de configuración Firebase", Toast.LENGTH_LONG).show();
        }

        // Inicializar Firebase
        initFirebase();
        initViews();
        initSharedPreferences();
        setupValidation();
        setupClickListeners();

        // Verificar si ya está logueado
        checkExistingSession();

        // TEST: Probar conexión Firebase
        testFirebaseConnection();
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        Log.d(TAG, "Firebase inicializado correctamente");
    }

    private void testFirebaseConnection() {
        Log.d(TAG, "=== TESTING FIREBASE CONNECTION ===");

        // VERIFICAR AUTENTICACIÓN PRIMERO
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG, "Usuario actual: " + (currentUser != null ? currentUser.getUid() : "No autenticado"));

        mDatabase.child("test").setValue("Firebase conectado: " + System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ FIREBASE: Conexión exitosa");
                    Toast.makeText(this, "Firebase conectado ✓", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ FIREBASE: Error de conexión", e);
                    // MOSTRAR ERROR DETALLADO:
                    String errorDetail = e.getClass().getSimpleName() + ": " + e.getMessage();
                    Toast.makeText(this, "Error Firebase: " + errorDetail, Toast.LENGTH_LONG).show();
                });

        // VERIFICAR CONEXIÓN A INTERNET
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            Log.e(TAG, "✗ No hay conexión a internet");
            Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
            return;
        }

        // VERIFICAR ESTADO DE FIREBASE
        FirebaseDatabase.getInstance().goOnline(); // Forzar conexión

        mDatabase.child("test").setValue("Firebase conectado: " + System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ FIREBASE: Conexión exitosa");
                    Toast.makeText(this, "Firebase conectado ✓", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ FIREBASE: Error de conexión", e);
                    Toast.makeText(this, "Error Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void initViews() {
        // IDs exactos de XML
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
                performFirebaseLogin();
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

        // Validación solo de email
        isValidForm = !usuario.isEmpty() &&
                password.length() >= 6 && // Firebase requiere mínimo 6 caracteres
                Patterns.EMAIL_ADDRESS.matcher(usuario).matches();

        btnLogin.setEnabled(isValidForm);
        hideErrorMessage();
    }

    private boolean validateAllFields() {
        boolean isValid = true;
        String usuario = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        Log.d(TAG, "Validando campos - Usuario: " + usuario + ", Password length: " + password.length());

        // Validar usuario (solo email)
        if (usuario.isEmpty()) {
            tilUsuario.setError("El correo es requerido");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(usuario).matches()) {
            tilUsuario.setError("Ingrese un correo válido");
            isValid = false;
        } else {
            tilUsuario.setError(null);
        }

        // Validar contraseña (Firebase requiere mínimo 6 caracteres)
        if (password.isEmpty()) {
            tilPassword.setError("La contraseña es requerida");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Mínimo 6 caracteres");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        Log.d(TAG, "Validación completada - isValid: " + isValid);
        return isValid;
    }

    private void performFirebaseLogin() {
        Log.d(TAG, "=== INICIANDO LOGIN CON FIREBASE ===");

        showLoading(true);
        hideErrorMessage();

        String email = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        Log.d(TAG, "Intentando login con email: " + email);

        // Autenticación con Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "✓ Autenticación Firebase exitosa");
                    FirebaseUser firebaseUser = authResult.getUser();

                    if (firebaseUser != null) {
                        String userId = firebaseUser.getUid();
                        Log.d(TAG, "User ID: " + userId);

                        // Obtener datos del usuario desde Realtime Database
                        getUserDataFromDatabase(userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Error en autenticación Firebase", e);
                    showLoading(false);

                    String errorMessage = "Error de autenticación";

                    // Manejo específico de errores Firebase
                    String errorCode = e.getMessage();
                    if (errorCode != null) {
                        if (errorCode.contains("password is invalid")) {
                            errorMessage = "Contraseña incorrecta";
                        } else if (errorCode.contains("no user record")) {
                            errorMessage = "Usuario no encontrado";
                        } else if (errorCode.contains("network error")) {
                            errorMessage = "Error de conexión";
                        } else if (errorCode.contains("too-many-requests")) {
                            errorMessage = "Demasiados intentos. Intenta más tarde";
                        }
                    }

                    Log.e(TAG, "Mensaje de error: " + errorMessage);
                    showErrorMessage(errorMessage);
                });
    }

    private void getUserDataFromDatabase(String userId) {
        Log.d(TAG, "Obteniendo datos del usuario desde database: " + userId);

        mDatabase.child("usuarios").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Log.d(TAG, "✓ Datos del usuario obtenidos");
                        showLoading(false);

                        if (dataSnapshot.exists()) {
                            try {
                                Usuario usuario = dataSnapshot.getValue(Usuario.class);
                                if (usuario != null) {
                                    Log.d(TAG, "Usuario: " + usuario.getNombre());
                                    handleLoginSuccess(usuario, userId);
                                } else {
                                    Log.e(TAG, "Error: usuario es null");
                                    showErrorMessage("Error obteniendo datos del usuario");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parseando datos del usuario", e);
                                showErrorMessage("Error procesando datos del usuario");
                            }
                        } else {
                            Log.w(TAG, "No existen datos para este usuario en la database");
                            showErrorMessage("Usuario no encontrado en el sistema");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "✗ Error obteniendo datos del usuario", error.toException());
                        showLoading(false);
                        showErrorMessage("Error de conexión con la base de datos");
                    }
                });
    }

    private void handleLoginSuccess(Usuario usuario, String userId) {
        Log.d(TAG, "=== MANEJANDO LOGIN EXITOSO ===");

        // Guardar datos de sesión
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, usuario.getNombre() + " " + usuario.getApellido());
        editor.putString(KEY_USER_EMAIL, usuario.getCorreo());

        // DEBUG ROL - Verificar qué rol tiene
        int idRol = usuario.getIdRol();
        Log.d(TAG, "ROL DEBUG - Usuario: " + usuario.getNombre());
        Log.d(TAG, "ROL DEBUG - ID Rol: " + idRol);
        Log.d(TAG, "ROL DEBUG - ¿Es Admin? " + (idRol == 1));

        editor.putInt(KEY_USER_ROL, idRol);
        editor.apply();

        Log.d(TAG, "Datos guardados en SharedPreferences");
        Log.d(TAG, "ID Usuario: " + userId);
        Log.d(TAG, "Rol: " + (idRol == 1 ? "Administrador" : "Empleado"));

        // Navegar según el rol
        navigateBasedOnRole(idRol);
    }


    private void navigateBasedOnRole(int idRol) {
        Log.d(TAG, "Navegando según rol: " + idRol);

        Intent intent;

        if (idRol == 1) { // Administrador
            Log.d(TAG, "Navegando a MainAdminActivity");
            intent = new Intent(this, MainAdminActivity.class);
        } else if (idRol == 2) { // Empleado
            Log.d(TAG, "Navegando a MainEmpleadoActivity");
            intent = new Intent(this, MainEmpleadoActivity.class);
        } else {
            //por defecto, enviar a empleado si hay rol desconocido
            Log.w(TAG, "Rol desconocido: "+ idRol);
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
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                int userRole = sharedPreferences.getInt(KEY_USER_ROL, -1);
                Log.d(TAG, "Sesión Firebase activa, rol: " + userRole);
                navigateBasedOnRole(userRole);
            } else {
                // Limpiar sesión si no hay usuario en Firebase
                logout();
            }
        }
    }

    public void logout() {
        Log.d(TAG, "Cerrando sesión");

        // Cerrar sesión en Firebase
        mAuth.signOut();

        // Limpiar SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
