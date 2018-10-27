#!/bin/sh

PATH=/sbin:/usr/sbin:$PATH

test -f /var/run/ppp0.pid || \
( pptpsetup --create ITPAVPN --server vpn.cs.fiu.edu --username itpa01 --password itpaHPDRC7 --encrypt --start && \
ip route add 131.94.130.156/32 via 10.128.4.1 )

# -- username itpa01 @ 10.128.4.250 or itpa02 @ 10.128.4.249 (as off 06/18/18 ) 
