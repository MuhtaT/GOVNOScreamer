package com.dayab.widget.service;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * Сервис для воспроизведения звуковых уведомлений
 * Создает ужасный звук для привлечения внимания при пампе
 * Поддерживает кастомные звуковые файлы
 */
public class SoundAlertService {
    
    private volatile boolean isPlaying = false;
    private Clip currentClip;
    
    /**
     * Воспроизводит звук тревоги при пампе
     * Если указан кастомный файл, воспроизводит его, иначе генерирует стандартный звук
     */
    public void playPumpAlert(String customSoundFile) {
        if (isPlaying) return;
        
        new Thread(() -> {
            try {
                isPlaying = true;
                
                if (customSoundFile != null && !customSoundFile.trim().isEmpty()) {
                    // Пытаемся воспроизвести кастомный файл
                    if (playCustomSoundFile(customSoundFile)) {
                        return; // Успешно воспроизвели кастомный звук
                    } else {
                        System.err.println("Не удалось воспроизвести кастомный файл, используем стандартный звук");
                    }
                }
                
                // Воспроизводим стандартный генерированный звук
                playGeneratedPumpSound();
                
            } catch (Exception e) {
                System.err.println("Ошибка воспроизведения звука: " + e.getMessage());
            } finally {
                isPlaying = false;
            }
        }).start();
    }
    
    /**
     * Воспроизводит стандартный звук пампа (без кастомного файла)
     */
    public void playPumpAlert() {
        playPumpAlert(null);
    }
    
    /**
     * Воспроизводит кастомный звуковой файл
     */
    private boolean playCustomSoundFile(String filePath) {
        try {
            File soundFile = new File(filePath);
            if (!soundFile.exists()) {
                System.err.println("Звуковой файл не найден: " + filePath);
                return false;
            }
            
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            currentClip = AudioSystem.getClip();
            currentClip.open(audioStream);
            
            // Воспроизводим файл один раз (можно настроить)
            currentClip.start();
            
            // Ждем завершения
            while (currentClip.isRunning()) {
                Thread.sleep(100);
            }
            
            currentClip.close();
            audioStream.close();
            
            System.out.println("Успешно воспроизведен кастомный звук: " + soundFile.getName());
            return true;
            
        } catch (Exception e) {
            System.err.println("Ошибка воспроизведения кастомного файла: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Воспроизводит стандартный генерированный звук пампа
     */
    private void playGeneratedPumpSound() {
        try {
            // Создаем ужасный звук - комбинация высоких и низких частот
            byte[] soundData = generatePumpSound();
            
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            AudioInputStream audioStream = new AudioInputStream(
                new ByteArrayInputStream(soundData), format, soundData.length / 2
            );
            
            currentClip = AudioSystem.getClip();
            currentClip.open(audioStream);
            
            // Воспроизводим звук 3 раза
            currentClip.loop(2);
            currentClip.start();
            
            // Ждем завершения
            while (currentClip.isRunning()) {
                Thread.sleep(100);
            }
            
            currentClip.close();
            
        } catch (Exception e) {
            System.err.println("Ошибка воспроизведения стандартного звука: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Воспроизводит короткий сигнал обновления данных
     */
    public void playUpdateSound() {
        new Thread(() -> {
            try {
                byte[] soundData = generateUpdateSound();
                
                AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
                AudioInputStream audioStream = new AudioInputStream(
                    new ByteArrayInputStream(soundData), format, soundData.length / 2
                );
                
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
                
                // Ждем завершения
                while (clip.isRunning()) {
                    Thread.sleep(10);
                }
                
                clip.close();
                
            } catch (Exception e) {
                System.err.println("Ошибка воспроизведения звука обновления: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Останавливает текущее воспроизведение
     */
    public void stopCurrentSound() {
        if (currentClip != null && currentClip.isRunning()) {
            currentClip.stop();
            currentClip.close();
            isPlaying = false;
        }
    }
    
    /**
     * Генерирует ужасный звук для алерта о пампе
     */
    private byte[] generatePumpSound() {
        int duration = 2; // 2 секунды
        int sampleRate = 44100;
        int samples = duration * sampleRate;
        byte[] soundData = new byte[samples * 2]; // 16-bit sound
        
        for (int i = 0; i < samples; i++) {
            double time = (double) i / sampleRate;
            
            // Комбинация раздражающих частот
            double frequency1 = 800 + Math.sin(time * 10) * 200; // Модулирующий тон
            double frequency2 = 1200 + Math.cos(time * 15) * 300; // Высокий пронзительный
            double frequency3 = 200 + Math.sin(time * 5) * 50;    // Низкий грохот
            
            // Создаем хаотичный звук
            double wave1 = Math.sin(2 * Math.PI * frequency1 * time);
            double wave2 = Math.sin(2 * Math.PI * frequency2 * time) * 0.7;
            double wave3 = Math.sin(2 * Math.PI * frequency3 * time) * 0.5;
            
            // Добавляем искажения
            double distortion = Math.sin(time * 50) * 0.3;
            double sample = (wave1 + wave2 + wave3) * (0.8 + distortion);
            
            // Ограничиваем амплитуду
            sample = Math.max(-1.0, Math.min(1.0, sample));
            
            // Конвертируем в 16-bit
            short sampleValue = (short) (sample * Short.MAX_VALUE);
            soundData[i * 2] = (byte) (sampleValue & 0xFF);
            soundData[i * 2 + 1] = (byte) ((sampleValue >> 8) & 0xFF);
        }
        
        return soundData;
    }
    
    /**
     * Генерирует тихий звук обновления данных
     */
    private byte[] generateUpdateSound() {
        int duration = 200; // 200мс
        int sampleRate = 44100;
        int samples = duration * sampleRate / 1000;
        byte[] soundData = new byte[samples * 2];
        
        for (int i = 0; i < samples; i++) {
            double time = (double) i / sampleRate;
            double frequency = 600; // Приятная частота
            
            // Простой синусоидальный сигнал с затуханием
            double envelope = 1.0 - (double) i / samples; // Затухание
            double sample = Math.sin(2 * Math.PI * frequency * time) * envelope * 0.3;
            
            short sampleValue = (short) (sample * Short.MAX_VALUE);
            soundData[i * 2] = (byte) (sampleValue & 0xFF);
            soundData[i * 2 + 1] = (byte) ((sampleValue >> 8) & 0xFF);
        }
        
        return soundData;
    }
    
    /**
     * Проверяет доступность аудио системы
     */
    public boolean isAudioAvailable() {
        try {
            return AudioSystem.getMixer(null) != null;
        } catch (Exception e) {
            return false;
        }
    }
} 