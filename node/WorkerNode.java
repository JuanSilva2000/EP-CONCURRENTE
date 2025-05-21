package node;

import common.models.Cliente;
import common.models.Cuenta;
import common.models.Transaccion;
import common.utils.Message;
import common.utils.Message.OperationType;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WorkerNode {
    private final int nodeId;
    private final int port;
    private final String dataFilesPath;
    private final Map<Integer, Cliente> clientes = new ConcurrentHashMap<>();
    private final Map<Integer, Cuenta> cuentas = new ConcurrentHashMap<>();
    private final Map<Integer, Transaccion> transacciones = new ConcurrentHashMap<>();
    private final Map<Integer, ReadWriteLock> accountLocks = new ConcurrentHashMap<>();
    private final ExecutorService taskPool;
    
    public WorkerNode(int nodeId, int port, String dataFilesPath) {
        this.nodeId = nodeId;
        this.port = port;
        this.dataFilesPath = dataFilesPath;
        // Usar tantos hilos como cores tiene la máquina
        this.taskPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }
    
    public void start() {
        // Cargar datos desde archivos
        loadData();
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Nodo trabajador " + nodeId + " iniciado en puerto " + port);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                taskPool.submit(() -> handleRequest(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Error en nodo trabajador " + nodeId + ": " + e.getMessage());
        }
    }
    
    private void loadData() {
        // Cargar clientes
        loadClientes();
        
        // Cargar cuentas
        loadCuentas();
        
        // Cargar transacciones
        loadTransacciones();
        
        // Inicializar locks para cada cuenta
        for (Integer idCuenta : cuentas.keySet()) {
            accountLocks.put(idCuenta, new ReentrantReadWriteLock());
        }
        
        System.out.println("Nodo " + nodeId + " - Datos cargados: " + 
                clientes.size() + " clientes, " +
                cuentas.size() + " cuentas, " +
                transacciones.size() + " transacciones");
    }
    
    private void loadClientes() {
        try (BufferedReader br = new BufferedReader(new FileReader(dataFilesPath + "/clientes.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    int idCliente = Integer.parseInt(parts[0].trim());
                    String nombre = parts[1].trim();
                    String email = parts[2].trim();
                    String telefono = parts[3].trim();
                    
                    clientes.put(idCliente, new Cliente(idCliente, nombre, email, telefono));
                }
            }
        } catch (IOException e) {
            System.err.println("Error cargando clientes: " + e.getMessage());
            // Crear algunos clientes de ejemplo si no se puede cargar el archivo
            clientes.put(1, new Cliente(1, "Juan Pérez", "juan@email.com", "987654321"));
            clientes.put(2, new Cliente(2, "María López", "maria@email.com", "998877665"));
        }
    }
    
    private void loadCuentas() {
        try (BufferedReader br = new BufferedReader(new FileReader(dataFilesPath + "/cuentas.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    int idCuenta = Integer.parseInt(parts[0].trim());
                    int idCliente = Integer.parseInt(parts[1].trim());
                    double saldo = Double.parseDouble(parts[2].trim());
                    String tipoCuenta = parts[3].trim();
                    
                    cuentas.put(idCuenta, new Cuenta(idCuenta, idCliente, saldo, tipoCuenta));
                }
            }
        } catch (IOException e) {
            System.err.println("Error cargando cuentas: " + e.getMessage());
            // Crear algunas cuentas de ejemplo si no se puede cargar el archivo
            cuentas.put(101, new Cuenta(101, 1, 1500.00, "Ahorros"));
            cuentas.put(102, new Cuenta(102, 2, 3200.50, "Corriente"));
        }
    }
    
    private void loadTransacciones() {
        try (BufferedReader br = new BufferedReader(new FileReader(dataFilesPath + "/transacciones.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                
                String[] parts = line.split("\\|");
                if (parts.length >= 6) {
                    int idTransaccion = Integer.parseInt(parts[0].trim());
                    int idOrigen = Integer.parseInt(parts[1].trim());
                    int idDestino = Integer.parseInt(parts[2].trim());
                    double monto = Double.parseDouble(parts[3].trim());
                    LocalDateTime fechaHora = LocalDateTime.parse(parts[4].trim());
                    String estado = parts[5].trim();
                    
                    transacciones.put(idTransaccion, 
                        new Transaccion(idTransaccion, idOrigen, idDestino, monto, fechaHora, estado));
                }
            }
        } catch (IOException e) {
            System.err.println("Error cargando transacciones: " + e.getMessage());
            // Crear algunas transacciones de ejemplo si no se puede cargar el archivo
            transacciones.put(1, new Transaccion(1, 101, 102, 500.00, 
                    LocalDateTime.parse("2025-05-02T14:30:00"), "Confirmada"));
            transacciones.put(2, new Transaccion(2, 102, 101, 200.00, 
                    LocalDateTime.parse("2025-05-02T15:00:00"), "Pendiente"));
        }
    }
    
    private void saveData() {
        // En una implementación real, aquí se guardarían los datos actualizados en archivos
        // o en una base de datos persistente
        System.out.println("Nodo " + nodeId + " - Guardando datos...");
    }
    
    private void handleRequest(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            
            Message request = (Message) in.readObject();
            Message response = null;
            
            switch (request.getType()) {
                case CONSULTAR_SALDO:
                    response = handleConsultarSaldo(request);
                    break;
                case TRANSFERIR_FONDOS:
                    response = handleTransferirFondos(request);
                    break;
                case HEARTBEAT:
                    response = new Message(OperationType.RESPONSE);
                    break;
                default:
                    response = new Message(OperationType.RESPONSE);
                    response.setError("Operación no soportada por el nodo");
            }
            
            out.writeObject(response);
            
        } catch (Exception e) {
            System.err.println("Error procesando solicitud en nodo " + nodeId + ": " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar socket en nodo " + nodeId + ": " + e.getMessage());
            }
        }
    }
    
    private Message handleConsultarSaldo(Message request) {
        Message response = new Message(OperationType.RESPONSE);
        
        try {
            int idCuenta = (Integer) request.getParams()[0];
            
            // Verificar si tenemos la cuenta
            Cuenta cuenta = cuentas.get(idCuenta);
            if (cuenta == null) {
                response.setError("Cuenta no encontrada en este nodo");
                return response;
            }
            
            // Adquirir lock de lectura
            ReadWriteLock lock = accountLocks.get(idCuenta);
            lock.readLock().lock();
            try {
                // Devolver el saldo actual
                response.setResult(cuenta.getSaldo());
            } finally {
                lock.readLock().unlock();
            }
            
        } catch (Exception e) {
            response.setError("Error consultando saldo: " + e.getMessage());
        }
        
        return response;
    }
    
    private Message handleTransferirFondos(Message request) {
        Message response = new Message(OperationType.RESPONSE);
        
        try {
            Object[] params = request.getParams();
            int idOrigen = (Integer) params[0];
            int idDestino = (Integer) params[1];
            double monto = (Double) params[2];
            int idTransaccion = (params.length > 3) ? (Integer) params[3] : generateTransactionId();
            
            // Verificar si tenemos las cuentas
            Cuenta cuentaOrigen = cuentas.get(idOrigen);
            Cuenta cuentaDestino = cuentas.get(idDestino);
            
            if (cuentaOrigen == null) {
                response.setError("Cuenta origen no encontrada en este nodo");
                return response;
            }
            
            if (cuentaDestino == null) {
                // Si no tenemos la cuenta destino localmente, simplemente debitamos
                // y confiamos en que el servidor central sincronizará con el nodo que tiene la cuenta destino
                System.out.println("Cuenta destino no encontrada en este nodo. Se procesará solo el débito.");
            }
            
            // Adquirir locks para ambas cuentas (prevenir deadlocks ordenando los IDs)
            List<Integer> accountIds = new ArrayList<>();
            accountIds.add(idOrigen);
            if (cuentaDestino != null) {
                accountIds.add(idDestino);
            }
            Collections.sort(accountIds);
            
            List<ReadWriteLock> locks = new ArrayList<>();
            for (Integer id : accountIds) {
                locks.add(accountLocks.get(id));
            }
            
            // Adquirir locks de escritura en orden
            for (ReadWriteLock lock : locks) {
                lock.writeLock().lock();
            }
            
            try {
                // Validar saldo suficiente
                if (cuentaOrigen.getSaldo() < monto) {
                    response.setError("Saldo insuficiente");
                    return response;
                }
                
                // Realizar la transferencia
                cuentaOrigen.setSaldo(cuentaOrigen.getSaldo() - monto);
                
                if (cuentaDestino != null) {
                    cuentaDestino.setSaldo(cuentaDestino.getSaldo() + monto);
                }
                
                // Registrar la transacción
                Transaccion transaccion = new Transaccion(
                    idTransaccion, idOrigen, idDestino, monto, LocalDateTime.now(), "Confirmada");
                transacciones.put(idTransaccion, transaccion);
                
                // Guardar cambios en disco (en un sistema real)
                saveData();
                
                // Devolver el nuevo saldo de la cuenta origen
                response.setResult(cuentaOrigen.getSaldo());
                
            } finally {
                // Liberar locks en orden inverso
                for (int i = locks.size() - 1; i >= 0; i--) {
                    locks.get(i).writeLock().unlock();
                }
            }
            
        } catch (Exception e) {
            response.setError("Error procesando transferencia: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }
    
    private synchronized int generateTransactionId() {
        int maxId = 0;
        for (Integer id : transacciones.keySet()) {
            if (id > maxId) {
                maxId = id;
            }
        }
        return maxId + 1;
    }
    
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso: java WorkerNode <nodeId> <port> <dataPath>");
            System.out.println("Ejemplo: java WorkerNode 0 9100 ./data");
            return;
        }
        
        int nodeId = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        String dataPath = args[2];
        
        WorkerNode node = new WorkerNode(nodeId, port, dataPath);
        node.start();
    }
}
