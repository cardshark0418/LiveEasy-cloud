package com.easylive.mapper;

import com.easylive.entity.po.UserMessage;
import com.easylive.entity.vo.UserMessageCountDto;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMessageMapper extends MPJBaseMapper<UserMessage> {
    @Select("SELECT message_type AS messageType, COUNT(*) AS messageCount " +
            "FROM user_message " +
            "WHERE user_id = #{userId} AND read_type = 0 " +
            "GROUP BY message_type")
    List<UserMessageCountDto> getNoReadCountGroup(@Param("userId") String userId);

}