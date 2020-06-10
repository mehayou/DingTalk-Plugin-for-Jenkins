package com.mehayou.dingtalk;

import hudson.EnvVars;
import hudson.model.*;
import jenkins.model.Jenkins;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Builder {

    private AbstractBuild build;
    private TaskListener listener;
    private String rootUrl;

    public boolean isNull() {
        return this.build == null;
    }

    private AbstractBuild get() {
        return this.build;
    }

    private TaskListener getListener() {
        return this.listener;
    }

    private String getRootUrl() {
        return this.rootUrl != null && !rootUrl.isEmpty() ? this.rootUrl.concat("/") : Jenkins.getInstance().getRootUrl();
    }

    public Builder(AbstractBuild build, TaskListener listener, String rootUrl) {
        this.build = build;
        this.listener = listener;
        this.rootUrl = rootUrl;
    }

    /**
     * @return 转化内容中的变量
     */
    public String transformVariables(String content) {
        if (content != null && !content.isEmpty()
                && content.indexOf("%") != content.lastIndexOf("%")) {
            AbstractBuild build = get();
            if (build != null) {

                //获取内容中的变量
                List<String> list = null;
                Pattern pattern = Pattern.compile("%\\w+%");
                Matcher matcher = pattern.matcher(content);
                while (matcher.find()) {
                    if (list == null) {
                        list = new ArrayList<>();
                    }
                    String group = matcher.group();
                    list.add(group.replace("%", ""));
                }

                //内容中含有变量
                if (list != null && !list.isEmpty()) {

                    //获取全局项目环境变量
                    EnvVars envVars = null;
                    TaskListener listener = getListener();
                    if (listener != null) {
                        try {
                            envVars = build.getEnvironment(listener);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    //获取并添加的构建变量
                    Map<String, String> map = build.getBuildVariables();
                    if (envVars == null && map != null && !map.isEmpty()) {
                        envVars = new EnvVars();
                    }
                    if (envVars != null) {
                        envVars.putAll(map);
                    }

                    //匹配替换变量
                    if (envVars != null && !envVars.isEmpty()) {
                        for (String key : list) {
                            String value = envVars.get(key);
                            if (value != null) {
                                content = content.replace("%".concat(key).concat("%"), value);
                            }
                        }
                    }

                }
            }
        }
        return content;
    }

    /**
     * @return 获取当前用户
     */
    public User getUser() {
        User user = null;
        AbstractBuild build = get();
        if (build != null) {
            CauseAction causeAction = build.getAction(CauseAction.class);
            List<Cause> causes = causeAction.getCauses();
            if (causes != null && !causes.isEmpty()) {
                for (Cause cause : causes) {
                    if (cause instanceof Cause.UserIdCause) {
                        Cause.UserIdCause userIdCause = (Cause.UserIdCause) cause;
                        String userId = userIdCause.getUserId();
                        if (userId != null && !userId.isEmpty()) {
                            user = User.get(userId);
                            break;
                        }
                    }
                }
            }
        }
        return user;
    }

    /**
     * @return 获取构建指定页面地址
     */
    public String getUrl(String action) {
        return getRootUrl() + get().getUrl() + (action != null ? action : "");
    }

    /**
     * @return 获取构建参数页面地址，没有会默认跳转构建主页地址
     */
    public String getParametersUrl() {
        return getUrl("parameters");
    }

    /**
     * @return 获取构建控制台页面地址
     */
    public String getConsoleUrl() {
        return getUrl("console");
    }

    /**
     * @return 项目名称
     */
    public String getProjectName() {
        return get().getProject().getDisplayName();
    }

    /**
     * @return 构建序列号，如：996
     */
    public String getId() {
        return get().getId();
    }

    /**
     * @return 构建序列名称，如：#996
     */
    public String getName() {
        return get().getDisplayName();
    }

    /**
     * @return 构建时长，如：1.1 sec
     */
    public String getDurationString() {
        return get().getDurationString();
    }

    /**
     * @return 构建日期
     */
    public String getTimestampString() {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(get().getTimestamp().getTime());
    }
}
