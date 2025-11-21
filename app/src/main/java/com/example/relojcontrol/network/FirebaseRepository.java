package com.example.relojcontrol.network;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.example.relojcontrol.models.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.TimeZone;


public class FirebaseRepository {
    private static final String TAG = "FirebaseRepository";
    private static FirebaseRepository instance;

    public FirebaseAuth mAuth;
    public DatabaseReference mDatabase;

    private FirebaseRepository() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        Log.d(TAG, "FirebaseRepository inicializado");
    }

    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) {
            instance = new FirebaseRepository();
        }
        return instance;
    }

    public FirebaseAuth getAuth() { return mAuth; }

    // === USUARIOS ===
    public void crearUsuario(Usuario usuario, String password, CrudCallback callback) {
        Log.d(TAG, "Iniciando creación robusta de usuario: " + usuario.getCorreo());

        mAuth.createUserWithEmailAndPassword(usuario.getCorreo(), password)
                .addOnSuccessListener(authResult -> {
                    String firebaseUid = authResult.getUser().getUid();

                    // 1. Obtener siguiente ID numérico
                    getNextId("usuarios", nextId -> {
                        usuario.setIdUsuario(nextId);

                        // 2. Guardar datos del usuario
                        mDatabase.child("usuarios").child(firebaseUid).setValue(usuario)
                                .addOnSuccessListener(aVoid -> {

                                    // 3. ¡PASO CRÍTICO! Guardar el Mapping ID -> UID
                                    mDatabase.child("userMappings").child(String.valueOf(nextId))
                                            .setValue(firebaseUid)
                                            .addOnSuccessListener(aVoid2 -> {
                                                Log.d(TAG, "✓ Usuario y Mapping creados correctamente. ID: " + nextId);
                                                callback.onSuccess();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error creando mapping, revirtiendo usuario", e);
                                                // Si falla el mapping, borramos el usuario para no dejar "zombies"
                                                mDatabase.child("usuarios").child(firebaseUid).removeValue();
                                                callback.onError(new Exception("Error de sincronización. Intente nuevamente."));
                                            });
                                })
                                .addOnFailureListener(callback::onError);
                    });
                })
                .addOnFailureListener(callback::onError);
    }

    public void obtenerUsuarios(DataCallback<List<Usuario>> callback) {
        Log.d(TAG, "Obteniendo usuarios");

        mDatabase.child("usuarios").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Usuario> usuarios = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Usuario usuario = snapshot.getValue(Usuario.class);
                    if (usuario != null) {
                        usuarios.add(usuario);
                    }
                }
                Log.d(TAG, "✓ Usuarios obtenidos: " + usuarios.size());
                callback.onSuccess(usuarios);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        });
    }

    public void eliminarUsuario(String firebaseUid, CrudCallback callback) {
        Log.d(TAG, "Eliminando usuario: " + firebaseUid);

        mDatabase.child("usuarios").child(firebaseUid).get().addOnSuccessListener(userSnapshot -> {
            Usuario usuario = userSnapshot.getValue(Usuario.class);
            if (usuario != null) {
                // Eliminar usuario
                mDatabase.child("usuarios").child(firebaseUid).removeValue();
                // Eliminar mapping
                mDatabase.child("userMappings").child(String.valueOf(usuario.getIdUsuario())).removeValue();

                Log.d(TAG, "✓ Usuario eliminado");
                callback.onSuccess();
            } else {
                callback.onError(new Exception("Usuario no encontrado"));
            }
        }).addOnFailureListener(callback::onError);
    }

    // === ASISTENCIAS ===
    public void registrarAsistencia(String firebaseUid, int tipoAccion, AsistenciaCallback callback) {
        Log.d(TAG, "Registrando asistencia - UID: " + firebaseUid + ", Tipo: " + tipoAccion);

        mDatabase.child("usuarios").child(firebaseUid).get().addOnSuccessListener(userSnapshot -> {
            Usuario usuario = userSnapshot.getValue(Usuario.class);
            if (usuario == null) {
                callback.onError(new Exception("Usuario no encontrado"));
                return;
            }

            getNextId("asistencias", nextId -> {
                Asistencia asistencia = new Asistencia();
                asistencia.setIdAsistencia(nextId);
                asistencia.setIdUsuario(usuario.getIdUsuario());
                asistencia.setFecha(getCurrentDate());
                asistencia.setHora(getCurrentTime());
                asistencia.setIdTipoAccion(tipoAccion);
                asistencia.setIdEstado(1);

                mDatabase.child("asistencias").child(String.valueOf(nextId)).setValue(asistencia)
                        .addOnSuccessListener(aVoid -> {
                            crearIndicesAsistencia(asistencia);
                            Log.d(TAG, "✓ Asistencia registrada - ID: " + nextId);
                            callback.onSuccess(asistencia);
                        })
                        .addOnFailureListener(callback::onError);
            });
        }).addOnFailureListener(callback::onError);
    }

    public void obtenerAsistenciaHoy(String firebaseUid, DataCallback<List<Asistencia>> callback) {
        mDatabase.child("usuarios").child(firebaseUid).get().addOnSuccessListener(userSnapshot -> {
            Usuario usuario = userSnapshot.getValue(Usuario.class);
            if (usuario == null) {
                callback.onSuccess(new ArrayList<>());
                return;
            }

            String fechaHoy = getCurrentDate();
            int userId = usuario.getIdUsuario();

            // Buscar asistencias por usuario y fecha
            mDatabase.child("asistencias").orderByChild("idUsuario").equalTo(userId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            List<Asistencia> asistenciasHoy = new ArrayList<>();

                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Asistencia asistencia = snapshot.getValue(Asistencia.class);
                                if (asistencia != null && fechaHoy.equals(asistencia.getFecha())) {
                                    asistenciasHoy.add(asistencia);
                                }
                            }

                            // Ordenar por hora
                            asistenciasHoy.sort((a, b) -> a.getHora().compareTo(b.getHora()));
                            callback.onSuccess(asistenciasHoy);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            callback.onError(error.toException());
                        }
                    });
        });
    }

    public void obtenerHistorialAsistencia(String firebaseUid, DataCallback<List<Asistencia>> callback) {
        mDatabase.child("usuarios").child(firebaseUid).get().addOnSuccessListener(userSnapshot -> {
            Usuario usuario = userSnapshot.getValue(Usuario.class);
            if (usuario == null) {
                callback.onSuccess(new ArrayList<>());
                return;
            }

            int userId = usuario.getIdUsuario();

            mDatabase.child("asistencias").orderByChild("idUsuario").equalTo(userId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            List<Asistencia> asistencias = new ArrayList<>();

                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Asistencia asistencia = snapshot.getValue(Asistencia.class);
                                if (asistencia != null) {
                                    asistencias.add(asistencia);
                                }
                            }

                            // Ordenar por fecha/hora descendente (más reciente primero)
                            asistencias.sort((a, b) -> {
                                int fechaCompare = b.getFecha().compareTo(a.getFecha());
                                if (fechaCompare != 0) return fechaCompare;
                                return b.getHora().compareTo(a.getHora());
                            });

                            callback.onSuccess(asistencias);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            callback.onError(error.toException());
                        }
                    });
        });
    }

    // === JUSTIFICACIONES (FIRMA CORREGIDA) ===
    public void crearJustificacion(Justificacion justificacion, JustificacionCallback callback) {
        Log.d(TAG, "Creando justificación");

        // Obtener UID del usuario actual autenticado
        String firebaseUid = mAuth.getCurrentUser().getUid();

        mDatabase.child("usuarios").child(firebaseUid).get().addOnSuccessListener(userSnapshot -> {
            Usuario usuario = userSnapshot.getValue(Usuario.class);
            if (usuario == null) {
                callback.onError(new Exception("Usuario no encontrado"));
                return;
            }

            getNextId("justificaciones", nextId -> {
                justificacion.setIdJustificacion(nextId);
                justificacion.setIdUsuario(usuario.getIdUsuario());
                justificacion.setIdEstado(2); // Pendiente

                // Desnormalizar datos para el adapter
                justificacion.setNombreUsuario(usuario.getNombreCompleto());
                justificacion.setRutUsuario(usuario.getRut());

                mDatabase.child("justificaciones").child(String.valueOf(nextId)).setValue(justificacion)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "✓ Justificación creada - ID: " + nextId);
                            callback.onSuccess(justificacion);
                        })
                        .addOnFailureListener(callback::onError);
            });
        }).addOnFailureListener(callback::onError);
    }

    public void obtenerJustificaciones(DataCallback<List<Justificacion>> callback) {
        Log.d(TAG, "Obteniendo justificaciones");

        mDatabase.child("justificaciones").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Justificacion> justificaciones = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Justificacion justificacion = snapshot.getValue(Justificacion.class);
                    if (justificacion != null) {
                        justificaciones.add(justificacion);
                    }
                }
                // Ordenar por ID descendente (más reciente primero)
                justificaciones.sort((a, b) -> Integer.compare(b.getIdJustificacion(), a.getIdJustificacion()));

                Log.d(TAG, "✓ Justificaciones obtenidas: " + justificaciones.size());
                callback.onSuccess(justificaciones);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        });
    }

    // === LICENCIAS ===
    public void crearLicencia(Licencia licencia, LicenciaCallback callback) {
        String firebaseUid = mAuth.getCurrentUser().getUid();

        mDatabase.child("usuarios").child(firebaseUid).get().addOnSuccessListener(userSnapshot -> {
            Usuario usuario = userSnapshot.getValue(Usuario.class);
            if (usuario == null) {
                callback.onError(new Exception("Usuario no encontrado"));
                return;
            }

            getNextId("licencias", nextId -> {
                licencia.setIdLicencia(nextId);
                licencia.setIdUsuario(usuario.getIdUsuario());
                licencia.setIdEstado(2); // Pendiente

                mDatabase.child("licencias").child(String.valueOf(nextId)).setValue(licencia)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "✓ Licencia creada - ID: " + nextId);
                            callback.onSuccess(licencia);
                        })
                        .addOnFailureListener(callback::onError);
            });
        }).addOnFailureListener(callback::onError);
    }

    public void obtenerLicencias(DataCallback<List<Licencia>> callback) {
        mDatabase.child("licencias").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Licencia> licencias = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Licencia licencia = snapshot.getValue(Licencia.class);
                    if (licencia != null) {
                        licencias.add(licencia);
                    }
                }
                callback.onSuccess(licencias);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        });
    }

    // === MÉTODOS AUXILIARES ===
    private void getNextId(String tabla, IdCallback callback) {
        DatabaseReference counterRef = mDatabase.child("counters").child(tabla);
        counterRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Integer currentValue = mutableData.getValue(Integer.class);
                if (currentValue == null) {
                    mutableData.setValue(2);
                } else {
                    mutableData.setValue(currentValue + 1);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                if (committed && dataSnapshot.exists()) {
                    Integer nextId = dataSnapshot.getValue(Integer.class);
                    callback.onId(nextId - 1);
                } else {
                    callback.onId(1);
                }
            }
        });
    }


    private void crearIndicesAsistencia(Asistencia asistencia) {
        int userId = asistencia.getIdUsuario();
        int asistenciaId = asistencia.getIdAsistencia();
        String fecha = asistencia.getFecha();

        // CONVERSIÓN CORREGIDA: int → String para Firebase keys
        mDatabase.child("indices").child("asistenciasPorUsuarioFecha")
                .child(userId + "_" + fecha).child(String.valueOf(asistenciaId)).setValue(true);
    }

    private String getCurrentDate() {
        TimeZone timezone = TimeZone.getTimeZone("America/Santiago");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(timezone);
        return sdf.format(new Date());
    }

    private String getCurrentTime() {
        TimeZone timezone = TimeZone.getTimeZone("America/Santiago");
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(timezone);
        return sdf.format(new Date());
    }

    // === INTERFACES ===
    public interface CrudCallback {
        void onSuccess();
        void onError(Exception error);
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(Exception error);
    }

    public interface AsistenciaCallback {
        void onSuccess(Asistencia asistencia);
        void onError(Exception error);
    }

    public interface JustificacionCallback {
        void onSuccess(Justificacion justificacion);
        void onError(Exception error);
    }

    public interface LicenciaCallback {
        void onSuccess(Licencia licencia);
        void onError(Exception error);
    }

    private interface IdCallback {
        void onId(int id);
    }
}
