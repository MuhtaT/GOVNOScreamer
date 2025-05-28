package com.dayab.widget.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import com.dayab.widget.model.CandleData;
import com.dayab.widget.model.PriceData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Сервис для работы с GeckoTerminal API
 * Получает данные о цене и OHLCV свечах для GOVNO/TON токена
 */
public class GeckoTerminalApiService {
    
    private static final String BASE_URL = "https://api.geckoterminal.com/api/v2";
    private static final String TON_NETWORK = "ton"; 
    private static final String TOKEN_ADDRESS = "EQBlWgKnh_qbFYTXfKgGAQPxkxFsArDOSr9nlARSzydpNPwA";
    
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // Кэш адреса пула для повторного использования
    private String cachedPoolAddress = null;
    
    public GeckoTerminalApiService() {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Получает текущую цену токена асинхронно
     */
    public CompletableFuture<PriceData> getTokenPrice() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Используем рекомендованный эндпоинт для цены с тенденцией за 24h
                String priceUrl = BASE_URL + "/simple/networks/" + TON_NETWORK + "/token_price/" + TOKEN_ADDRESS + "?include_24hr_price_change=true";
                System.out.println("Запрашиваем цену токена: " + priceUrl);
                
                String priceResponse = makeHttpRequest(priceUrl);
                
                if (priceResponse == null) {
                    return createErrorPriceData("Ошибка получения данных цены");
                }
                
                System.out.println("Ответ цены получен, длина: " + priceResponse.length());
                
                PriceData priceData = parseSimplePriceData(priceResponse);
                
                // Дополнительно получаем тенденцию из pools API, так как Simple API не всегда её возвращает
                enrichWithPoolTrendData(priceData);
                
                return priceData;
                
            } catch (Exception e) {
                System.err.println("Ошибка API запроса: " + e.getMessage());
                return createErrorPriceData("Ошибка: " + e.getMessage());
            }
        });
    }
    
    /**
     * Получает OHLCV данные для свечного графика
     */
    public CompletableFuture<List<CandleData>> getCandleData(String timeframe, int aggregate, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Получаем адрес пула если не кэширован
                if (cachedPoolAddress == null) {
                    cachedPoolAddress = getTopPoolAddress();
                    if (cachedPoolAddress == null) {
                        System.err.println("Не удалось получить адрес пула");
                        return new ArrayList<>();
                    }
                }
                
                // Формируем правильный URL для OHLCV
                String ohlcvUrl = BASE_URL + "/networks/" + TON_NETWORK + "/pools/" + cachedPoolAddress + 
                                 "/ohlcv/" + timeframe + "?aggregate=" + aggregate + "&limit=" + limit;
                
                System.out.println("Запрашиваем OHLCV данные: " + ohlcvUrl);
                
                String ohlcvResponse = makeHttpRequest(ohlcvUrl);
                
                if (ohlcvResponse == null) {
                    System.err.println("Ошибка получения OHLCV данных - пустой ответ");
                    return new ArrayList<>();
                }
                
                System.out.println("OHLCV ответ получен, длина: " + ohlcvResponse.length());
                
                return parseOhlcvData(ohlcvResponse);
                
            } catch (Exception e) {
                System.err.println("Ошибка получения OHLCV данных: " + e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * Получает адрес топового пула для токена
     */
    private String getTopPoolAddress() {
        try {
            String poolsUrl = BASE_URL + "/networks/" + TON_NETWORK + "/tokens/" + TOKEN_ADDRESS + "/pools";
            System.out.println("Запрашиваем пулы: " + poolsUrl);
            
            String poolsResponse = makeHttpRequest(poolsUrl);
            
            if (poolsResponse == null) {
                System.err.println("Пустой ответ при получении пулов");
                return null;
            }
            
            System.out.println("Ответ пулов получен, длина: " + poolsResponse.length());
            
            JsonNode poolsData = objectMapper.readTree(poolsResponse);
            JsonNode poolsArray = poolsData.get("data");
            
            if (poolsArray == null || poolsArray.size() == 0) {
                System.err.println("Массив пулов пуст или отсутствует");
                if (poolsData.has("errors")) {
                    System.err.println("Ошибки API: " + poolsData.get("errors"));
                }
                return null;
            }
            
            // Получаем первый пул и извлекаем его адрес из ID
            JsonNode firstPool = poolsArray.get(0);
            String poolId = firstPool.get("id").asText();
            
            // ID пула в формате "ton_pooladdress", нужно извлечь адрес
            String poolAddress = poolId;
            if (poolId.contains("_")) {
                poolAddress = poolId.substring(poolId.indexOf("_") + 1);
            }
            
            System.out.println("Найден топовый пул: " + poolId + " -> адрес: " + poolAddress);
            
            // Логируем информацию о пуле
            if (firstPool.has("attributes")) {
                JsonNode attributes = firstPool.get("attributes");
                if (attributes.has("name")) {
                    System.out.println("Название пула: " + attributes.get("name").asText());
                }
                if (attributes.has("reserve_in_usd")) {
                    System.out.println("Ликвидность: $" + attributes.get("reserve_in_usd").asText());
                }
            }
            
            return poolAddress;
            
        } catch (Exception e) {
            System.err.println("Ошибка получения адреса пула: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Парсит OHLCV данные из JSON ответа
     */
    private List<CandleData> parseOhlcvData(String jsonResponse) {
        List<CandleData> candleList = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode data = root.get("data");
            
            if (data == null) {
                return candleList;
            }
            
            JsonNode ohlcvArray = data.get("attributes").get("ohlcv_list");
            if (ohlcvArray == null || !ohlcvArray.isArray()) {
                return candleList;
            }
            
            for (JsonNode ohlcvItem : ohlcvArray) {
                if (ohlcvItem.isArray() && ohlcvItem.size() >= 6) {
                    try {
                        // Формат: [timestamp, open, high, low, close, volume]
                        long timestamp = ohlcvItem.get(0).asLong();
                        BigDecimal open = new BigDecimal(ohlcvItem.get(1).asText());
                        BigDecimal high = new BigDecimal(ohlcvItem.get(2).asText());
                        BigDecimal low = new BigDecimal(ohlcvItem.get(3).asText());
                        BigDecimal close = new BigDecimal(ohlcvItem.get(4).asText());
                        BigDecimal volume = new BigDecimal(ohlcvItem.get(5).asText());
                        
                        LocalDateTime dateTime = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
                        
                        CandleData candle = new CandleData(dateTime, open, high, low, close, volume);
                        candleList.add(candle);
                        
                    } catch (Exception e) {
                        System.err.println("Ошибка парсинга свечи: " + e.getMessage());
                    }
                }
            }
            
            System.out.println("Получено " + candleList.size() + " свечей");
            
        } catch (Exception e) {
            System.err.println("Ошибка парсинга OHLCV JSON: " + e.getMessage());
        }
        
        return candleList;
    }
    
    /**
     * Выполняет HTTP запрос
     */
    private String makeHttpRequest(String url) {
        try {
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/json");
            
            ClassicHttpResponse response = httpClient.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getCode() == 200) {
                return responseBody;
            } else {
                System.err.println("HTTP Error: " + response.getCode() + " - " + responseBody);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("HTTP Request Error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Парсит данные цены из Simple API ответа
     */
    private PriceData parseSimplePriceData(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode data = root.get("data");
            
            if (data == null) {
                return createErrorPriceData("Неверный формат ответа Simple API");
            }
            
            JsonNode attributes = data.get("attributes");
            if (attributes == null) {
                return createErrorPriceData("Отсутствуют атрибуты в Simple API");
            }
            
            PriceData priceData = new PriceData();
            
            // Текущая цена в USD
            JsonNode priceUsd = attributes.get("token_prices");
            if (priceUsd != null && priceUsd.isObject()) {
                // Берем первый (и единственный) токен из объекта
                JsonNode tokenPrice = priceUsd.fields().next().getValue();
                if (tokenPrice != null && !tokenPrice.isNull()) {
                    priceData.setCurrentPrice(new BigDecimal(tokenPrice.asText()));
                    System.out.println("Цена токена: $" + tokenPrice.asText());
                }
            }
            
            // Изменение цены за 24h в процентах
            JsonNode priceChange24h = attributes.get("token_price_percent_changes");
            if (priceChange24h != null && priceChange24h.isObject()) {
                // Берем первый токен из объекта
                JsonNode tokenChange = priceChange24h.fields().next().getValue();
                if (tokenChange != null && tokenChange.has("24h")) {
                    JsonNode change24h = tokenChange.get("24h");
                    if (change24h != null && !change24h.isNull()) {
                        BigDecimal changePercent = new BigDecimal(change24h.asText());
                        priceData.setPriceChangePercent24h(changePercent);
                        System.out.println("Изменение за 24h: " + changePercent + "%");
                    }
                }
            }
            
            // Устанавливаем символ токена
            priceData.setSymbol("GOVNO");
            priceData.setTimestamp(LocalDateTime.now());
            
            return priceData;
            
        } catch (Exception e) {
            System.err.println("Ошибка парсинга Simple API JSON: " + e.getMessage());
            e.printStackTrace();
            return createErrorPriceData("Ошибка парсинга данных Simple API");
        }
    }
    
    /**
     * Создает объект PriceData с ошибкой
     */
    private PriceData createErrorPriceData(String error) {
        PriceData errorData = new PriceData();
        errorData.setSymbol("ERROR");
        errorData.setCurrentPrice(BigDecimal.ZERO);
        errorData.setTimestamp(LocalDateTime.now());
        System.err.println("GeckoTerminal API Error: " + error);
        return errorData;
    }
    
    /**
     * Закрывает HTTP клиент
     */
    public void close() {
        try {
            httpClient.close();
        } catch (Exception e) {
            System.err.println("Ошибка закрытия HTTP клиента: " + e.getMessage());
        }
    }
    
    /**
     * Дополнительно получает данные о тенденции, ликвидности и объеме из pools API
     */
    private void enrichWithPoolTrendData(PriceData priceData) {
        try {
            // Получаем адрес пула если не кэширован
            if (cachedPoolAddress == null) {
                cachedPoolAddress = getTopPoolAddress();
                if (cachedPoolAddress == null) {
                    System.err.println("Не удалось получить адрес пула для дополнительных данных");
                    return;
                }
            }
            
            String poolUrl = BASE_URL + "/networks/" + TON_NETWORK + "/pools/" + cachedPoolAddress;
            String poolResponse = makeHttpRequest(poolUrl);
            
            if (poolResponse == null) {
                System.err.println("Не удалось получить данные пула");
                return;
            }
            
            JsonNode poolData = objectMapper.readTree(poolResponse);
            JsonNode data = poolData.get("data");
            
            if (data != null && data.has("attributes")) {
                JsonNode attributes = data.get("attributes");
                
                // Получаем тенденцию за 24 часа
                if (attributes.has("price_change_percentage") && 
                    attributes.get("price_change_percentage").has("h24")) {
                    String changePercent = attributes.get("price_change_percentage").get("h24").asText();
                    try {
                        BigDecimal change24h = new BigDecimal(changePercent);
                        priceData.setPriceChangePercent24h(change24h);
                        System.out.println("Получена тенденция из пула: " + change24h + "%");
                    } catch (NumberFormatException e) {
                        System.err.println("Ошибка парсинга тенденции: " + changePercent);
                    }
                }
                
                // Получаем ликвидность
                if (attributes.has("reserve_in_usd")) {
                    String liquidityStr = attributes.get("reserve_in_usd").asText();
                    try {
                        BigDecimal liquidity = new BigDecimal(liquidityStr);
                        priceData.setLiquidityUsd(liquidity);
                        System.out.println("Получена ликвидность: $" + liquidity);
                    } catch (NumberFormatException e) {
                        System.err.println("Ошибка парсинга ликвидности: " + liquidityStr);
                    }
                }
                
                // Получаем объем торгов за 24 часа
                if (attributes.has("volume_usd") && 
                    attributes.get("volume_usd").has("h24")) {
                    String volumeStr = attributes.get("volume_usd").get("h24").asText();
                    try {
                        BigDecimal volume24h = new BigDecimal(volumeStr);
                        priceData.setVolumeUsd24h(volume24h);
                        System.out.println("Получен объем за 24ч: $" + volume24h);
                    } catch (NumberFormatException e) {
                        System.err.println("Ошибка парсинга объема: " + volumeStr);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Ошибка получения дополнительных данных пула: " + e.getMessage());
        }
    }
} 