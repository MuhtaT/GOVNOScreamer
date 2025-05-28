package com.dayab.widget;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Главное приложение виджета для Windows
 * Современный виджет с красивым UI и функциональностью
 */
public class WidgetApplication extends Application {
    
    private Label timeLabel;
    private Label dateLabel;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Timeline timeline;
    private Stage primaryStage;
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    
    // Переменные для перетаскивания окна
    private double mouseX, mouseY;
    
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        
        // Создаем основное окно
        createMainWindow(stage);
        
        // Инициализируем системный трей
        initSystemTray();
        
        // Запускаем таймер для обновления времени
        startTimeUpdater();
        
        // Настраиваем перетаскивание
        setupWindowDragging();
        
        stage.show();
    }
    
    private void createMainWindow(Stage stage) {
        // Настройка окна
        stage.setTitle("GOVNOScreamer Widget");
        stage.initStyle(StageStyle.TRANSPARENT); // Полностью прозрачное окно
        stage.setResizable(false);
        stage.setAlwaysOnTop(true);
        
        // Основная панель
        VBox mainBox = new VBox(15);
        mainBox.setAlignment(Pos.CENTER);
        mainBox.setPadding(new Insets(20));
        
        // Создаем градиентный фон
        LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, 
            null, 
            new Stop(0, Color.web("#667eea")),
            new Stop(1, Color.web("#764ba2"))
        );
        
        Background background = new Background(new BackgroundFill(gradient, 
            new CornerRadii(15), Insets.EMPTY));
        mainBox.setBackground(background);
        
        // Добавляем тень
        DropShadow shadow = new DropShadow();
        shadow.setRadius(10);
        shadow.setColor(Color.BLACK.deriveColor(0, 1, 1, 0.3));
        mainBox.setEffect(shadow);
        
        // Заголовок
        Label titleLabel = new Label("🚀 ГОВНО Скример");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);
        
        // Время
        timeLabel = new Label();
        timeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        timeLabel.setTextFill(Color.WHITE);
        
        // Дата
        dateLabel = new Label();
        dateLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        dateLabel.setTextFill(Color.web("#E0E0E0"));
        
        // Статус
        statusLabel = new Label("Готов к работе");
        statusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        statusLabel.setTextFill(Color.web("#90EE90"));
        
        // Прогресс бар
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressBar.setStyle("-fx-accent: #4CAF50;");
        
        // Кнопки
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button startButton = createStyledButton("Запустить", "#4CAF50");
        Button stopButton = createStyledButton("Остановить", "#f44336");
        Button settingsButton = createStyledButton("Настройки", "#2196F3");
        
        // Обработчики кнопок
        startButton.setOnAction(e -> startProcess());
        stopButton.setOnAction(e -> stopProcess());
        settingsButton.setOnAction(e -> showSettings());
        
        buttonBox.getChildren().addAll(startButton, stopButton, settingsButton);
        
        // Добавляем все элементы
        mainBox.getChildren().addAll(
            titleLabel,
            timeLabel,
            dateLabel,
            new Separator(),
            statusLabel,
            progressBar,
            buttonBox
        );
        
        Scene scene = new Scene(mainBox, 320, 280);
        scene.setFill(Color.TRANSPARENT); // Прозрачный фон сцены
        stage.setScene(scene);
        
        // Позиционируем окно в правом верхнем углу
        stage.setX(Toolkit.getDefaultToolkit().getScreenSize().width - 350);
        stage.setY(30);
    }
    
    private void setupWindowDragging() {
        Scene scene = primaryStage.getScene();
        
        scene.setOnMousePressed(event -> {
            mouseX = event.getSceneX();
            mouseY = event.getSceneY();
        });
        
        scene.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - mouseX);
            primaryStage.setY(event.getScreenY() - mouseY);
        });
        
        // Правый клик - контекстное меню
        scene.setOnMouseClicked(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                showContextMenu(event);
            }
        });
    }
    
    private void showContextMenu(javafx.scene.input.MouseEvent event) {
        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
        
        javafx.scene.control.MenuItem hideItem = new javafx.scene.control.MenuItem("Скрыть в трей");
        javafx.scene.control.MenuItem settingsItem = new javafx.scene.control.MenuItem("Настройки");
        javafx.scene.control.MenuItem exitItem = new javafx.scene.control.MenuItem("Выход");
        
        hideItem.setOnAction(e -> primaryStage.hide());
        settingsItem.setOnAction(e -> showSettings());
        exitItem.setOnAction(e -> {
            Platform.exit();
            System.exit(0);
        });
        
        contextMenu.getItems().addAll(hideItem, settingsItem, new javafx.scene.control.SeparatorMenuItem(), exitItem);
        contextMenu.show(primaryStage, event.getScreenX(), event.getScreenY());
    }
    
    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        button.setTextFill(Color.WHITE);
        button.setStyle(
            "-fx-background-color: " + color + "; " +
            "-fx-background-radius: 20; " +
            "-fx-border-radius: 20; " +
            "-fx-padding: 8 16 8 16;"
        );
        
        // Эффект при наведении
        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: derive(" + color + ", -20%); " +
            "-fx-background-radius: 20; " +
            "-fx-border-radius: 20; " +
            "-fx-padding: 8 16 8 16;"
        ));
        
        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: " + color + "; " +
            "-fx-background-radius: 20; " +
            "-fx-border-radius: 20; " +
            "-fx-padding: 8 16 8 16;"
        ));
        
        return button;
    }
    
    private void initSystemTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray не поддерживается");
            return;
        }
        
        systemTray = SystemTray.getSystemTray();
        
        // Создаем простую иконку для трея
        java.awt.Image image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = ((java.awt.image.BufferedImage) image).createGraphics();
        g.setColor(java.awt.Color.BLUE);
        g.fillOval(0, 0, 16, 16);
        g.dispose();
        
        PopupMenu popup = new PopupMenu();
        MenuItem showItem = new MenuItem("Показать");
        MenuItem exitItem = new MenuItem("Выход");
        
        showItem.addActionListener(e -> Platform.runLater(() -> {
            primaryStage.show();
            primaryStage.toFront();
        }));
        
        exitItem.addActionListener(e -> Platform.runLater(() -> {
            Platform.exit();
            System.exit(0);
        }));
        
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
        
        // Скрываем в трей при закрытии
        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            primaryStage.hide();
        });
    }
    
    private void startTimeUpdater() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTime()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
    
    private void updateTime() {
        LocalDateTime now = LocalDateTime.now();
        timeLabel.setText(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        dateLabel.setText(now.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
    }
    
    private void startProcess() {
        statusLabel.setText("Процесс запущен");
        statusLabel.setTextFill(Color.web("#FFD700"));
        
        // Анимация прогресс бара
        Timeline progressTimeline = new Timeline();
        for (int i = 0; i <= 100; i++) {
            final int progress = i;
            progressTimeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(i * 50), 
                    e -> progressBar.setProgress(progress / 100.0))
            );
        }
        progressTimeline.setOnFinished(e -> {
            statusLabel.setText("Процесс завершен");
            statusLabel.setTextFill(Color.web("#90EE90"));
        });
        progressTimeline.play();
    }
    
    private void stopProcess() {
        statusLabel.setText("Процесс остановлен");
        statusLabel.setTextFill(Color.web("#f44336"));
        progressBar.setProgress(0);
    }
    
    private void showSettings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Настройки");
        alert.setHeaderText("Настройки виджета");
        alert.setContentText("Здесь будут настройки вашего виджета!");
        
        // Стилизуем диалоговое окно
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white;");
        
        alert.showAndWait();
    }
    
    @Override
    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
    }
    
    public static void main(String[] args) {
        // Устанавливаем системные свойства для лучшей совместимости с Windows
        System.setProperty("javafx.platform", "desktop");
        System.setProperty("glass.platform", "win");
        
        launch(args);
    }
} 