package com.lundong.ascentialsync.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.contact.v3.ContactService;
import com.lark.oapi.service.contact.v3.model.P2UserCreatedV3;
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
            .onP2UserCreatedV3(new ContactService.P2UserCreatedV3Handler() {
                // 用户创建
                @Override
                public void handle(P2UserCreatedV3 event) {
                    log.info("P2UserCreatedV3: {}", Jsons.DEFAULT.toJson(event));
                    // 处理用户创建事件
                    // 1.获取处理订阅消息体
                    String resultJson = Jsons.DEFAULT.toJson(event);
                    JSONObject eventsObject = (JSONObject) JSONObject.parse(resultJson);
                    JSONObject eventObject = (JSONObject) eventsObject.get("event");
                    JSONObject object = (JSONObject) eventObject.get("object");
                    String user_id = object.getString("user_id");
                    String name = object.getString("name");
                    String mobile = object.getString("mobile");
                    JSONArray department_ids = (JSONArray) object.get("department_ids");
                    String employee_no = object.getString("employee_no");
                    String join_time = object.getString("join_time");
                    String job_title = object.getString("job_title");

                }
            }).onSpendFormUpdateV1(new CustomP2SpendFormUpdateV1Handler() {
                @Override
                public void handle(SpendFormUpdateEvent event) {
                    log.info("SpendFormUpdateV1: {}", Jsons.DEFAULT.toJson(event));
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