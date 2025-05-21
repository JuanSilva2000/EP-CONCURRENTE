package common.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Transaccion implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int idTransaccion;
    private int idOrigen;
    private int idDestino;
    private double monto;
    private LocalDateTime fechaHora;
    private String estado;
    
    public Transaccion(int idTransaccion, int idOrigen, int idDestino, double monto, LocalDateTime fechaHora, String estado) {
        this.idTransaccion = idTransaccion;
        this.idOrigen = idOrigen;
        this.idDestino = idDestino;
        this.monto = monto;
        this.fechaHora = fechaHora;
        this.estado = estado;
    }
    
    // Constructor sin ID para cuando se crea una nueva transacción
    public Transaccion(int idOrigen, int idDestino, double monto) {
        this.idOrigen = idOrigen;
        this.idDestino = idDestino;
        this.monto = monto;
        this.fechaHora = LocalDateTime.now();
        this.estado = "Pendiente";
    }
    
    // Getters y setters
    public int getIdTransaccion() { return idTransaccion; }
    public void setIdTransaccion(int idTransaccion) { this.idTransaccion = idTransaccion; }
    public int getIdOrigen() { return idOrigen; }
    public void setIdOrigen(int idOrigen) { this.idOrigen = idOrigen; }
    public int getIdDestino() { return idDestino; }
    public void setIdDestino(int idDestino) { this.idDestino = idDestino; }
    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    @Override
    public String toString() {
        return idTransaccion + "|" + idOrigen + "|" + idDestino + "|" + monto + "|" + fechaHora + "|" + estado;
    }
}