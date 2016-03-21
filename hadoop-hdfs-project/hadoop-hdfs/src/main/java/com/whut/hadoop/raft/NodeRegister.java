package com.whut.hadoop.raft;

import org.apache.commons.lang.StringUtils;
import org.jgroups.JChannel;

public class NodeRegister {

    protected static void registerToGroup(JChannel jChannel, String groupId, String nodeId) throws Exception {
        if (jChannel != null && StringUtils.isNotBlank(groupId) && StringUtils.isNotBlank(nodeId)) {
        	jChannel.setName(nodeId);
            jChannel.connect(groupId);
        }
    }
}
