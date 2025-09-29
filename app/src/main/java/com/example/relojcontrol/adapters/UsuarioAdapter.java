package com.example.relojcontrol.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Usuario;
import java.util.List;

public class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder> {
    private List<Usuario> usuarios;
    private OnUsuarioClickListener listener;

    public interface OnUsuarioClickListener {
        void onUsuarioClick(Usuario usuario);
        void onEditClick(Usuario usuario);
        void onDeleteClick(Usuario usuario);
        void onToggleUsuarioStatus(Usuario usuario);
    }

    public UsuarioAdapter(List<Usuario> usuarios, OnUsuarioClickListener listener) {
        this.usuarios = usuarios;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_usuario, parent, false);
        return new UsuarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        Usuario usuario = usuarios.get(position);
        holder.bind(usuario, listener);
    }

    @Override
    public int getItemCount() {
        return usuarios.size();
    }

    public void updateData(List<Usuario> nuevosUsuarios) {
        this.usuarios = nuevosUsuarios;
        notifyDataSetChanged();
    }

    static class UsuarioViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNombre, tvEmail, tvRol, tvEstado;

        public UsuarioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tv_nombre_usuario);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvRol = itemView.findViewById(R.id.tv_rol);
            tvEstado = itemView.findViewById(R.id.tv_estado_usuario);
        }

        // En el metodo bind del ViewHolder:
        public void bind(final Usuario usuario, final OnUsuarioClickListener listener) {
            // Configurar datos del usuario
            tvNombre.setText(usuario.getNombreCompleto());
            tvEmail.setText(usuario.getCorreo());

            // Configurar rol
            if (usuario.getIdRol() == 1) {
                tvRol.setText("Admin");
                tvRol.setBackgroundResource(R.drawable.badge_rol_admin);
            } else {
                tvRol.setText("Emp");
                tvRol.setBackgroundResource(R.drawable.badge_rol_empleado);
            }

            // Configurar estado
            if (usuario.getEstadoUsuario().equals("Activo")) {
                tvEstado.setText("Activo");
                tvEstado.setBackgroundResource(R.drawable.badge_estado_activo);
            } else {
                tvEstado.setText("Inactivo");
                tvEstado.setBackgroundResource(R.drawable.badge_estado_inactivo);
            }

            // Click en el item completo
            itemView.setOnClickListener(v -> listener.onUsuarioClick(usuario));
        }
    }
}
