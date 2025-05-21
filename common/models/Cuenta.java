package common.models;

import java.io.Serializable;

public class Cuenta implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int idCuenta;
    private int idCliente;
    private double saldo;
    private String tipoCuenta;
    
    public Cuenta(int idCuenta, int idCliente, double saldo, String tipoCuenta) {
        this.idCuenta = idCuenta;
        this.idCliente = idCliente;
        this.saldo = saldo;
        this.tipoCuenta = tipoCuenta;
    }
    
    // Getters y setters
    public int getIdCuenta() { return idCuenta; }
    public void setIdCuenta(int idCuenta) { this.idCuenta = idCuenta; }
    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
    public String getTipoCuenta() { return tipoCuenta; }
    public void setTipoCuenta(String tipoCuenta) { this.tipoCuenta = tipoCuenta; }
    
    @Override
    public String toString() {
        return idCuenta + "|" + idCliente + "|" + saldo + "|" + tipoCuenta;
    }
}