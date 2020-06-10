package com.mehayou.dingtalk;

import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import lombok.Getter;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DingTalkUserProperty extends UserProperty {

    @Getter
    private String mobile;

    public DingTalkUserProperty(String mobile) {
        this.mobile = mobile;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    @Extension
    public static final class DingTalkUserPropertyDescriptor extends UserPropertyDescriptor {

        public DingTalkUserPropertyDescriptor() {
            super(DingTalkUserProperty.class);
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "DingTalk Mobile";
        }

        @Override
        public UserProperty newInstance(User user) {
            return new DingTalkUserProperty(null);
        }

        @Override
        public UserProperty newInstance(@Nullable StaplerRequest req, @Nonnull JSONObject formData) {
            return new DingTalkUserProperty(formData.optString("mobile"));
        }
    }
}
