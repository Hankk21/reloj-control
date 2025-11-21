package com.example.relojcontrol.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.relojcontrol.R;
import com.example.relojcontrol.models.Asistencia;
import java.util.List;

public class AsistenciaAdapter extends RecyclerView.Adapter<AsistenciaAdapter.ViewHolder> {

    private List<Asistencia> listaAsistencias;

    public AsistenciaAdapter(List<Asistencia> listaAsistencias) {
        this.listaAsistencias = listaAsistencias;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_asistencia_historial, parent, false);
        // Asegúrate de tener este layout o usa uno genérico simple
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Asistencia asistencia = listaAsistencias.get(position);

        holder.tvFecha.setText(asistencia.getFecha());
        holder.tvHora.setText(asistencia.getHora());

        // Lógica visual simple: Entrada vs Salida
        if (asistencia.getIdTipoAccion() == 1) {
            holder.tvTipo.setText("ENTRADA");
            holder.tvTipo.setTextColor(Color.parseColor("#4CAF50")); // Verde
        } else {
            holder.tvTipo.setText("SALIDA");
            holder.tvTipo.setTextColor(Color.parseColor("#F44336")); // Rojo
        }
    }

    @Override
    public int getItemCount() {
        return listaAsistencias.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvHora, tvTipo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ajusta estos IDs según tu XML de item (item_asistencia_historial.xml)
            // Si no tienes el XML, avísame y te paso uno genérico.
            tvFecha = itemView.findViewById(R.id.tv_fecha_asistencia);
            tvHora = itemView.findViewById(R.id.tv_hora_asistencia);
            tvTipo = itemView.findViewById(R.id.tv_tipo_accion);
        }
    }
}
