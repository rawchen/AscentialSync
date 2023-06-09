/*
 * MIT License
 *
 * Copyright (c) 2022 Lark Technologies Pte. Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice, shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.lundong.ascentialsync.config;

import com.google.gson.annotations.SerializedName;
import com.lark.oapi.event.model.BaseEvent;

public class SpendFormUpdateEvent extends BaseEvent {

    @SerializedName("event")
    private SpendFormUpdateEventData event;

    public SpendFormUpdateEventData getEvent() {
        return event;
    }

    public void setEvent(SpendFormUpdateEventData event) {
        this.event = event;
    }

    public static class SpendFormUpdateEventData {

        @SerializedName("form_code")
        private String formCode;
        @SerializedName("form_header_id")
        private String formHeaderId;
        @SerializedName("action")
        private String action;
        @SerializedName("biz_unit_code")
        private String bizUnitCode;
        @SerializedName("instance_id")
        private String instanceId;
        @SerializedName("modifier_union_id")
        private String modifierUnionId;
        @SerializedName("modifier_name")
        private String modifierName;
        @SerializedName("bpm_status")
        private String bpmStatus;
        @SerializedName("status")
        private String status;

        public String getFormCode() {
            return formCode;
        }

        public void setFormCode(String formCode) {
            this.formCode = formCode;
        }

        public String getFormHeaderId() {
            return formHeaderId;
        }

        public void setFormHeaderId(String formHeaderId) {
            this.formHeaderId = formHeaderId;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getBizUnitCode() {
            return bizUnitCode;
        }

        public void setBizUnitCode(String bizUnitCode) {
            this.bizUnitCode = bizUnitCode;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }

        public String getModifierUnionId() {
            return modifierUnionId;
        }

        public void setModifierUnionId(String modifierUnionId) {
            this.modifierUnionId = modifierUnionId;
        }

        public String getModifierName() {
            return modifierName;
        }

        public void setModifierName(String modifierName) {
            this.modifierName = modifierName;
        }

        public String getBpmStatus() {
            return bpmStatus;
        }

        public void setBpmStatus(String bpmStatus) {
            this.bpmStatus = bpmStatus;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
