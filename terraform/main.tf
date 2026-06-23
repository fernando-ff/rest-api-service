# ==============================================================================
# ORACLE CLOUD INFRASTRUCTURE (OCI) TERRAFORM CONFIGURATION
# Purpose: Provision an Always Free Ampere ARM Instance for a Quarkus Application
# Region: Brazil Southeast (Vinhedo) [sa-vinhedo-1]
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. INPUT VARIABLES
# ------------------------------------------------------------------------------
variable "compartment_ocid" {
  type        = string
  description = "Your OCI Compartment OCID (Found in Identity -> Compartments)"
}

variable "ssh_public_key" {
  type        = string
  description = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIDy59cGcMIHU2s59ddvOMWV9m/BNkLWZIoU9492hMs5e fferreiralf.1@gmail.com"
}

# ------------------------------------------------------------------------------
# 2. VIRTUAL CLOUD NETWORK (VCN)
# ------------------------------------------------------------------------------
resource "oci_core_vcn" "quarkus_vcn" {
  cidr_block     = "10.0.0.0/16"
  compartment_id = var.compartment_ocid
  display_name   = "quarkus_vcn"
  dns_label      = "quarkusvcn"
}

# ------------------------------------------------------------------------------
# 3. INTERNET GATEWAY (Enables Public Internet Access)
# ------------------------------------------------------------------------------
resource "oci_core_internet_gateway" "quarkus_ig" {
  compartment_id = var.compartment_ocid
  display_name   = "quarkus_internet_gateway"
  vcn_id         = oci_core_vcn.quarkus_vcn.id
}

# ------------------------------------------------------------------------------
# 4. ROUTE TABLE (Directs Outbound Traffic to the Internet Gateway)
# ------------------------------------------------------------------------------
resource "oci_core_default_route_table" "quarkus_rt" {
  manage_default_resource_id = oci_core_vcn.quarkus_vcn.default_route_table_id

  route_rules {
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
    network_entity_id = oci_core_internet_gateway.quarkus_ig.id
  }
}

# ------------------------------------------------------------------------------
# 5. SECURITY LIST / FIREWALL (Custom Dedicated Rules)
# ------------------------------------------------------------------------------
resource "oci_core_security_list" "quarkus_custom_sl" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.quarkus_vcn.id
  display_name   = "quarkus_custom_security_list"

  # Egress: Permite todo o tráfego de saída do servidor para a internet
  egress_security_rules {
    destination = "0.0.0.0/0"
    protocol    = "all" # Todos os protocolos
    stateless   = false
  }

  # Ingress: Permite SSH (Porta 22) de qualquer origem
  ingress_security_rules {
    protocol    = "6" # TCP
    source      = "0.0.0.0/0"
    stateless   = false
    description = "Allow SSH Management"
    
    tcp_options {
      min = 22
      max = 22
    }
  }

  # Ingress: Permite tráfego Web para o Quarkus (Porta 8080) de qualquer origem
  ingress_security_rules {
    protocol    = "6" # TCP
    source      = "0.0.0.0/0"
    stateless   = false
    description = "Allow Quarkus Application Port"

    tcp_options {
      min = 8080
      max = 8080
    }
  }
}

# ------------------------------------------------------------------------------
# 6. PUBLIC SUBNET (Hosting Space for Public-Facing Resources)
# ------------------------------------------------------------------------------
resource "oci_core_subnet" "quarkus_subnet" {
  cidr_block        = "10.0.1.0/24"
  compartment_id    = var.compartment_ocid
  vcn_id            = oci_core_vcn.quarkus_vcn.id
  display_name      = "quarkus_subnet"
  dns_label         = "quarkussubnet"
  
  # ATENÇÃO: Mudança feita aqui para apontar para o novo recurso customizado
  security_list_ids = [oci_core_security_list.quarkus_custom_sl.id]
}

# ------------------------------------------------------------------------------
# 7. ALWAYS FREE AMPERE ARM VIRTUAL MACHINE INSTANCE
# ------------------------------------------------------------------------------
resource "oci_core_instance" "quarkus_server" {
  availability_domain = data.oci_identity_availability_domains.ads.availability_domains[0].name
  compartment_id      = var.compartment_ocid
  shape               = "VM.Standard.E2.1.Micro"
  
  display_name = "quarkus-free-tier-server"

  create_vnic_details {
    subnet_id        = oci_core_subnet.quarkus_subnet.id
    assign_public_ip = true
  }

  source_details {
    source_type = "image"
    source_id   = data.oci_core_images.ubuntu_arm.images[0].id
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
    
    # Versão definitiva: Usa comandos nativos do iptables em vez de editar arquivos texto com SED
    user_data = base64encode(<<-EOF
      #!/bin/bash
      # 1. Aguarda o sistema operacional carregar o serviço de rede
      sleep 15
      
      # 2. Força a inserção da regra na linha 1 do firewall em memória
      sudo iptables -I INPUT 1 -p tcp --dport 8080 -j ACCEPT
      
      # 3. Salva a memória atual direto no arquivo definitivo do Ubuntu
      sudo netfilter-persistent save
      sudo netfilter-persistent reload

      # 4. Prepara o diretório para o deploy do GitHub Actions
      mkdir -p /home/ubuntu/app
      chown ubuntu:ubuntu /home/ubuntu/app
    EOF
    )
  }
}

# ------------------------------------------------------------------------------
# 8. DATA SOURCES / HELPERS
# ------------------------------------------------------------------------------
data "oci_identity_availability_domains" "ads" {
  compartment_id = var.compartment_ocid
}

# ------------------------------------------------------------------------------
# 9. AUTOMATED OUTPUTS
# ------------------------------------------------------------------------------
output "instance_public_ip" {
  value       = oci_core_instance.quarkus_server.public_ip
  description = "The public IP address of your Always Free Quarkus Server."
}

data "oci_core_images" "ubuntu_arm" {
  compartment_id           = var.compartment_ocid
  operating_system         = "Canonical Ubuntu"
  operating_system_version = "22.04"
  shape                    = "VM.Standard.E2.1.Micro" # Changed filter  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}
