package com.mehayou.dingtalk;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

public class DingTalkNotifier extends Notifier {

    private String detailsUrl;
    private String resourceUrl;

    private boolean onNotifySelf;
    private String notifyUsers;

    private String messageJson;

    private boolean onSuccess;
    private boolean onFailed;

    public String getDetailsUrl() {
        return detailsUrl;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public boolean isOnNotifySelf() {
        return onNotifySelf;
    }

    public String getNotifyUsers() {
        return notifyUsers;
    }

    public boolean isOnSuccess() {
        return this.onSuccess;
    }

    public boolean isOnFailed() {
        return this.onFailed;
    }

    public String getMessageJson() {
        return messageJson;
    }

    @DataBoundConstructor
    public DingTalkNotifier(String detailsActionName, String detailsUrl,
                            String resourceActionName, String resourceUrl,
                            boolean onNotifySelf, String notifyUsers,
                            boolean onSuccess, boolean onFailed,
                            String messageJson) {
        super();
        this.detailsUrl = detailsUrl;
        this.resourceUrl = resourceUrl;
        this.onNotifySelf = onNotifySelf;
        this.notifyUsers = notifyUsers;
        this.onSuccess = onSuccess;
        this.onFailed = onFailed;
        this.messageJson = messageJson;
    }

    public DingTalkService getDingTalkService(AbstractBuild build, TaskListener listener) {
        DingTalkGlobalConfig config = DingTalkGlobalConfig.getInstance();
        return new DingTalkServiceImpl(build, listener,
                config.getRootUrl(),
                config.getAppKey(), config.getAppSecret(), config.getAgentId(),
                getDetailsUrl(), getResourceUrl(),
                isOnNotifySelf(), getNotifyUsers(),
                isOnSuccess(), isOnFailed(),
                getMessageJson());
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }


    @Override
    public DingTalkNotifierDescriptor getDescriptor() {
        return (DingTalkNotifierDescriptor) super.getDescriptor();
    }

    @Extension
    public static class DingTalkNotifierDescriptor extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "DingTalk Notification Plugin Plus";
        }

    }
}
