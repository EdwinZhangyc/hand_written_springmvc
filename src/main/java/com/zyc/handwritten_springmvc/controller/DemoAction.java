package com.zyc.handwritten_springmvc.controller;

import com.zyc.handwritten_springmvc.annotation.HandWrittenAutowired;
import com.zyc.handwritten_springmvc.annotation.HandWrittenController;
import com.zyc.handwritten_springmvc.annotation.HandWrittenRequestMapping;
import com.zyc.handwritten_springmvc.annotation.HandWrittenRequestParam;
import com.zyc.handwritten_springmvc.service.IDemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author zyc
 * @date 2018/12/21
 * @description 实例 action
 */
@HandWrittenController
@HandWrittenRequestMapping("/demo")
public class DemoAction {

    @HandWrittenAutowired
    private IDemoService demoService;

    @HandWrittenRequestMapping("/query.json")
    public void query (HttpServletRequest request, HttpServletResponse response,
                        @HandWrittenRequestParam("name") String name) {
        String result = demoService.getName (name);
        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}