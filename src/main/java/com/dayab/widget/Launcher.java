package com.dayab.widget;

/**
 * Launcher класс для запуска GOVNO Screamer Widget
 */
public class Launcher {
    public static void main(String[] args) {
        System.out.println("Запуск GOVNO Screamer Widget...");
        
        // Запускаем наш Swing виджет
        try {
            CryptoScreamerWidget.main(args);
        } catch (Exception e) {
            System.err.println("Ошибка запуска виджета: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
} 