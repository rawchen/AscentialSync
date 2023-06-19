package com.lundong.ascentialsync.controller;

import com.lundong.ascentialsync.config.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author RawChen
 * @date 2023-03-02 11:19
 */
@Slf4j
@RestController
@RequestMapping
public class EventController {

    @Autowired
    private CustomServletAdapter servletAdapter;

    // 注册消息处理器
    private final CustomEventDispatcher EVENT_DISPATCHER = CustomEventDispatcher
            .newBuilder(Constants.VERIFICATION_TOKEN, Constants.ENCRYPT_KEY)
            .onSpendFormUpdateV1(new CustomP2SpendFormUpdateV1Handler() {
                @Override
                public void handle(SpendFormUpdateEvent event) {
//                    log.info("SpendFormUpdateV1: {}", Jsons.DEFAULT.toJson(event));
                }
            })
            .build();

    /**
     * 飞书订阅事件回调
     *
     * @param request
     * @param response
     * @throws Throwable
     */
    @RequestMapping(value = "/feishu/webhook/event")
    public void event(HttpServletRequest request, HttpServletResponse response)
            throws Throwable {
        servletAdapter.handleEvent(request, response, EVENT_DISPATCHER);
    }
}