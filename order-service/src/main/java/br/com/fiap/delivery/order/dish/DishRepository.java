package br.com.fiap.delivery.order.dish;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DishRepository extends JpaRepository<Dish, Long> {

    /**
     * SELECT ... FOR UPDATE: the row stays locked until the surrounding transaction ends,
     * so concurrent orders for the same dish are processed one at a time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Dish d where d.id = :id")
    Optional<Dish> findByIdForUpdate(@Param("id") Long id);
}
