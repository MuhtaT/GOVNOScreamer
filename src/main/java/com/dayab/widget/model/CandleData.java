package com.dayab.widget.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Модель данных для свечей OHLCV из GeckoTerminal API
 */
public class CandleData {
    private LocalDateTime timestamp;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    
    public CandleData() {}
    
    public CandleData(LocalDateTime timestamp, BigDecimal open, BigDecimal high, 
                     BigDecimal low, BigDecimal close, BigDecimal volume) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }
    
    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public BigDecimal getOpen() {
        return open;
    }
    
    public void setOpen(BigDecimal open) {
        this.open = open;
    }
    
    public BigDecimal getHigh() {
        return high;
    }
    
    public void setHigh(BigDecimal high) {
        this.high = high;
    }
    
    public BigDecimal getLow() {
        return low;
    }
    
    public void setLow(BigDecimal low) {
        this.low = low;
    }
    
    public BigDecimal getClose() {
        return close;
    }
    
    public void setClose(BigDecimal close) {
        this.close = close;
    }
    
    public BigDecimal getVolume() {
        return volume;
    }
    
    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }
    
    /**
     * Возвращает true если свеча бычья (close > open)
     */
    public boolean isBullish() {
        if (open == null || close == null) return false;
        return close.compareTo(open) < 0;
    }
    
    /**
     * Возвращает true если свеча медвежья (close < open)
     */
    public boolean isBearish() {
        if (open == null || close == null) return false;
        return close.compareTo(open) > 0;
    }
    
    /**
     * Возвращает размах свечи (high - low)
     */
    public BigDecimal getRange() {
        if (high == null || low == null) return BigDecimal.ZERO;
        return high.subtract(low);
    }
    
    /**
     * Возвращает тело свечи (abs(close - open))
     */
    public BigDecimal getBody() {
        if (open == null || close == null) return BigDecimal.ZERO;
        return close.subtract(open).abs();
    }
} 