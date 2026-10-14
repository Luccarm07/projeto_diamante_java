package br.com.fiap.delivery.order.dish;

import br.com.fiap.delivery.order.exception.DishNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dishes")
public class DishController {

    private final DishRepository dishRepository;

    public DishController(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @GetMapping
    public List<DishResponse> listAll() {
        return dishRepository.findAll(Sort.by("id")).stream()
                .map(DishResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public DishResponse findById(@PathVariable Long id) {
        return dishRepository.findById(id)
                .map(DishResponse::from)
                .orElseThrow(DishNotFoundException::new);
    }
}
