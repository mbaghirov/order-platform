package com.mansur.orderplatform.orderservice.product;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;

/**
 * Removes cached products whose stock was changed by an order.
 * Eviction runs after the transaction commits, so a concurrent read
 * cannot put the old stock value back into Redis.
 */
@Component
public class ProductCacheEvictor {

    private final CacheManager cacheManager;

    public ProductCacheEvictor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictAfterCommit(Collection<Long> productIds) {
        List<Long> ids = List.copyOf(productIds);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict(ids);
                }
            });
        } else {
            evict(ids);
        }
    }

    private void evict(List<Long> ids) {
        Cache cache = cacheManager.getCache(ProductService.CACHE_NAME);
        if (cache != null) {
            ids.forEach(cache::evict);
        }
    }
}
