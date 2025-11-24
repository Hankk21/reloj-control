package com.example.relojcontrol.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.relojcontrol.R;
import com.example.relojcontrol.activities.admin.LicenciasAdminActivity;
import com.example.relojcontrol.models.Licencia;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LicenciasAdminAdapter extends RecyclerView.Adapter<LicenciasAdminAdapter.ViewHolder> {

    private List<Licencia> licencias;
    private LicenciasAdminActivity activity;

    public LicenciasAdminAdapter(List<Licencia> licencias, LicenciasAdminActivity activity) {
        this.licencias = licencias;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_licencia_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Licencia licencia = licencias.get(position);

        holder.tvMotivo.setText(licencia.getMotivo());
        holder.tvFechaInicio.setText("Desde: " + licencia.getFechaInicio());
        holder.tvFechaFin.setText("Hasta: " + licencia.getFechaFin());
        holder.tvFechaCreacion.setText("Solicitado: " + licencia.getFechaCreacion());

        // Calcular días de licencia
        int dias = calcularDiasEntreFechas(licencia.getFechaInicio(), licencia.getFechaFin());
        holder.tvDuracion.setText("Duración: " + dias + " día(s)");

        // Obtener nombre del empleado
        holder.tvEmpleado.setText("Empleado ID: " + licencia.getIdUsuario());

        // Botón aprobar
        holder.btnAprobar.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                activity.aprobarLicencia(licencia.getId(), adapterPosition);
            }
        });

        // Botón rechazar
        holder.btnRechazar.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                activity.rechazarLicencia(licencia.getId(), adapterPosition);
            }
        });

        // Opcional: mostrar documento si existe
        if (licencia.getUrlDocumento() != null && !licencia.getUrlDocumento().isEmpty()) {
            holder.tvDocumento.setVisibility(View.VISIBLE);
            holder.tvDocumento.setText("📎 Documento adjunto");
        } else {
            holder.tvDocumento.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return licencias.size();
    }

    private int calcularDiasEntreFechas(String fechaInicio, String fechaFin) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date inicio = sdf.parse(fechaInicio);
            Date fin = sdf.parse(fechaFin);

            if (inicio != null && fin != null) {
                long diferencia = fin.getTime() - inicio.getTime();
                return (int) TimeUnit.DAYS.convert(diferencia, TimeUnit.MILLISECONDS) + 1;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvMotivo;
        TextView tvFechaInicio;
        TextView tvFechaFin;
        TextView tvFechaCreacion;
        TextView tvDuracion;
        TextView tvEmpleado;
        TextView tvDocumento;
        MaterialButton btnAprobar;
        MaterialButton btnRechazar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvMotivo = itemView.findViewById(R.id.tvMotivo);
            tvFechaInicio = itemView.findViewById(R.id.tvFechaInicio);
            tvFechaFin = itemView.findViewById(R.id.tvFechaFin);
            tvFechaCreacion = itemView.findViewById(R.id.tvFechaCreacion);
            tvDuracion = itemView.findViewById(R.id.tvDuracion);
            tvEmpleado = itemView.findViewById(R.id.tvEmpleado);
            tvDocumento = itemView.findViewById(R.id.tvDocumento);
            btnAprobar = itemView.findViewById(R.id.btnAprobar);
            btnRechazar = itemView.findViewById(R.id.btnRechazar);
        }
    }
}

