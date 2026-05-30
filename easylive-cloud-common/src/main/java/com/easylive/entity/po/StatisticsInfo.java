package com.easylive.entity.po;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


/**
 * 数据统计
 */
@Setter
@Getter
public class StatisticsInfo implements Serializable {


	/**
	 * 统计日期
	 */
	private String statisticsDate;

	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 数据统计类型
	 */
	private Integer dataType;

	/**
	 * 统计数量
	 */
	private Integer statisticsCount;


    @Override
	public String toString (){
		return "统计日期:"+(statisticsDate == null ? "空" : statisticsDate)+"，用户ID:"+(userId == null ? "空" : userId)+"，数据统计类型:"+(dataType == null ? "空" : dataType)+"，统计数量:"+(statisticsCount == null ? "空" : statisticsCount);
	}
}
