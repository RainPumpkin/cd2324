#!/bin/bash
sudo service glusterd start 
sleep 30 
sudo mount -t glusterfs tf-node2:/glustervol /var/sharedfiles 
/usr/local/sbin/spread -c /usr/local/etc/vmsSpread.conf > /tmp/spreadlogs 2>&1 &