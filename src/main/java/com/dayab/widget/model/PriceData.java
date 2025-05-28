package com.dayab.widget.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Модель данных для хранения информации о цене токена
 */
public class PriceData {
    private BigDecimal currentPrice;
    private BigDecimal previousPrice;
    private BigDecimal priceChange24h;
    private BigDecimal priceChangePercent24h;
    private String symbol;
    private LocalDateTime timestamp;
    private boolean isPump;
    
    // Добавляем поля для ликвидности и объема
    private BigDecimal liquidityUsd;    // Ликвидность в USD
    private BigDecimal volumeUsd24h;    // Объем торгов за 24 часа в USD
    
    public PriceData() {}
    
    public PriceData(BigDecimal currentPrice, String symbol) {
        this.currentPrice = currentPrice;
        this.symbol = symbol;
        this.timestamp = LocalDateTime.now();
        this.isPump = false;
    }
    
    // Getters and Setters
    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public BigDecimal getPreviousPrice() {
        return previousPrice;
    }
    
    public void setPreviousPrice(BigDecimal previousPrice) {
        this.previousPrice = previousPrice;
    }
    
    public BigDecimal getPriceChange24h() {
        return priceChange24h;
    }
    
    public void setPriceChange24h(BigDecimal priceChange24h) {
        this.priceChange24h = priceChange24h;
    }
    
    public BigDecimal getPriceChangePercent24h() {
        return priceChangePercent24h;
    }
    
    public void setPriceChangePercent24h(BigDecimal priceChangePercent24h) {
        this.priceChangePercent24h = priceChangePercent24h;
        // Проверяем на памп более 50%
        if (priceChangePercent24h != null && priceChangePercent24h.compareTo(new BigDecimal("50")) > 0) {
            this.isPump = true;
        }
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isPump() {
        return isPump;
    }
    
    public void setPump(boolean pump) {
        isPump = pump;
    }
    
    public BigDecimal getLiquidityUsd() {
        return liquidityUsd;
    }
    
    public void setLiquidityUsd(BigDecimal liquidityUsd) {
        this.liquidityUsd = liquidityUsd;
    }
    
    public BigDecimal getVolumeUsd24h() {
        return volumeUsd24h;
    }
    
    public void setVolumeUsd24h(BigDecimal volumeUsd24h) {
        this.volumeUsd24h = volumeUsd24h;
    }
    
    /**
     * Возвращает тренд цены как строку
     */
    public String getTrend() {
        if (priceChangePercent24h == null) return "—";
        
        if (priceChangePercent24h.compareTo(BigDecimal.ZERO) > 0) {
            return "↗ +" + priceChangePercent24h.setScale(2, BigDecimal.ROUND_HALF_UP) + "%";
        } else if (priceChangePercent24h.compareTo(BigDecimal.ZERO) < 0) {
            return "↘ " + priceChangePercent24h.setScale(2, BigDecimal.ROUND_HALF_UP) + "%";
        } else {
            return "→ 0.00%";
        }
    }
    
    /**
     * Возвращает форматированную цену
     */
    public String getFormattedPrice() {
        if (currentPrice == null) return "—";
        
        if (currentPrice.compareTo(new BigDecimal("0.01")) < 0) {
            return "$" + currentPrice.setScale(6, BigDecimal.ROUND_HALF_UP).toPlainString();
        } else {
            return "$" + currentPrice.setScale(4, BigDecimal.ROUND_HALF_UP).toPlainString();
        }
    }
    
    /**
     * Возвращает форматированную ликвидность
     */
    public String getFormattedLiquidity() {
        if (liquidityUsd == null) return "—";
        
        BigDecimal value = liquidityUsd;
        if (value.compareTo(new BigDecimal("1000000")) >= 0) {
            return "$" + value.divide(new BigDecimal("1000000"), 1, BigDecimal.ROUND_HALF_UP) + "M";
        } else if (value.compareTo(new BigDecimal("1000")) >= 0) {
            return "$" + value.divide(new BigDecimal("1000"), 1, BigDecimal.ROUND_HALF_UP) + "K";
        } else {
            return "$" + value.setScale(0, BigDecimal.ROUND_HALF_UP);
        }
    }
    
    /**
     * Возвращает форматированный объем торгов
     */
    public String getFormattedVolume() {
        if (volumeUsd24h == null) return "—";
        
        BigDecimal value = volumeUsd24h;
        if (value.compareTo(new BigDecimal("1000000")) >= 0) {
            return "$" + value.divide(new BigDecimal("1000000"), 1, BigDecimal.ROUND_HALF_UP) + "M";
        } else if (value.compareTo(new BigDecimal("1000")) >= 0) {
            return "$" + value.divide(new BigDecimal("1000"), 1, BigDecimal.ROUND_HALF_UP) + "K";
        } else {
            return "$" + value.setScale(0, BigDecimal.ROUND_HALF_UP);
        }
    }
} 