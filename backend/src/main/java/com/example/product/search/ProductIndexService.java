package com.example.product.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import com.example.product.config.AppProperties;
import com.example.product.entity.BizCategory;
import com.example.product.entity.BizProduct;
import com.example.product.repository.BizCategoryRepository;
import com.example.product.repository.BizProductRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Elasticsearch 索引维护：创建 mapping、单条同步、全量重建。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIndexService {

    private static final int BULK_BATCH_SIZE = 500;

    private final ElasticsearchClient esClient;
    private final BizProductRepository productRepository;
    private final BizCategoryRepository categoryRepository;
    private final AppProperties appProperties;

    public String indexName() {
        return appProperties.getSearch().getProductIndex();
    }

    /** 索引是否已存在 */
    public boolean indexExists() {
        try {
            return esClient.indices().exists(e -> e.index(indexName())).value();
        } catch (Exception e) {
            log.warn("检查索引是否存在失败: {}", e.getMessage());
            return false;
        }
    }

    /** 确保索引存在（不存在则按 mapping 创建） */
    public void ensureIndex() {
        try {
            if (indexExists()) {
                return;
            }
            createIndex();
        } catch (Exception e) {
            log.error("创建 Elasticsearch 索引失败: {}", e.getMessage());
        }
    }

    /** 删除并重建索引（全量重建的第一步） */
    public void recreateIndex() {
        try {
            if (indexExists()) {
                esClient.indices().delete(DeleteIndexRequest.of(b -> b.index(indexName())));
                log.info("已删除旧索引: {}", indexName());
            }
        } catch (Exception e) {
            log.warn("删除索引失败（可能不存在）: {}", e.getMessage());
        }
        createIndex();
    }

    private void createIndex() {
        String analyzer = appProperties.getSearch().getAnalyzer();
        String searchAnalyzer = appProperties.getSearch().getSearchAnalyzer();
        String mappingJson = buildMappingJson(analyzer, searchAnalyzer);
        try {
            esClient.indices().create(CreateIndexRequest.of(b -> b
                    .index(indexName())
                    .withJson(new StringReader(mappingJson))));
            log.info("索引 {} 创建成功 (analyzer={})", indexName(), analyzer);
        } catch (Exception e) {
            // 例如未安装 IK 分词插件时，回退到内置 standard 分词器
            log.warn("使用 analyzer={} 创建索引失败({})，回退 standard", analyzer, e.getMessage());
            String fallback = buildMappingJson("standard", "standard");
            try {
                esClient.indices().create(CreateIndexRequest.of(b -> b
                        .index(indexName())
                        .withJson(new StringReader(fallback))));
                log.info("索引 {} 使用 standard 分词器创建成功", indexName());
            } catch (Exception ex) {
                throw new IllegalStateException("创建 Elasticsearch 索引失败: " + ex.getMessage(), ex);
            }
        }
    }

    private String buildMappingJson(String analyzer, String searchAnalyzer) {
        return """
                {
                  "settings": {
                    "number_of_shards": 1,
                    "number_of_replicas": 0,
                    "refresh_interval": "1s"
                  },
                  "mappings": {
                    "properties": {
                      "id":           { "type": "long" },
                      "name":         { "type": "text", "analyzer": "%s", "search_analyzer": "%s" },
                      "code":         { "type": "keyword" },
                      "brand":        { "type": "text", "analyzer": "%s", "search_analyzer": "%s",
                                        "fields": { "keyword": { "type": "keyword" } } },
                      "categoryId":   { "type": "long" },
                      "categoryName": { "type": "keyword" },
                      "price":        { "type": "double" },
                      "stock":        { "type": "integer" },
                      "status":       { "type": "integer" },
                      "sales":        { "type": "integer" },
                      "description":  { "type": "text", "analyzer": "%s", "search_analyzer": "%s" },
                      "coverUrl":     { "type": "keyword" },
                      "createdAt":    { "type": "date", "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis" }
                    }
                  }
                }
                """.formatted(analyzer, searchAnalyzer, analyzer, searchAnalyzer, analyzer, searchAnalyzer);
    }

    /** 单条写入 / 更新索引（商品增改时同步调用） */
    public void indexOne(BizProduct product, String categoryName) {
        try {
            ensureIndex();
            ProductDoc doc = ProductDoc.from(product, categoryName);
            esClient.index(i -> i
                    .index(indexName())
                    .id(String.valueOf(doc.getId()))
                    .document(doc)
                    .refresh(co.elastic.clients.elasticsearch._types.Refresh.WaitFor));
            log.debug("商品 {} 已同步到 ES 索引", doc.getId());
        } catch (Exception e) {
            // 搜索索引异常不应影响主业务
            log.warn("同步商品 {} 到 ES 失败: {}", product.getId(), e.getMessage());
        }
    }

    /** 删除索引中的一条文档 */
    public void deleteOne(Long productId) {
        try {
            if (!indexExists()) {
                return;
            }
            esClient.delete(d -> d.index(indexName()).id(String.valueOf(productId)));
            log.debug("商品 {} 已从 ES 索引删除", productId);
        } catch (Exception e) {
            log.warn("从 ES 删除商品 {} 失败: {}", productId, e.getMessage());
        }
    }

    /**
     * 全量重建索引：删除旧索引 -> 重建 mapping -> 批量写入全部商品。
     */
    public RebuildResult rebuildAll() {
        long start = System.currentTimeMillis();
        recreateIndex();

        List<BizProduct> products = productRepository.findAllForIndex();
        Map<Long, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(BizCategory::getId, BizCategory::getName, (a, b) -> a));

        long success = 0L;
        long failed = 0L;
        for (int i = 0; i < products.size(); i += BULK_BATCH_SIZE) {
            List<BizProduct> batch = products.subList(i, Math.min(i + BULK_BATCH_SIZE, products.size()));
            BulkRequest.Builder builder = new BulkRequest.Builder();
            for (BizProduct product : batch) {
                ProductDoc doc = ProductDoc.from(product, categoryNames.get(product.getCategoryId()));
                builder.operations(op -> op.index(idx -> idx
                        .index(indexName())
                        .id(String.valueOf(doc.getId()))
                        .document(doc)));
            }
            try {
                BulkResponse response = esClient.bulk(builder.build());
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        failed++;
                        log.warn("批量索引失败 id={}: {}", item.id(), item.error().reason());
                    } else {
                        success++;
                    }
                }
            } catch (Exception e) {
                failed += batch.size();
                log.error("批量写入 ES 失败: {}", e.getMessage());
            }
        }

        // 立即刷新，保证搜索能马上看到数据
        try {
            esClient.indices().refresh(r -> r.index(indexName()));
        } catch (Exception e) {
            log.warn("刷新索引失败: {}", e.getMessage());
        }

        // 校正文档总数
        long count = countDocuments();
        long duration = System.currentTimeMillis() - start;
        log.info("索引 {} 全量重建完成: 扫描 {} 条, 成功 {}, 失败 {}, 索引文档数 {}, 耗时 {} ms",
                indexName(), products.size(), success, failed, count, duration);
        return new RebuildResult(products.size(), success, failed, count, duration);
    }

    /** 索引中的文档总数 */
    public long countDocuments() {
        try {
            return esClient.count(c -> c.index(indexName())).count();
        } catch (Exception e) {
            return 0L;
        }
    }

    /** 索引状态概要 */
    public Map<String, Object> indexStatus() {
        boolean exists = indexExists();
        return Map.of(
                "indexName", indexName(),
                "exists", exists,
                "documentCount", exists ? countDocuments() : 0L,
                "analyzer", appProperties.getSearch().getAnalyzer());
    }

    /** 重建结果 */
    @Getter
    public static class RebuildResult {
        private final long total;
        private final long success;
        private final long failed;
        private final long documentCount;
        private final long durationMs;

        public RebuildResult(long total, long success, long failed, long documentCount, long durationMs) {
            this.total = total;
            this.success = success;
            this.failed = failed;
            this.documentCount = documentCount;
            this.durationMs = durationMs;
        }
    }

    /** 未使用占位，保持函数式引用一致性 */
    @SuppressWarnings("unused")
    private Function<BizProduct, ProductDoc> mapper(Map<Long, String> names) {
        return p -> ProductDoc.from(p, names.get(p.getCategoryId()));
    }
}
