package common.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Clase de utilidad para registrar logs del sistema
 */
public class Logger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Object lock = new Object();
    private static PrintWriter writer;
    private static String logFile = "system.log";
    
    public static void setLogFile(String filename) {
        logFile = filename;
        try {
            synchronized (lock) {
                if (writer != null) {
                    writer.close();
                }
                writer = new PrintWriter(new FileWriter(logFile, true), true);
            }
        } catch (IOException e) {
            System.err.println("Error configurando archivo de log: " + e.getMessage());
        }
    }
    
    public static void log(String component, String message) {
        try {
            String timestamp = LocalDateTime.now().format(formatter);
            String logMessage = timestamp + " [" + component + "] " + message;
            
            synchronized (lock) {
                if (writer == null) {
                    writer = new PrintWriter(new FileWriter(logFile, true), true);
                }
                writer.println(logMessage);
                System.out.println(logMessage);
            }
        } catch (IOException e) {
            System.err.println("Error escribiendo log: " + e.getMessage());
        }
    }
    
    public static void error(String component, String message, Throwable e) {
        log(component, "ERROR: " + message + " - " + e.getMessage());
    }
}