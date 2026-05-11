package com.mootdx.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面跳转控制器
 */
@Controller
public class PageController {

    /**
     * 通达信数据导入页面
     */
    @GetMapping("/tdx-import")
    public String tdxImportPage() {
        return "redirect:/tdx-import.html";
    }

    /**
     * 首页
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/tdx-import.html";
    }
}
