package com.mehayou.dingtalk;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiGettokenRequest;
import com.dingtalk.api.request.OapiMessageCorpconversationAsyncsendV2Request;
import com.dingtalk.api.request.OapiUserGetByMobileRequest;
import com.dingtalk.api.request.OapiUserSimplelistRequest;
import com.dingtalk.api.response.OapiGettokenResponse;
import com.dingtalk.api.response.OapiMessageCorpconversationAsyncsendV2Response;
import com.dingtalk.api.response.OapiUserGetByMobileResponse;
import com.dingtalk.api.response.OapiUserSimplelistResponse;
import com.taobao.api.ApiException;
import com.taobao.api.internal.util.json.JSONWriter;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.User;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class DingTalkServiceImpl implements DingTalkService {

    private Logger logger = Logger.getLogger(DingTalkServiceImpl.class.getName());

    private void logger(String message) {
        if (logger != null) {
            logger.info("DingTalk-Plugin: " + message);
        }
    }

    private static final int RESULT_SUCCESS = 1;
    private static final int RESULT_FAILED = -1;

    //特定下载链接设定
    private static final String PROPERTIES_SUFFIX = ".properties";
    private static final String PROPERTIES_KEY_DOWNLOAD_URL = "APK_URL";

    private Builder builder;

    private Builder getBuilder() {
        return builder;
    }

    private String appKey;
    private String appSecret;
    private String agentId;

    private String detailsUrl;
    private String resourceUrl;

    private boolean OnNotifySelf;
    private String notifyUsers;

    private boolean onSuccess;
    private boolean onFailed;

    private String messageJson;

    public String getAppKey() {
        return appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getDetailsUrl() {
        return detailsUrl;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public String getNotifyUsers() {
        return notifyUsers;
    }

    public String getMessageJson() {
        return messageJson;
    }

    public DingTalkServiceImpl(AbstractBuild build, TaskListener listener,
                               String rootUrl,
                               String appKey, String appSecret, String agentId,
                               String detailsUrl, String resourceUrl,
                               boolean OnNotifySelf, String notifyUsers,
                               boolean onSuccess, boolean onFailed,
                               String messageJson) {
        this.builder = new Builder(build, listener, rootUrl);

        this.appKey = getBuilder().transformVariables(appKey);
        this.appSecret = getBuilder().transformVariables(appSecret);
        this.agentId = getBuilder().transformVariables(agentId);

        this.detailsUrl = getBuilder().transformVariables(detailsUrl);
        this.resourceUrl = getBuilder().transformVariables(resourceUrl);
        this.OnNotifySelf = OnNotifySelf;
        this.notifyUsers = getBuilder().transformVariables(notifyUsers);
        this.onSuccess = onSuccess;
        this.onFailed = onFailed;
        this.messageJson = getBuilder().transformVariables(messageJson);
    }

    @Override
    public void success() {
        if (this.onSuccess) {
            logger("-------- SUCCESS --------");
            sendMessage(RESULT_SUCCESS);
        }
    }

    @Override
    public void failed() {
        if (this.onFailed) {
            logger("-------- FAILED --------");
            sendMessage(RESULT_FAILED);
        }
    }

    @Override
    public void other() {
        logger("-------- UNKNOWN --------");
        sendMessage(-1);
    }

    /**
     * @return 从特定文件指定KEY中获取URL链接
     */
    private String getUrlForProperties(String path) {
        Properties properties = null;
        if (path != null && !path.isEmpty()
                && path.toLowerCase().endsWith(PROPERTIES_SUFFIX)) {
            FileReader fr = null;
            try {
                fr = new FileReader(path);
                properties = new Properties();
                properties.load(fr);
                fr.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fr != null) {
                    try {
                        fr.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return properties != null ? properties.getProperty(PROPERTIES_KEY_DOWNLOAD_URL) : path;
    }

    /**
     * @return 截取URL中文件名称
     */
    private String getUrlForFileName(String url) {
        if (url != null && !url.isEmpty()) {
            int lastIndex;
            if (url.contains("\\") && !url.contains("/")) {
                lastIndex = url.lastIndexOf("\\");
            } else {
                lastIndex = url.lastIndexOf("/");
            }
            if (lastIndex != -1) {
                return url.substring(lastIndex + 1);
            }
        }
        return null;
    }

    private String getStatus(int result) {
        switch (result) {
            case RESULT_SUCCESS:
                return "构建成功";
            case RESULT_FAILED:
                return "构建发生了意外";
            default:
                return "构建发生了异常";
        }
    }

    private String getAtUsers(User user) {
        String userName = null;
        if (user != null) {
            //用户名
            userName = user.getFullName();
        }
        String notifyUsers = getNotifyUsers();

        String atUsers = null;
        if (this.OnNotifySelf && userName != null && !userName.isEmpty()) {
            atUsers = userName;
        }
        if (notifyUsers != null && !notifyUsers.isEmpty()) {
            if (atUsers != null) {
                atUsers = atUsers.concat(",").concat(notifyUsers);
            } else {
                atUsers = notifyUsers;
            }
        }
        return atUsers;
    }

    private String getAtMobiles(User user) {
        String userMobile = null;
        if (user != null) {
            //手机号
            DingTalkUserProperty property = user.getProperty(DingTalkUserProperty.class);
            if (property != null) {
                userMobile = property.getMobile();
            }
        }

        String notifyUsers = getNotifyUsers();

        String atUsers = null;
        if (this.OnNotifySelf && userMobile != null && !userMobile.isEmpty()) {
            atUsers = userMobile;
        }
        if (notifyUsers != null && !notifyUsers.isEmpty()) {
            if (atUsers != null) {
                atUsers = atUsers.concat(",").concat(notifyUsers);
            } else {
                atUsers = notifyUsers;
            }
        }
        return atUsers;
    }

    private void sendMessage(int result) {
        if (getBuilder().isNull()) {
            logger("Builder isNull");
            return;
        }

        //构建用户
        User user = getBuilder().getUser();
        //用户ID
        String userId = null;
        if (user != null) {
            userId = user.getId();
        }
        //at用户
        String atMobiles = getAtMobiles(user);

        //项目名称
        String projectName = getBuilder().getProjectName();
        //构建序列名称
        String buildName = getBuilder().getName();

        logger("ProjectName:" + projectName + "[" + buildName + "]" + ", BindUserId:" + userId + ", atMobiles:" + atMobiles);

        if (atMobiles == null || atMobiles.isEmpty()) {
            //无指定用户，则退出
            return;
        }

        //JSON内容
        String messageJson = getMessageJson();
        if (result == RESULT_SUCCESS && messageJson != null && !messageJson.isEmpty()) {
            logger("Message:Json");
            //发送信息
            sendMessage(messageJson, atMobiles);
        } else {

            String url = getResourceUrl();
            //下载地址
            String downloadUrl = getUrlForProperties(url);
            //下载的文件名
            String fileName = getUrlForFileName(downloadUrl);

            //标题
            String title = String.format("【%s】%s %s", getStatus(result), projectName, buildName);
            //内容
            String markdown = String.format("### 【%s】\n\n", getStatus(result))
                    + String.format("# %s\n\n", projectName)
                    //执行用户
                    + String.format("> **执行用户：** %s\n\n", userId != null && !userId.isEmpty() ? userId : "匿名")
                    //构建信息
                    + String.format("> **构建序列：** [%s](%s)\n\n", buildName, getBuilder().getParametersUrl())
                    + String.format("> **构建时长：** %s\n\n", getBuilder().getDurationString())
                    + String.format("> **构建日期：** %s\n\n", getBuilder().getTimestampString())
                    //成功后，标记输出文件
                    + (result == RESULT_SUCCESS && fileName != null && !fileName.isEmpty() ? String.format("> **附件名称：** %s\n\n", fileName) : "");

            // 按钮
            List<OapiMessageCorpconversationAsyncsendV2Request.BtnJsonList> actionList = new ArrayList<>();
            OapiMessageCorpconversationAsyncsendV2Request.BtnJsonList action1 = new OapiMessageCorpconversationAsyncsendV2Request.BtnJsonList();

            String detailsUrl = getDetailsUrl();
            if (result == RESULT_SUCCESS && detailsUrl != null && !detailsUrl.isEmpty()) {
                action1.setTitle("查看详情");
                action1.setActionUrl(detailsUrl);
            } else {
                action1.setTitle("查看控制台");
                action1.setActionUrl(getBuilder().getConsoleUrl());
            }
            actionList.add(action1);

            if (result == RESULT_SUCCESS && downloadUrl != null && !downloadUrl.isEmpty()) {
                OapiMessageCorpconversationAsyncsendV2Request.BtnJsonList action2 = new OapiMessageCorpconversationAsyncsendV2Request.BtnJsonList();
                action2.setTitle("下载附件");
                action2.setActionUrl(downloadUrl);
                actionList.add(action2);
            }

            OapiMessageCorpconversationAsyncsendV2Request.Msg msg = new OapiMessageCorpconversationAsyncsendV2Request.Msg();
            msg.setMsgtype("action_card");
            msg.setActionCard(new OapiMessageCorpconversationAsyncsendV2Request.ActionCard());
            msg.getActionCard().setBtnOrientation("1");
            msg.getActionCard().setTitle(title);
            msg.getActionCard().setMarkdown(markdown);
            msg.getActionCard().setBtnJsonList(actionList);
            logger("Message:Msg");
            sendMessage(msg, atMobiles);
        }
    }

    private void sendMessage(OapiMessageCorpconversationAsyncsendV2Request.Msg msg, String mobiles) {
        sendMessage((new JSONWriter(false, false, true)).write(msg), mobiles);
    }

    private void sendMessage(String msg, String mobiles) {
        try {
            sendMessage(getAppKey(), getAppSecret(), Long.parseLong(getAgentId()), mobiles, msg);
        } catch (Exception e) {
        }
    }

    private void sendMessage(String appkey, String appsecret, long agentId, String mobiles,
                             String msg) throws ApiException {
        String accessToken = getAccessToken(appkey, appsecret);
        String userList = getUserIdsForMobiles(accessToken, mobiles);
        logger("Send Users: " + userList);
        if (userList == null || userList.isEmpty()) {
            return;
        }

        OapiMessageCorpconversationAsyncsendV2Request request = new OapiMessageCorpconversationAsyncsendV2Request();
        request.setAgentId(agentId);
        request.setUseridList(userList);

        request.setMsg(msg);

        logger("Send Message:Start");
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2");
        OapiMessageCorpconversationAsyncsendV2Response response = client.execute(request, accessToken);
        logger("Send Message:Over, Errcode:" + response.getErrcode() + ", Errmsg:" + response.getErrmsg() + ", TaskId:" + response.getTaskId());
    }

    private String getAccessToken(String appkey, String appsecret) throws ApiException {
        if (appkey == null || appkey.isEmpty()
                || appsecret == null || appsecret.isEmpty()) {
            return null;
        }
        OapiGettokenRequest request = new OapiGettokenRequest();
        request.setAppkey(appkey);
        request.setAppsecret(appsecret);
        request.setHttpMethod("GET");
        DefaultDingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/gettoken");
        OapiGettokenResponse response = client.execute(request);
        return response.getAccessToken();
    }

    private String getUserIdsForMobiles(String accessToken, String mobiles) throws ApiException {
        if (mobiles != null && !mobiles.isEmpty()) {
            String[] split = mobiles.split(",");
            //去重
            split = new LinkedHashSet<>(Arrays.asList(split)).toArray(new String[0]);
            if (split.length > 0) {
                List<String> list = new ArrayList<>();
                for (String mobile : split) {
                    if (mobile != null && !mobile.isEmpty()
                            && mobile.length() == 11 && mobile.matches("^\\d+$")) {
                        String userId = getUserId(accessToken, mobile);
                        logger("Send Message:Mobile=" + mobile + ", userId=" + userId);
                        if (userId != null && !userId.isEmpty()) {
                            list.add(userId);
                        }
                    }
                }
                if (!list.isEmpty()) {
                    return list.toString()
                            .replace("[", "")
                            .replace("]", "")
                            .replace(" ", "");
                }
            }
        }
        return null;
    }

    private String getUserId(String accessToken, String mobile) throws ApiException {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/get_by_mobile");
        OapiUserGetByMobileRequest request = new OapiUserGetByMobileRequest();
        request.setMobile(mobile);
        OapiUserGetByMobileResponse response = client.execute(request, accessToken);
        return response.getUserid();
    }

    private String getUserIds(String accessToken, String names) throws ApiException {
        if (accessToken == null || accessToken.isEmpty()
                || names == null || names.isEmpty()) {
            return null;
        }
        OapiUserSimplelistRequest request = new OapiUserSimplelistRequest();
        request.setDepartmentId(1L);
        request.setHttpMethod("GET");
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/simplelist");
        OapiUserSimplelistResponse response = client.execute(request, accessToken);

        List<OapiUserSimplelistResponse.Userlist> userlist = response.getUserlist();
        if (userlist != null && !userlist.isEmpty()) {
            List<String> list = new ArrayList<>();
            for (OapiUserSimplelistResponse.Userlist user : userlist) {
                String name = user.getName();
                String userid = user.getUserid();
                if (name != null && !name.isEmpty() && userid != null && !userid.isEmpty()
                        && names.contains(name) && !list.contains(userid)) {
                    list.add(userid);
                }
            }
            if (!list.isEmpty()) {
                return list.toString()
                        .replace("[", "")
                        .replace("]", "")
                        .replace(" ", "");
            }
        }
        return null;
    }
}
