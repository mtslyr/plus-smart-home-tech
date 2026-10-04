package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.mapper.ProductMapper;
import ru.yandex.practicum.product.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public List<ProductDto> getAllActive() {
        return productRepository.findAllByActiveTrue().stream().map(ProductMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ProductDto getById(Long id) {
        return ProductMapper.toDto(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getByCategory(Long categoryId) {
        return productRepository.findAllByCategoryIdAndActiveTrue(categoryId).stream().map(ProductMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDto> search(String query) {
        return productRepository.findAllByNameContainingIgnoreCaseAndActiveTrue(query).stream().map(ProductMapper::toDto).toList();
    }

    @Transactional
    public ProductDto create(CreateProductRequest request) {
        Category category = request.categoryId() == null ? null : categoryService.getEntity(request.categoryId());
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .category(category)
                .imageUrl(request.imageUrl())
                .active(true)
                .build();
        return ProductMapper.toDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto update(Long id, UpdateProductRequest request) {
        Product product = getEntity(id);
        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.categoryId() != null) {
            product.setCategory(categoryService.getEntity(request.categoryId()));
        }
        if (request.imageUrl() != null) {
            product.setImageUrl(request.imageUrl());
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }
        return ProductMapper.toDto(product);
    }

    private Product getEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Товар с id=" + id + " не найден"));
    }
}
