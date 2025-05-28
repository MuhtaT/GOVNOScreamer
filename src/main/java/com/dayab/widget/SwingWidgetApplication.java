package com.dayab.widget;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Альтернативная версия виджета на Swing
 * Гарантированная совместимость с любой Java версией
 */
public class SwingWidgetApplication extends JFrame {
    
    private JLabel timeLabel;
    private JLabel dateLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private Timer timer;
    private Timer progressTimer;
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    
    // Переменные для перетаскивания окна
    private int mouseX, mouseY;
    
    public SwingWidgetApplication() {
        initializeUI();
        initSystemTray();
        startTimeUpdater();
        setupWindowDragging();
    }
    
    private void initializeUI() {
        setTitle("GOVNOScreamer Widget");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setResizable(false);
        setAlwaysOnTop(true);
        setUndecorated(true); // Убираем декорации окна
        setBackground(new Color(0, 0, 0, 0)); // Полностью прозрачный фон
        
        // Создаем основную панель с градиентом
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Градиентный фон
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(102, 126, 234),
                    getWidth(), getHeight(), new Color(118, 75, 162)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            }
        };
        
        mainPanel.setOpaque(false); // Делаем панель прозрачной
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Заголовок
        JLabel titleLabel = new JLabel("🚀 ГОВНО Скример", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Время
        timeLabel = new JLabel("", JLabel.CENTER);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 18));
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Дата
        dateLabel = new JLabel("", JLabel.CENTER);
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        dateLabel.setForeground(new Color(224, 224, 224));
        dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Статус
        statusLabel = new JLabel("Готов к работе", JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(144, 238, 144));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Прогресс бар
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setMaximumSize(new Dimension(200, 25));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Кнопки
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false); // Прозрачная панель кнопок
        
        JButton startButton = createStyledButton("Запустить", new Color(76, 175, 80));
        JButton stopButton = createStyledButton("Остановить", new Color(244, 67, 54));
        JButton settingsButton = createStyledButton("Настройки", new Color(33, 150, 243));
        
        // Обработчики кнопок
        startButton.addActionListener(e -> startProcess());
        stopButton.addActionListener(e -> stopProcess());
        settingsButton.addActionListener(e -> showSettings());
        
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(settingsButton);
        
        // Добавляем компоненты
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(timeLabel);
        mainPanel.add(dateLabel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(new JSeparator());
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(progressBar);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(buttonPanel);
        
        add(mainPanel);
        
        // Размер и позиция
        setSize(320, 280);
        
        // Позиционируем в правом верхнем углу
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width - getWidth() - 30, 30);
        
        updateTime(); // Устанавливаем начальное время
    }
    
    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Color bgColor = getModel().isPressed() ? color.darker() : 
                               getModel().isRollover() ? color.brighter() : color;
                
                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                g2d.setColor(Color.WHITE);
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 2;
                g2d.drawString(getText(), x, y);
            }
        };
        
        button.setFont(new Font("Arial", Font.BOLD, 10));
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(80, 30));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        
        return button;
    }
    
    private void initSystemTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray не поддерживается");
            return;
        }
        
        systemTray = SystemTray.getSystemTray();
        
        // Создаем иконку для трея
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(102, 126, 234));
        g.fillOval(0, 0, 16, 16);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString("G", 5, 12);
        g.dispose();
        
        PopupMenu popup = new PopupMenu();
        MenuItem showItem = new MenuItem("Показать");
        MenuItem exitItem = new MenuItem("Выход");
        
        showItem.addActionListener(e -> {
            setVisible(true);
            setState(JFrame.NORMAL);
            toFront();
        });
        
        exitItem.addActionListener(e -> {
            System.exit(0);
        });
        
        popup.add(showItem);
        popup.addSeparator();
        popup.add(exitItem);
        
        trayIcon = new TrayIcon(image, "GOVNOScreamer Widget", popup);
        trayIcon.setImageAutoSize(true);
        
        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("Не удалось добавить иконку в трей");
        }
    }
    
    private void startTimeUpdater() {
        timer = new Timer(1000, e -> updateTime());
        timer.start();
    }
    
    private void updateTime() {
        LocalDateTime now = LocalDateTime.now();
        timeLabel.setText(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        dateLabel.setText(now.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
    }
    
    private void startProcess() {
        statusLabel.setText("Процесс запущен");
        statusLabel.setForeground(new Color(255, 215, 0)); // Золотой
        
        progressBar.setValue(0);
        
        // Анимация прогресс бара
        if (progressTimer != null) {
            progressTimer.stop();
        }
        
        progressTimer = new Timer(50, new ActionListener() {
            int progress = 0;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                progress++;
                progressBar.setValue(progress);
                
                if (progress >= 100) {
                    progressTimer.stop();
                    statusLabel.setText("Процесс завершен");
                    statusLabel.setForeground(new Color(144, 238, 144)); // Светло-зеленый
                }
            }
        });
        progressTimer.start();
    }
    
    private void stopProcess() {
        if (progressTimer != null) {
            progressTimer.stop();
        }
        
        statusLabel.setText("Процесс остановлен");
        statusLabel.setForeground(new Color(244, 67, 54)); // Красный
        progressBar.setValue(0);
    }
    
    private void showSettings() {
        JOptionPane.showMessageDialog(
            this,
            "Здесь будут настройки вашего виджета!\n\n" +
            "• Автозапуск\n" +
            "• Позиция окна\n" +
            "• Цветовая схема\n" +
            "• Звуковые уведомления",
            "Настройки виджета",
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    private void setupWindowDragging() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                // Правый клик - показываем контекстное меню
                if (e.getButton() == MouseEvent.BUTTON3) {
                    showContextMenu(e);
                }
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - mouseX;
                int dy = e.getY() - mouseY;
                setLocation(getX() + dx, getY() + dy);
            }
        });
    }
    
    private void showContextMenu(MouseEvent e) {
        javax.swing.JPopupMenu contextMenu = new javax.swing.JPopupMenu();
        
        javax.swing.JMenuItem hideItem = new javax.swing.JMenuItem("Скрыть в трей");
        javax.swing.JMenuItem settingsItem = new javax.swing.JMenuItem("Настройки");
        javax.swing.JMenuItem exitItem = new javax.swing.JMenuItem("Выход");
        
        hideItem.addActionListener(ev -> setVisible(false));
        settingsItem.addActionListener(ev -> showSettings());
        exitItem.addActionListener(ev -> System.exit(0));
        
        contextMenu.add(hideItem);
        contextMenu.add(settingsItem);
        contextMenu.addSeparator();
        contextMenu.add(exitItem);
        
        contextMenu.show(this, e.getX(), e.getY());
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SwingWidgetApplication app = new SwingWidgetApplication();
            app.setVisible(true);
        });
    }
} 