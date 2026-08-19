package com.easylive.component;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easylive.config.AppConfig;
import com.easylive.config.EmbeddingProperties;
import com.easylive.config.HybridSearchProperties;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.SimplePage;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.VideoInfoEsDto;
import com.easylive.enums.SearchOrderTypeEnum;
import com.easylive.exception.BusinessException;
import com.easylive.mapper.UserInfoMapper;
import com.easylive.mapper.VideoInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component("esSearchUtils")
@Slf4j
public class EsSearchComponent {

    @Resource
    private AppConfig appConfig;

    @Resource
    private EmbeddingProperties embeddingProperties;

    @Resource
    private EmbeddingClient embeddingClient;

    @Resource
    private HybridSearchProperties hybridSearchProperties;

    @Resource
    private RestHighLevelClient restHighLevelClient;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private VideoInfoMapper videoInfoMapper;

    private Boolean isExistIndex() throws IOException {
        GetIndexRequest getIndexRequest = new GetIndexRequest(appConfig.getEsIndexVideoName());
        return restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
    }

    public void createIndex() {
        try {
            Boolean existIndex = isExistIndex();
            if (existIndex) {
                ensureVectorMapping();
                return;
            }
            CreateIndexRequest request = new CreateIndexRequest(appConfig.getEsIndexVideoName());
            request.settings(
                    "{\"analysis\": {\n" +
                            "      \"analyzer\": {\n" +
                            "        \"comma\": {\n" +
                            "          \"type\": \"pattern\",\n" +
                            "          \"pattern\": \",\"\n" +
                            "        }\n" +
                            "      }\n" +
                            "    }}", XContentType.JSON);

            // videoName 用 standard：本机 ES 未装 IK 插件时 ik_max_word 会导致建索引失败，留下空 mapping
            request.mapping(
                    "{\"properties\": {\n" +
                            "      \"videoId\":{\n" +
                            "        \"type\": \"keyword\",\n" +
                            "        \"index\": false\n" +
                            "      },\n" +
                            "      \"userId\":{\n" +
                            "        \"type\": \"keyword\",\n" +
                            "        \"index\": false\n" +
                            "      },\n" +
                            "      \"videoCover\":{\n" +
                            "        \"type\": \"keyword\",\n" +
                            "        \"index\": false\n" +
                            "      },\n" +
                            "      \"videoName\":{\n" +
                            "        \"type\": \"text\",\n" +
                            "        \"analyzer\": \"standard\"\n" +
                            "      },\n" +
                            "      \"tags\":{\n" +
                            "        \"type\": \"text\",\n" +
                            "        \"analyzer\": \"comma\"\n" +
                            "      },\n" +
                            "      \"playCount\":{\n" +
                            "        \"type\":\"integer\"\n" +
                            "      },\n" +
                            "      \"danmuCount\":{\n" +
                            "        \"type\":\"integer\"\n" +
                            "      },\n" +
                            "      \"collectCount\":{\n" +
                            "        \"type\":\"integer\"\n" +
                            "      },\n" +
                            "      \"createTime\":{\n" +
                            "        \"type\":\"date\",\n" +
                            "        \"format\": \"yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis\",\n" +
                            "        \"index\": false\n" +
                            "      },\n" +
                            "      \"videoVector\":{\n" +
                            "        \"type\": \"dense_vector\",\n" +
                            "        \"dims\": " + embeddingProperties.getDimension() + "\n" +
                            "      }\n" +
                            " }}", XContentType.JSON);

            CreateIndexResponse createIndexResponse = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
            boolean acknowledged = createIndexResponse.isAcknowledged();
            if (!acknowledged) {
                throw new BusinessException("初始化es失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("初始化es失败", e);
            throw new BusinessException("初始化es失败");
        }
    }

    public void saveDoc(VideoInfo videoInfo) {
        try {
            List<Float> videoVector = buildVideoVector(videoInfo);
            if (docExist(videoInfo.getVideoId())) {
                updateDoc(videoInfo, videoVector);
            }
            else {
                VideoInfoEsDto videoInfoEsDto = BeanUtil.copyProperties(videoInfo, VideoInfoEsDto.class);
                videoInfoEsDto.setCollectCount(0);
                videoInfoEsDto.setPlayCount(0);
                videoInfoEsDto.setDanmuCount(0);
                videoInfoEsDto.setVideoVector(videoVector);
                IndexRequest request = new IndexRequest(appConfig.getEsIndexVideoName());
                request.id(videoInfo.getVideoId()).source(JSONUtil.toJsonStr(videoInfoEsDto, JSONConfig.create().setDateFormat("yyyy-MM-dd HH:mm:ss")), XContentType.JSON);
                restHighLevelClient.index(request, RequestOptions.DEFAULT);


            }
        } catch (Exception e) {
            log.error("新增视频到es失败", e);
            throw new BusinessException("保存失败");
        }
    }

    private Boolean docExist(String id) throws IOException {
        GetRequest getRequest = new GetRequest(appConfig.getEsIndexVideoName(), id);
        // 执行查询
        GetResponse response = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
        return response.isExists();
    }

    private void updateDoc(VideoInfo videoInfo, List<Float> videoVector) {
        try {
            //时间不更新
            videoInfo.setLastUpdateTime(null);
            videoInfo.setCreateTime(null);
            Map<String, Object> dataMap = BeanUtil.beanToMap(videoInfo, false, true);
            if (videoVector != null) {
                dataMap.put("videoVector", videoVector);
            }
            UpdateRequest updateRequest = new UpdateRequest(appConfig.getEsIndexVideoName(), videoInfo.getVideoId());
            updateRequest.doc(dataMap);
            restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.error("新增视频到es失败", e);
            throw new BusinessException("保存失败");
        }
    }

    public void updateDocCount(String videoId, String fieldName, Integer count) {
        if (count == null || fieldName == null) {
            throw new BusinessException("保存失败 count或者fieldName为null");
        }
        try {
            UpdateRequest updateRequest = new UpdateRequest(appConfig.getEsIndexVideoName(), videoId);
            Script script = new Script(ScriptType.INLINE, "painless", "ctx._source." + fieldName + " += params.count", Collections.singletonMap("count", count));
            updateRequest.script(script);
            restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.error("更新数量到es失败", e);
            throw new BusinessException("保存失败");
        }
    }

    public void delDoc(String videoId) {
        try {
            DeleteRequest deleteRequest = new DeleteRequest(appConfig.getEsIndexVideoName(), videoId);
            restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.error("从es删除视频失败", e);
            throw new BusinessException("删除视频失败");
        }
    }

    public PaginationResultVO<VideoInfo> search(Boolean highlight, String keyword, Integer orderType, Integer pageNo, Integer pageSize) {
        try {
            SearchOrderTypeEnum searchOrderTypeEnum = SearchOrderTypeEnum.getByType(orderType);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            //关键字
            searchSourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, "videoName", "tags"));
            //高亮
            if (highlight) {
                HighlightBuilder highlightBuilder = new HighlightBuilder();
                highlightBuilder.field("videoName"); // 替换为你想要高亮的字段名
                highlightBuilder.preTags("<span class='highlight'>");
                highlightBuilder.postTags("</span>");
                searchSourceBuilder.highlighter(highlightBuilder);
            }
            //排序（带 unmappedType，避免索引无字段/空 mapping 时 sort 直接 500）
            if (orderType != null && searchOrderTypeEnum != null) {
                String unmappedType = "createTime".equals(searchOrderTypeEnum.getField()) ? "date" : "long";
                searchSourceBuilder.sort(new FieldSortBuilder(searchOrderTypeEnum.getField())
                        .order(SortOrder.DESC)
                        .unmappedType(unmappedType));
            } else {
                searchSourceBuilder.sort("_score", SortOrder.DESC);
            }
            pageNo = pageNo == null ? 1 : pageNo;
            //分页查询
            pageSize = pageSize == null ? 20 : pageSize;
            searchSourceBuilder.size(pageSize);
            searchSourceBuilder.from((pageNo - 1) * pageSize);

            SearchRequest searchRequest = new SearchRequest(appConfig.getEsIndexVideoName());
            searchRequest.source(searchSourceBuilder);

            // 执行查询
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // 处理查询结果
            SearchHits hits = searchResponse.getHits();
            Integer totalCount = (int) hits.getTotalHits().value;

            List<VideoInfo> videoInfoList = new ArrayList<>();

            List<String> userIdList = new ArrayList<>();
            for (SearchHit hit : hits.getHits()) {
                VideoInfo videoInfo = JSONUtil.toBean(hit.getSourceAsString(), VideoInfo.class);
                if (hit.getHighlightFields().get("videoName") != null) {
                    videoInfo.setVideoName(hit.getHighlightFields().get("videoName").fragments()[0].string());
                }
                videoInfoList.add(videoInfo);

                userIdList.add(videoInfo.getUserId());
            }
            if (videoInfoList.isEmpty()) {
                // 如果 ES 没搜到结果，直接返回空的分页结果，不要往下查数据库了
                return new PaginationResultVO<>(totalCount, pageSize, pageNo, 0, videoInfoList);
            }
            List<UserInfo> userInfoList = userInfoMapper.selectList(new LambdaQueryWrapper<UserInfo>().in(UserInfo::getUserId,userIdList));
            Map<String, UserInfo> userInfoMap = userInfoList.stream().collect(Collectors.toMap(item -> item.getUserId(), Function.identity(), (data1, data2) -> data2));
            videoInfoList.forEach(item -> {
                UserInfo userInfo = userInfoMap.get(item.getUserId());
                if (userInfo != null) {
                    item.setNickName(userInfo.getNickName());
                }
            });
            SimplePage page = new SimplePage(pageNo, totalCount, pageSize);
            PaginationResultVO<VideoInfo> result = new PaginationResultVO(totalCount, page.getPageSize(), page.getPageNo(), page.getPageTotal(), videoInfoList);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询视频到es失败", e);
            throw new BusinessException("查询失败");
        }
    }

    /**
     * 使用视频向量进行语义检索。向量服务不可用时自动回退到关键词检索。
     */
    public PaginationResultVO<VideoInfo> semanticSearch(Boolean highlight, String keyword, Integer orderType, Integer pageNo, Integer pageSize) {
        if (!embeddingClient.isEnabled() || keyword == null || keyword.trim().isEmpty()) {
            return search(highlight, keyword, orderType, pageNo, pageSize);
        }
        try {
            List<Float> queryVector = embeddingClient.embed(keyword.trim());
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.scriptScoreQuery(
                    QueryBuilders.existsQuery("videoVector"),
                    new Script(ScriptType.INLINE, "painless",
                            "cosineSimilarity(params.query_vector, 'videoVector') + 1.0",
                            Collections.<String, Object>singletonMap("query_vector", queryVector))));
            SearchOrderTypeEnum searchOrderTypeEnum = SearchOrderTypeEnum.getByType(orderType);
            if (orderType != null && searchOrderTypeEnum != null) {
                String unmappedType = "createTime".equals(searchOrderTypeEnum.getField()) ? "date" : "long";
                sourceBuilder.sort(new FieldSortBuilder(searchOrderTypeEnum.getField())
                        .order(SortOrder.DESC)
                        .unmappedType(unmappedType));
            } else {
                sourceBuilder.sort("_score", SortOrder.DESC);
            }
            pageNo = pageNo == null || pageNo <= 0 ? 1 : pageNo;
            pageSize = pageSize == null || pageSize <= 0 ? 20 : pageSize;
            sourceBuilder.size(pageSize);
            sourceBuilder.from((pageNo - 1) * pageSize);
            if (Boolean.TRUE.equals(highlight)) {
                HighlightBuilder highlightBuilder = new HighlightBuilder();
                highlightBuilder.field("videoName");
                highlightBuilder.preTags("<span class='highlight'>");
                highlightBuilder.postTags("</span>");
                sourceBuilder.highlighter(highlightBuilder);
            }

            SearchRequest request = new SearchRequest(appConfig.getEsIndexVideoName());
            request.source(sourceBuilder);
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            if (hits.getHits().length == 0) {
                return search(highlight, keyword, orderType, pageNo, pageSize);
            }
            return buildSearchResult(hits, highlight, pageNo, pageSize);
        } catch (Exception e) {
            log.warn("语义搜索失败，回退到关键词搜索", e);
            return search(highlight, keyword, orderType, pageNo, pageSize);
        }
    }

    /**
     * 同时执行关键词和向量检索，先过滤低相关结果，再使用 RRF 按排名融合。
     */
    public PaginationResultVO<VideoInfo> hybridSearch(Boolean highlight, String keyword, Integer orderType,
                                                       Integer pageNo, Integer pageSize) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return search(highlight, keyword, orderType, pageNo, pageSize);
        }
        if (!embeddingClient.isEnabled()) {
            return search(highlight, keyword, orderType, pageNo, pageSize);
        }

        int recallSize = hybridSearchProperties.getRecallSize() == null
                || hybridSearchProperties.getRecallSize() <= 0 ? 10 : hybridSearchProperties.getRecallSize();
        double rrfK = hybridSearchProperties.getRrfK() == null
                || hybridSearchProperties.getRrfK() < 0 ? 60D : hybridSearchProperties.getRrfK();
        float keywordMinScore = hybridSearchProperties.getKeywordMinScore() == null
                ? 0F : hybridSearchProperties.getKeywordMinScore();
        float vectorMinCosine = hybridSearchProperties.getVectorMinCosineSimilarity() == null
                ? 0F : hybridSearchProperties.getVectorMinCosineSimilarity();

        try {
            List<Float> queryVector = embeddingClient.embed(keyword.trim());
            SearchResponse keywordResponse = restHighLevelClient.search(
                    buildKeywordRequest(keyword.trim(), highlight, recallSize, keywordMinScore),
                    RequestOptions.DEFAULT);
            SearchResponse vectorResponse = restHighLevelClient.search(
                    buildVectorRequest(queryVector, highlight, recallSize, vectorMinCosine),
                    RequestOptions.DEFAULT);

            Map<String, HybridCandidate> candidates = new LinkedHashMap<>();
            mergeCandidates(candidates, keywordResponse.getHits(), true, rrfK);
            mergeCandidates(candidates, vectorResponse.getHits(), false, rrfK);

            List<HybridCandidate> sortedCandidates = candidates.values().stream()
                    .sorted(Comparator.comparing(HybridCandidate::getFusionScore).reversed())
                    .collect(Collectors.toList());
            int normalizedPageNo = pageNo == null || pageNo <= 0 ? 1 : pageNo;
            int normalizedPageSize = pageSize == null || pageSize <= 0 ? 20 : pageSize;
            int from = Math.min((normalizedPageNo - 1) * normalizedPageSize, sortedCandidates.size());
            int to = Math.min(from + normalizedPageSize, sortedCandidates.size());
            List<VideoInfo> resultList = sortedCandidates.subList(from, to).stream()
                    .map(HybridCandidate::getVideoInfo)
                    .collect(Collectors.toList());
            return buildSearchResult(resultList, sortedCandidates.size(), normalizedPageNo, normalizedPageSize);
        } catch (Exception e) {
            log.warn("混合检索失败，回退到关键词检索", e);
            return search(highlight, keyword, orderType, pageNo, pageSize);
        }
    }

    private SearchRequest buildKeywordRequest(String keyword, Boolean highlight, int size, float minScore) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.multiMatchQuery(keyword, "videoName", "tags"))
                .minScore(minScore)
                .size(size)
                .sort("_score", SortOrder.DESC);
        addHighlight(sourceBuilder, highlight);
        return new SearchRequest(appConfig.getEsIndexVideoName()).source(sourceBuilder);
    }

    private SearchRequest buildVectorRequest(List<Float> queryVector, Boolean highlight, int size,
                                             float minCosineSimilarity) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.scriptScoreQuery(
                        QueryBuilders.existsQuery("videoVector"),
                        new Script(ScriptType.INLINE, "painless",
                                "cosineSimilarity(params.query_vector, 'videoVector') + 1.0",
                                Collections.<String, Object>singletonMap("query_vector", queryVector))))
                .minScore(1F + minCosineSimilarity)
                .size(size)
                .sort("_score", SortOrder.DESC);
        addHighlight(sourceBuilder, highlight);
        return new SearchRequest(appConfig.getEsIndexVideoName()).source(sourceBuilder);
    }

    private void addHighlight(SearchSourceBuilder sourceBuilder, Boolean highlight) {
        if (Boolean.TRUE.equals(highlight)) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("videoName");
            highlightBuilder.preTags("<span class='highlight'>");
            highlightBuilder.postTags("</span>");
            sourceBuilder.highlighter(highlightBuilder);
        }
    }

    private void mergeCandidates(Map<String, HybridCandidate> candidates, SearchHits hits,
                                 boolean keywordRoute, double rrfK) {
        SearchHit[] searchHits = hits == null ? new SearchHit[0] : hits.getHits();
        for (int index = 0; index < searchHits.length; index++) {
            SearchHit hit = searchHits[index];
            VideoInfo videoInfo = JSONUtil.toBean(hit.getSourceAsString(), VideoInfo.class);
            if (Boolean.TRUE.equals(keywordRoute) && hit.getHighlightFields().get("videoName") != null) {
                videoInfo.setVideoName(hit.getHighlightFields().get("videoName").fragments()[0].string());
            }
            String videoId = videoInfo.getVideoId() == null ? hit.getId() : videoInfo.getVideoId();
            HybridCandidate candidate = candidates.get(videoId);
            if (candidate == null) {
                candidate = new HybridCandidate(videoInfo);
                candidates.put(videoId, candidate);
            } else if (keywordRoute) {
                candidate.setVideoInfo(videoInfo);
            }
            candidate.addRank(index + 1, rrfK);
        }
    }

    private PaginationResultVO<VideoInfo> buildSearchResult(List<VideoInfo> videoInfoList,
                                                              int totalCount, int pageNo, int pageSize) {
        if (videoInfoList == null || videoInfoList.isEmpty()) {
            return new PaginationResultVO<>(totalCount, pageSize, pageNo,
                    (totalCount + pageSize - 1) / pageSize, new ArrayList<>());
        }
        List<String> userIdList = videoInfoList.stream()
                .map(VideoInfo::getUserId)
                .filter(item -> item != null && !item.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (!userIdList.isEmpty()) {
            List<UserInfo> userInfoList = userInfoMapper.selectList(
                    new LambdaQueryWrapper<UserInfo>().in(UserInfo::getUserId, userIdList));
            Map<String, UserInfo> userInfoMap = userInfoList.stream()
                    .collect(Collectors.toMap(UserInfo::getUserId, Function.identity(), (data1, data2) -> data2));
            videoInfoList.forEach(item -> {
                UserInfo userInfo = userInfoMap.get(item.getUserId());
                if (userInfo != null) {
                    item.setNickName(userInfo.getNickName());
                }
            });
        }
        return new PaginationResultVO<>(totalCount, pageSize, pageNo,
                (totalCount + pageSize - 1) / pageSize, videoInfoList);
    }

    private static class HybridCandidate {
        private VideoInfo videoInfo;
        private double fusionScore;

        private HybridCandidate(VideoInfo videoInfo) {
            this.videoInfo = videoInfo;
        }

        private void addRank(int rank, double rrfK) {
            fusionScore += 1D / (rrfK + rank);
        }

        private VideoInfo getVideoInfo() {
            return videoInfo;
        }

        private void setVideoInfo(VideoInfo videoInfo) {
            this.videoInfo = videoInfo;
        }

        private double getFusionScore() {
            return fusionScore;
        }
    }

    /**
     * 为已经存在的正式视频补建向量。新视频在审核通过写入 ES 时会自动生成向量。
     */
    public Map<String, Integer> rebuildVideoVectors() {
        if (!embeddingClient.isEnabled()) {
            throw new BusinessException("向量模型未启用，请先配置 embedding.api-key");
        }
        int successCount = 0;
        int failCount = 0;
        try {
            List<VideoInfo> videoInfoList = videoInfoMapper.selectList(null);
            for (VideoInfo videoInfo : videoInfoList) {
                try {
                    List<Float> vector = embeddingClient.embed(buildSemanticText(videoInfo));
                    upsertVideoVector(videoInfo, vector);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.warn("视频向量补建失败，视频编号：{}", videoInfo.getVideoId(), e);
                }
            }
        } catch (Exception e) {
            log.error("视频向量补建失败", e);
            throw new BusinessException("视频向量补建失败");
        }
        Map<String, Integer> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        return result;
    }

    private List<Float> buildVideoVector(VideoInfo videoInfo) {
        if (!embeddingClient.isEnabled()) {
            return null;
        }
        try {
            return embeddingClient.embed(buildSemanticText(videoInfo));
        } catch (Exception e) {
            log.warn("视频向量生成失败，视频编号：{}，将保留关键词检索", videoInfo.getVideoId(), e);
            return null;
        }
    }

    private void upsertVideoVector(VideoInfo videoInfo, List<Float> vector) throws IOException {
        if (docExist(videoInfo.getVideoId())) {
            UpdateRequest updateRequest = new UpdateRequest(appConfig.getEsIndexVideoName(), videoInfo.getVideoId());
            updateRequest.doc(Collections.<String, Object>singletonMap("videoVector", vector));
            restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
            return;
        }

        VideoInfoEsDto dto = BeanUtil.copyProperties(videoInfo, VideoInfoEsDto.class);
        dto.setVideoVector(vector);
        IndexRequest indexRequest = new IndexRequest(appConfig.getEsIndexVideoName())
                .id(videoInfo.getVideoId())
                .source(JSONUtil.toJsonStr(dto, JSONConfig.create().setDateFormat("yyyy-MM-dd HH:mm:ss")), XContentType.JSON);
        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    private String buildSemanticText(VideoInfo videoInfo) {
        StringBuilder text = new StringBuilder();
        appendText(text, "标题", videoInfo.getVideoName());
        appendText(text, "标签", videoInfo.getTags());
        appendText(text, "简介", videoInfo.getIntroduction());
        appendText(text, "分类", videoInfo.getCategoryFullName());
        if (text.length() == 0) {
            text.append(videoInfo.getVideoId());
        }
        return text.toString();
    }

    private void appendText(StringBuilder text, String name, String value) {
        if (value != null && !value.trim().isEmpty()) {
            text.append(name).append("：").append(value.trim()).append("；");
        }
    }

    private void ensureVectorMapping() throws IOException {
        PutMappingRequest request = new PutMappingRequest(appConfig.getEsIndexVideoName());
        request.source("{\"properties\":{\"videoVector\":{\"type\":\"dense_vector\",\"dims\":"
                + embeddingProperties.getDimension() + "}}}", XContentType.JSON);
        restHighLevelClient.indices().putMapping(request, RequestOptions.DEFAULT);
    }

    private PaginationResultVO<VideoInfo> buildSearchResult(SearchHits hits, Boolean highlight, Integer pageNo, Integer pageSize) {
        Integer totalCount = (int) hits.getTotalHits().value;
        List<VideoInfo> videoInfoList = new ArrayList<>();
        List<String> userIdList = new ArrayList<>();
        for (SearchHit hit : hits.getHits()) {
            VideoInfo videoInfo = JSONUtil.toBean(hit.getSourceAsString(), VideoInfo.class);
            if (Boolean.TRUE.equals(highlight) && hit.getHighlightFields().get("videoName") != null) {
                videoInfo.setVideoName(hit.getHighlightFields().get("videoName").fragments()[0].string());
            }
            videoInfoList.add(videoInfo);
            userIdList.add(videoInfo.getUserId());
        }
        if (videoInfoList.isEmpty()) {
            return new PaginationResultVO<>(totalCount, pageSize, pageNo, 0, videoInfoList);
        }
        List<UserInfo> userInfoList = userInfoMapper.selectList(new LambdaQueryWrapper<UserInfo>().in(UserInfo::getUserId, userIdList));
        Map<String, UserInfo> userInfoMap = userInfoList.stream()
                .collect(Collectors.toMap(UserInfo::getUserId, Function.identity(), (data1, data2) -> data2));
        videoInfoList.forEach(item -> {
            UserInfo userInfo = userInfoMap.get(item.getUserId());
            if (userInfo != null) {
                item.setNickName(userInfo.getNickName());
            }
        });
        SimplePage page = new SimplePage(pageNo, totalCount, pageSize);
        return new PaginationResultVO<>(totalCount, page.getPageSize(), page.getPageNo(), page.getPageTotal(), videoInfoList);
    }

}
