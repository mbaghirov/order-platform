package com.mansur.orderplatform.orderservice.product;

import com.mansur.orderplatform.orderservice.common.PageResponse;
import com.mansur.orderplatform.orderservice.common.exception.ResourceNotFoundException;
import com.mansur.orderplatform.orderservice.product.dto.ProductRequest;
import com.mansur.orderplatform.orderservice.product.dto.ProductResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product catalog. Single-product reads are cached in Redis ("products" cache, key = product id).
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    public static final String CACHE_NAME = "products";

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product(request.name().trim(), request.description(),
                request.price(), request.stockQuantity());
        return ProductResponse.from(productRepository.save(product));
    }

    @Cacheable(cacheNames = CACHE_NAME, key = "#id")
    public ProductResponse getById(Long id) {
        return ProductResponse.from(findProduct(id));
    }

    public PageResponse<ProductResponse> list(Pageable pageable) {
        return PageResponse.of(productRepository.findAll(pageable), ProductResponse::from);
    }

    @Transactional
    @CachePut(cacheNames = CACHE_NAME, key = "#id")
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        product.update(request.name().trim(), request.description(), request.price(), request.stockQuantity());
        return ProductResponse.from(productRepository.saveAndFlush(product));
    }

    @Transactional
    @CacheEvict(cacheNames = CACHE_NAME, key = "#id")
    public void delete(Long id) {
        productRepository.delete(findProduct(id));
        productRepository.flush();
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }
}
