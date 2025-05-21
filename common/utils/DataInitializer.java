package common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad para inicializar archivos de datos
 */
public class DataInitializer {
    
    public static void initializeDataFiles(String dataPath) throws IOException {
        File dir = new File(dataPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // Inicializar archivo de clientes si no existe
        File clientesFile = new File(dataPath + "/clientes.txt");
        if (!clientesFile.exists()) {
            try (FileWriter writer = new FileWriter(clientesFile)) {
                writer.write("# ID_CLIENTE | NOMBRE | EMAIL | TELÉFONO\n");
                writer.write("1|Juan Pérez|juan@email.com|987654321\n");
                writer.write("2|María López|maria@email.com|998877665\n");
                writer.write("3|Carlos Ruiz|carlos@email.com|955544333\n");
                writer.write("4|Ana Torres|ana@email.com|911122334\n");
                writer.write("5|Luis Mendez|luis@email.com|966677889\n");
            }
        }
        
        // Inicializar archivo de cuentas si no existe
        File cuentasFile = new File(dataPath + "/cuentas.txt");
        if (!cuentasFile.exists()) {
            try (FileWriter writer = new FileWriter(cuentasFile)) {
                writer.write("# ID_CUENTA | ID_CLIENTE | SALDO | TIPO_CUENTA\n");
                writer.write("101|1|1500.00|Ahorros\n");
                writer.write("102|2|3200.50|Corriente\n");
                writer.write("103|3|2750.75|Ahorros\n");
                writer.write("104|4|5000.00|Ahorros\n");
                writer.write("105|5|12500.25|Corriente\n");
                writer.write("106|1|800.00|Corriente\n");
                writer.write("107|2|1200.50|Ahorros\n");
                writer.write("108|3|3500.00|Corriente\n");
                writer.write("109|4|750.30|Ahorros\n");
                writer.write("110|5|9200.00|Ahorros\n");
            }
        }
        
        // Inicializar archivo de transacciones si no existe
        File transaccionesFile = new File(dataPath + "/transacciones.txt");
        if (!transaccionesFile.exists()) {
            try (FileWriter writer = new FileWriter(transaccionesFile)) {
                writer.write("# ID_TRANSACC | ID_ORIG | ID_DEST | MONTO | FECHA_HORA | ESTADO\n");
                writer.write("1|101|102|500.00|2025-05-02T14:30:00|Confirmada\n");
                writer.write("2|102|101|200.00|2025-05-02T15:00:00|Confirmada\n");
                writer.write("3|103|105|300.50|2025-05-03T09:15:00|Confirmada\n");
                writer.write("4|104|106|150.25|2025-05-03T12:30:00|Confirmada\n");
                writer.write("5|107|108|1000.00|2025-05-04T08:45:00|Confirmada\n");
            }
        }
        
        System.out.println("Archivos de datos inicializados en: " + dataPath);
    }
    
    public static void main(String[] args) {
        try {
            initializeDataFiles("./data");
        } catch (IOException e) {
            System.err.println("Error inicializando datos: " + e.getMessage());
        }
    }
}