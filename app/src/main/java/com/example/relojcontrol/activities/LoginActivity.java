package com.example.relojcontrol.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.relojcontrol.R;
import com.example.relojcontrol.activities.empleado.MainEmpleadoActivity;
import com.example.relojcontrol.models.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "RelojControl";

    // Views del nuevo XML
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Inicializar Vistas 
        initViews();

        // Verificar sesión activa
        checkSesionActiva();

        // Listener del botón
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    private void checkSesionActiva() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Si ya hay usuario, verificamos su rol y redirigimos
            checkUserRole(currentUser.getUid());
        }
    }

    private void attemptLogin() {
        // Resetear errores visuales
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // Validación simple
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("La contraseña es requerida");
            focusView = etPassword;
            cancel = true;
        }

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("El correo es requerido");
            focusView = etEmail;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            // Iniciar sesión en Firebase
            performFirebaseLogin(email, password);
        }
    }

    private void performFirebaseLogin(String email, String password) {
        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserRole(user.getUid());
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, "Autenticación fallida.",
                                Toast.LENGTH_SHORT).show();
                        tilPassword.setError("Credenciales incorrectas");
                    }
                });
    }

    private void checkUserRole(String uid) {
        // rol que tiene este usuario en BD
        mDatabase.child("usuarios").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showLoading(false);
                if (snapshot.exists()) {
                    Usuario usuario = snapshot.getValue(Usuario.class);
                    if (usuario != null) {
                        // Guardar sesión localmente
                        saveSession(usuario);
                        // Redirigir según rol
                        redirectBasedOnRole(usuario.getIdRol());
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Error: Usuario no encontrado en base de datos", Toast.LENGTH_SHORT).show();
                    mAuth.signOut(); // Cerrar sesión fantasma
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Log.e(TAG, "Error checking role", error.toException());
            }
        });
    }

    private void saveSession(Usuario usuario) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("user_uid", mAuth.getCurrentUser().getUid());
        editor.putString("user_name", usuario.getNombre() + " " + usuario.getApellido());
        editor.putInt("user_role", usuario.getIdRol());
        //guardado de id numerico para las consultas de asistencia
        editor.putInt("user_id_num", usuario.getIdUsuario());
        editor.apply();
    }

    private void redirectBasedOnRole(int roleId) {
        Intent intent;
        if (roleId == 1) { // 1 = Admin
            intent = new Intent(LoginActivity.this, MainAdminActivity.class);
        } else { // 2 = Empleado
            intent = new Intent(LoginActivity.this, MainEmpleadoActivity.class);
        }
        // Limpiar pila para que no pueda volver atrás al login
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
    }
}