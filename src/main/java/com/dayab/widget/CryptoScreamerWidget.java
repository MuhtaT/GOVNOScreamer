package com.dayab.widget;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.dayab.widget.model.CandleData;
import com.dayab.widget.model.PriceData;
import com.dayab.widget.service.GeckoTerminalApiService;
import com.dayab.widget.service.SoundAlertService;

/**
 * GOVNO/TON Crypto Screamer Widget в стиле Apple
 * Квадратная форма со свечным графиком
 */
public class CryptoScreamerWidget extends JFrame {
    
    // Темы оформления
    public enum Theme {
        DARK, LIGHT
    }
    
    // Варианты компоновки
    public enum Layout {
        CLASSIC("Классический"), COMPACT("Компактный");
        
        private final String displayName;
        
        Layout(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Типы графика
    public enum ChartType {
        CANDLESTICK("Свечи"), LINE("Линия");
        
        private final String displayName;
        
        ChartType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private Theme currentTheme = Theme.DARK;
    private Layout currentLayout = Layout.CLASSIC;
    private ChartType currentChartType = ChartType.CANDLESTICK;
    
    // Сервисы
    private GeckoTerminalApiService apiService;
    private SoundAlertService soundService;
    
    // UI компоненты
    private JLabel priceLabel;
    private JLabel symbolLabel;
    private JLabel trendLabel;
    private JLabel liquidityLabel;  // Добавляем ликвидность
    private JLabel volumeLabel;     // Добавляем объем
    private CandlestickChartPanel chartPanel;
    
    // Системный трей
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    
    // Данные
    private PriceData currentPriceData;
    private List<CandleData> candleHistory;
    private Timer updateTimer;
    
    // Настройки
    private boolean soundEnabled = true;
    private int updateInterval = 5000; // 5 секунд
    private BigDecimal pumpThreshold = new BigDecimal("50");
    private String customSoundFile = null; // Путь к кастомному звуковому файлу
    
    // Настройки временного интервала для свечей
    private String currentTimeframe = "minute";
    private int currentAggregate = 15;
    private int currentLimit = 48;
    
    // Перетаскивание
    private int mouseX, mouseY;
    
    // Шрифты Apple
    private Font sfProDisplayBold;
    private Font sfProDisplayRegular;
    private Font sfProDisplaySmall;
    
    // Иконки стрелок
    private ImageIcon upArrowIcon;
    private ImageIcon downArrowIcon;
    private ImageIcon poopIcon;  // Добавляем иконку какашки
    
    // Цветовые схемы
    private static class ColorScheme {
        public final Color background;
        public final Color border;
        public final Color primaryText;
        public final Color secondaryText;
        public final Color tertiaryText;
        public final Color gridMain;
        public final Color gridSub;
        public final Color gridMicro;
        public final Color bullish;
        public final Color bearish;
        public final Color neutral;
        
        public ColorScheme(Color background, Color border, Color primaryText, Color secondaryText, 
                          Color tertiaryText, Color gridMain, Color gridSub, Color gridMicro,
                          Color bullish, Color bearish, Color neutral) {
            this.background = background;
            this.border = border;
            this.primaryText = primaryText;
            this.secondaryText = secondaryText;
            this.tertiaryText = tertiaryText;
            this.gridMain = gridMain;
            this.gridSub = gridSub;
            this.gridMicro = gridMicro;
            this.bullish = bullish;
            this.bearish = bearish;
            this.neutral = neutral;
        }
    }
    
    private static final ColorScheme DARK_SCHEME = new ColorScheme(
        new Color(28, 28, 30, 255),     // background - темный серый
        new Color(72, 72, 74, 120),     // border - серый полупрозрачный
        Color.WHITE,                     // primaryText - белый
        new Color(174, 174, 178),       // secondaryText - светло-серый
        new Color(142, 142, 147),       // tertiaryText - серый
        new Color(72, 72, 74, 80),      // gridMain - основные линии сетки
        new Color(72, 72, 74, 30),      // gridSub - промежуточные линии
        new Color(72, 72, 74, 15),      // gridMicro - мелкие линии
        new Color(52, 199, 89),         // bullish - зеленый
        new Color(255, 69, 58),         // bearish - красный
        new Color(142, 142, 147)        // neutral - серый
    );
    
    private static final ColorScheme LIGHT_SCHEME = new ColorScheme(
        new Color(248, 248, 248, 255),  // background - светло-серый фон
        new Color(210, 210, 215, 150),  // border - темно-серая граница
        new Color(28, 28, 30),          // primaryText - темный текст
        new Color(99, 99, 102),         // secondaryText - темно-серый
        new Color(142, 142, 147),       // tertiaryText - серый
        new Color(210, 210, 215, 100),  // gridMain - основные линии сетки
        new Color(210, 210, 215, 50),   // gridSub - промежуточные линии
        new Color(210, 210, 215, 25),   // gridMicro - мелкие линии
        new Color(40, 167, 69),         // bullish - темно-зеленый
        new Color(220, 53, 69),         // bearish - темно-красный
        new Color(142, 142, 147)        // neutral - серый
    );
    
    private ColorScheme getCurrentColorScheme() {
        return currentTheme == Theme.DARK ? DARK_SCHEME : LIGHT_SCHEME;
    }
    
    public CryptoScreamerWidget() {
        initFonts();
        initIcons();
        initServices();
        initData();
        initUI();
        initSystemTray();
        setupDragging();
        startMonitoring();
    }
    
    /**
     * Инициализация шрифтов San Francisco
     */
    private void initFonts() {
        try {
            String[] sfFontNames = {
                "SF Pro Display", "San Francisco", ".SF NS Text", 
                "Segoe UI", "Arial"
            };
            
            String selectedFontName = "Arial";
            for (String fontName : sfFontNames) {
                Font testFont = new Font(fontName, Font.PLAIN, 12);
                if (!testFont.getFamily().equals("Dialog")) {
                    selectedFontName = fontName;
                    break;
                }
            }
            
            sfProDisplayBold = new Font(selectedFontName, Font.BOLD, 18);        // Цена
            sfProDisplayRegular = new Font(selectedFontName, Font.PLAIN, 11);    // Общий текст
            sfProDisplaySmall = new Font(selectedFontName, Font.PLAIN, 13);      // Тенденция (увеличено с 9 до 13)
            
            System.out.println("Используемый шрифт: " + selectedFontName);
            
        } catch (Exception e) {
            System.err.println("Ошибка загрузки шрифтов: " + e.getMessage());
            sfProDisplayBold = new Font("Arial", Font.BOLD, 18);        // Цена
            sfProDisplayRegular = new Font("Arial", Font.PLAIN, 11);    // Общий текст
            sfProDisplaySmall = new Font("Arial", Font.PLAIN, 13);      // Тенденция (увеличено с 9 до 13)
        }
    }
    
    /**
     * Инициализация иконок стрелок
     */
    private void initIcons() {
        try {
            // Сначала пытаемся загрузить готовые иконки
            upArrowIcon = loadIcon("/icons/arrow-trend-up.png");
            downArrowIcon = loadIcon("/icons/arrow-trend-down.png");
            poopIcon = loadIcon("/icons/poop.png");
            
            // Если стрелочки не найдены, создаем программно
            if (upArrowIcon == null) {
                upArrowIcon = createUpArrowIcon();
                System.out.println("Создана программная иконка стрелки вверх");
            }
            if (downArrowIcon == null) {
                downArrowIcon = createDownArrowIcon();
                System.out.println("Создана программная иконка стрелки вниз");
            }
            
            if (upArrowIcon != null && downArrowIcon != null && poopIcon != null) {
                System.out.println("Все иконки загружены успешно");
            } else {
                System.out.println("Некоторые иконки не найдены, будут использоваться текстовые символы");
            }
            
        } catch (Exception e) {
            System.err.println("Ошибка загрузки иконок: " + e.getMessage());
            upArrowIcon = null;
            downArrowIcon = null;
            poopIcon = null;
        }
    }
    
    private ImageIcon loadIcon(String path) {
        try {
            URL iconUrl = getClass().getResource(path);
            if (iconUrl != null) {
                Image img = ImageIO.read(iconUrl);
                // Для какашки - 20x20, для стрелок - 16x16 (уменьшено с 24x24)
                int size = path.contains("poop") ? 20 : 16;
                Image scaledImg = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImg);
            } else {
                System.err.println("Иконка не найдена: " + path);
                return null;
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки иконки " + path + ": " + e.getMessage());
            return null;
        }
    }
    
    private void initServices() {
        apiService = new GeckoTerminalApiService();
        soundService = new SoundAlertService();
    }
    
    private void initData() {
        candleHistory = new ArrayList<>();
        currentPriceData = new PriceData(BigDecimal.ZERO, "💩 GOVNO");
    }
    
    private void initUI() {
        setTitle("GOVNO Screamer");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        
        // Устанавливаем undecorated только если окно еще не displayable
        if (!isDisplayable()) {
            setUndecorated(true);
            setBackground(new Color(0, 0, 0, 0));
            setAlwaysOnTop(true);
            setResizable(false);
            
            // Убираем отображение в панели задач и ALT+TAB
            setType(Type.UTILITY);
        }
        
        // Размер зависит от компоновки
        if (currentLayout == Layout.CLASSIC) {
            setSize(320, 380);
        } else {
            setSize(320, 400); // Компактный чуть выше для размещения информации сверху
        }
        
        // Главная панель с уменьшенными отступами
        AppleStylePanel mainPanel = new AppleStylePanel();
        mainPanel.setLayout(new BorderLayout(4, 4));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        mainPanel.setOpaque(false);
        
        if (currentLayout == Layout.CLASSIC) {
            setupClassicLayout(mainPanel);
        } else {
            setupCompactLayout(mainPanel);
        }
        
        add(mainPanel);
        
        // Позиция в правом верхнем углу (только если окно еще не отображается)
        if (!isVisible()) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(screenSize.width - getWidth() - 20, 20);
        }
        
        applyTheme();
    }
    
    private void setupClassicLayout(JPanel mainPanel) {
        // Верх: символ и цена
        JPanel topPanel = createTopPanel();
        
        // Центр: свечной график
        chartPanel = new CandlestickChartPanel();
        chartPanel.setPreferredSize(new Dimension(240, 150));
        chartPanel.setOpaque(false);
        
        // Низ: тренд, ликвидность и объем
        JPanel bottomPanel = createBottomPanel();
        
        // Создаем панель с таймлайном
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setOpaque(false);
        bottomContainer.add(bottomPanel, BorderLayout.NORTH);
        bottomContainer.add(createTimelinePanel(), BorderLayout.SOUTH);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(chartPanel, BorderLayout.CENTER);
        mainPanel.add(bottomContainer, BorderLayout.SOUTH);
    }
    
    private void setupCompactLayout(JPanel mainPanel) {
        // Верх: вся информация в одном блоке
        JPanel topInfoPanel = createCompactTopPanel();
        
        // Центр: график большего размера
        chartPanel = new CandlestickChartPanel();
        chartPanel.setPreferredSize(new Dimension(240, 200)); // Больше места для графика
        chartPanel.setOpaque(false);
        
        // Низ: только таймлайн
        JPanel bottomPanel = createTimelinePanel();
        
        mainPanel.add(topInfoPanel, BorderLayout.NORTH);
        mainPanel.add(chartPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Верхняя панель: символ с картинкой какашки слева, цена центрирована и увеличена
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        symbolLabel = new JLabel("GOVNO", SwingConstants.LEFT);
        symbolLabel.setFont(new Font(sfProDisplayBold.getName(), Font.BOLD, 16));
        
        // Добавляем иконку какашки если есть
        if (poopIcon != null) {
            symbolLabel.setIcon(poopIcon);
            symbolLabel.setIconTextGap(6); // Отступ между иконкой и текстом
            symbolLabel.setText("GOVNO"); // Только символ без эмодзи
            System.out.println("Иконка какашки установлена успешно");
        } else {
            symbolLabel.setText("💩 GOVNO"); // Fallback на эмодзи
            System.out.println("Иконка какашки не найдена, используем эмодзи");
        }
        
        priceLabel = new JLabel("$0.0000", SwingConstants.CENTER);
        priceLabel.setFont(new Font(sfProDisplayBold.getName(), Font.BOLD, 24));
        
        panel.add(symbolLabel, BorderLayout.WEST);
        panel.add(priceLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Нижняя панель: тренд, ликвидность и объем
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setOpaque(false);
        
        // Верхняя строка: тренд по центру
        trendLabel = new JLabel("—", SwingConstants.CENTER);
        trendLabel.setFont(sfProDisplaySmall);
        
        // Нижняя строка: ликвидность и объем
        JPanel metricsPanel = new JPanel(new BorderLayout());
        metricsPanel.setOpaque(false);
        
        liquidityLabel = new JLabel("Ликв: —", SwingConstants.LEFT);
        liquidityLabel.setFont(sfProDisplaySmall);
        
        volumeLabel = new JLabel("Объем: —", SwingConstants.RIGHT);
        volumeLabel.setFont(sfProDisplaySmall);
        
        metricsPanel.add(liquidityLabel, BorderLayout.WEST);
        metricsPanel.add(volumeLabel, BorderLayout.EAST);
        
        panel.add(trendLabel, BorderLayout.NORTH);
        panel.add(metricsPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Компактная верхняя панель: вся информация сверху
     */
    private JPanel createCompactTopPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        
        // Первая строка: символ и цена
        JPanel symbolPricePanel = new JPanel(new BorderLayout());
        symbolPricePanel.setOpaque(false);
        
        symbolLabel = new JLabel("GOVNO", SwingConstants.LEFT);
        symbolLabel.setFont(new Font(sfProDisplayBold.getName(), Font.BOLD, 14));
        
        if (poopIcon != null) {
            symbolLabel.setIcon(poopIcon);
            symbolLabel.setIconTextGap(6);
            symbolLabel.setText("GOVNO");
        } else {
            symbolLabel.setText("💩 GOVNO");
        }
        
        priceLabel = new JLabel("$0.0000", SwingConstants.RIGHT);
        priceLabel.setFont(new Font(sfProDisplayBold.getName(), Font.BOLD, 20));
        
        symbolPricePanel.add(symbolLabel, BorderLayout.WEST);
        symbolPricePanel.add(priceLabel, BorderLayout.EAST);
        
        // Вторая строка: тренд по центру
        JPanel trendPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 2));
        trendPanel.setOpaque(false);
        
        trendLabel = new JLabel("—", SwingConstants.CENTER);
        trendLabel.setFont(new Font(sfProDisplayBold.getName(), Font.BOLD, 16));
        trendPanel.add(trendLabel);
        
        // Третья строка: ликвидность и объем
        JPanel metricsPanel = new JPanel(new BorderLayout());
        metricsPanel.setOpaque(false);
        
        liquidityLabel = new JLabel("Ликв: —", SwingConstants.LEFT);
        liquidityLabel.setFont(sfProDisplaySmall);
        
        volumeLabel = new JLabel("Объем: —", SwingConstants.RIGHT);
        volumeLabel.setFont(sfProDisplaySmall);
        
        metricsPanel.add(liquidityLabel, BorderLayout.WEST);
        metricsPanel.add(volumeLabel, BorderLayout.EAST);
        
        panel.add(symbolPricePanel);
        panel.add(trendPanel);
        panel.add(metricsPanel);
        
        return panel;
    }
    
    /**
     * Панель в стиле Apple с непрозрачным тёмным фоном
     */
    private class AppleStylePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // Непрозрачный тёмный фон #1C1C1E
            Color backgroundColor = getCurrentColorScheme().background;
            Color borderColor = getCurrentColorScheme().border;
            
            // Основной фон
            RoundRectangle2D roundRect = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18);
            g2d.setColor(backgroundColor);
            g2d.fill(roundRect);
            
            // Тонкая граница
            g2d.setColor(borderColor);
            g2d.setStroke(new BasicStroke(0.5f));
            g2d.draw(roundRect);
        }
    }
    
    /**
     * Универсальный график (свечи или линия) в стиле GeckoTerminal
     */
    private class CandlestickChartPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            
            // Очищаем фон (прозрачный!)
            g2d.setComposite(java.awt.AlphaComposite.Clear);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setComposite(java.awt.AlphaComposite.SrcOver);
            
            if (candleHistory.size() < 2) {
                // Показываем "загрузка" если нет данных
                g2d.setColor(getCurrentColorScheme().neutral);
                g2d.setFont(sfProDisplaySmall);
                String text = "Загрузка графика...";
                int textWidth = g2d.getFontMetrics().stringWidth(text);
                g2d.drawString(text, (getWidth() - textWidth) / 2, getHeight() / 2);
                return;
            }
            
            if (currentChartType == ChartType.CANDLESTICK) {
                drawCandlestickChart(g2d);
            } else {
                drawLineChart(g2d);
            }
        }
        
        private void drawLineChart(Graphics2D g2d) {
            int width = getWidth();
            int height = getHeight();
            int leftPadding = 45;
            int rightPadding = 8;
            int topPadding = 8;
            int bottomPadding = 25;
            
            // Находим мин, макс и ATH цены
            BigDecimal minPrice = candleHistory.stream()
                .map(CandleData::getClose)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
            BigDecimal maxPrice = candleHistory.stream()
                .map(CandleData::getClose)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);
            BigDecimal athPrice = maxPrice; // ATH это максимальная цена за период
            
            // Расширяем диапазон на 20% вверх и вниз от ATH для лучшего отображения
            BigDecimal priceRange = maxPrice.subtract(minPrice);
            if (priceRange.compareTo(BigDecimal.ZERO) == 0) {
                priceRange = maxPrice.multiply(new BigDecimal("0.01"));
            }
            
            // Рисуем ценовую шкалу слева
            drawPriceScale(g2d, minPrice, maxPrice, leftPadding, topPadding, height - bottomPadding);
            
            int chartWidth = width - leftPadding - rightPadding;
            int chartHeight = height - topPadding - bottomPadding;
            
            // Рисуем центральную ATH линию (как в DYOR.io)
            int athY = topPadding + (int) ((maxPrice.subtract(athPrice).doubleValue() / priceRange.doubleValue()) * chartHeight);
            g2d.setColor(getCurrentColorScheme().gridMain);
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                                       0, new float[]{5, 5}, 0)); // Пунктирная линия
            g2d.drawLine(leftPadding, athY, leftPadding + chartWidth, athY);
            
            // Подпись ATH
            g2d.setFont(new Font("Arial", Font.PLAIN, 9));
            g2d.setColor(getCurrentColorScheme().primaryText);
            String athText = "ATH $" + athPrice.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
            g2d.drawString(athText, leftPadding + 5, athY - 5);
            
            // Определяем цвет линии на основе общего тренда
            BigDecimal firstPrice = candleHistory.get(0).getClose();
            BigDecimal lastPrice = candleHistory.get(candleHistory.size() - 1).getClose();
            boolean isUptrend = lastPrice.compareTo(firstPrice) > 0;
            
            Color lineColor = isUptrend ? 
                getCurrentColorScheme().bullish :     // Зеленый для роста
                getCurrentColorScheme().bearish;      // Красный для падения
                
            // Рисуем градиентную заливку под линией (более прозрачную)
            drawGradientFill(g2d, lineColor, leftPadding, chartWidth, chartHeight, topPadding, bottomPadding, 
                           minPrice, maxPrice, priceRange);
            
            // Рисуем основную линию цены
            g2d.setColor(lineColor);
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            int pointSpacing = Math.max(1, chartWidth / (candleHistory.size() - 1));
            
            for (int i = 0; i < candleHistory.size() - 1; i++) {
                CandleData currentCandle = candleHistory.get(i);
                CandleData nextCandle = candleHistory.get(i + 1);
                
                if (currentCandle.getClose() == null || nextCandle.getClose() == null) {
                    continue;
                }
                
                int x1 = leftPadding + i * pointSpacing;
                int x2 = leftPadding + (i + 1) * pointSpacing;
                
                int y1 = topPadding + (int) ((maxPrice.subtract(currentCandle.getClose()).doubleValue() / priceRange.doubleValue()) * chartHeight);
                int y2 = topPadding + (int) ((maxPrice.subtract(nextCandle.getClose()).doubleValue() / priceRange.doubleValue()) * chartHeight);
                
                g2d.drawLine(x1, y1, x2, y2);
            }
            
            // Рисуем точки только на начале и конце (как в DYOR.io)
            g2d.setColor(lineColor);
            int[] keyIndices = {0, candleHistory.size() - 1};
            
            for (int i : keyIndices) {
                if (i >= candleHistory.size()) continue;
                CandleData candle = candleHistory.get(i);
                if (candle.getClose() == null) continue;
                
                int x = leftPadding + i * pointSpacing;
                int y = topPadding + (int) ((maxPrice.subtract(candle.getClose()).doubleValue() / priceRange.doubleValue()) * chartHeight);
                
                // Белый круг с цветной обводкой
                g2d.setColor(Color.WHITE);
                g2d.fillOval(x - 4, y - 4, 8, 8);
                g2d.setColor(lineColor);
                g2d.setStroke(new BasicStroke(2.0f));
                g2d.drawOval(x - 4, y - 4, 8, 8);
            }
            
            // Рисуем временные метки снизу
            drawTimeLabels(g2d, leftPadding, chartWidth, height - bottomPadding + 5);
        }
        
        private void drawCandlestickChart(Graphics2D g2d) {
            int width = getWidth();
            int height = getHeight();
            int leftPadding = 45; // Увеличиваем левый отступ для ценовой шкалы
            int rightPadding = 8;
            int topPadding = 8;
            int bottomPadding = 25; // Увеличиваем нижний отступ для временных меток
            
            // Находим мин и макс цены
            BigDecimal minPrice = candleHistory.stream()
                .map(CandleData::getLow)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
            BigDecimal maxPrice = candleHistory.stream()
                .map(CandleData::getHigh)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);
            
            BigDecimal priceRange = maxPrice.subtract(minPrice);
            if (priceRange.compareTo(BigDecimal.ZERO) == 0) {
                priceRange = maxPrice.multiply(new BigDecimal("0.01"));
            }
            
            // Рисуем ценовую шкалу слева
            drawPriceScale(g2d, minPrice, maxPrice, leftPadding, topPadding, height - bottomPadding);
            
            int chartWidth = width - leftPadding - rightPadding;
            int chartHeight = height - topPadding - bottomPadding;
            
            // Ширина свечи и расстояние
            int candleWidth = Math.max(1, chartWidth / candleHistory.size());
            int candleSpacing = Math.max(0, candleWidth / 4);
            
            // Рисуем свечи
            for (int i = 0; i < candleHistory.size(); i++) {
                CandleData candle = candleHistory.get(i);
                
                if (candle.getHigh() == null || candle.getLow() == null || 
                    candle.getOpen() == null || candle.getClose() == null) {
                    continue;
                }
                
                int x = leftPadding + i * (candleWidth + candleSpacing);
                
                // Координаты Y для high, low, open, close
                int highY = topPadding + (int) ((maxPrice.subtract(candle.getHigh()).doubleValue() / priceRange.doubleValue()) * chartHeight);
                int lowY = topPadding + (int) ((maxPrice.subtract(candle.getLow()).doubleValue() / priceRange.doubleValue()) * chartHeight);
                int openY = topPadding + (int) ((maxPrice.subtract(candle.getOpen()).doubleValue() / priceRange.doubleValue()) * chartHeight);
                int closeY = topPadding + (int) ((maxPrice.subtract(candle.getClose()).doubleValue() / priceRange.doubleValue()) * chartHeight);
                
                // Цвета как в GeckoTerminal  
                Color candleColor;
                if (candle.isBullish()) {
                    candleColor = getCurrentColorScheme().bullish; // Зеленый для роста (close > open)
                } else if (candle.isBearish()) {
                    candleColor = getCurrentColorScheme().bearish; // Красный для падения (close < open)
                } else {
                    candleColor = getCurrentColorScheme().neutral; // Серый
                }
                
                g2d.setColor(candleColor);
                g2d.setStroke(new BasicStroke(1.0f));
                
                // Рисуем фитиль (high-low линия)
                int wickX = x + candleWidth / 2;
                g2d.drawLine(wickX, highY, wickX, lowY);
                
                // Рисуем тело свечи
                int bodyTop = Math.min(openY, closeY);
                int bodyHeight = Math.abs(closeY - openY);
                
                if (bodyHeight < 1) bodyHeight = 1; // Минимальная высота
                
                if (candle.isBullish()) {
                    // Бычья свеча (зеленая, цена выросла) - заполненная
                    g2d.fillRect(x, bodyTop, candleWidth, bodyHeight);
                } else if (candle.isBearish()) {
                    // Медвежья свеча (красная, цена упала) - заполненная  
                    g2d.fillRect(x, bodyTop, candleWidth, bodyHeight);
                } else {
                    // Доджи свеча (серая, цена не изменилась) - только линия
                    g2d.drawRect(x, bodyTop, candleWidth, bodyHeight);
                }
            }
            
            // Рисуем временные метки снизу
            drawTimeLabels(g2d, leftPadding, chartWidth, height - bottomPadding + 5);
        }
        
        /**
         * Рисует градиентную заливку под линией графика
         */
        private void drawGradientFill(Graphics2D g2d, Color lineColor, int leftPadding, int chartWidth, 
                                    int chartHeight, int topPadding, int bottomPadding, 
                                    BigDecimal minPrice, BigDecimal maxPrice, BigDecimal priceRange) {
            if (candleHistory.size() < 2) return;
            
            // Создаем путь для заливки
            java.awt.geom.Path2D.Float path = new java.awt.geom.Path2D.Float();
            
            int pointSpacing = Math.max(1, chartWidth / (candleHistory.size() - 1));
            
            // Начинаем с первой точки
            CandleData firstCandle = candleHistory.get(0);
            if (firstCandle.getClose() != null) {
                int x = leftPadding;
                int y = topPadding + (int) ((maxPrice.subtract(firstCandle.getClose()).doubleValue() / priceRange.doubleValue()) * chartHeight);
                path.moveTo(x, y);
                
                // Добавляем все остальные точки
                for (int i = 1; i < candleHistory.size(); i++) {
                    CandleData candle = candleHistory.get(i);
                    if (candle.getClose() == null) continue;
                    
                    x = leftPadding + i * pointSpacing;
                    y = topPadding + (int) ((maxPrice.subtract(candle.getClose()).doubleValue() / priceRange.doubleValue()) * chartHeight);
                    path.lineTo(x, y);
                }
                
                // Замыкаем путь до нижней части графика
                int lastX = leftPadding + (candleHistory.size() - 1) * pointSpacing;
                int bottomY = topPadding + chartHeight;
                path.lineTo(lastX, bottomY);
                path.lineTo(leftPadding, bottomY);
                path.closePath();
                
                // Создаем градиент от цвета линии к прозрачному
                Color startColor = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 80);
                Color endColor = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 10);
                
                java.awt.GradientPaint gradient = new java.awt.GradientPaint(
                    0, topPadding, startColor,
                    0, bottomY, endColor
                );
                
                g2d.setPaint(gradient);
                g2d.fill(path);
            }
        }
        
        /**
         * Рисует ценовую шкалу слева от графика
         */
        private void drawPriceScale(Graphics2D g2d, BigDecimal minPrice, BigDecimal maxPrice, 
                                   int leftPadding, int topPadding, int bottomY) {
            g2d.setColor(getCurrentColorScheme().primaryText);
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            
            // Вычисляем ширину области графика
            int chartWidth = getWidth() - leftPadding - 8; // 8 это rightPadding
            
            // Количество основных делений на шкале
            int mainDivisions = 5;
            BigDecimal priceStep = maxPrice.subtract(minPrice).divide(new BigDecimal(mainDivisions), 6, BigDecimal.ROUND_HALF_UP);
            
            // Рисуем основные деления с подписями
            for (int i = 0; i <= mainDivisions; i++) {
                BigDecimal price = minPrice.add(priceStep.multiply(new BigDecimal(i)));
                int y = bottomY - (i * (bottomY - topPadding) / mainDivisions);
                
                // Форматируем цену
                String priceText;
                if (price.compareTo(new BigDecimal("0.01")) < 0) {
                    priceText = "$" + price.setScale(4, BigDecimal.ROUND_HALF_UP).toPlainString();
                } else {
                    priceText = "$" + price.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
                }
                
                // Рисуем текст справа от левого края
                g2d.drawString(priceText, 2, y + 3);
                
                // Рисуем основную горизонтальную линию сетки (более яркую)
                g2d.setColor(getCurrentColorScheme().gridMain);
                g2d.setStroke(new BasicStroke(0.8f));
                g2d.drawLine(leftPadding, y, getWidth() - 8, y);
                
                // Возвращаем цвет текста
                g2d.setColor(getCurrentColorScheme().primaryText);
            }
            
            // Рисуем промежуточные линии между основными делениями
            int subDivisions = 2; // Количество промежуточных линий между основными
            for (int i = 0; i < mainDivisions; i++) {
                for (int j = 1; j <= subDivisions; j++) {
                    int y = bottomY - ((i * subDivisions + j) * (bottomY - topPadding) / (mainDivisions * subDivisions));
                    
                    // Рисуем тонкую промежуточную линию
                    g2d.setColor(getCurrentColorScheme().gridSub);
                    g2d.setStroke(new BasicStroke(0.3f));
                    g2d.drawLine(leftPadding, y, getWidth() - 8, y);
                }
            }
            
            // Добавляем дополнительные мелкие деления для еще большей детализации
            int microDivisions = 5; // Еще более мелкие линии
            for (int i = 0; i < mainDivisions * subDivisions; i++) {
                for (int j = 1; j < microDivisions; j++) {
                    int y = bottomY - ((i * microDivisions + j) * (bottomY - topPadding) / (mainDivisions * subDivisions * microDivisions));
                    
                    // Рисуем очень тонкие микро-линии (только в правой части графика)
                    g2d.setColor(getCurrentColorScheme().gridMicro);
                    g2d.setStroke(new BasicStroke(0.2f));
                    g2d.drawLine(leftPadding + chartWidth * 3/4, y, getWidth() - 8, y);
                }
            }
        }
        
        /**
         * Рисует временные метки снизу графика
         */
        private void drawTimeLabels(Graphics2D g2d, int leftPadding, int chartWidth, int y) {
            if (candleHistory.isEmpty()) return;
            
            g2d.setColor(getCurrentColorScheme().primaryText);
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            
            // Показываем время для первой, средней и последней свечи
            int[] indices = {0, candleHistory.size() / 2, candleHistory.size() - 1};
            
            for (int i : indices) {
                if (i >= candleHistory.size()) continue;
                
                CandleData candle = candleHistory.get(i);
                if (candle.getTimestamp() == null) continue;
                
                // Форматируем время в MSK
                java.time.format.DateTimeFormatter formatter = 
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm");
                String timeText = candle.getTimestamp().format(formatter);
                
                // Вычисляем позицию X для метки
                int candleWidth = Math.max(1, chartWidth / candleHistory.size());
                int candleSpacing = Math.max(0, candleWidth / 4);
                int x = leftPadding + i * (candleWidth + candleSpacing) + candleWidth / 2;
                
                // Центрируем текст
                int textWidth = g2d.getFontMetrics().stringWidth(timeText);
                g2d.drawString(timeText, x - textWidth / 2, y + 12);
            }
        }
    }
    
    private void applyTheme() {
        Color textColor = getCurrentColorScheme().primaryText;
        Color secondaryTextColor = getCurrentColorScheme().secondaryText;
        Color tertiaryTextColor = getCurrentColorScheme().tertiaryText;
        
        symbolLabel.setForeground(secondaryTextColor);
        priceLabel.setForeground(textColor);
        trendLabel.setForeground(textColor);
        liquidityLabel.setForeground(tertiaryTextColor);
        volumeLabel.setForeground(tertiaryTextColor);
        
        repaint();
    }
    
    private void setupDragging() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    showContextMenu(e);
                }
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                setLocation(getX() + e.getX() - mouseX, getY() + e.getY() - mouseY);
            }
        });
    }
    
    private void showContextMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        // Подменю для выбора временного интервала
        JMenu timeframeMenu = new JMenu("⏰ Интервал свечей");
        
        // Варианты временных интервалов
        String[][] timeframes = {
            {"1m", "minute", "1", "60"},
            {"5m", "minute", "5", "48"}, 
            {"15m", "minute", "15", "48"},
            {"1h", "hour", "1", "24"},
            {"4h", "hour", "4", "24"},
            {"1d", "day", "1", "30"}
        };
        
        for (String[] tf : timeframes) {
            String displayName = tf[0];
            String timeframe = tf[1];
            int aggregate = Integer.parseInt(tf[2]);
            int limit = Integer.parseInt(tf[3]);
            
            JMenuItem timeframeItem = new JMenuItem(displayName);
            
            // Отмечаем текущий интервал
            if (timeframe.equals(currentTimeframe) && aggregate == currentAggregate) {
                timeframeItem.setText("✓ " + displayName);
                timeframeItem.setForeground(getCurrentColorScheme().bullish);
            }
            
            timeframeItem.addActionListener(ev -> {
                currentTimeframe = timeframe;
                currentAggregate = aggregate;
                currentLimit = limit;
                updateCandleData(); // Обновляем график с новым интервалом
                
                if (trayIcon != null) {
                    trayIcon.displayMessage("📊 Интервал изменен", 
                        "Свечи теперь показывают " + displayName, 
                        TrayIcon.MessageType.INFO);
                }
            });
            
            timeframeMenu.add(timeframeItem);
        }
        
        // Подменю для выбора типа графика
        JMenu chartTypeMenu = new JMenu("📈 Тип графика");
        
        for (ChartType type : ChartType.values()) {
            JMenuItem chartItem = new JMenuItem(type.getDisplayName());
            
            if (type == currentChartType) {
                chartItem.setText("✓ " + type.getDisplayName());
                chartItem.setForeground(getCurrentColorScheme().bullish);
            }
            
            chartItem.addActionListener(ev -> {
                currentChartType = type;
                chartPanel.repaint();
                
                if (trayIcon != null) {
                    trayIcon.displayMessage("📊 График изменен", 
                        "Теперь отображается " + type.getDisplayName(), 
                        TrayIcon.MessageType.INFO);
                }
            });
            
            chartTypeMenu.add(chartItem);
        }
        
        // Подменю для выбора дизайна
        JMenu layoutMenu = new JMenu("🎨 Дизайн");
        
        for (Layout layout : Layout.values()) {
            JMenuItem layoutItem = new JMenuItem(layout.getDisplayName());
            
            if (layout == currentLayout) {
                layoutItem.setText("✓ " + layout.getDisplayName());
                layoutItem.setForeground(getCurrentColorScheme().bullish);
            }
            
            layoutItem.addActionListener(ev -> {
                currentLayout = layout;
                
                // Мягко пересоздаем интерфейс с новым дизайном
                SwingUtilities.invokeLater(() -> {
                    // Сохраняем текущую позицию окна
                    java.awt.Point currentLocation = getLocation();
                    
                    // Очищаем содержимое
                    getContentPane().removeAll();
                    
                    // Пересоздаем UI компоненты (без вызова setUndecorated)
                    if (currentLayout == Layout.CLASSIC) {
                        setSize(320, 380);
                        AppleStylePanel mainPanel = new AppleStylePanel();
                        mainPanel.setLayout(new BorderLayout(4, 4));
                        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
                        mainPanel.setOpaque(false);
                        setupClassicLayout(mainPanel);
                        add(mainPanel);
                    } else {
                        setSize(320, 400);
                        AppleStylePanel mainPanel = new AppleStylePanel();
                        mainPanel.setLayout(new BorderLayout(4, 4));
                        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
                        mainPanel.setOpaque(false);
                        setupCompactLayout(mainPanel);
                        add(mainPanel);
                    }
                    
                    // Восстанавливаем позицию
                    setLocation(currentLocation);
                    
                    // Применяем тему и обновляем
                    applyTheme();
                    revalidate();
                    repaint();
                    
                    if (trayIcon != null) {
                        trayIcon.displayMessage("🎨 Дизайн изменен", 
                            "Активирован " + layout.getDisplayName() + " дизайн", 
                            TrayIcon.MessageType.INFO);
                    }
                });
            });
            
            layoutMenu.add(layoutItem);
        }
        
        // Подменю настроек звука
        JMenu soundMenu = new JMenu("🔊 Настройки звука");
        
        JMenuItem soundToggleItem = new JMenuItem(soundEnabled ? "🔇 Отключить звук" : "🔊 Включить звук");
        soundToggleItem.addActionListener(ev -> {
            soundEnabled = !soundEnabled;
            if (soundEnabled) {
                soundService.playUpdateSound();
            }
        });
        
        JMenuItem customSoundItem = new JMenuItem("📁 Выбрать звук пампа");
        customSoundItem.addActionListener(ev -> {
            javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Звуковые файлы", "wav", "mp3", "aiff"));
            
            if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
                customSoundFile = fileChooser.getSelectedFile().getAbsolutePath();
                if (trayIcon != null) {
                    trayIcon.displayMessage("🔊 Звук изменен", 
                        "Новый звук: " + fileChooser.getSelectedFile().getName(), 
                        TrayIcon.MessageType.INFO);
                }
            }
        });
        
        JMenuItem resetSoundItem = new JMenuItem("🔄 Сбросить на стандартный");
        resetSoundItem.addActionListener(ev -> {
            customSoundFile = null;
            if (trayIcon != null) {
                trayIcon.displayMessage("🔊 Звук сброшен", 
                    "Используется стандартный звук", 
                    TrayIcon.MessageType.INFO);
            }
        });
        
        JMenuItem pumpThresholdItem = new JMenuItem("⚡ Порог пампа: " + pumpThreshold + "%");
        pumpThresholdItem.addActionListener(ev -> {
            String input = javax.swing.JOptionPane.showInputDialog(this, 
                "Введите процент пампа для уведомления:", pumpThreshold.toString());
            if (input != null && !input.trim().isEmpty()) {
                try {
                    BigDecimal newThreshold = new BigDecimal(input.trim());
                    if (newThreshold.compareTo(BigDecimal.ZERO) > 0 && newThreshold.compareTo(new BigDecimal("1000")) <= 0) {
                        pumpThreshold = newThreshold;
                        if (trayIcon != null) {
                            trayIcon.displayMessage("⚡ Порог изменен", 
                                "Уведомления при пампе от " + pumpThreshold + "%", 
                                TrayIcon.MessageType.INFO);
                        }
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(this, 
                            "Введите число от 0.1 до 1000", "Ошибка", 
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    javax.swing.JOptionPane.showMessageDialog(this, 
                        "Введите корректное число", "Ошибка", 
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        soundMenu.add(soundToggleItem);
        soundMenu.addSeparator();
        soundMenu.add(customSoundItem);
        soundMenu.add(resetSoundItem);
        soundMenu.addSeparator();
        soundMenu.add(pumpThresholdItem);
        
        JMenuItem themeItem = new JMenuItem(currentTheme == Theme.DARK ? "🌞 Светлая тема" : "🌙 Тёмная тема");
        themeItem.addActionListener(ev -> {
            currentTheme = currentTheme == Theme.DARK ? Theme.LIGHT : Theme.DARK;
            applyTheme();
        });
        
        JMenuItem hideItem = new JMenuItem("👁️ Скрыть в трей");
        hideItem.addActionListener(ev -> setVisible(false));
        
        JMenuItem exitItem = new JMenuItem("❌ Выход");
        exitItem.addActionListener(ev -> System.exit(0));
        
        menu.add(timeframeMenu);
        menu.add(chartTypeMenu);
        menu.add(layoutMenu);
        menu.addSeparator();
        menu.add(soundMenu);
        menu.add(themeItem);
        menu.addSeparator();
        menu.add(hideItem);
        menu.add(exitItem);
        
        menu.show(this, e.getX(), e.getY());
    }
    
    private void initSystemTray() {
        if (!SystemTray.isSupported()) return;
        
        systemTray = SystemTray.getSystemTray();
        Image icon = createTrayIcon();
        
        PopupMenu popup = new PopupMenu();
        MenuItem showHideItem = new MenuItem("Скрыть/Показать");
        MenuItem exitItem = new MenuItem("Выход");
        
        showHideItem.addActionListener(e -> {
            if (isVisible()) {
                setVisible(false);
            } else {
                setVisible(true);
                toFront();
            }
        });
        
        exitItem.addActionListener(e -> System.exit(0));
        
        popup.add(showHideItem);
        popup.addSeparator();
        popup.add(exitItem);
        
        trayIcon = new TrayIcon(icon, "GOVNO Screamer", popup);
        
        // Двойной клик по иконке в трее тоже переключает видимость
        trayIcon.addActionListener(e -> {
            if (isVisible()) {
                setVisible(false);
            } else {
                setVisible(true);
                toFront();
            }
        });
        
        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("Не удалось добавить в трей");
        }
    }
    
    private Image createTrayIcon() {
        int size = 16;
        var image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.setColor(getCurrentColorScheme().bullish);
        g.fillOval(0, 0, size, size);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString("$", 5, 12);
        
        g.dispose();
        return image;
    }
    
    private void startMonitoring() {
        // Первоначальная загрузка
        updatePriceData();
        updateCandleData();
        
        // Таймер обновления каждые 5 секунд
        updateTimer = new Timer(updateInterval, e -> {
            updatePriceData();
            // График обновляем реже - каждые 30 секунд
            if (System.currentTimeMillis() % 30000 < updateInterval) {
                updateCandleData();
            }
        });
        updateTimer.start();
        
        System.out.println("Мониторинг запущен, обновление каждые " + (updateInterval/1000) + " секунд");
    }
    
    private void updatePriceData() {
        CompletableFuture<PriceData> future = apiService.getTokenPrice();
        
        future.thenAccept(priceData -> {
            SwingUtilities.invokeLater(() -> {
                boolean wasPump = false;
                
                // Проверяем памп с пользовательским порогом
                if (currentPriceData != null && priceData.getPriceChangePercent24h() != null && 
                    priceData.getPriceChangePercent24h().compareTo(pumpThreshold) > 0 && soundEnabled) {
                    wasPump = true;
                    soundService.playPumpAlert(customSoundFile); // Передаем кастомный файл
                    
                    if (trayIcon != null) {
                        trayIcon.displayMessage("🚀 GOVNO PUMP!", 
                            "Памп " + updateTrendDisplay(priceData) + "!", 
                            TrayIcon.MessageType.WARNING);
                    }
                }
                
                currentPriceData = priceData;
                updateUI(wasPump);
                
                if (!wasPump && soundEnabled) {
                    soundService.playUpdateSound();
                }
            });
        }).exceptionally(throwable -> {
            System.err.println("Ошибка обновления данных: " + throwable.getMessage());
            return null;
        });
    }
    
    private void updateCandleData() {
        // Используем выбранные пользователем настройки временного интервала
        CompletableFuture<List<CandleData>> future = apiService.getCandleData(currentTimeframe, currentAggregate, currentLimit);
        
        future.thenAccept(candles -> {
            SwingUtilities.invokeLater(() -> {
                candleHistory = candles;
                chartPanel.repaint();
                System.out.println("График обновлен: " + candles.size() + " свечей (" + 
                                 currentTimeframe + ", " + currentAggregate + ", " + currentLimit + ")");
            });
        }).exceptionally(throwable -> {
            System.err.println("Ошибка обновления графика: " + throwable.getMessage());
            return null;
        });
    }
    
    private void updateUI(boolean isPump) {
        if (currentPriceData == null) return;
        
        // Отображаем символ
        String symbol = currentPriceData.getSymbol();
        
        if (poopIcon != null) {
            // Используем картинку какашки
            symbolLabel.setIcon(poopIcon);
            symbolLabel.setText(symbol);
        } else {
            // Fallback на эмодзи
            if (!symbol.contains("💩")) {
                symbol = "💩 " + symbol;
            }
            symbolLabel.setIcon(null);
            symbolLabel.setText(symbol);
        }
        
        priceLabel.setText(currentPriceData.getFormattedPrice());
        
        // Обновляем тренд с правильным отображением
        String trendText = updateTrendDisplay(currentPriceData);
        trendLabel.setText(trendText);
        
        // Обновляем ликвидность и объем
        if (currentPriceData.getLiquidityUsd() != null) {
            liquidityLabel.setText("Ликв: " + currentPriceData.getFormattedLiquidity());
        } else {
            liquidityLabel.setText("Ликв: —");
        }
        
        if (currentPriceData.getVolumeUsd24h() != null) {
            volumeLabel.setText("Объем: " + currentPriceData.getFormattedVolume());
        } else {
            volumeLabel.setText("Объем: —");
        }
        
        // Цвета для тренда
        if (currentPriceData.getPriceChangePercent24h() != null) {
            if (currentPriceData.getPriceChangePercent24h().compareTo(BigDecimal.ZERO) > 0) {
                trendLabel.setForeground(getCurrentColorScheme().bullish);
                if (isPump) {
                    startBlinkEffect();
                }
            } else if (currentPriceData.getPriceChangePercent24h().compareTo(BigDecimal.ZERO) < 0) {
                trendLabel.setForeground(getCurrentColorScheme().bearish);
            } else {
                trendLabel.setForeground(getCurrentColorScheme().neutral);
            }
        }
    }
    
    private void startBlinkEffect() {
        Timer blinkTimer = new Timer(200, null);
        blinkTimer.addActionListener(blinkEvent -> {
            Color currentColor = trendLabel.getForeground();
            trendLabel.setForeground(currentColor.equals(getCurrentColorScheme().bearish) ? 
                getCurrentColorScheme().bullish : getCurrentColorScheme().bearish);
        });
        blinkTimer.setRepeats(true);
        blinkTimer.start();
        
        Timer stopTimer = new Timer(3000, stopEvent -> {
            blinkTimer.stop();
            trendLabel.setForeground(getCurrentColorScheme().bullish);
        });
        stopTimer.setRepeats(false);
        stopTimer.start();
    }
    
    private String updateTrendDisplay(PriceData priceData) {
        if (priceData == null || priceData.getPriceChangePercent24h() == null) {
            trendLabel.setIcon(null);
            return "—";
        }
        
        BigDecimal change = priceData.getPriceChangePercent24h();
        String percentText = change.setScale(2, BigDecimal.ROUND_HALF_UP).toString() + "%";
        
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            if (upArrowIcon != null) {
                trendLabel.setIcon(upArrowIcon);
                return "+" + percentText;
            } else {
                trendLabel.setIcon(null);
                return "↗ +" + percentText;
            }
        } else if (change.compareTo(BigDecimal.ZERO) < 0) {
            if (downArrowIcon != null) {
                trendLabel.setIcon(downArrowIcon);
                return percentText;
            } else {
                trendLabel.setIcon(null);
                return "↘ " + percentText;
            }
        } else {
            trendLabel.setIcon(null);
            return "→ 0.00%";
        }
    }
    
    /**
     * Создает иконку зеленой стрелки вверх 16x16 на основе Heroicons SVG
     */
    private ImageIcon createUpArrowIcon() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        
        // Зеленый цвет как в GeckoTerminal
        g2d.setColor(getCurrentColorScheme().bullish);
        g2d.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // Масштабируем координаты с 24x24 до 16x16 (коэффициент 16/24 = 0.667)
        // Вертикальная линия: от (8,2) до (8,14)
        g2d.drawLine(8, 2, 8, 14);
        
        // Левая часть стрелки: от (6,5) до (8,2)
        g2d.drawLine(6, 5, 8, 2);
        
        // Правая часть стрелки: от (8,2) до (10,5)
        g2d.drawLine(8, 2, 10, 5);
        
        g2d.dispose();
        return new ImageIcon(image);
    }
    
    /**
     * Создает иконку красной стрелки вниз 16x16 на основе Heroicons SVG
     */
    private ImageIcon createDownArrowIcon() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        
        // Красный цвет как в GeckoTerminal
        g2d.setColor(getCurrentColorScheme().bearish);
        g2d.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // Масштабируем координаты с 24x24 до 16x16
        // Вертикальная линия: от (8,2) до (8,14)
        g2d.drawLine(8, 2, 8, 14);
        
        // Левая часть стрелки: от (10,11) до (8,14)
        g2d.drawLine(10, 11, 8, 14);
        
        // Правая часть стрелки: от (8,14) до (6,11)
        g2d.drawLine(8, 14, 6, 11);
        
        g2d.dispose();
        return new ImageIcon(image);
    }
    
    /**
     * Создает таймлайн панель с кнопками временных интервалов
     */
    private JPanel createTimelinePanel() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 4, 4));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        panel.setName("timelinePanel"); // Добавляем имя для поиска
        
        // Варианты временных интервалов
        String[][] timeframes = {
            {"1м", "minute", "1", "60"},
            {"5м", "minute", "5", "48"}, 
            {"15м", "minute", "15", "48"},
            {"1ч", "hour", "1", "24"},
            {"4ч", "hour", "4", "24"},
            {"1д", "day", "1", "30"}
        };
        
        for (String[] tf : timeframes) {
            String displayName = tf[0];
            String timeframe = tf[1];
            int aggregate = Integer.parseInt(tf[2]);
            int limit = Integer.parseInt(tf[3]);
            
            javax.swing.JButton button = new javax.swing.JButton(displayName);
            button.setFont(new Font("Arial", Font.PLAIN, 9));
            button.setPreferredSize(new Dimension(28, 20));
            button.setMargin(new java.awt.Insets(1, 2, 1, 2));
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setOpaque(false);
            
            // Стиль кнопки в зависимости от активности
            boolean isActive = timeframe.equals(currentTimeframe) && aggregate == currentAggregate;
            if (isActive) {
                button.setForeground(getCurrentColorScheme().primaryText);
                button.setOpaque(true);
                button.setBackground(getCurrentColorScheme().gridMain);
            } else {
                button.setForeground(getCurrentColorScheme().secondaryText);
            }
            
            // Обработчик клика
            button.addActionListener(e -> {
                currentTimeframe = timeframe;
                currentAggregate = aggregate;
                currentLimit = limit;
                updateCandleData();
                
                // Пересоздаем таймлайн с обновленными стилями
                recreateTimelinePanel();
                
                if (trayIcon != null) {
                    trayIcon.displayMessage("📊 Интервал изменен", 
                        "Свечи: " + displayName, TrayIcon.MessageType.INFO);
                }
            });
            
            panel.add(button);
        }
        
        return panel;
    }
    
    /**
     * Пересоздает панель таймлайна с обновленными стилями
     */
    private void recreateTimelinePanel() {
        SwingUtilities.invokeLater(() -> {
            // Находим контейнер с таймлайном
            java.awt.Container contentPane = getContentPane();
            JPanel mainPanel = (JPanel) contentPane.getComponent(0);
            
            if (currentLayout == Layout.CLASSIC) {
                // В классическом дизайне таймлайн находится в bottomContainer
                JPanel bottomContainer = (JPanel) mainPanel.getComponent(2); // BorderLayout.SOUTH
                bottomContainer.remove(1); // Убираем BorderLayout.SOUTH (старый таймлайн)
                bottomContainer.add(createTimelinePanel(), BorderLayout.SOUTH);
                bottomContainer.revalidate();
                bottomContainer.repaint();
            } else {
                // В компактном дизайне таймлайн находится прямо в mainPanel
                mainPanel.remove(2); // Убираем BorderLayout.SOUTH (старый таймлайн)
                mainPanel.add(createTimelinePanel(), BorderLayout.SOUTH);
                mainPanel.revalidate();
                mainPanel.repaint();
            }
        });
    }
    
    @Override
    public void dispose() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        if (apiService != null) {
            apiService.close();
        }
        if (soundService != null) {
            soundService.stopCurrentSound();
        }
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
        super.dispose();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new CryptoScreamerWidget().setVisible(true);
        });
    }
} 