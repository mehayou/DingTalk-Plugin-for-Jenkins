package com.mehayou.dingtalk;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.tasks.Publisher;

import javax.annotation.Nonnull;
import java.util.Map;

@Extension
public class JobListener extends RunListener<AbstractBuild> {

    public JobListener() {
        super(AbstractBuild.class);
    }

    @Override
    public void onStarted(AbstractBuild build, TaskListener listener) {
        //DingTalkService service = getService(build, listener);
        //if (service != null) {
        //    service.start();
        //}
    }

    @Override
    public void onCompleted(AbstractBuild build, @Nonnull TaskListener listener) {
        Result result = build.getResult();
        DingTalkService service = getService(build, listener);
        if (service != null && result != null) {
            if (result.equals(Result.SUCCESS)) {
                service.success();
            } else if (result.equals(Result.FAILURE)) {
                service.failed();
            } else if (result.equals(Result.ABORTED)) {
                //service.abort();
            } else {
                service.other();
            }
        }
    }

    private DingTalkService getService(AbstractBuild build, TaskListener listener) {
        Map<Descriptor<Publisher>, Publisher> map = build.getProject().getPublishersList().toMap();
        for (Publisher publisher : map.values()) {
            if (publisher instanceof DingTalkNotifier) {
                return ((DingTalkNotifier) publisher).getDingTalkService(build, listener);
            }
        }
        return null;
    }
}
