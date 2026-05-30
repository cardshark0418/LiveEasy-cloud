package com.easylive.controller;

import com.easylive.api.consumer.ResourceClient;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;

@RestController
@RequestMapping("/file")
@Validated
public class FileController  {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileController.class);

    @Resource
    private ResourceClient resourceClient;

    @RequestMapping("/getResource")
    public void getResource(HttpServletResponse servletResponse, @RequestParam String sourceName) {
        Response resource = resourceClient.getResource(sourceName);
        convertFileResponse2Stream(servletResponse,resource);
    }


    public void convertFileResponse2Stream(HttpServletResponse servletResponse, Response response) {
        Response.Body body = response.body();
        try (InputStream fileInputStream = body.asInputStream();
             OutputStream outStream = servletResponse.getOutputStream()) {
            byte[] bytes = new byte[1024];
            int len;
            while ((len = fileInputStream.read(bytes)) != -1) {
                outStream.write(bytes, 0, len);
            }
            outStream.flush();
        } catch (Exception e) {
            log.error("读取文件流失败", e);
        }
    }
}