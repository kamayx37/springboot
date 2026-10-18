package com.example.product.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.product.common.PageResult;
import com.example.product.config.AppProperties;
import com.example.product.dto.ProductSearchRequest;
import com.example.product.dto.ProductSearchVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 全文检索：关键词 + 组合条件筛选 + 结果高亮。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private static final String PRE_TAG = "<em class=\"hl\">";
    private static final String POST_TAG = "</em>";

    private final ElasticsearchClient esClient;
    private final ProductIndexService indexService;
    private final AppProperties appProperties;

    /**
     * 执行检索。
     */
    public PageResult<ProductSearchVO> search(ProductSearchRequest request) {
        String indexName = indexService.indexName();
        int page = request.safePage();
        int size = request.safeSize();

        try {
            if (!indexService.indexExists()) {
                log.warn("索引 {} 不存在，请先执行全量重建", indexName);
                return PageResult.empty(page, size);
            }

            SearchResponse<ProductDoc> response = esClient.search(s -> {
                s.index(indexName).from((page - 1) * size).size(size);
                s.query(buildQuery(request));
                // 高亮配置
                s.highlight(h -> h
                        .preTags(PRE_TAG)
                        .postTags(POST_TAG)
                        .requireFieldMatch(false)
                        .fields("name", HighlightField.of(f -> f.numberOfFragments(0)))
                        .fields("description", HighlightField.of(f -> f.fragmentSize(120).numberOfFragments(2)))
                        .fields("brand", HighlightField.of(f -> f.numberOfFragments(0)))
                        .fields("code", HighlightField.of(f -> f.numberOfFragments(0))));
                applySort(s, request);
                return s;
            }, ProductDoc.class);

            long total = response.hits().total() == null ? 0L : response.hits().total().value();
            List<ProductSearchVO> records = new ArrayList<>();
            for (Hit<ProductDoc> hit : response.hits().hits()) {
                records.add(toVO(hit));
            }
            long pages = size == 0 ? 0 : (total + size - 1) / size;
            return new PageResult<>(records, total, page, size, pages);

        } catch (Exception e) {
            log.error("Elasticsearch 检索失败: {}", e.getMessage(), e);
            return PageResult.empty(page, size);
        }
    }

    /** 组装查询：关键词走 multi_match，其余条件走 filter */
    private Query buildQuery(ProductSearchRequest request) {
        return Query.of(q -> q.bool(b -> {
            if (StringUtils.hasText(request.getKeyword())) {
                String keyword = request.getKeyword().trim();
                b.must(m -> m.multiMatch(mm -> mm
                        .query(keyword)
                        .fields("name^3", "brand^2", "description", "code^2")
                        .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)));
            } else {
                b.must(m -> m.matchAll(ma -> ma));
            }
            if (request.getCategoryId() != null) {
                b.filter(f -> f.term(t -> t.field("categoryId").value(request.getCategoryId())));
            }
            if (StringUtils.hasText(request.getBrand())) {
                b.filter(f -> f.term(t -> t.field("brand.keyword").value(request.getBrand())));
            }
            if (request.getStatus() != null) {
                b.filter(f -> f.term(t -> t.field("status").value(request.getStatus())));
            }
            if (request.getMinPrice() != null || request.getMaxPrice() != null) {
                b.filter(f -> f.range(r -> {
                    r.field("price");
                    if (request.getMinPrice() != null) {
                        r.gte(co.elastic.clients.json.JsonData.of(request.getMinPrice()));
                    }
                    if (request.getMaxPrice() != null) {
                        r.lte(co.elastic.clients.json.JsonData.of(request.getMaxPrice()));
                    }
                    return r;
                }));
            }
            return b;
        }));
    }

    private void applySort(co.elastic.clients.elasticsearch.core.SearchRequest.Builder s,
                           ProductSearchRequest request) {
        String field = request.getSortField();
        SortOrder order = "asc".equalsIgnoreCase(request.getSortOrder()) ? SortOrder.Asc : SortOrder.Desc;
        if (!StringUtils.hasText(field) || "_score".equals(field)) {
            s.sort(so -> so.score(sc -> sc.order(order)));
            return;
        }
        String sortable = switch (field) {
            case "price", "sales", "stock", "createdAt", "id" -> field;
            default -> null;
        };
        if (sortable == null) {
            s.sort(so -> so.score(sc -> sc.order(order)));
            return;
        }
        s.sort(so -> so.field(f -> f.field(sortable).order(order)));
    }

    /** 把命中结果转成带高亮的 VO */
    private ProductSearchVO toVO(Hit<ProductDoc> hit) {
        ProductDoc doc = hit.source();
        ProductSearchVO vo = new ProductSearchVO();
        if (doc == null) {
            return vo;
        }
        vo.setId(doc.getId());
        vo.setName(doc.getName());
        vo.setCode(doc.getCode());
        vo.setBrand(doc.getBrand());
        vo.setCategoryId(doc.getCategoryId());
        vo.setCategoryName(doc.getCategoryName());
        vo.setPrice(doc.getPrice());
        vo.setStock(doc.getStock());
        vo.setStatus(doc.getStatus());
        vo.setSales(doc.getSales());
        vo.setDescription(doc.getDescription());
        vo.setCoverUrl(doc.getCoverUrl());
        vo.setScore(hit.score());

        Map<String, List<String>> highlights = hit.highlight();
        if (highlights != null && !highlights.isEmpty()) {
            highlights.forEach((field, fragments) -> {
                if (fragments == null || fragments.isEmpty()) {
                    return;
                }
                switch (field) {
                    case "name" -> vo.setNameHighlight(fragments.get(0));
                    case "description" -> vo.setDescriptionHighlight(String.join(" … ", fragments));
                    case "brand" -> vo.setBrandHighlight(fragments.get(0));
                    default -> {
                        // 其他字段不单独展示，统一放进 highlights 列表
                    }
                }
                fragments.forEach(fragment ->
                        vo.getHighlights().add(new ProductSearchVO.HighlightFragment(field, fragment)));
            });
        }
        // 没有高亮时退化为原文
        if (vo.getNameHighlight() == null) {
            vo.setNameHighlight(vo.getName());
        }
        return vo;
    }

    /** 搜索建议：返回命中的品牌与分类，用于前端筛选下拉 */
    public Map<String, Object> filters() {
        return Map.of(
                "indexStatus", indexService.indexStatus(),
                "pageSize", appProperties.getSearch().getPageSize());
    }
}
