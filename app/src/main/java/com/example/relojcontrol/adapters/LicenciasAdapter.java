package com.example.relojcontrol.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Licencia;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LicenciasAdapter extends RecyclerView.Adapter<LicenciasAdapter.ViewHolder> {

    private List<Licencia> licencias;
    private Context context;

    public LicenciasAdapter(List<Licencia> licencias, Context context) {
        this.licencias = licencias;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_licencia_historial, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Licencia licencia = licencias.get(position);

        // Motivo
        holder.tvMotivo.setText(licencia.getMotivo());

        // Fechas
        holder.tvFechaInicio.setText("Desde: " + licencia.getFechaInicio());
        holder.tvFechaFin.setText("Hasta: " + licencia.getFechaFin());

        // Duración en días
        int dias = calcularDiasEntreFechas(licencia.getFechaInicio(), licencia.getFechaFin());
        holder.tvDuracion.setText(dias + " día(s)");

        // Fecha de creación
        if (licencia.getFechaCreacion() != null) {
            holder.tvFechaCreacion.setText("Solicitado: " + licencia.getFechaCreacion());
        }

        // Estado
        String estadoTexto = licencia.getEstadoTexto();
        holder.tvEstado.setText(estadoTexto);

        // Color según estado
        switch (licencia.getIdEstado()) {
            case 1: // Registrado
                holder.tvEstado.setBackgroundResource(R.drawable.bg_estado_registrado);
                break;
            case 2: // Pendiente
                holder.tvEstado.setBackgroundResource(R.drawable.bg_estado_pendiente);
                break;
            case 3: // Aprobado
                holder.tvEstado.setBackgroundResource(R.drawable.bg_estado_aprobado);
                break;
            case 4: // Rechazado
                holder.tvEstado.setBackgroundResource(R.drawable.bg_estado_rechazado);
                break;
        }

        // Documento adjunto
        if (licencia.tieneDocumento()) {
            holder.tvDocumento.setVisibility(View.VISIBLE);
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
        TextView tvDuracion;
        TextView tvFechaCreacion;
        TextView tvEstado;
        TextView tvDocumento;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvMotivo = itemView.findViewById(R.id.tvMotivo);
            tvFechaInicio = itemView.findViewById(R.id.tvFechaInicio);
            tvFechaFin = itemView.findViewById(R.id.tvFechaFin);
            tvDuracion = itemView.findViewById(R.id.tvDuracion);
            tvFechaCreacion = itemView.findViewById(R.id.tvFechaCreacion);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvDocumento = itemView.findViewById(R.id.tvDocumento);
        }
    }
}

