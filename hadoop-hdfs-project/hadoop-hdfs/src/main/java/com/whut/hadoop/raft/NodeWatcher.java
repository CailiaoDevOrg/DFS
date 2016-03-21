package com.whut.hadoop.raft;

import org.jgroups.*;

/**
 * Created by niuyang on 3/18/16.
 */
public abstract class NodeWatcher extends ReceiverAdapter {

    protected JChannel jChannel;

    protected void watcher(JChannel jChannel, String groupId, String nodeId) {
        if (jChannel == null) {
            return;
        }
        try {
            NodeRegister.registerToGroup(jChannel, groupId, nodeId);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        jChannel.addChannelListener(new ChannelListener() {
            @Override
            public void channelConnected(Channel channel) {
                doChannelConnected(channel);
            }
            @Override
            public void channelDisconnected(Channel channel) {
                doChannelDisconnected(channel);
            }
            @Override
            public void channelClosed(Channel channel) {
                doChannelClosed(channel);
            }
        });
    }

	private void doChannelConnected(Channel channel) {
		System.out.println("doChannelConnected");
		if (channel != null) {
			System.out.println("name = " + channel.getName());
		}
	}
	
	protected abstract void doChannelDisconnected(Channel channel);
	
	protected abstract void doChannelClosed(Channel channel);

    @Override
    public void receive(Message msg) {
        System.out.println("rec ===" + msg.getObject());
    }

    @Override
    public void viewAccepted(View view) {
        System.out.println("view ============ " + view);
    }
}
