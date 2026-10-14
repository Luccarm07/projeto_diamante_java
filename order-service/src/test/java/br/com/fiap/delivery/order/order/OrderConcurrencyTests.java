package br.com.fiap.delivery.order.order;

import br.com.fiap.delivery.order.dish.Dish;
import br.com.fiap.delivery.order.dish.DishRepository;
import br.com.fiap.delivery.order.exception.OutOfStockException;
import br.com.fiap.delivery.order.exception.PaymentFailedException;
import br.com.fiap.delivery.order.payment.PaymentClient;
import br.com.fiap.delivery.order.payment.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "app.rate-limit.enabled=false"
})
class OrderConcurrencyTests {

    private static final long FLASH_SALE_DISH_ID = 1L;

    @Autowired
    private OrderService orderService;

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private PaymentClient paymentClient;

    @BeforeEach
    void resetFlashSaleStock() {
        orderRepository.deleteAll();
        Dish dish = dishRepository.findById(FLASH_SALE_DISH_ID).orElseThrow();
        dish.setStock(10);
        dishRepository.save(dish);
    }

    @Test
    void fiftySimultaneousOrdersConfirmExactlyTen() throws Exception {
        when(paymentClient.charge(any(BigDecimal.class))).thenReturn(new PaymentResponse("APPROVED", 8081));

        int requests = 50;
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicInteger confirmed = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            Callable<Void> task = () -> {
                startSignal.await(); // every thread fires at the same moment
                try {
                    orderService.placeOrder(new CreateOrderRequest(FLASH_SALE_DISH_ID, 1));
                    confirmed.incrementAndGet();
                } catch (OutOfStockException ex) {
                    outOfStock.incrementAndGet();
                }
                return null;
            };
            futures.add(pool.submit(task));
        }

        startSignal.countDown();
        for (Future<?> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(confirmed.get()).isEqualTo(10);
        assertThat(outOfStock.get()).isEqualTo(40);
        assertThat(dishRepository.findById(FLASH_SALE_DISH_ID).orElseThrow().getStock()).isZero();
        assertThat(orderRepository.countByDishIdAndStatus(FLASH_SALE_DISH_ID, OrderStatus.CONFIRMED)).isEqualTo(10);
    }

    @Test
    void failedPaymentKeepsStockIntactAndSavesNoOrder() {
        when(paymentClient.charge(any(BigDecimal.class)))
                .thenThrow(new RestClientException("All payment attempts failed"));

        assertThatThrownBy(() -> orderService.placeOrder(new CreateOrderRequest(FLASH_SALE_DISH_ID, 2)))
                .isInstanceOf(PaymentFailedException.class);

        assertThat(dishRepository.findById(FLASH_SALE_DISH_ID).orElseThrow().getStock()).isEqualTo(10);
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void orderingMoreThanTheStockIsRejected() {
        assertThatThrownBy(() -> orderService.placeOrder(new CreateOrderRequest(FLASH_SALE_DISH_ID, 11)))
                .isInstanceOf(OutOfStockException.class);

        assertThat(dishRepository.findById(FLASH_SALE_DISH_ID).orElseThrow().getStock()).isEqualTo(10);
    }
}
