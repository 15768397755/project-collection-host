package com.es.webSocket;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 相当于controller的处理器
 */
public class MyHandler extends TextWebSocketHandler implements Cloneable{
    private static int onlineCount = 0;
    //存放连接的客户端
    private static ConcurrentHashMap<String, MyHandler> webSocketSet = new ConcurrentHashMap<>();

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private WebSocketSession webSocketSession;
    private static Logger log =  LogManager.getLogger(MyHandler.class);


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

         System.out.println("=====接受到的数据"+payload);
        session.sendMessage(new TextMessage("服务器返回收到的信息," + payload));


        //可以自己约定字符串内容，比如   “进行轮询态势|0“ 表示信息群发，   “进行轮询态势|x“ 表示信息发给id为X的用户
        String sendMessage = message.getPayload().split("[|]")[0];
        String sendUserId = message.getPayload().split("[|]")[1];
        try {
            if(sendUserId.equals("0"))
                sendtoAll(sendMessage);
            else
                sendtoUser(sendMessage,sendUserId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
    }



    //建立成功事件
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        System.out.println("建立成功事件");
        session.sendMessage(new TextMessage("服务器返回:建立成功事件" ));

        this.webSocketSession = session;
        //深拷贝对象
        MyHandler myHandler = this.clone();

        webSocketSet.put(session.getId(), myHandler);     //接收到发送消息的人员编号 加入set中
        addOnlineCount();           //在线数加1
        log.info("用户"+session.getId()+"加入！当前在线人数为" + getOnlineCount());
        try {
            sendMessage("连接成功");
        } catch (IOException e) {
            log.error("websocket IO异常");
        }


    }

    //移除事件
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("移除事件");
        session.sendMessage(new TextMessage("服务器返回:移除事件" ));

        webSocketSet.remove(this);  //从set中删除
        subOnlineCount();           //在线数减1
        log.info("有一连接关闭！当前在线人数为" + getOnlineCount());
    }
    /**
     * 发送信息给指定ID用户，如果用户不在线则返回不在线信息给自己
     * @param message
     * @param sendUserId
     * @throws
     */
    public void sendtoUser(String message,String sendUserId) throws IOException {
        if (webSocketSet.get(sendUserId) != null) {
            for(int a = 0; a < 10; a++){
                if(!webSocketSession.getId().equals(sendUserId)){
                    webSocketSet.get(sendUserId).sendMessage( "用户" + webSocketSession.getId() + "发来消息：" + " <br/> " + message);
                }else{
                    webSocketSet.get(sendUserId).sendMessage(message + a);
                }
                try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
            }
        } else {
            //如果用户不在线则返回不在线信息给自己
            sendtoUser("当前用户不在线",webSocketSession.getId());
        }
    }



    /**
     * 发送信息给所有人
     * @param message
     * @throws IOException
     */
    public void sendtoAll(String message) throws IOException {
        for (String key : webSocketSet.keySet()) {
            try {
                webSocketSet.get(key).sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void sendMessage(String message) throws IOException {
        this.webSocketSession.sendMessage(new TextMessage(message));
    }
    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        MyHandler.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        MyHandler.onlineCount--;
    }


    //重载clone() 支持深拷贝
    @Override
    public MyHandler clone() throws CloneNotSupportedException {
        return (MyHandler) super.clone();
    }
}