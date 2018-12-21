package com.zyc.handwritten_springmvc.service.impl;

import com.zyc.handwritten_springmvc.annotation.HandWrittenService;
import com.zyc.handwritten_springmvc.service.IDemoService;

/**
 * @author zyc
 * @date 2018/12/21
 * @description
 */
@HandWrittenService
public class DemoServiceImpl implements IDemoService {
    @Override
    public String getName(String name) {
        return "test---------------------------------->";
    }
}