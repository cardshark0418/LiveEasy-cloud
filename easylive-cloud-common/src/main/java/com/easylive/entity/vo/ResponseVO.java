package com.easylive.entity.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ResponseVO implements Serializable {

    private Integer code;
    private String status;
    private String info;
    private Object data;

    public static ResponseVO getSuccessResponseVO(Object data) {
        ResponseVO vo = new ResponseVO();
        vo.setCode(200);
        vo.setStatus("success");
        vo.setInfo("操作成功");
        vo.setData(data);
        return vo;
    }

    public static ResponseVO getFailResponseVO(String msg) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus("error");
        vo.setInfo(msg);
        vo.setCode(600); // 你可以根据业务定义错误码
        return vo;
    }
}