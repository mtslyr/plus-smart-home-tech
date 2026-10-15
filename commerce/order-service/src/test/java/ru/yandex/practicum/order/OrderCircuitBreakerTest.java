package ru.yandex.practicum.order;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Проверяет поведение оформления заказа через настоящие Feign-клиенты и Circuit Breaker.
 * product-service и inventory-service подменены HTTP-заглушкой, которую находит LoadBalancer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderCircuitBreakerTest {

    private static final String PRODUCT_PATH = "/api/products/";
    private static final String RESERVE_PATH = "/api/inventory/reserve";
    private static final String RELEASE_PATH = "/api/inventory/release";

    private static final HttpServer STUB = startStub();
    private static final Map<String, StubResponse> RESPONSES = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> HITS = new ConcurrentHashMap<>();
    private static final List<String> RELEASE_BODIES = new CopyOnWriteArrayList<>();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        String uri = "http://localhost:" + STUB.getAddress().getPort();
        registry.add("spring.cloud.discovery.client.simple.instances.product-service[0].uri", () -> uri);
        registry.add("spring.cloud.discovery.client.simple.instances.inventory-service[0].uri", () -> uri);
    }

    @AfterAll
    static void stopStub() {
        STUB.stop(0);
    }

    @BeforeEach
    void setUp() {
        RESPONSES.clear();
        HITS.clear();
        RELEASE_BODIES.clear();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
        orderRepository.deleteAll();

        RESPONSES.put(RESERVE_PATH, new StubResponse(200,
                "{\"success\":true,\"availableQuantity\":10,\"message\":\"ok\"}"));
        RESPONSES.put(RELEASE_PATH, new StubResponse(200,
                "{\"success\":true,\"availableQuantity\":10,\"message\":\"ok\"}"));
    }

    @Test
    void shouldConfirmOrderWhenAllServicesRespond() throws Exception {
        product(1L, true);

        MvcResult response = createOrder(1L);

        assertThat(status(response)).isEqualTo(201);
        Map<String, Object> order = readMap(response);
        assertThat(order.get("status")).isEqualTo("CONFIRMED");
        assertThat(order.get("statusDetails")).isNull();
        assertThat(order.get("totalPrice")).isNotNull();
    }

    @Test
    void shouldRejectOrderWhenProductNotFound() throws Exception {
        RESPONSES.put(PRODUCT_PATH + 1, new StubResponse(404, "{\"message\":\"not found\"}"));

        assertRejected(createOrder(1L), "не найден");
        assertThat(hits(RESERVE_PATH)).isZero();
    }

    @Test
    void shouldRejectOrderWhenProductInactive() throws Exception {
        product(1L, false);

        assertRejected(createOrder(1L), "снят с продажи");
        assertThat(hits(RESERVE_PATH)).isZero();
    }

    @Test
    void shouldRejectOrderWhenInventoryRecordMissing() throws Exception {
        product(1L, true);
        RESPONSES.put(RESERVE_PATH, new StubResponse(404, "{\"message\":\"not found\"}"));

        assertRejected(createOrder(1L), "Складская запись");
    }

    @Test
    void shouldRejectOrderAndReleaseReservedWhenStockInsufficient() throws Exception {
        product(1L, true);
        product(2L, true);
        RESPONSES.put(RESERVE_PATH, new StubResponse(200,
                "{\"success\":true,\"availableQuantity\":10,\"message\":\"ok\"}",
                body -> body.contains("\"productId\":2")
                        ? new StubResponse(409, "{\"message\":\"insufficient\"}")
                        : null));

        assertRejected(createOrder(1L, 2L), "Недостаточно товара id=2");
        assertThat(RELEASE_BODIES)
                .as("Резерв уже зарезервированного товара должен быть снят")
                .singleElement()
                .satisfies(body -> assertThat(body).contains("\"productId\":1"));
    }

    @Test
    void shouldKeepCircuitClosedOnBusinessErrors() throws Exception {
        RESPONSES.put(PRODUCT_PATH + 1, new StubResponse(404, "{\"message\":\"not found\"}"));

        for (int i = 0; i < 10; i++) {
            assertThat(status(createOrder(1L))).isEqualTo(409);
        }

        assertThat(hits(PRODUCT_PATH + 1))
                .as("404 не должен открывать цепь: каждый запрос доходит до product-service")
                .isEqualTo(10);
    }

    @Test
    void shouldSavePendingOrderWhenProductServiceUnavailable() throws Exception {
        RESPONSES.put(PRODUCT_PATH + 1, new StubResponse(503, "{}"));

        Map<String, Object> order = assertPending(createOrder(1L));

        assertThat((String) order.get("statusDetails")).contains("Каталог");
        assertThat(order.get("totalPrice")).isNull();
        assertThat(hits(RESERVE_PATH))
                .as("Наличие на складе проверяется даже при недоступном каталоге")
                .isEqualTo(1);
    }

    @Test
    void shouldSavePendingOrderWhenProductServiceTimesOut() throws Exception {
        RESPONSES.put(PRODUCT_PATH + 1, new StubResponse(200, productJson(1L, true), null, 2_000));

        assertPending(createOrder(1L));
    }

    @Test
    void shouldSavePendingOrderWhenInventoryServiceUnavailable() throws Exception {
        product(1L, true);
        RESPONSES.put(RESERVE_PATH, new StubResponse(500, "{}"));

        Map<String, Object> order = assertPending(createOrder(1L));

        assertThat((String) order.get("statusDetails")).contains("Склад");
        assertThat(order.get("totalPrice"))
                .as("Данные каталога получены, поэтому сумма заказа известна")
                .isNotNull();
    }

    @Test
    void shouldSavePendingOrderWithoutCallingServiceWhenCircuitOpen() throws Exception {
        RESPONSES.put(PRODUCT_PATH + 1, new StubResponse(500, "{}"));

        for (int i = 0; i < 5; i++) {
            assertPending(createOrder(1L));
        }
        assertThat(circuitBreakerRegistry.getAllCircuitBreakers())
                .filteredOn(cb -> cb.getName().contains("ProductClient"))
                .singleElement()
                .extracting(CircuitBreaker::getState)
                .isEqualTo(CircuitBreaker.State.OPEN);

        int hitsBefore = hits(PRODUCT_PATH + 1);
        assertPending(createOrder(1L));

        assertThat(hits(PRODUCT_PATH + 1))
                .as("При открытой цепи запрос в product-service не отправляется")
                .isEqualTo(hitsBefore);
    }

    private void assertRejected(MvcResult response, String message) throws Exception {
        assertThat(status(response)).isEqualTo(409);
        assertThat((String) readMap(response).get("message")).contains(message);
        assertThat(orderRepository.count()).as("Заказ не должен сохраняться").isZero();
    }

    private Map<String, Object> assertPending(MvcResult response) throws Exception {
        assertThat(status(response)).isEqualTo(201);
        Map<String, Object> order = readMap(response);
        assertThat(order.get("status")).isEqualTo("PENDING_CONFIRMATION");
        assertThat(order.get("statusDetails")).isNotNull();
        return order;
    }

    private static void product(Long id, boolean active) {
        RESPONSES.put(PRODUCT_PATH + id, new StubResponse(200, productJson(id, active)));
    }

    private static String productJson(Long id, boolean active) {
        return "{\"id\":%d,\"name\":\"Product %d\",\"description\":\"d\",\"price\":100.00,\"active\":%s}"
                .formatted(id, id, active);
    }

    private static int hits(String path) {
        AtomicInteger counter = HITS.get(path);
        return counter == null ? 0 : counter.get();
    }

    private MvcResult createOrder(Long... productIds) throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "Buyer",
                "buyer@example.com",
                java.util.Arrays.stream(productIds).map(id -> new OrderItemRequest(id, 1)).toList()
        );
        return mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andReturn();
    }

    private static int status(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private Map<String, Object> readMap(MvcResult result) throws Exception {
        return json.readValue(result.getResponse().getContentAsString(StandardCharsets.UTF_8), new TypeReference<>() {
        });
    }

    private static HttpServer startStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", OrderCircuitBreakerTest::handle);
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        HITS.computeIfAbsent(path, p -> new AtomicInteger()).incrementAndGet();
        if (RELEASE_PATH.equals(path)) {
            RELEASE_BODIES.add(body);
        }

        StubResponse response = RESPONSES.getOrDefault(path, new StubResponse(500, "{}"));
        if (response.override() != null) {
            StubResponse overridden = response.override().apply(body);
            if (overridden != null) {
                response = overridden;
            }
        }
        if (response.delayMillis() > 0) {
            try {
                Thread.sleep(response.delayMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        try {
            exchange.sendResponseHeaders(response.status(), bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (IOException ignored) {
            // клиент уже отключился по таймауту
        } finally {
            exchange.close();
        }
    }

    private record StubResponse(int status,
                                String body,
                                java.util.function.Function<String, StubResponse> override,
                                long delayMillis) {
        StubResponse(int status, String body) {
            this(status, body, null, 0);
        }

        StubResponse(int status, String body, java.util.function.Function<String, StubResponse> override) {
            this(status, body, override, 0);
        }
    }
}
