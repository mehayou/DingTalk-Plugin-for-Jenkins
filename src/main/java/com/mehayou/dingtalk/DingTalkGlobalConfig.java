package com.mehayou.dingtalk;

import hudson.Extension;
import hudson.views.GlobalDefaultViewConfiguration;
import jenkins.model.GlobalConfiguration;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

@Getter
@Setter
@ToString
@Extension
public class DingTalkGlobalConfig extends GlobalDefaultViewConfiguration {

    private String rootUrl;

    private String appKey;
    private String appSecret;
    private String agentId;

    public String getRootUrl() {
        return rootUrl;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getAgentId() {
        return agentId;
    }

    public DingTalkGlobalConfig() {
        this.load();
    }

    @DataBoundConstructor
    public DingTalkGlobalConfig(String rootUrl,
                                String appKey, String appSecret, String agentId) {
        this.rootUrl = rootUrl;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.agentId = agentId;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        this.save();
        return super.configure(req, json);
    }

    public static DingTalkGlobalConfig getInstance() {
        return GlobalConfiguration.all().get(DingTalkGlobalConfig.class);
    }
}
