package common.utils;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum OperationType {
        CONSULTAR_SALDO,
        TRANSFERIR_FONDOS,
        RESPONSE,
        HEARTBEAT,
        REGISTER_NODE,
        NODE_STATUS
    }
    
    private OperationType type;
    private Object[] params;
    private Object result;
    private String status;  // "OK" o mensaje de error
    
    public Message(OperationType type, Object... params) {
        this.type = type;
        this.params = params;
        this.status = "OK";
    }
    
    // Getters y setters
    public OperationType getType() { return type; }
    public void setType(OperationType type) { this.type = type; }
    public Object[] getParams() { return params; }
    public void setParams(Object[] params) { this.params = params; }
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public void setError(String errorMessage) {
        this.status = errorMessage;
    }
    
    public boolean isOk() {
        return "OK".equals(status);
    }
}