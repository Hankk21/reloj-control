package com.example.relojcontrol.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.relojcontrol.R;
import com.example.relojcontrol.activities.admin.JustificacionesAdminActivity;
import com.example.relojcontrol.models.Justificacion;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class JustificacionesAdminAdapter extends RecyclerView.Adapter<JustificacionesAdminAdapter.ViewHolder> {

    private List<Justificacion> justificaciones;
    private JustificacionesAdminActivity activity;

    public JustificacionesAdminAdapter(List<Justificacion> justificaciones, JustificacionesAdminActivity activity) {
        this.justificaciones = justificaciones;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_justificacion_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Justificacion justificacion = justificaciones.get(position);

        holder.tvMotivo.setText(justificacion.getMotivo());
        holder.tvDescripcion.setText(justificacion.getDescripcion());
        holder.tvFecha.setText("Fecha a justificar: " + justificacion.getFechaJustificar());
        holder.tvFechaCreacion.setText("Solicitado: " + justificacion.getFechaCreacion());

        // Obtener nombre del empleado si tienes el idUsuario
        holder.tvEmpleado.setText("Empleado ID: " + justificacion.getIdUsuario());

        // Botón aprobar
        holder.btnAprobar.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                activity.aprobarJustificacion(justificacion.getId(), adapterPosition);
            }
        });

        // Botón rechazar
        holder.btnRechazar.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                activity.rechazarJustificacion(justificacion.getId(), adapterPosition);
            }
        });

        // Opcional: mostrar documento si existe
        if (justificacion.getUrlDocumento() != null && !justificacion.getUrlDocumento().isEmpty()) {
            holder.tvDocumento.setVisibility(View.VISIBLE);
            holder.tvDocumento.setText("📎 Documento adjunto");
        } else {
            holder.tvDocumento.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return justificaciones.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvMotivo;
        TextView tvDescripcion;
        TextView tvFecha;
        TextView tvFechaCreacion;
        TextView tvEmpleado;
        TextView tvDocumento;
        MaterialButton btnAprobar;
        MaterialButton btnRechazar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvMotivo = itemView.findViewById(R.id.tvMotivo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvFechaCreacion = itemView.findViewById(R.id.tvFechaCreacion);
            tvEmpleado = itemView.findViewById(R.id.tvEmpleado);
            tvDocumento = itemView.findViewById(R.id.tvDocumento);
            btnAprobar = itemView.findViewById(R.id.btnAprobar);
            btnRechazar = itemView.findViewById(R.id.btnRechazar);
        }
    }
}

