package br.com.fiap.delivery.order.order;

import br.com.fiap.delivery.order.dish.Dish;
import br.com.fiap.delivery.order.dish.DishRepository;
import br.com.fiap.delivery.order.exception.DishNotFoundException;
import br.com.fiap.delivery.order.exception.OrderNotFoundException;
import br.com.fiap.delivery.order.exception.OutOfStockException;
import br.com.fiap.delivery.order.exception.PaymentFailedException;
import br.com.fiap.delivery.order.payment.PaymentClient;
import br.com.fiap.delivery.order.payment.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final DishRepository dishRepository;
    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;

    public OrderService(DishRepository dishRepository, OrderRepository orderRepository,
                        PaymentClient paymentClient) {
        this.dishRepository = dishRepository;
        this.orderRepository = orderRepository;
        this.paymentClient = paymentClient;
    }

    /**
     * Places an order in a single transaction:
     * <ol>
     *   <li>locks the dish row (PESSIMISTIC_WRITE), so concurrent requests wait in line;</li>
     *   <li>checks the stock (409 when there is not enough);</li>
     *   <li>charges the payment (with retry, inside PaymentClient);</li>
     *   <li>only then decrements the stock and saves the CONFIRMED order.</li>
     * </ol>
     * If the payment fails after every retry, the exception rolls the transaction back:
     * nothing is saved and the stock is left untouched (502).
     */
    @Transactional
    public OrderResponse placeOrder(CreateOrderRequest request) {
        int quantity = request.quantity();

        Dish dish = dishRepository.findByIdForUpdate(request.dishId())
                .orElseThrow(DishNotFoundException::new);

        if (!dish.hasStockFor(quantity)) {
            log.info("Order rejected: dish {} has stock {}, requested {}", dish.getId(), dish.getStock(), quantity);
            throw new OutOfStockException();
        }

        BigDecimal totalPrice = dish.getPrice().multiply(BigDecimal.valueOf(quantity));

        PaymentResponse payment;
        try {
            payment = paymentClient.charge(totalPrice);
        } catch (RuntimeException ex) {
            throw new PaymentFailedException(ex);
        }

        dish.reserve(quantity);
        CustomerOrder order = orderRepository.save(new CustomerOrder(
                dish.getId(), quantity, totalPrice, OrderStatus.CONFIRMED,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));

        log.info("Order {} CONFIRMED: dish {} x{} = {} (paid by instance {}, stock left {})",
                order.getId(), dish.getId(), quantity, totalPrice, payment.instance(), dish.getStock());
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(OrderNotFoundException::new);
    }
}
