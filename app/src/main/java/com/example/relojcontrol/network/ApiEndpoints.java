package com.example.relojcontrol.network;

public class ApiEndpoints {
    public void loginUser(String email, String password, LoginCallback callback) {
        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = authResult.getUser().getUid();
                    // Obtener datos del usuario desde Realtime Database
                    FirebaseDatabase.getInstance()
                            .getReference("usuarios")
                            .child(userId)
                            .get()
                            .addOnSuccessListener(dataSnapshot -> {
                                Usuario usuario = dataSnapshot.getValue(Usuario.class);
                                callback.onSuccess(usuario);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void crearUsuario(Usuario usuario, String password, CrudCallback callback) {
        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(usuario.getEmail(), password)
                .addOnSuccessListener(authResult -> {
                    String userId = authResult.getUser().getUid();
                    usuario.setId(userId);

                    FirebaseDatabase.getInstance()
                            .getReference("usuarios")
                            .child(userId)
                            .setValue(usuario)
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void obtenerUsuarios(DataCallback<List<Usuario>> callback) {
        FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<Usuario> usuarios = new ArrayList<>();
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            Usuario usuario = userSnapshot.getValue(Usuario.class);
                            usuarios.add(usuario);
                        }
                        callback.onSuccess(usuarios);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.toException());
                    }
                });
    }

    public void registrarAsistencia(String userId, TipoAsistencia tipo, AsistenciaCallback callback) {
        Asistencia asistencia = new Asistencia();
        asistencia.setUsuarioId(userId);
        asistencia.setTipo(tipo);
        asistencia.setFechaHora(System.currentTimeMillis());
        asistencia.setFecha(getCurrentDate());

        String asistenciaId = FirebaseDatabase.getInstance()
                .getReference("asistencias")
                .push().getKey();

        FirebaseDatabase.getInstance()
                .getReference("asistencias")
                .child(asistenciaId)
                .setValue(asistencia)
                .addOnSuccessListener(aVoid -> callback.onSuccess(asistencia))
                .addOnFailureListener(callback::onError);
    }




}
