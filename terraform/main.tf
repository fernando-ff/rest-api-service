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
# 5. SECURITY LIST / FIREWALL (Stateful Ingress & Egress Rules)
# ------------------------------------------------------------------------------
resource "oci_core_default_security_list" "quarkus_sl" {
  manage_default_resource_id = oci_core_vcn.quarkus_vcn.default_security_list_id

  # Egress: Allow all outbound traffic
  egress_security_rules {
    destination = "0.0.0.0/0"
    protocol    = "all"
  }

  # Ingress: Allow SSH Management (Port 22)
  ingress_security_rules {
    protocol = "6" # TCP
    source   = "0.0.0.0/0"
    tcp_options {
      min = 22
      max = 22
    }
  }

  # Ingress: Allow Quarkus Web Application Framework (Port 8080)
  ingress_security_rules {
    protocol = "6" # TCP
    source   = "0.0.0.0/0"
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
  security_list_ids = [oci_core_vcn.quarkus_vcn.default_security_list_id]
}

# ------------------------------------------------------------------------------
# 7. ALWAYS FREE AMPERE ARM VIRTUAL MACHINE INSTANCE (4 OCPUs, 24 GB RAM)
# ------------------------------------------------------------------------------
resource "oci_core_instance" "quarkus_server" {
  availability_domain = data.oci_identity_availability_domains.ads.availability_domains[0].name
  compartment_id      = var.compartment_ocid
  shape               = "VM.Standard.E2.1.Micro" # Changed from A1.Flex
  
  display_name = "quarkus-free-tier-server"

  create_vnic_details {
    subnet_id        = oci_core_subnet.quarkus_subnet.id
    assign_public_ip = true
  }

  source_details {
    source_type = "image"
    # Replaces the hardcoded sa-vinhedo-1 string with the latest image ID found
    source_id   = data.oci_core_images.ubuntu_arm.images[0].id
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
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

