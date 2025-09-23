package com.example.relojcontrol.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

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

    // Patrones de validación
    private static final Pattern RUT_PATTERN = Pattern.compile("^[0-9]+-[0-9kK]$");
    private static final Pattern PASSWORD_NUMBER = Pattern.compile(".*[0-9].*");
    private static final Pattern PASSWORD_UPPERCASE = Pattern.compile(".*[A-Z].*");

    // Roles disponibles
    private String[] roles = {"Empleado", "Administrador"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anadir_usuario);

        initViews();
        setupToolbar();
        setupRolSpinner();
        setupValidation();
        setupClickListeners();
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

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        spinnerRol.setText(roles[0], false); // Default to "Empleado"
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

        // Password validation
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
                crearUsuario();
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
            etRut.addTextChangedListener((TextWatcher) etRut.getTag());
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

            return calculatedDigit.equals(digit);
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
                isPasswordValid;

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

        return isValid && isRutValid && isPasswordValid;
    }

    private void crearUsuario() {
        // Mostrar loading
        layoutLoading.setVisibility(View.VISIBLE);
        btnCrearUsuario.setEnabled(false);
        cardSuccess.setVisibility(View.GONE);

        // Crear objeto usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(etNombre.getText().toString().trim());
        usuario.setApellido(etApellido.getText().toString().trim());
        usuario.setRut(etRut.getText().toString().trim());
        usuario.setCorreo(etCorreo.getText().toString().trim());
        usuario.setPassword(etPassword.getText().toString());
        usuario.setRol(spinnerRol.getText().toString());

        // Llamada a la API
        ApiEndpoints apiService = ApiClient.getClient().create(ApiEndpoints.class);
        Call<Usuario> call = apiService.crearUsuario(usuario);

        call.enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                layoutLoading.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    // Éxito
                    mostrarMensajeExito();
                    limpiarFormulario();
                } else {
                    // Error del servidor
                    btnCrearUsuario.setEnabled(true);
                    Toast.makeText(AnadirUsuarioActivity.this,
                            "Error al crear usuario: " + response.message(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                layoutLoading.setVisibility(View.GONE);
                btnCrearUsuario.setEnabled(true);
                Toast.makeText(AnadirUsuarioActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarMensajeExito() {
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        tvSuccessMessage.setText("El usuario " + nombre + " " + apellido + " ha sido registrado exitosamente en el sistema.");
        cardSuccess.setVisibility(View.VISIBLE);
    }

    private void limpiarFormulario() {
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