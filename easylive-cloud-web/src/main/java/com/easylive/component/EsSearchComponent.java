package com.easylive.component;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easylive.config.AppConfig;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.SimplePage;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.VideoInfoEsDto;
import com.easylive.enums.SearchOrderTypeEnum;
import com.easylive.exception.BusinessException;
import com.easylive.mapper.UserInfoMapper;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component("esSearchUtils")
@Slf4j
public class EsSearchComponent {

    @Resource
    private AppConfig appConfig;

    @Resource
    private RestHighLevelClient restHighLevelClient;

    @Resource
    private UserInfoMapper userInfoMapper;

    private Boolean isExistIndex() throws IOException {
        GetIndexRequest getIndexRequest = new GetIndexRequest(appConfig.getEsIndexVideoName());
        return restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
    }

    public void createIndex() {
        try {
            Boolean existIndex = isExistIndex();
            if (existIndex) {
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
            if (docExist(videoInfo.getVideoId())) {
                updateDoc(videoInfo);
            }
            else {
                VideoInfoEsDto videoInfoEsDto = BeanUtil.copyProperties(videoInfo, VideoInfoEsDto.class);
                videoInfoEsDto.setCollectCount(0);
                videoInfoEsDto.setPlayCount(0);
                videoInfoEsDto.setDanmuCount(0);
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

    private void updateDoc(VideoInfo videoInfo) {
        try {
            //时间不更新
            videoInfo.setLastUpdateTime(null);
            videoInfo.setCreateTime(null);
            Map<String, Object> dataMap = BeanUtil.beanToMap(videoInfo, false, true);
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

}