package ru.yandex.practicum.product.mapper;

import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.entity.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryDto toDto(Category category) {
        return new CategoryDto(category.getId(), category.getName(), category.getDescription());
    }
}
