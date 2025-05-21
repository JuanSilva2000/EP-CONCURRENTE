package scripts;

import common.utils.Message;
import common.utils.Message.OperationType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Script para realizar pruebas de carga en el sistema
 */
public class LoadTester {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000;
    private static final int NUM_THREADS = 50;
    private static final int OPERATIONS_PER_THREAD = 100;
    private static final Random random = new Random();
    
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failCount = new AtomicInteger(0);
    private static final AtomicInteger totalOperations = new AtomicInteger(0);
    
    public static void main(String[] args) {
        System.out.println("Iniciando prueba de carga...");
        System.out.println("Threads: " + NUM_THREADS);
        System.out.println("Operaciones por thread: " + OPERATIONS_PER_THREAD);
        System.out.println("Total operaciones: " + (NUM_THREADS * OPERATIONS_PER_THREAD));
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(new ClientSimulator(i));
        }
        
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(500);
                System.out.print("\rProgreso: " + totalOperations.get() + 
                        " / " + (NUM_THREADS * OPERATIONS_PER_THREAD) + " operaciones");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        long endTime = System.currentTimeMillis();
        double elapsedSeconds = (endTime - startTime) / 1000.0;
        
        System.out.println("\n\nPrueba de carga completada");
        System.out.println("Tiempo total: " + elapsedSeconds + " segundos");
        System.out.println("Operaciones exitosas: " + successCount.get());
        System.out.println("Operaciones fallidas: " + failCount.get());
        System.out.println("Tasa de éxito: " + 
                (successCount.get() * 100.0 / (NUM_THREADS * OPERATIONS_PER_THREAD)) + "%");
        System.out.println("Operaciones por segundo: " + 
                (NUM_THREADS * OPERATIONS_PER_THREAD / elapsedSeconds));
    }
    
    private static class ClientSimulator implements Runnable {
        private final int clientId;
        
        public ClientSimulator(int clientId) {
            this.clientId = clientId;
        }
        
        @Override
        public void run() {
            try {
                for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                    // Simular delay aleatorio entre operaciones (entre 10ms y 100ms)
                    Thread.sleep(10 + random.nextInt(90));
                    
                    // Decidir aleatoriamente entre consulta y transferencia
                    if (random.nextDouble() < 0.7) { // 70% consultas, 30% transferencias
                        // Consultar saldo de una cuenta aleatoria (entre 101 y 110)
                        int idCuenta = 101 + random.nextInt(10);
                        consultarSaldo(idCuenta);
                    } else {
                        // Transferir entre cuentas aleatorias
                        int idOrigen = 101 + random.nextInt(10);
                        int idDestino;
                        do {
                            idDestino = 101 + random.nextInt(10);
                        } while (idDestino == idOrigen);
                        
                        // Monto aleatorio entre 1 y 100
                        double monto = 1 + random.nextDouble() * 99;
                        monto = Math.round(monto * 100) / 100.0; // Redondear a 2 decimales
                        
                        realizarTransferencia(idOrigen, idDestino, monto);
                    }
                    
                    totalOperations.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        private void consultarSaldo(int idCuenta) {
            try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                
                Message message = new Message(OperationType.CONSULTAR_SALDO, idCuenta);
                out.writeObject(message);
                
                Message response = (Message) in.readObject();
                if (response.isOk()) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
                
            } catch (Exception e) {
                failCount.incrementAndGet();
            }
        }
        
        private void realizarTransferencia(int idOrigen, int idDestino, double monto) {
            try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                
                Message message = new Message(OperationType.TRANSFERIR_FONDOS, idOrigen, idDestino, monto);
                out.writeObject(message);
                
                Message response = (Message) in.readObject();
                if (response.isOk()) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
                
            } catch (Exception e) {
                failCount.incrementAndGet();
            }
        }
    }
}