package com.ePingy.model;

import com.ePingy.ChatInterface;
import com.ePingy.service.EPingyService;

/**
 * Created by jhansi on 07/03/15.
 */
public class Session {

    private int id;
    private String deviceName;
    private ChatInterface chatInterface;
    private EPingyService.UseChannelState useChannelState;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public ChatInterface getChatInterface() {
        return chatInterface;
    }

    public void setChatInterface(ChatInterface chatInterface) {
        this.chatInterface = chatInterface;
    }

    public EPingyService.UseChannelState getUseChannelState() {
        return useChannelState;
    }

    public void setUseChannelState(EPingyService.UseChannelState useChannelState) {
        this.useChannelState = useChannelState;
    }
}
