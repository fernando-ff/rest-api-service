#!/bin/bash
# Aguarda inicialização da rede
sleep 15

# Permite tráfego na porta do Quarkus no firewall nativo do Ubuntu
sudo iptables -I INPUT 1 -p tcp --dport 8080 -j ACCEPT
sudo netfilter-persistent save
sudo netfilter-persistent reload

# Prepara ambiente para deploy
mkdir -p /home/ubuntu/app
chown ubuntu:ubuntu /home/ubuntu/app