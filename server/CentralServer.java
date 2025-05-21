package server;

import common.utils.Message;
import common.utils.Message.OperationType;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class CentralServer {
    private static final int SERVER_PORT = 9000;
    private static final int NODE_PORT_BASE = 9100;
    private static final int MAX_NODES = 3;
    
    private Map<Integer, NodeInfo> activeNodes = new ConcurrentHashMap<>();
    private Map<Integer, Set<Integer>> nodeDataPartitions = new ConcurrentHashMap<>();
    private ExecutorService clientHandlerPool;
    private ScheduledExecutorService nodeMonitorPool;
    private Random random = new Random();
    private int nextTransactionId = 1;
    
    private static class NodeInfo {
        String host;
        int port;
        boolean isActive;
        long lastHeartbeat;
        
        public NodeInfo(String host, int port) {
            this.host = host;
            this.port = port;
            this.isActive = true;
            this.lastHeartbeat = System.currentTimeMillis();
        }
    }
    
    public CentralServer() {
        // Usar un pool de tamaño fijo para manejar conexiones de clientes
        this.clientHandlerPool = Executors.newFixedThreadPool(50);
        this.nodeMonitorPool = Executors.newScheduledThreadPool(1);
        
        // Inicializar nodos
        for (int i = 0; i < MAX_NODES; i++) {
            activeNodes.put(i, new NodeInfo("localhost", NODE_PORT_BASE + i));
            nodeDataPartitions.put(i, new HashSet<>());
        }
        
        // Distribuir ranges de IDs de cuenta entre los nodos
        // Cada nodo tendrá un tercio de las cuentas como principal
        // y todos tendrán réplicas completas para simplicidad
        for (int i = 0; i < MAX_NODES; i++) {
            // Particiones principales (asumiendo IDs de cuenta del 101 al 200)
            int rangeSize = 33; // ~100 cuentas / 3 nodos
            int start = 101 + (i * rangeSize);
            int end = (i == MAX_NODES - 1) ? 200 : start + rangeSize - 1;
            
            Set<Integer> partitions = nodeDataPartitions.get(i);
            for (int id = start; id <= end; id++) {
                partitions.add(id);
            }
        }
    }
    
    public void start() {
        // Iniciar monitoreo de nodos
        startNodeMonitoring();
        
        // Arrancar servidor central
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Servidor central iniciado en puerto " + SERVER_PORT);
            System.out.println("Esperando conexiones de clientes...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientHandlerPool.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor central: " + e.getMessage());
        }
    }
    
    private void startNodeMonitoring() {
        nodeMonitorPool.scheduleAtFixedRate(() -> {
            for (Map.Entry<Integer, NodeInfo> entry : activeNodes.entrySet()) {
                int nodeId = entry.getKey();
                NodeInfo node = entry.getValue();
                
                // Si no hemos recibido heartbeat en los últimos 10 segundos, marcar como inactivo
                if (System.currentTimeMillis() - node.lastHeartbeat > 10000) {
                    if (node.isActive) {
                        System.out.println("Nodo " + nodeId + " marcado como inactivo");
                        node.isActive = false;
                    }
                    
                    // Intentar reconectar
                    tryReconnectNode(nodeId, node);
                }
                
                // Enviar heartbeat a nodos activos
                if (node.isActive) {
                    sendHeartbeat(nodeId, node);
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
    
    private void tryReconnectNode(int nodeId, NodeInfo node) {
        try (Socket socket = new Socket(node.host, node.port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            Message message = new Message(OperationType.HEARTBEAT);
            out.writeObject(message);
            
            Message response = (Message) in.readObject();
            if (response.isOk()) {
                System.out.println("Nodo " + nodeId + " reconectado.");
                node.isActive = true;
                node.lastHeartbeat = System.currentTimeMillis();
            }
        } catch (Exception e) {
            // Nodo sigue sin responder
        }
    }
    
    private void sendHeartbeat(int nodeId, NodeInfo node) {
        try (Socket socket = new Socket(node.host, node.port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            Message message = new Message(OperationType.HEARTBEAT);
            out.writeObject(message);
            
            Message response = (Message) in.readObject();
            if (response.isOk()) {
                node.lastHeartbeat = System.currentTimeMillis();
            }
        } catch (Exception e) {
            System.out.println("Heartbeat fallido para nodo " + nodeId + ": " + e.getMessage());
            node.isActive = false;
        }
    }
    
    // Encuentra los nodos que tienen la réplica del dato que queremos consultar
    private List<Integer> findNodesForAccount(int accountId) {
        List<Integer> nodeIds = new ArrayList<>();
        
        for (Map.Entry<Integer, NodeInfo> entry : activeNodes.entrySet()) {
            int nodeId = entry.getKey();
            NodeInfo node = entry.getValue();
            
            if (node.isActive && nodeDataPartitions.get(nodeId).contains(accountId)) {
                nodeIds.add(nodeId);
            }
        }
        
        // Ordenar aleatoriamente para balanceo de carga
        Collections.shuffle(nodeIds);
        return nodeIds;
    }
    
    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        
        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
        
        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {
                
                Message request = (Message) in.readObject();
                Message response;
                
                switch (request.getType()) {
                    case CONSULTAR_SALDO:
                        response = procesarConsultaSaldo(request);
                        break;
                    case TRANSFERIR_FONDOS:
                        response = procesarTransferencia(request);
                        break;
                    default:
                        response = new Message(OperationType.RESPONSE);
                        response.setError("Operación no soportada");
                }
                
                out.writeObject(response);
                
            } catch (Exception e) {
                System.err.println("Error procesando solicitud de cliente: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error al cerrar socket: " + e.getMessage());
                }
            }
        }
        
        private Message procesarConsultaSaldo(Message request) {
            Message response = new Message(OperationType.RESPONSE);
            try {
                int idCuenta = (Integer) request.getParams()[0];
                
                // Buscar nodos que tengan este dato
                List<Integer> nodesWithData = findNodesForAccount(idCuenta);
                
                if (nodesWithData.isEmpty()) {
                    response.setError("No hay nodos disponibles para procesar la consulta");
                    return response;
                }
                
                // Intentar en cada nodo hasta obtener respuesta
                for (Integer nodeId : nodesWithData) {
                    NodeInfo node = activeNodes.get(nodeId);
                    
                    try (Socket nodeSocket = new Socket(node.host, node.port);
                         ObjectOutputStream nodeOut = new ObjectOutputStream(nodeSocket.getOutputStream());
                         ObjectInputStream nodeIn = new ObjectInputStream(nodeSocket.getInputStream())) {
                        
                        nodeOut.writeObject(request);
                        Message nodeResponse = (Message) nodeIn.readObject();
                        
                        if (nodeResponse.isOk()) {
                            response.setResult(nodeResponse.getResult());
                            return response;
                        }
                    } catch (Exception e) {
                        // Si falla, marcar nodo como inactivo y probar con el siguiente
                        node.isActive = false;
                        System.out.println("Nodo " + nodeId + " marcado como inactivo tras error: " + e.getMessage());
                    }
                }
                
                // Si llegamos aquí, todos los nodos fallaron
                response.setError("No se pudo procesar la consulta en ningún nodo");
                
            } catch (Exception e) {
                response.setError("Error al procesar consulta: " + e.getMessage());
            }
            
            return response;
        }
        
        private Message procesarTransferencia(Message request) {
            Message response = new Message(OperationType.RESPONSE);
            try {
                int idOrigen = (Integer) request.getParams()[0];
                int idDestino = (Integer) request.getParams()[1];
                double monto = (Double) request.getParams()[2];
                
                // Generar ID de transacción único
                int idTransaccion;
                synchronized (this) {
                    idTransaccion = nextTransactionId++;
                }
                
                // Verificar que tenemos nodos disponibles para ambas cuentas
                List<Integer> nodesForOrigin = findNodesForAccount(idOrigen);
                List<Integer> nodesForDest = findNodesForAccount(idDestino);
                
                if (nodesForOrigin.isEmpty() || nodesForDest.isEmpty()) {
                    response.setError("No hay nodos disponibles para procesar la transferencia");
                    return response;
                }
                
                // Agregar ID de transacción al mensaje
                Object[] paramsWithId = new Object[request.getParams().length + 1];
                System.arraycopy(request.getParams(), 0, paramsWithId, 0, request.getParams().length);
                paramsWithId[paramsWithId.length - 1] = idTransaccion;
                request.setParams(paramsWithId);
                
                // Intentar procesar la transferencia en un nodo que tenga la cuenta origen
                boolean transferSuccess = false;
                String errorMsg = "No se pudo procesar la transferencia";
                double newBalance = 0;
                
                for (Integer nodeId : nodesForOrigin) {
                    NodeInfo node = activeNodes.get(nodeId);
                    
                    try (Socket nodeSocket = new Socket(node.host, node.port);
                         ObjectOutputStream nodeOut = new ObjectOutputStream(nodeSocket.getOutputStream());
                         ObjectInputStream nodeIn = new ObjectInputStream(nodeSocket.getInputStream())) {
                        
                        nodeOut.writeObject(request);
                        Message nodeResponse = (Message) nodeIn.readObject();
                        
                        if (nodeResponse.isOk()) {
                            transferSuccess = true;
                            newBalance = (Double) nodeResponse.getResult();
                            break;
                        } else {
                            errorMsg = nodeResponse.getStatus();
                        }
                    } catch (Exception e) {
                        node.isActive = false;
                        errorMsg = "Error de conexión con nodo: " + e.getMessage();
                    }
                }
                
                if (transferSuccess) {
                    response.setResult(newBalance);
                    
                    // Sincronizar la actualización con otros nodos en segundo plano
                    CompletableFuture.runAsync(() -> {
                        syncTransferToOtherNodes(request, nodesForOrigin.get(0));
                    });
                } else {
                    response.setError(errorMsg);
                }
                
            } catch (Exception e) {
                response.setError("Error al procesar transferencia: " + e.getMessage());
            }
            
            return response;
        }
        
        private void syncTransferToOtherNodes(Message transferRequest, int sourceNodeId) {
            // Este método simula la sincronización de la transacción a otros nodos que tienen réplicas
            // En una implementación real, aquí se propagaría la transacción a todos los nodos con réplicas
            
            Object[] params = transferRequest.getParams();
            int idOrigen = (Integer) params[0];
            int idDestino = (Integer) params[1];
            
            // Sincronizar con otros nodos que tienen la cuenta origen
            for (Map.Entry<Integer, NodeInfo> entry : activeNodes.entrySet()) {
                int nodeId = entry.getKey();
                NodeInfo node = entry.getValue();
                
                // Saltamos el nodo que ya procesó la transacción
                if (nodeId == sourceNodeId || !node.isActive) {
                    continue;
                }
                
                if (nodeDataPartitions.get(nodeId).contains(idOrigen) || 
                    nodeDataPartitions.get(nodeId).contains(idDestino)) {
                    try (Socket nodeSocket = new Socket(node.host, node.port);
                         ObjectOutputStream nodeOut = new ObjectOutputStream(nodeSocket.getOutputStream());
                         ObjectInputStream nodeIn = new ObjectInputStream(nodeSocket.getInputStream())) {
                        
                        // Marcar el mensaje como sincronización
                        Message syncMessage = new Message(transferRequest.getType(), params);
                        nodeOut.writeObject(syncMessage);
                        nodeIn.readObject(); // Leer respuesta pero no la necesitamos
                        
                    } catch (Exception e) {
                        node.isActive = false;
                        System.out.println("Error sincronizando nodo " + nodeId + ": " + e.getMessage());
                    }
                }
            }
        }
    }
    
    public static void main(String[] args) {
        CentralServer server = new CentralServer();
        server.start();
    }
}