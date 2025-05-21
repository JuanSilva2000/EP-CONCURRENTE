package client;

import common.utils.Message;
import common.utils.Message.OperationType;

import java.io.*;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BankClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000;
    private static final Random random = new Random();
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== CLIENTE BANCARIO ===");
        System.out.println("1. Simular múltiples clientes concurrentes");
        System.out.println("2. Modo interactivo");
        System.out.print("Seleccione una opción: ");
        int option = scanner.nextInt();
        
        if (option == 1) {
            simulateConcurrentClients();
        } else {
            interactiveMode();
        }
    }
    
    private static void interactiveMode() {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n=== MENÚ CLIENTE BANCARIO ===");
            System.out.println("1. Consultar saldo");
            System.out.println("2. Realizar transferencia");
            System.out.println("3. Salir");
            System.out.print("Seleccione una opción: ");
            
            int option = scanner.nextInt();
            
            switch (option) {
                case 1:
                    consultarSaldo(scanner);
                    break;
                case 2:
                    realizarTransferencia(scanner);
                    break;
                case 3:
                    System.out.println("¡Gracias por usar nuestro sistema bancario!");
                    return;
                default:
                    System.out.println("Opción no válida.");
            }
        }
    }
    
    private static void consultarSaldo(Scanner scanner) {
        System.out.print("Ingrese el ID de la cuenta: ");
        int idCuenta = scanner.nextInt();
        
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            // Crear mensaje para consultar saldo
            Message message = new Message(OperationType.CONSULTAR_SALDO, idCuenta);
            out.writeObject(message);
            
            // Recibir respuesta
            Message response = (Message) in.readObject();
            
            if (response.isOk()) {
                System.out.println("Saldo de la cuenta " + idCuenta + ": " + response.getResult());
            } else {
                System.out.println("Error: " + response.getStatus());
            }
            
        } catch (Exception e) {
            System.out.println("Error al conectar con el servidor: " + e.getMessage());
        }
    }
    
    private static void realizarTransferencia(Scanner scanner) {
        System.out.print("Ingrese el ID de la cuenta origen: ");
        int idOrigen = scanner.nextInt();
        System.out.print("Ingrese el ID de la cuenta destino: ");
        int idDestino = scanner.nextInt();
        System.out.print("Ingrese el monto a transferir: ");
        double monto = scanner.nextDouble();
        
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            // Crear mensaje para transferir fondos
            Message message = new Message(OperationType.TRANSFERIR_FONDOS, idOrigen, idDestino, monto);
            out.writeObject(message);
            
            // Recibir respuesta
            Message response = (Message) in.readObject();
            
            if (response.isOk()) {
                System.out.println("Transferencia realizada con éxito. Nuevo saldo: " + response.getResult());
            } else {
                System.out.println("Error en la transferencia: " + response.getStatus());
            }
            
        } catch (Exception e) {
            System.out.println("Error al conectar con el servidor: " + e.getMessage());
        }
    }
    
    private static void simulateConcurrentClients() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Número de clientes a simular: ");
        int numClients = scanner.nextInt();
        System.out.print("Número de operaciones por cliente: ");
        int numOperations = scanner.nextInt();
        
        ExecutorService executor = Executors.newFixedThreadPool(numClients);
        
        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < numOperations; j++) {
                        // Simular delay aleatorio entre operaciones (entre 100ms y 2s)
                        Thread.sleep(100 + random.nextInt(1900));
                        
                        // Decidir aleatoriamente entre consulta y transferencia
                        if (random.nextBoolean()) {
                            // Consultar saldo de una cuenta aleatoria (asumiendo IDs entre 101 y 110)
                            int idCuenta = 101 + random.nextInt(10);
                            consultarSaldo(idCuenta, clientId);
                        } else {
                            // Transferir entre cuentas aleatorias
                            int idOrigen = 101 + random.nextInt(10);
                            int idDestino;
                            do {
                                idDestino = 101 + random.nextInt(10);
                            } while (idDestino == idOrigen);
                            
                            // Monto aleatorio entre 10 y 500
                            double monto = 10 + random.nextDouble() * 490;
                            realizarTransferencia(idOrigen, idDestino, monto, clientId);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.shutdown();
        System.out.println("Simulación iniciada con " + numClients + " clientes.");
    }
    
    private static void consultarSaldo(int idCuenta, int clientId) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            Message message = new Message(OperationType.CONSULTAR_SALDO, idCuenta);
            out.writeObject(message);
            
            Message response = (Message) in.readObject();
            System.out.println("Cliente " + clientId + " - Consulta saldo cuenta " + idCuenta + 
                    ": " + (response.isOk() ? response.getResult() : "Error: " + response.getStatus()));
            
        } catch (Exception e) {
            System.out.println("Cliente " + clientId + " - Error: " + e.getMessage());
        }
    }
    
    private static void realizarTransferencia(int idOrigen, int idDestino, double monto, int clientId) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            Message message = new Message(OperationType.TRANSFERIR_FONDOS, idOrigen, idDestino, monto);
            out.writeObject(message);
            
            Message response = (Message) in.readObject();
            System.out.println("Cliente " + clientId + " - Transferencia " + idOrigen + " -> " + idDestino + 
                    " por $" + String.format("%.2f", monto) + ": " + 
                    (response.isOk() ? "Exitosa" : "Error: " + response.getStatus()));
            
        } catch (Exception e) {
            System.out.println("Cliente " + clientId + " - Error: " + e.getMessage());
        }
    }
}