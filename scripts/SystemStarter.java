package scripts;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.utils.DataInitializer;

/**
 * Script para iniciar todo el sistema bancario distribuido
 */
public class SystemStarter {
    private static final int NODE_COUNT = 3;
    private static final int NODE_PORT_BASE = 9100;
    
    public static void main(String[] args) {
        try {
            // Inicializar archivos de datos
            DataInitializer.initializeDataFiles("./data");
            
            // Crear directorios para cada nodo
            for (int i = 0; i < NODE_COUNT; i++) {
                String nodeDataPath = "./data/node" + i;
                new File(nodeDataPath).mkdirs();
                // Copiar datos a cada nodo (en una implementación real serían particiones)
                // Para simplificar, cada nodo tiene todos los datos
                copyFiles("./data", nodeDataPath);
            }
            
            // Iniciar nodos trabajadores en hilos separados
            ExecutorService executor = Executors.newFixedThreadPool(NODE_COUNT + 1);
            
            for (int i = 0; i < NODE_COUNT; i++) {
                final int nodeId = i;
                executor.submit(() -> {
                    String[] nodeArgs = {
                        String.valueOf(nodeId),
                        String.valueOf(NODE_PORT_BASE + nodeId),
                        "./data/node" + nodeId
                    };
                    node.WorkerNode.main(nodeArgs);
                });
                
                // Esperar un poco para que el nodo se inicie
                Thread.sleep(1000);
            }
            
            // Iniciar servidor central
            executor.submit(() -> {
                server.CentralServer.main(new String[0]);
            });
            
            System.out.println("Sistema bancario distribuido iniciado correctamente");
            System.out.println("Servidor central en puerto 9000");
            for (int i = 0; i < NODE_COUNT; i++) {
                System.out.println("Nodo " + i + " en puerto " + (NODE_PORT_BASE + i));
            }
            
        } catch (Exception e) {
            System.err.println("Error iniciando sistema: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void copyFiles(String sourceDir, String targetDir) throws IOException {
        // En una implementación real, aquí se copiarían los archivos necesarios
        // Para simplificar, vamos a crear nuevos archivos idénticos
        DataInitializer.initializeDataFiles(targetDir);
    }
}