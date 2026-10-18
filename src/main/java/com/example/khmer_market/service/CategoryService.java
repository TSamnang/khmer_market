package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.Category;
import com.example.khmer_market.exception.ResourceNotFoundException;
import com.example.khmer_market.repository.CategoryRepository;
import com.example.khmer_market.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    public CategoryService(CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           FileStorageService fileStorageService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Category> findAllActive() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    @Transactional
    public Category createCategory(String name, String description, MultipartFile imageFile) {
        if (categoryRepository.existsByName(name.trim())) {
            throw new IllegalArgumentException("A category with this name already exists");
        }

        Category category = new Category();
        category.setName(name.trim());
        category.setDescription(description != null ? description.trim() : "");

        if (imageFile != null && !imageFile.isEmpty()) {
            String filename = fileStorageService.storeFile(imageFile);
            category.setImageUrl(filename);
        }

        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, String name, String description, MultipartFile imageFile) {
        Category category = getCategory(id);

        if (categoryRepository.existsByNameAndIdNot(name.trim(), id)) {
            throw new IllegalArgumentException("Another category with this name already exists");
        }

        category.setName(name.trim());
        category.setDescription(description != null ? description.trim() : "");

        if (imageFile != null && !imageFile.isEmpty()) {
            if (category.getImageUrl() != null) {
                fileStorageService.deleteFile(category.getImageUrl());
            }
            String filename = fileStorageService.storeFile(imageFile);
            category.setImageUrl(filename);
        }

        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategory(id);
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new IllegalArgumentException("Cannot delete category because " + productCount + " products are currently assigned to it.");
        }
        if (category.getImageUrl() != null) {
            fileStorageService.deleteFile(category.getImageUrl());
        }
        categoryRepository.delete(category);
    }
}
